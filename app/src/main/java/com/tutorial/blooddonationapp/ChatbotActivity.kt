package com.tutorial.blooddonationapp

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient // 🌟 Import WebViewClient for smoother loading
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ChatbotActivity : AppCompatActivity() {
    private lateinit var webView: WebView

    private val BOTPRESS_URL = "https://www.stack-ai.com/chat/6917581c1f7a0840cf222cac-7j3g343mv9j3hwQH2RoBuX"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        webView = findViewById(R.id.webViewChat)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true

        webView.webViewClient = WebViewClient()

        webView.loadUrl(BOTPRESS_URL)


    }
}