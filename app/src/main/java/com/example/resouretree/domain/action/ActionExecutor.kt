package com.example.resouretree.domain.action

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import com.example.resouretree.domain.model.*

fun interface ClipboardWriter { fun write(text: String) }
fun interface PackageLauncher { fun launch(packageName: String): Boolean }
fun interface ContentSharer { fun share(content: ResourceContent, target: String) }
fun interface WebPageOpener { fun open(url: String) }

sealed interface ActionResult {
    data class Success(val message: String) : ActionResult
    data class Error(val message: String) : ActionResult
}

class ActionExecutor(private val clipboard: ClipboardWriter, private val launcher: PackageLauncher,
    private val sharer: ContentSharer = ContentSharer { _, _ -> error(AppText.get(R.string.share_unavailable)) },
    private val web: WebPageOpener = WebPageOpener { error(AppText.get(R.string.web_unavailable)) }) {
    fun execute(content: ResourceContent, action: ResourceAction): ActionResult {
        var copied = false
        return try {
            if (action.type == ActionType.COPY || action.type == ActionType.COPY_AND_LAUNCH) {
                require(content.type == ContentType.TEXT) { AppText.get(R.string.cannot_copy_media) }
                clipboard.write(content.text)
                copied = true
            }
            if (action.type == ActionType.LAUNCH_APP || action.type == ActionType.COPY_AND_LAUNCH) {
                if (action.target.isBlank() || !launcher.launch(action.target)) {
                    return ActionResult.Error(AppText.get(R.string.target_not_found, if (copied) AppText.get(R.string.copied_prefix) else "", action.target))
                }
            }
            if (action.type == ActionType.SHARE) sharer.share(content, action.target)
            if (action.type == ActionType.OPEN_WEBVIEW) {
                require(content.type == ContentType.TEXT) { AppText.get(R.string.web_needs_text) }
                web.open(requireNotNull(webUrl(content.text)) { AppText.get(R.string.enter_url) })
            }
            ActionResult.Success(when {
                copied -> AppText.get(R.string.copied)
                action.type == ActionType.NONE -> AppText.get(R.string.no_action)
                else -> AppText.get(R.string.app_opened)
            })
        } catch (e: Exception) {
            ActionResult.Error(AppText.get(R.string.action_failed, if (copied) AppText.get(R.string.copied_prefix) else "", e.message ?: AppText.get(R.string.system_rejected)))
        }
    }
}
