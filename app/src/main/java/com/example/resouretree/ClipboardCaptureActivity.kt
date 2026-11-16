package com.example.resouretree

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import com.example.resouretree.localization.LocalizedActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.resouretree.domain.model.ActionType
import com.example.resouretree.domain.model.NodeType
import com.example.resouretree.ui.screens.EditorScreen
import com.example.resouretree.ui.theme.ResoureTreeTheme
import com.example.resouretree.ui.viewmodel.EditorViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Visible, user-initiated clipboard window. Its separate task preserves any open MainActivity editor. */
class ClipboardCaptureActivity : LocalizedActivity() {
    private var seed by mutableStateOf<Bundle?>(null)
    private var attempted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seed = savedInstanceState?.getBundle("captureSeed")
        attempted = seed != null
        setFinishOnTouchOutside(false)
        val app = application as ResourceTreeApplication
        setContent {
            ResoureTreeTheme {
                Surface(Modifier.fillMaxSize(), shape = RoundedCornerShape(20.dp)) {
                    val initial = seed
                    if (initial == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(AppText.get(R.string.reading_clipboard))
                    } else {
                        val editor: EditorViewModel = viewModel(factory = viewModelFactory { initializer {
                            val handle = createSavedStateHandle()
                            // Saved editor changes win over the original clipboard after recreation.
                            if (!handle.contains("contentType")) {
                                handle["name"] = initial.getString("name").orEmpty()
                                handle["contentType"] = "TEXT"
                                handle["contentText"] = initial.getString("text").orEmpty()
                                handle["actionType"] = initial.getString("action").orEmpty()
                                handle["target"] = initial.getString("target").orEmpty()
                            }
                            EditorViewModel(app.repository, handle, null, null, NodeType.ITEM, app.appCatalog, app.documents)
                        } })
                        EditorScreen(editor, editing = false, fromClipboard = true, onBack = ::finishAndRemoveTask)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val metrics = resources.displayMetrics
        window.setLayout((metrics.widthPixels * .94f).toInt(), (metrics.heightPixels * .88f).toInt())
    }

    override fun onResume() {
        super.onResume()
        window.decorView.post { captureWhenFocused() }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) captureWhenFocused()
    }

    private fun captureWhenFocused() {
        if (attempted || isFinishing || !hasWindowFocus() || !lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        attempted = true
        val app = application as ResourceTreeApplication
        val clip = try { getSystemService(ClipboardManager::class.java).primaryClip }
        catch (_: SecurityException) {
            Toast.makeText(this, AppText.get(R.string.clipboard_denied), Toast.LENGTH_SHORT).show()
            finishAndRemoveTask(); return
        }
        val draft = app.clipboardDrafts.read(clip, explicit = true)
        if (draft == null) { finishAndRemoveTask(); return }
        lifecycleScope.launch {
            val target = if (draft.actionType == ActionType.OPEN_WEBVIEW) "" else try { app.appCatalog.resolveTarget(draft.targets) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { draft.targets.first() }
            if (isFinishing) return@launch
            seed = Bundle().apply {
                putString("name", draft.name); putString("text", draft.text)
                putString("action", draft.actionType.name); putString("target", target)
            }
            app.clipboardDrafts.consumed(draft.token)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        seed?.let { outState.putBundle("captureSeed", it) }
        super.onSaveInstanceState(outState)
    }
}
