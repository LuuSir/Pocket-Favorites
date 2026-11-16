package com.example.resouretree.overlay

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import android.app.PendingIntent
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
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

    private fun toggle() {
        try {
            when {
                FloatingEntryService.running.value -> stopService(Intent(this, FloatingEntryService::class.java))
                !Settings.canDrawOverlays(this) -> {
                    // After granting permission, the user taps the tile again to enable it.
                    openFromTile(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()))
                }
                else -> {
                    // SystemUI starts a visible activity even after process reclamation. Starting
                    // the FGS directly here is blocked by MIUI once the foreground grace expires.
                    openFromTile(Intent(this, FloatingEntryStartActivity::class.java))
                }
            }
        } catch (_: Exception) {
            Toast.makeText(this, AppText.get(R.string.tile_failed), Toast.LENGTH_LONG).show()
        }
        // Never show an optimistic ON state: only the actual overlay service can activate it.
        updateState()
    }

    // The Intent overload remains necessary on API 26–33; API 34+ uses PendingIntent.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openFromTile(intent: Intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun updateState() {
        qsTile?.apply {
            val enabled = FloatingEntryService.running.value && Settings.canDrawOverlays(this@FloatingEntryTileService)
            label = getString(com.example.resouretree.R.string.floating_tile_label)
            state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            contentDescription = AppText.get(R.string.tile_state, label, AppText.get(if (enabled) R.string.on else R.string.off))
            if (Build.VERSION.SDK_INT >= 29) subtitle = if (enabled) AppText.get(R.string.on) else AppText.get(R.string.off)
            updateTile()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        updateState()
    }
}
