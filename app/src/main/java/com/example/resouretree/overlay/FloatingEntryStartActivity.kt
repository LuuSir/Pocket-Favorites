package com.example.resouretree.overlay

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** User-visible QS launch window: start the overlay only after this activity is resumed. */
class FloatingEntryStartActivity : ComponentActivity() {
    private var starting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)
        setContentView(TextView(this).apply {
            text = "正在开启悬浮窗…"
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
        })
    }

    override fun onPostResume() {
        super.onPostResume()
        // Run after the resume callback so Android sees a foreground activity, including a cold start.
        window.decorView.post {
            if (!starting && !isFinishing && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                starting = true
                startOverlay()
            }
        }
    }

    private fun startOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            fail("尚未允许悬浮窗权限，请在 ResourceTree 的悬浮按钮设置中授权")
            return
        }
        if (FloatingEntryService.running.value) {
            finishAndRemoveTask()
            return
        }
        lifecycleScope.launch {
            try {
                FloatingEntryService.error.value = null
                ContextCompat.startForegroundService(this@FloatingEntryStartActivity,
                    Intent(this@FloatingEntryStartActivity, FloatingEntryService::class.java))
                val ready = withTimeoutOrNull(8_000) {
                    combine(FloatingEntryService.running, FloatingEntryService.error) { running, error -> running to error }
                        .first { (running, error) -> running || error != null }
                }
                if (ready?.first == true) finishAndRemoveTask()
                else fail(ready?.second ?: "悬浮窗启动超时，请检查系统的后台运行限制后重试")
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                fail("无法开启悬浮窗，请检查悬浮窗权限和系统的后台运行限制")
            }
        }
    }

    private fun fail(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finishAndRemoveTask()
    }
}
