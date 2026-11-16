package com.example.resouretree

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.*
import android.view.ViewGroup
import com.example.resouretree.localization.LocalizedActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.resouretree.domain.model.webUrl
import com.example.resouretree.ui.theme.ResoureTreeTheme
import com.example.resouretree.ui.web.WebNavigation

/** Private activity: URLs come from item content, never an external exported intent. */
class WebPageActivity : LocalizedActivity() {
    private var web: WebView? = null
    private var navigation: WebNavigation? = null

    @SuppressLint("SetJavaScriptEnabled")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialUrl = webUrl(intent.getStringExtra("url").orEmpty())
        if (initialUrl == null) { finish(); return }
        enableEdgeToEdge()
        setContent {
            var address by remember { mutableStateOf(initialUrl) }
            var progress by remember { mutableIntStateOf(0) }
            var error by remember { mutableStateOf<String?>(null) }
            val back = { if (web?.canGoBack() == true) web?.goBack() else finish() }
            BackHandler { back() }
            ResoureTreeTheme {
                Scaffold(topBar = {
                    TopAppBar(title = { Text(AppText.get(R.string.web_page), maxLines = 1) },
                        navigationIcon = { TextButton(onClick = { back() }) { Text(AppText.get(R.string.back)) } },
                        actions = {
                            TextButton(onClick = { error = null; navigation?.reset(); web?.reload() }) { Text(AppText.get(R.string.refresh)) }
                            TextButton(onClick = { finish() }) { Text(AppText.get(R.string.close)) }
                        })
                }) { padding ->
                    Column(Modifier.padding(padding).fillMaxSize()) {
                        Text(address, Modifier.padding(horizontal = 16.dp, vertical = 4.dp), maxLines = 2,
                            overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                        if (progress < 100) LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                        error?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error) }
                        AndroidView(modifier = Modifier.fillMaxWidth().weight(1f), factory = { context ->
                            WebView(context).also { view ->
                                web = view
                                val links = WebNavigation(
                                    launch = { startActivity(it) },
                                    loadFallback = { view.loadUrl(it) },
                                    showError = { error = it }
                                ).also { navigation = it }
                                view.settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    allowFileAccess = false
                                    allowContentAccess = false
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                    setSupportMultipleWindows(false)
                                }
                                view.webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView, newProgress: Int) { progress = newProgress }
                                }
                                view.webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        return links.navigate(request.url.toString(), request.isForMainFrame, request.hasGesture())
                                    }
                                    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                                        address = url; error = null
                                    }
                                    override fun onPageFinished(view: WebView, url: String) { address = url }
                                    override fun onReceivedError(view: WebView, request: WebResourceRequest, detail: WebResourceError) {
                                        if (request.isForMainFrame) error = if (detail.description.toString().contains("ERR_CLEARTEXT_NOT_PERMITTED"))
                                            AppText.get(R.string.http_blocked) else AppText.get(R.string.page_failed)
                                    }
                                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                                        if (request.isForMainFrame) error = AppText.get(R.string.http_error, response.statusCode)
                                    }
                                    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, sslError: android.net.http.SslError) {
                                        handler.cancel(); error = AppText.get(R.string.certificate_invalid)
                                    }
                                }
                                if (savedInstanceState?.getBundle("web")?.let(view::restoreState) == null) view.loadUrl(initialUrl)
                            }
                        })
                    }
                }
            }
        }
    }

    override fun onResume() { super.onResume(); web?.onResume() }
    override fun onPause() { web?.onPause(); super.onPause() }
    override fun onSaveInstanceState(outState: Bundle) {
        web?.let { view -> outState.putBundle("web", Bundle().also { view.saveState(it) }) }
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        web?.let { view -> (view.parent as? ViewGroup)?.removeView(view); view.stopLoading(); view.destroy() }
        web = null
        super.onDestroy()
    }
}
