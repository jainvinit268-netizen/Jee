package com.eduniti.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

private const val WEBSITE_URL = "https://jainvinit268-netizen.github.io/Jee/"
private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent { EdunitiWebApp() }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EdunitiWebApp() {
    var showSplash by remember { mutableStateOf(true) }
    var showExit by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val splashAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        splashAlpha.animateTo(1f, tween(550))
        delay(900)
        splashAlpha.animateTo(0f, tween(350))
        showSplash = false
    }

    BackHandler(enabled = !showSplash) {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else showExit = true
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Black, surface = Black, onBackground = White, onSurface = White
    )) {
        Box(Modifier.fillMaxSize().background(Black)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadsImagesAutomatically = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.setSupportMultipleWindows(false)
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        overScrollMode = WebView.OVER_SCROLL_NEVER
                        setBackgroundColor(android.graphics.Color.BLACK)
                        loadUrl(WEBSITE_URL)
                        webView = this
                    }
                },
                update = { webView = it }
            )

            if (showSplash) {
                Box(
                    Modifier.fillMaxSize().background(Black).alpha(splashAlpha.value),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(220.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("EN", color = White, fontSize = 86.sp, fontWeight = FontWeight.Black)
                        Text(
                            "EDUNITI",
                            color = White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp)
                        )
                    }
                }
            }

            if (showExit) {
                AlertDialog(
                    onDismissRequest = { showExit = false },
                    containerColor = Color(0xFF151515),
                    title = { Text("Exit EDUNITI?", color = White) },
                    text = { Text("Do you want to close the app?", color = White) },
                    confirmButton = {
                        TextButton(onClick = {
                            showExit = false
                            webView?.stopLoading()
                            (webView?.context as? android.app.Activity)?.finish()
                        }) { Text("Exit", color = White) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExit = false }) { Text("Cancel", color = White) }
                    }
                )
            }
        }
    }
}
