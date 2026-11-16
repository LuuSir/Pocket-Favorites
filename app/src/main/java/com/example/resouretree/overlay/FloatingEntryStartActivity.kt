package com.example.resouretree.overlay

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import com.example.resouretree.localization.LocalizedActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** User-visible QS launch window: start the overlay only after this activity is resumed. */
class FloatingEntryStartActivity : LocalizedActivity() {
    private var starting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)
        setContentView(TextView(this).apply {
            text = AppText.get(R.string.overlay_starting)
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
            fail(AppText.get(R.string.overlay_permission_missing))
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
                else fail(ready?.second ?: AppText.get(R.string.overlay_timeout))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                fail(AppText.get(R.string.overlay_start_failed))
            }
        }
    }

    private fun fail(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finishAndRemoveTask()
    }
}
