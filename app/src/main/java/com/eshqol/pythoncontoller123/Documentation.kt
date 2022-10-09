package com.eshqol.pythoncontoller123

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import com.google.android.material.bottomnavigation.BottomNavigationView

class Documentation : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documentation)

        title = getString(R.string.doc_title)

        findViewById<WebView>(R.id.webview).apply {
            settings.javaScriptEnabled = true
            loadUrl("https://www.python.eshqol.com/documentation")
        }

        startNavigation(this, R.id.documentation)
    }
}