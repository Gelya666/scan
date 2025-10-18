package com.example.scanner.ui.activities

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.scanner.R
import java.io.File

class ScreenFileActivity : AppCompatActivity()
{
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.screen_file)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.screen)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        webView = findViewById(R.id.webView)
        val pdfUri = intent.getParcelableExtra<Uri>("pdfUri")
        if (pdfUri != null) {
            loadPdfInWebView(pdfUri)
        } else {
            Toast.makeText(this, "Файл не выбран", Toast.LENGTH_SHORT).show()
            finish()
        }
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
            }
        }
    }
    @SuppressLint("SetJavaScriptEnabled")
    private fun loadPdfInWebView(uri: Uri) {
        webView.settings.apply {
            javaScriptEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            loadWithOverviewMode = true
            useWideViewPort = true
        }
    }
    private fun loadWithGoogleDocs(uri: Uri) {
        progressBar.visibility = View.VISIBLE
        val filePath = getFilePathFromUri(uri)
        if (filePath != null) {
            val file = File(filePath)
            val googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=${Uri.fromFile(file)}"
            webView.loadUrl(googleDocsUrl)
        } else {
            val googleDocsUrl = "https://docs.google.com/gview?embedded=true&url=${uri}"
            webView.loadUrl(googleDocsUrl)
        }

    }
    private fun getFilePathFromUri(uri: Uri): String? {
        return when (uri.scheme) {
            "file" -> uri.path
            "content" -> getContentFilePath(uri)
            else -> null
        }
    }
    private fun getContentFilePath(uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                return cursor.getString(columnIndex)
            }
        }
        return null
    }
}


