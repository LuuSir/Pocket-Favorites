package com.example.resouretree.localization

import android.app.LocaleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.service.quicksettings.TileService
import androidx.activity.ComponentActivity
import com.example.resouretree.overlay.FloatingEntryTileService

enum class AppLanguage(val tag: String) {
    SYSTEM(""), CHINESE("zh-Hans"), ENGLISH("en")
}

/** Native per-app locales on Android 13+, with a preference-backed fallback on older devices. */
object AppLanguages {
    const val ACTION_CHANGED = "com.example.resouretree.LANGUAGE_CHANGED"
    private const val PREFERENCES = "app-language"

    fun selected(context: Context): AppLanguage {
        val tag = if (Build.VERSION.SDK_INT >= 33)
            context.getSystemService(LocaleManager::class.java).applicationLocales.toLanguageTags()
        else context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString("locale", "").orEmpty()
        return when {
            tag.startsWith("zh", ignoreCase = true) -> AppLanguage.CHINESE
            tag.startsWith("en", ignoreCase = true) -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }
    }

    fun set(context: Context, language: AppLanguage) {
        if (language == selected(context)) return
        if (Build.VERSION.SDK_INT >= 33)
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(language.tag)
        else context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString("locale", language.tag).apply()
        AppText.invalidate()
        context.sendBroadcast(Intent(ACTION_CHANGED).setPackage(context.packageName))
        TileService.requestListeningState(context, ComponentName(context, FloatingEntryTileService::class.java))
    }

    fun context(context: Context): Context {
        val language = selected(context)
        if (language == AppLanguage.SYSTEM) return context
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocales(LocaleList.forLanguageTags(language.tag))
        return context.createConfigurationContext(configuration)
    }
}

/** Retains ComponentActivity and existing saved-state/navigation behavior. */
open class LocalizedActivity : ComponentActivity() {
    private var attachedLanguage = AppLanguage.SYSTEM

    override fun attachBaseContext(newBase: Context) {
        attachedLanguage = AppLanguages.selected(newBase)
        super.attachBaseContext(AppLanguages.context(newBase))
    }

    override fun onResume() {
        super.onResume()
        // Other tasks, such as the floating clipboard editor, may have been stopped during a change.
        if (Build.VERSION.SDK_INT < 33 && attachedLanguage != AppLanguages.selected(this)) recreate()
    }
}
