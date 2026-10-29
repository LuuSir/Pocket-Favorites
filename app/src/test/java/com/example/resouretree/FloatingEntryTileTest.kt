package com.example.resouretree

import android.content.Intent
import android.app.PendingIntent
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.resouretree.overlay.FloatingEntryService
import com.example.resouretree.overlay.FloatingEntryTileService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implements
import org.robolectric.annotation.Implementation
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowTileService

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 33, 35], shadows = [FloatingEntryTileTest.TileHostShadow::class])
class FloatingEntryTileTest {
    // Robolectric 4.16's TileService shadow does not inherit ShadowService and lacks
    // the API 34 PendingIntent overload. Supply only the missing SystemUI plumbing.
    @Implements(TileService::class)
    class TileHostShadow : ShadowTileService() {
        @Implementation
        protected fun onDestroy() = Unit

        @Implementation(minSdk = 34)
        protected fun startActivityAndCollapse(intent: PendingIntent) { intent.send() }
    }
    @Test fun tileStartsOverlayAndTracksExternalShutdownAndNextPanelOpening() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller = Robolectric.buildService(FloatingEntryTileService::class.java).create()
        val tile = controller.get()
        val overlay = Robolectric.buildService(FloatingEntryService::class.java).create()
        try {
            tile.onStartListening()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(Tile.STATE_INACTIVE, tile.qsTile.state)
            tile.onClick()
            val started = shadowOf(tile.application).nextStartedService
            assertEquals(FloatingEntryService::class.java.name, started.component?.className)
            assertNull(shadowOf(tile.application).nextStartedActivity)
            assertEquals(Tile.STATE_INACTIVE, tile.qsTile.state) // Until the window really exists.
            overlay.get().onStartCommand(started, 0, 1)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(Tile.STATE_ACTIVE, tile.qsTile.state)
            tile.onClick()
            assertEquals(FloatingEntryService::class.java.name, shadowOf(tile.application).nextStoppedService.component?.className)
            overlay.destroy()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(Tile.STATE_INACTIVE, tile.qsTile.state)
            tile.onStopListening()
            val external = Robolectric.buildService(FloatingEntryService::class.java).create()
            try {
                external.get().onStartCommand(Intent(), 0, 1)
                tile.onStartListening()
                shadowOf(Looper.getMainLooper()).idle()
                assertEquals(Tile.STATE_ACTIVE, tile.qsTile.state)
            } finally { external.destroy() }
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(Tile.STATE_INACTIVE, tile.qsTile.state)
        } finally {
            if (FloatingEntryService.running.value) overlay.destroy()
            controller.destroy()
        }
    }

    @Test
    fun missingPermissionOpensAuthorizationWithoutStartingOverlay() {
        ShadowSettings.setCanDrawOverlays(false)
        val controller = Robolectric.buildService(FloatingEntryTileService::class.java).create()
        try {
            val tile = controller.get()
            tile.onStartListening()
            tile.onClick()
            val permission = shadowOf(tile.application).nextStartedActivity
            assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, permission.action)
            assertEquals("package:${tile.packageName}", permission.dataString)
            assertNull(shadowOf(tile.application).nextStartedService)
            assertEquals(Tile.STATE_INACTIVE, tile.qsTile.state)
        } finally { controller.destroy() }
    }
}

