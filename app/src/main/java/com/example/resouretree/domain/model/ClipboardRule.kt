package com.example.resouretree.domain.model

import com.example.resouretree.R
import com.example.resouretree.localization.AppText

data class ClipboardRule(
    val id: String,
    val prefix: String,
    val name: String = "",
    val targets: List<String>,
    val enabled: Boolean = true,
    val actionType: ActionType = ActionType.COPY_AND_LAUNCH,
    val matchType: ClipboardMatchType = ClipboardMatchType.PREFIX
) {
    fun validated(): ClipboardRule {
        val result = copy(prefix = prefix.trim(), name = name.trim(), targets = targets.map(String::trim).filter(String::isNotEmpty).distinct())
        require(result.prefix.isNotEmpty() && result.prefix.length <= 100) { AppText.get(R.string.rule_pattern_length) }
        require(result.name.length <= 80) { AppText.get(R.string.rule_name_length) }
        require(actionType in listOf(ActionType.COPY_AND_LAUNCH, ActionType.OPEN_WEBVIEW)) { AppText.get(R.string.rule_action_invalid) }
        if (actionType == ActionType.OPEN_WEBVIEW) return result.copy(targets = emptyList())
        require(result.targets.size in 1..20) { AppText.get(R.string.rule_target_count) }
        require(result.targets.all { Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+").matches(it) }) { AppText.get(R.string.rule_target_invalid) }
        return result
    }

    fun matches(text: String): Boolean = when (matchType) {
        ClipboardMatchType.PREFIX -> text.startsWith(prefix)
        ClipboardMatchType.WILDCARD -> clipboardGlobMatches(prefix, text)
    }

    val specificity: Int get() = prefix.codePoints().toArray().count {
        matchType == ClipboardMatchType.PREFIX || (it != '*'.code && it != '?'.code)
    }

    companion object {
        val defaults = listOf(
            ClipboardRule("bilibili", "BV", targets = listOf("tv.danmaku.bili", "com.bilibili.app.in")),
            ClipboardRule("taobao", "【淘宝】", "淘宝分享", listOf("com.taobao.taobao"))
        )
    }
}
