package com.example.resouretree

import android.content.Intent
import android.app.PendingIntent
import android.os.Looper
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.resouretree.overlay.FloatingEntryService
import com.example.resouretree.overlay.FloatingEntryTileService
import com.example.resouretree.overlay.FloatingEntryStartActivity
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
    @org.junit.Before fun grantCompatibilityReceiverPermission() {
        // Android grants the merged signature permission; Robolectric API 26 needs it explicitly.
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(app.packageName + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
    }
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
            val started = shadowOf(tile.application).nextStartedActivity
            assertEquals(FloatingEntryStartActivity::class.java.name, started.component?.className)
            assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
            assertNull(shadowOf(tile.application).nextStartedService)
            assertEquals(Tile.STATE_INACTIVE, tile.qsTile.state) // Until the window really exists.
            overlay.get().onStartCommand(Intent(), 0, 1)
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

    @Test fun newTileServiceCanRequestColdLaunchWithoutAnActivityOrOverlayRunning() {
        ShadowSettings.setCanDrawOverlays(true)
        val first = Robolectric.buildService(FloatingEntryTileService::class.java).create()
        first.destroy()
        val recreated = Robolectric.buildService(FloatingEntryTileService::class.java).create()
        try {
            assertFalse(FloatingEntryService.running.value)
            recreated.get().onClick()
            assertEquals(FloatingEntryStartActivity::class.java.name,
                shadowOf(recreated.get().application).nextStartedActivity.component?.className)
            assertNull(shadowOf(recreated.get().application).nextStartedService)
        } finally { recreated.destroy() }
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

