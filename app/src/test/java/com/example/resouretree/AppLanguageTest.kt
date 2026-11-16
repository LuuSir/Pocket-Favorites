package com.example.resouretree

import android.app.LocaleManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.example.resouretree.localization.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26, 33, 35])
class AppLanguageTest {
    private val app get() = RuntimeEnvironment.getApplication() as ResourceTreeApplication

    @Test fun defaultsToSystemAndChineseSystemUsesChineseName() {
        assertEquals(AppLanguage.SYSTEM, AppLanguages.selected(app))
        assertEquals("外置收藏夹", AppText.get(R.string.app_name))
    }

    @Test fun explicitEnglishAndChinesePersistAcrossActivityCreation() {
        AppLanguages.set(app, AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, AppLanguages.selected(app))
        assertEquals("Pocket Favorites", AppText.get(R.string.app_name))
        assertEquals("Copy and open app", AppText.get(R.string.action_copy_launch))
        Robolectric.buildActivity(MainActivity::class.java).setup().use {
            assertEquals("Pocket Favorites", it.get().getString(R.string.app_name))
        }
        AppLanguages.set(app, AppLanguage.CHINESE)
        assertEquals("外置收藏夹", AppText.get(R.string.app_name))
        assertEquals("复制并打开 App", AppText.get(R.string.action_copy_launch))
    }

    @Test fun followSystemClearsOverrideWithoutChangingRulesOrOverlaySize() {
        val rules = app.clipboardRules.rules.value
        val size = com.example.resouretree.overlay.FloatingEntrySettings(app).sizeDp
        AppLanguages.set(app, AppLanguage.ENGLISH)
        AppLanguages.set(app, AppLanguage.SYSTEM)
        assertEquals(AppLanguage.SYSTEM, AppLanguages.selected(app))
        if (Build.VERSION.SDK_INT >= 33)
            assertTrue(app.getSystemService(LocaleManager::class.java).applicationLocales.isEmpty)
        assertEquals("外置收藏夹", AppText.get(R.string.app_name))
        assertEquals(rules, app.clipboardRules.rules.value)
        assertEquals(size, com.example.resouretree.overlay.FloatingEntrySettings(app).sizeDp)
    }

    @Test fun untranslatedSystemLanguageFallsBackToEnglish() {
        val config = Configuration(app.resources.configuration).apply { setLocales(LocaleList.forLanguageTags("fr-FR")) }
        val french = app.createConfigurationContext(config)
        assertEquals("Pocket Favorites", AppLanguages.context(french).getString(R.string.app_name))
    }

    @Test fun followSystemUpdatesCachedMessagesWhenConfigurationChanges() {
        assertEquals(AppLanguage.SYSTEM, AppLanguages.selected(app))
        assertEquals("外置收藏夹", AppText.get(R.string.app_name))
        RuntimeEnvironment.setQualifiers("en")
        assertEquals("Pocket Favorites", AppText.get(R.string.app_name))
        RuntimeEnvironment.setQualifiers("zh-rCN")
        assertEquals("外置收藏夹", AppText.get(R.string.app_name))
    }

    @Test fun nonComposeErrorsAndNotificationUseSelectedLanguage() {
        AppLanguages.set(app, AppLanguage.ENGLISH)
        assertEquals("Enter a name", AppText.get(R.string.enter_name))
        assertEquals("Floating button size: 48 dp", AppText.get(R.string.bubble_size_value, 48))
        assertEquals("Pocket Favorites overlay", AppText.get(R.string.floating_tile_label))
        assertEquals("Pocket Favorites floating button is on", AppText.get(R.string.overlay_notification_title))
        val error = com.example.resouretree.domain.action.ActionExecutor({ }, { false })
            .execute(com.example.resouretree.domain.model.ResourceContent(text = "BV123"),
                com.example.resouretree.domain.model.ResourceAction(com.example.resouretree.domain.model.ActionType.COPY_AND_LAUNCH, "missing.app"))
        assertTrue((error as com.example.resouretree.domain.action.ActionResult.Error).message.startsWith("Copied; Target app not found"))
    }
}
