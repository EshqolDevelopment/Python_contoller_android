package com.eshqol.pythoncontoller123


import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.method.ScrollingMovementMethod
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.security.MessageDigest
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.concurrent.thread


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private var code = ""
    private val salt = "sigma"
    private val helper = Helper()
    private var tic = System.currentTimeMillis()
    private var allowOutput = false


    private fun send(message: String) {
        val data = encrypt(message, code)

        helper.writeFirebase("Root/${sha256(code)}", data).addOnSuccessListener {
            println("Success")
        }.addOnFailureListener {
            println("Failure")
        }
    }

    private fun getHash(key: String, tic: Long): String {
        return MessageDigest.getInstance("SHA-256").digest((key + tic / 100_000).toByteArray())
            .toHex().substring(0, 16)
    }

    private fun encrypt(text: String, key1: String): String {
        val data = text + salt
        val key = getHash(key1, System.currentTimeMillis())
        val iv = IvParameterSpec("BBBBBBBBBBBBBBBB".toByteArray())
        val skeySpec = SecretKeySpec(key.toByteArray(), "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv)
        val encrypted = cipher.doFinal(data.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    private fun decrypt(text: String, key1: String, time: Long): String? {
        return try {
            val key = getHash(key1, time)
            val iv = IvParameterSpec("BBBBBBBBBBBBBBBB".toByteArray())
            val skeySpec = SecretKeySpec(key.toByteArray(), "AES")
            val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv)
            val decrypted = cipher.doFinal(Base64.decode(text, Base64.DEFAULT))
            String(decrypted).substringBefore(salt)
        } catch (e: Exception) {
            if (e is BadPaddingException) null else e.toString()
        }
    }

    private fun decryptWithTimeOffset(text: String, key1: String): String {
        return decrypt(text, key1, System.currentTimeMillis()) ?:
        decrypt(text, key1, System.currentTimeMillis() - 100_000) ?:
        decrypt(text, key1, System.currentTimeMillis() + 100_1000) ?: ""
    }

    private fun changeConnectState(isConnected: Boolean) {
        runOnUiThread {
            val stateText = findViewById<TextView>(R.id.textView3)
            val sendButton = findViewById<Button>(R.id.button)

            if (isConnected){
                stateText.text = getString(R.string.connected)
                stateText.setTextColor(getColor(R.color.Green))
                sendButton.isEnabled = true
                sendButton.setBackgroundColor(resources.getColor(R.color.Green1))
            }
            else{
                stateText.text = getString(R.string.disconnected)
                stateText.setTextColor(getColor(R.color.Red))
                sendButton.isEnabled = false
                sendButton.setBackgroundColor(resources.getColor(R.color.Gray_Dolphin))
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = getString(R.string.app_name1)
        code = intent.getStringExtra("code") ?: helper.readLocalStorage(this, "code", "")

        val sendButton = findViewById<Button>(R.id.button)
        val editText = findViewById<EditText>(R.id.functionText)
        val outputText = findViewById<TextView>(R.id.textView)

        changeConnectState(false)

        editText.setText(helper.readLocalStorage(this, "text", ""))

        helper.refreshColorOnStart(editText)
        helper.refreshColors(editText)

        editText.doAfterTextChanged {
            helper.writeLocalStorage(this, "text", editText.text.toString())
        }

        sendButton.setOnClickListener {
            allowOutput = true
            send(editText.text.toString())
            helper.closeKeyboard(this)
        }

        helper.listenForChanges("Root/${sha256(code)}-output") { dataSnapshot ->
            if (!allowOutput) return@listenForChanges

            dataSnapshot.value?.let { _ ->
                val data = decryptWithTimeOffset(dataSnapshot.value.toString(), code)
                tic = System.currentTimeMillis()

                when {
                    "@@@" in data -> {
                        val parts = data.split("@@@")
                        val lst = parts.filter { "autotasks123" in it }
                        val temp = lst.map { it.split("autotasks123") }
                        for (b64 in temp) {
                            // ask for permission to read external storage

                            if (ContextCompat.checkSelfPermission(
                                    this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                ActivityCompat.requestPermissions(
                                    this,
                                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                    1002
                                )
                            }

                            val (fileData, name) = b64
                            val file = Base64.decode(fileData, Base64.DEFAULT)
                            val downloadPath =
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val path = downloadPath.absolutePath + "/$name"
                            try {
                                File(path).writeBytes(file)
                            } catch (e: Exception) {
                                outputText.text = "Error: $e"
                            }

                            "The file $name has been saved in your Download's directory".snack(
                                this,
                                duration = 5_000,
                                buttonText = "Open",
                                func = { openFile(path) })
                        }
                    }

                    !data.startsWith("--checking if running") -> {
                        outputText.text = data
                    }
                }
            }
        }
        outputText.movementMethod = ScrollingMovementMethod()

        helper.listenForChanges("Root/${sha256(code)}-online") {
            if (it.value.toString() != "null") {
                tic = (it.value.toString().toDouble() * 1000.0).toLong()
                if (System.currentTimeMillis() - tic < 6000) changeConnectState(true)
            }
        }

        thread {
            while (true) {
                if (System.currentTimeMillis() - tic > 6000) changeConnectState(false)
                Thread.sleep(100)
            }
        }

       startNavigation(this, R.id.home)
    }

    private fun openFile(path: String) {
        try {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW

            val type = when (path.split(".").last()) {
                "pdf" -> "application/pdf"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "xls" -> "application/vnd.ms-excel"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "ppt" -> "application/vnd.ms-powerpoint"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "zip" -> "application/zip"
                "rar" -> "application/x-rar-compressed"
                "png" -> "image/*"
                "jpg" -> "image/*"
                "jpeg" -> "image/*"
                "gif" -> "image/*"
                "mp3" -> "audio/*"
                "wav" -> "audio/*"
                "mp4" -> "video/*"
                "3gp" -> "video/*"
                "mkv" -> "video/*"
                "avi" -> "video/*"
                "mov" -> "video/*"
                "wmv" -> "video/*"
                "flv" -> "video/*"
                "m4v" -> "video/*"
                "mpg" -> "video/*"
                "mpeg" -> "video/*"
                "webm" -> "video/*"
                else -> "*/*"
            }
            intent.setDataAndType(Uri.parse(path), type)

            startActivity(intent)
        } catch (_: Exception) {
            "Error: Could not open file".toast(this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.home_screen, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when {
            item.toString() == "Scan QR Code" -> {
                val intent = Intent(this, ScanQRCode::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.activity_show, R.anim.activity_hide)
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}