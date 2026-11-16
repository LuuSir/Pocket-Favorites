package com.example.resouretree.localization

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/** Application-only context for messages produced outside Compose (validation, services, ViewModels). */
object AppText {
    private lateinit var application: Context
    private data class Cache(val revision: Long, val configuration: Configuration, val resources: Resources)
    @Volatile private var cache: Cache? = null
    @Volatile private var revision = 0L

    fun initialize(context: Context) { application = context.applicationContext; invalidate() }

    @Synchronized fun invalidate() { revision++; cache = null }

    private fun resources(): Resources {
        val currentRevision = revision
        val configuration = application.resources.configuration
        cache?.takeIf { it.revision == currentRevision && it.configuration == configuration }?.let { return it.resources }
        return AppLanguages.context(application).resources.also {
            cache = Cache(currentRevision, Configuration(configuration), it)
        }
    }

    fun get(@StringRes id: Int, vararg arguments: Any): String {
        val resources = resources()
        return if (arguments.isEmpty()) resources.getString(id) else resources.getString(id, *arguments)
    }

    fun quantity(@PluralsRes id: Int, count: Int): String = resources().getQuantityString(id, count, count)
}
