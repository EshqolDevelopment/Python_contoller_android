package com.eshqol.pythoncontoller123

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.SurfaceHolder
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.eshqol.pythoncontoller123.databinding.ActivityScanQrcodeBinding
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Detector.Detections
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.IOException


class ScanQRCode : AppCompatActivity() {
    private val requestCodeCameraPermission = 1001
    private lateinit var cameraSource: CameraSource
    private lateinit var barcodeDetector: BarcodeDetector
    private var scannedValue = ""
    private lateinit var binding: ActivityScanQrcodeBinding
    private var scannedValuesSet = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanQrcodeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        title = getString(R.string.connect_to_your_device)

        if (ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            askForCameraPermission()
        } else {
            setupControls()
        }

        val aniSlide: Animation =
            AnimationUtils.loadAnimation(this, R.anim.scanner_animation)
        binding.barcodeLine.startAnimation(aniSlide)

        val editTextCode = findViewById<EditText>(R.id.editTextTextPassword)
        val connectBtn = findViewById<Button>(R.id.button2)

        editTextCode.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                connectBtn.performClick()
                true
            } else false
        }

        connectBtn.setOnClickListener {
            Helper().closeKeyboard(this)
            if (editTextCode.text.toString().isNotBlank()) {
                goToMainActivity(editTextCode.text.toString())
            } else "Please enter a code".toast(this)
        }

    }

    private fun setupControls() {
        barcodeDetector =
            BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.ALL_FORMATS).build()

        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(1920, 1080)
            .setAutoFocusEnabled(true) //you should add this feature
            .build()

        binding.cameraSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder) {
                try {
                    //Start preview after 1s delay
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            @SuppressLint("MissingPermission")
            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                try {
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraSource.stop()
            }
        })


        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
                Toast.makeText(applicationContext, "Scanner has been closed", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun receiveDetections(detections: Detections<Barcode>) {
                val barcodes = detections.detectedItems
                if (barcodes.size() == 1) {
                    scannedValue = barcodes.valueAt(0).rawValue

                    runOnUiThread {
                        if (scannedValue !in scannedValuesSet) {
                            scannedValuesSet.add(scannedValue)
                            goToMainActivity()
                        }
                    }
                } else {
                    "value- else".toast(this@ScanQRCode)
                }
            }
        })
    }

    fun goToMainActivity(code: String = scannedValue) {
        Helper().read("Root/${sha256(code)}").addOnSuccessListener {
            if (it.exists()) {
                cameraSource.stop()

                Helper().writeLocalStorage(this, "code", code)

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("code", code)
                startActivity(intent)
                overridePendingTransition(R.anim.activity_show, R.anim.activity_hide)
                finish()

            } else "Device not found".toast(this)

        }
    }

    private fun goBack() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun askForCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            requestCodeCameraPermission
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeCameraPermission && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) setupControls()
            else "Permission Denied".toast(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cameraSource.stop()
        } catch (_: Exception) {
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.scan, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        goBack()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.toString()) {
            "Go back" -> {
                goBack()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}