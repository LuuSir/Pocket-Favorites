package com.example.resouretree

import android.app.LocaleConfig
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only installed-resource checks; does not change app/system locales, content, or clipboard. */
@RunWith(AndroidJUnit4::class)
class LocaleResourcesInstrumentedTest {
    @Test fun installedApkContainsBothCompleteLanguageResources() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fun localized(tag: String) = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(tag))
        })
        val english = localized("en")
        val chinese = localized("zh-Hans")
        assertEquals("Pocket Favorites", english.getString(R.string.app_name))
        assertEquals("外置收藏夹", chinese.getString(R.string.app_name))
        assertEquals("Follow system", english.getString(R.string.language_system))
        assertEquals("跟随系统", chinese.getString(R.string.language_system))
        assertEquals("Copy and open app", english.getString(R.string.action_copy_launch))
        assertEquals("复制并打开 App", chinese.getString(R.string.action_copy_launch))
        assertEquals("Move 1 item to…", english.resources.getQuantityString(R.plurals.move_count_to, 1, 1))
        assertEquals("Move 2 items to…", english.resources.getQuantityString(R.plurals.move_count_to, 2, 2))
    }

    @Test fun systemAppLanguagesAdvertiseEnglishAndChinese() {
        assumeTrue(Build.VERSION.SDK_INT >= 33)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val locales = requireNotNull(LocaleConfig(context).supportedLocales).toLanguageTags()
        assertTrue(locales.contains("en"))
        assertTrue(locales.contains("zh-Hans"))
    }
}
