package com.example.resouretree

import android.app.NotificationManager
import android.content.Intent
import android.view.MotionEvent
import android.widget.ImageView
import android.view.WindowManager
import android.os.Looper
import android.view.ViewConfiguration
import com.example.resouretree.overlay.FloatingEntrySettings
import com.example.resouretree.overlay.FloatingEntryService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FloatingEntryServiceTest {
    private fun bubble(service: FloatingEntryService): ImageView? =
        FloatingEntryService::class.java.getDeclaredField("bubble").apply { isAccessible = true }.get(service) as ImageView?

    @Test fun sizeUpdatesExistingWindowAndSurvivesRestartWithoutMovingOffScreen() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller = Robolectric.buildService(FloatingEntryService::class.java).create()
        val service = controller.get()
        val settings = FloatingEntrySettings(service)
        settings.setSize(36f)
        service.onStartCommand(Intent(), 0, 1)
        val view = requireNotNull(bubble(service))
        val density = service.resources.displayMetrics.density
        assertEquals((36 * density).toInt(), (view.layoutParams as WindowManager.LayoutParams).width)
        settings.setSize(96f)
        shadowOf(Looper.getMainLooper()).idle()
        assertSame(view, bubble(service))
        val layout = view.layoutParams as WindowManager.LayoutParams
        assertEquals((96 * density).toInt(), layout.width)
        assertEquals(layout.width, layout.height)
        assertTrue(layout.x + layout.width <= service.resources.displayMetrics.widthPixels)
        controller.destroy()
        val restarted = Robolectric.buildService(FloatingEntryService::class.java).create()
        try {
            restarted.get().onStartCommand(Intent(), 0, 1)
            assertEquals((96 * density).toInt(), (bubble(restarted.get())!!.layoutParams as WindowManager.LayoutParams).width)
        } finally { restarted.destroy() }
    }

    @Test fun missingOverlayPermissionStopsWithoutWindow() {
        ShadowSettings.setCanDrawOverlays(false)
        val controller = Robolectric.buildService(FloatingEntryService::class.java).create()
        val service = controller.get()
        try {
            service.onStartCommand(Intent(), 0, 1)
            assertNull(bubble(service))
            assertTrue(shadowOf(service).isStoppedBySelf)
        } finally { controller.destroy() }
    }

    @Test fun tapOpensCaptureDirectlyDragDoesNotLaunchAndStopRemovesWindow() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller = Robolectric.buildService(FloatingEntryService::class.java).create()
        val service = controller.get()
        try {
            service.onStartCommand(Intent(), 0, 1)
            assertTrue(FloatingEntryService.error.value, FloatingEntryService.running.value)
            val view = requireNotNull(bubble(service))
            service.onStartCommand(Intent(), 0, 2)
            assertSame(view, bubble(service))
            fun touch(action: Int, x: Float, y: Float) {
                val event = MotionEvent.obtain(0, 10, action, x, y, 0)
                view.dispatchTouchEvent(event)
                event.recycle()
            }
            touch(MotionEvent.ACTION_DOWN, 20f, 20f)
            touch(MotionEvent.ACTION_MOVE, 150f, 200f)
            touch(MotionEvent.ACTION_UP, 150f, 200f)
            assertNull(shadowOf(service).nextStartedActivity)
            touch(MotionEvent.ACTION_DOWN, 20f, 20f)
            touch(MotionEvent.ACTION_UP, 20f, 20f)
            val capture = requireNotNull(shadowOf(service).nextStartedActivity)
            assertEquals(ClipboardCaptureActivity::class.java.name, capture.component?.className)
            assertTrue(capture.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
            assertNull(capture.action)
            val notification = shadowOf(service.getSystemService(NotificationManager::class.java)).getNotification(4201)
            assertNotNull(notification.contentIntent)
            assertEquals("关闭悬浮按钮", notification.actions.single().title)
            service.onStartCommand(Intent().setAction(FloatingEntryService.ACTION_STOP), 0, 3)
            assertTrue(shadowOf(service).isStoppedBySelf)
        } finally { controller.destroy() }
        assertNull(bubble(service))
        assertFalse(FloatingEntryService.running.value)
    }

    @Test fun oldLockIsIgnoredAndHoldingCanDragWithoutLaunching() {
        ShadowSettings.setCanDrawOverlays(true)
        val controller = Robolectric.buildService(FloatingEntryService::class.java).create()
        val service = controller.get()
        // A user upgrading from 0.7 must not inherit an invisible, unclickable bubble.
        service.getSharedPreferences("floating-entry", 0).edit().putBoolean("locked", true).apply()
        fun touch(view: ImageView, action: Int, time: Long = 10, x: Float = 20f) {
            MotionEvent.obtain(0, time, action, x, 20f, 0).also { view.dispatchTouchEvent(it); it.recycle() }
        }
        try {
            service.onStartCommand(Intent(), 0, 1)
            val view = requireNotNull(bubble(service))
            assertEquals(1f, view.alpha, .001f)
            assertTrue(view.isClickable)
            assertFalse(view.isLongClickable)
            val held = ViewConfiguration.getLongPressTimeout().toLong() + 10
            touch(view, MotionEvent.ACTION_DOWN, 0)
            touch(view, MotionEvent.ACTION_UP, held)
            assertNull(shadowOf(service).nextStartedActivity)
            val params = view.layoutParams as WindowManager.LayoutParams
            val beforeX = params.x
            touch(view, MotionEvent.ACTION_DOWN, 0)
            touch(view, MotionEvent.ACTION_MOVE, held, 100f)
            touch(view, MotionEvent.ACTION_UP, held + 10, 100f)
            assertTrue(params.x > beforeX)
            assertNull(shadowOf(service).nextStartedActivity)
            touch(view, MotionEvent.ACTION_DOWN)
            touch(view, MotionEvent.ACTION_CANCEL)
            touch(view, MotionEvent.ACTION_UP)
            assertNull(shadowOf(service).nextStartedActivity)
            touch(view, MotionEvent.ACTION_DOWN)
            touch(view, MotionEvent.ACTION_POINTER_DOWN)
            touch(view, MotionEvent.ACTION_UP)
            assertNull(shadowOf(service).nextStartedActivity)
            touch(view, MotionEvent.ACTION_DOWN)
            touch(view, MotionEvent.ACTION_UP)
            assertEquals(ClipboardCaptureActivity::class.java.name, shadowOf(service).nextStartedActivity.component?.className)
        } finally { controller.destroy() }
    }
}
