package com.example.resouretree

import android.app.NotificationManager
import android.content.Intent
import android.os.Looper
import com.example.resouretree.localization.*
import com.example.resouretree.overlay.FloatingEntryService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 33, 35])
class FloatingEntryLanguageTest {
    @Test fun languageChangeRefreshesLiveNotificationWithoutReplacingBubble() {
        val app = RuntimeEnvironment.getApplication()
        shadowOf(app).grantPermissions(app.packageName + ".DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        ShadowSettings.setCanDrawOverlays(true)
        val controller = Robolectric.buildService(FloatingEntryService::class.java).create()
        val service = controller.get()
        try {
            service.onStartCommand(Intent(), 0, 1)
            assertTrue(FloatingEntryService.running.value)
            val notifications = shadowOf(service.getSystemService(NotificationManager::class.java))
            assertEquals("关闭悬浮按钮", notifications.getNotification(4201).actions.single().title)
            AppLanguages.set(app, AppLanguage.ENGLISH)
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals("Disable floating button", notifications.getNotification(4201).actions.single().title)
            assertTrue(FloatingEntryService.running.value)
        } finally { controller.destroy() }
    }
}
