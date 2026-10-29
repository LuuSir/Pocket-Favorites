package com.example.resouretree.data.clipboard

import android.content.ClipData
import android.content.Context
import com.example.resouretree.domain.model.ClipboardDraft
import com.example.resouretree.domain.model.ClipboardDraftParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/** Clipboard reads belong to focused windows; explicit captures don't enqueue navigation in MainActivity. */
class ClipboardDraftInbox(context: Context, private val rules: ClipboardRuleStore = ClipboardRuleStore(context)) {
    private val preferences = context.applicationContext.getSharedPreferences("clipboard-import", Context.MODE_PRIVATE)
    private val mutable = MutableStateFlow<ClipboardDraft?>(null)
    val pending = mutable.asStateFlow()

    fun offer(clip: ClipData?) {
        mutable.value = read(clip)
    }

    fun read(clip: ClipData?, explicit: Boolean = false): ClipboardDraft? {
        // ResourceTree's own COPY actions must not turn into another new item when returning to the app.
        if (clip == null || clip.itemCount == 0 || (!explicit && clip.description.label?.toString() == "ResourceTree") ||
            clip.description.extras?.getBoolean("android.content.extra.IS_SENSITIVE", false) == true) {
            return null
        }
        val text = clip.getItemAt(0).text?.toString()
        if (text == null || text.length > 100_000) return null
        val fingerprint = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        val token = "${clip.description.timestamp}:$fingerprint"
        return if (!explicit && token == preferences.getString("handled", null)) null else ClipboardDraftParser.parse(text, token, rules.rules.value)
    }

    fun consumed(token: String) {
        preferences.edit().putString("handled", token).apply()
        if (mutable.value?.token == token) mutable.value = null
    }

    fun clearPending() { mutable.value = null }
}
