package com.example.resouretree

import android.content.Intent
import android.os.Looper
import com.example.resouretree.overlay.FloatingEntryService
import com.example.resouretree.overlay.FloatingEntryStartActivity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings
import org.robolectric.shadows.ShadowToast
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 33, 35])
class FloatingEntryStartTest {
    @org.junit.Before fun grantCompatibilityReceiverPermission() {
        // Android grants the merged signature permission; Robolectric API 26 needs it explicitly.
        val app = org.robolectric.RuntimeEnvironment.getApplication()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(app.packageName + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
    }
    @Test fun startsOnlyAfterResumeWaitsForWindowThenFinishesWithoutLaunchingHome() {
        ShadowSettings.setCanDrawOverlays(true)
        val activity = Robolectric.buildActivity(FloatingEntryStartActivity::class.java).create().start()
        val host = activity.get()
        assertNull(shadowOf(host).nextStartedService)
        val service = Robolectric.buildService(FloatingEntryService::class.java).create()
        try {
            activity.resume().visible()
            shadowOf(Looper.getMainLooper()).idle()
            val request = requireNotNull(shadowOf(host).nextStartedService)
            assertEquals(FloatingEntryService::class.java.name, request.component?.className)
            assertFalse(host.isFinishing)
            service.get().onStartCommand(request, 0, 1)
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(FloatingEntryService.running.value)
            assertTrue(host.isFinishing)
            assertNull(shadowOf(host).nextStartedActivity)
        } finally { service.destroy(); activity.pause().stop().destroy() }
    }

    @Test fun permissionRevocationAndServiceFailureNeverHangOrLeaveAFalseActiveState() {
        ShadowSettings.setCanDrawOverlays(false)
        val denied = Robolectric.buildActivity(FloatingEntryStartActivity::class.java).setup()
        try {
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(denied.get().isFinishing)
            assertNull(shadowOf(denied.get()).nextStartedService)
            assertFalse(FloatingEntryService.running.value)
        } finally { denied.pause().stop().destroy() }
        ShadowSettings.setCanDrawOverlays(true)
        val failed = Robolectric.buildActivity(FloatingEntryStartActivity::class.java).setup()
        try {
            shadowOf(Looper.getMainLooper()).idle()
            FloatingEntryService.error.value = "启动失败验证"
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(failed.get().isFinishing)
            assertEquals("启动失败验证", ShadowToast.getTextOfLatestToast())
            assertFalse(FloatingEntryService.running.value)
        } finally { FloatingEntryService.error.value = null; failed.pause().stop().destroy() }
    }

    @Test fun alreadyRunningSkipsSecondStartAndTimeoutClosesWindow() {
        ShadowSettings.setCanDrawOverlays(true)
        val overlay = Robolectric.buildService(FloatingEntryService::class.java).create()
        overlay.get().onStartCommand(Intent(), 0, 1)
        val existing = Robolectric.buildActivity(FloatingEntryStartActivity::class.java).setup()
        try {
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(existing.get().isFinishing)
            assertNull(shadowOf(existing.get()).nextStartedService)
        } finally { existing.pause().stop().destroy(); overlay.destroy() }
        val timeout = Robolectric.buildActivity(FloatingEntryStartActivity::class.java).setup()
        try {
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(9))
            assertTrue(timeout.get().isFinishing)
            assertTrue(ShadowToast.getTextOfLatestToast().contains("启动超时"))
            assertFalse(FloatingEntryService.running.value)
        } finally { timeout.pause().stop().destroy() }
    }
}
