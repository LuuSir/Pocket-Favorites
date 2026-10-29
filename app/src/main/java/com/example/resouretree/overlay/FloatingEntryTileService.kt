package com.example.resouretree.overlay

import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** A regular (non-active) tile: refresh on every panel opening and while it is visible. */
class FloatingEntryTileService : TileService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listening: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        listening?.cancel()
        listening = scope.launch { FloatingEntryService.running.collect { updateState() } }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun { toggle() } else toggle()
    }

    // Lint's tile check ignores the SDK guard: the Intent overload is required on API 26–33.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun toggle() {
        try {
            when {
                FloatingEntryService.running.value -> stopService(Intent(this, FloatingEntryService::class.java))
                !Settings.canDrawOverlays(this) -> {
                    // After granting permission, the user taps the tile again to enable it.
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (Build.VERSION.SDK_INT >= 34) {
                        startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                    } else {
                        @Suppress("DEPRECATION")
                        startActivityAndCollapse(intent)
                    }
                }
                else -> {
                    FloatingEntryService.error.value = null
                    ContextCompat.startForegroundService(this, Intent(this, FloatingEntryService::class.java))
                }
            }
        } catch (_: Exception) {
            Toast.makeText(this, "无法切换悬浮按钮，请到 ResourceTree 的悬浮按钮设置中检查权限", Toast.LENGTH_LONG).show()
        }
        // Never show an optimistic ON state: only the actual overlay service can activate it.
        updateState()
    }

    private fun updateState() {
        qsTile?.apply {
            val enabled = FloatingEntryService.running.value && Settings.canDrawOverlays(this@FloatingEntryTileService)
            label = getString(com.example.resouretree.R.string.floating_tile_label)
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            contentDescription = "$label，${if (enabled) "已开启" else "已关闭"}"
            if (Build.VERSION.SDK_INT >= 29) subtitle = if (enabled) "已开启" else "已关闭"
            updateTile()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
