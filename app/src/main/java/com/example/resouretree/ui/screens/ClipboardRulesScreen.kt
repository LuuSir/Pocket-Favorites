package com.example.resouretree.ui.screens

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.resouretree.data.clipboard.ClipboardRuleStore
import com.example.resouretree.domain.model.ClipboardRule
import com.example.resouretree.domain.model.ActionType
import com.example.resouretree.domain.model.ClipboardMatchType
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipboardRulesScreen(store: ClipboardRuleStore, onBack: () -> Unit) {
    val rules by store.rules.collectAsStateWithLifecycle()
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    if (editingId != null) {
        key(editingId) {
            ClipboardRuleEditor(rules.find { it.id == editingId } ?: ClipboardRule(editingId!!, "", targets = emptyList()),
                onSave = { store.save(it); editingId = null }, onBack = { editingId = null })
        }
        return
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(AppText.get(R.string.clipboard_rules)) }, navigationIcon = { TextButton(onClick = onBack) { Text(AppText.get(R.string.back)) } },
            actions = { TextButton(onClick = { editingId = UUID.randomUUID().toString() }) { Text(AppText.get(R.string.add_rule)) } })
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(AppText.get(R.string.rules_intro), style = MaterialTheme.typography.bodyMedium) }
            items(rules, key = { it.id }) { rule ->
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    ListItem(headlineContent = { Text(rule.prefix, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(rule.name.ifBlank { AppText.get(R.string.name_from_clipboard) }) },
                        trailingContent = { Switch(checked = rule.enabled, onCheckedChange = { store.save(rule.copy(enabled = it)) }, modifier = Modifier.testTag("rule-enabled-${rule.id}")) })
                    Text("${if (rule.matchType == ClipboardMatchType.PREFIX) AppText.get(R.string.prefix_match) else AppText.get(R.string.wildcard_match)} · ${rule.actionType.label()}", Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
                    if (rule.actionType != ActionType.OPEN_WEBVIEW) Text(rule.targets.joinToString("\n"), Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { editingId = rule.id }, modifier = Modifier.testTag("rule-edit-${rule.id}")) { Text(AppText.get(R.string.edit)) }
                        TextButton(onClick = { deletingId = rule.id }, modifier = Modifier.testTag("rule-delete-${rule.id}")) { Text(AppText.get(R.string.delete)) }
                    }
                }
            }
            if (rules.isEmpty()) item { Text(AppText.get(R.string.no_rules)) }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
    deletingId?.let { id ->
        AlertDialog(onDismissRequest = { deletingId = null }, title = { Text(AppText.get(R.string.delete_rule)) },
            text = { Text(AppText.get(R.string.delete_rule_hint)) },
            confirmButton = { TextButton(onClick = { store.delete(id); deletingId = null }) { Text(AppText.get(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text(AppText.get(R.string.cancel)) } })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClipboardRuleEditor(rule: ClipboardRule, onSave: (ClipboardRule) -> Unit, onBack: () -> Unit) {
    var prefix by rememberSaveable { mutableStateOf(rule.prefix) }
    var name by rememberSaveable { mutableStateOf(rule.name) }
    var targets by rememberSaveable { mutableStateOf(rule.targets.joinToString("\n")) }
    var action by rememberSaveable { mutableStateOf(rule.actionType) }
    var matchType by rememberSaveable { mutableStateOf(rule.matchType) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val dirty = prefix != rule.prefix || name != rule.name || targets != rule.targets.joinToString("\n") || action != rule.actionType || matchType != rule.matchType
    val back = { if (dirty) discard = true else onBack() }
    BackHandler { back() }
    Scaffold(topBar = {
        TopAppBar(title = { Text(AppText.get(R.string.edit_rule)) }, navigationIcon = { TextButton(onClick = back) { Text(AppText.get(R.string.back)) } },
            actions = { TextButton(onClick = {
                try { onSave(rule.copy(prefix = prefix, name = name, targets = targets.lines(), actionType = action, matchType = matchType).validated()) }
                catch (e: IllegalArgumentException) { error = e.message }
            }) { Text(AppText.get(R.string.save_rule)) } })
    }) { padding ->
        Column(Modifier.padding(padding).imePadding().fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ClipboardMatchType.entries.forEach { type ->
                    FilterChip(selected = matchType == type, onClick = { matchType = type; error = null },
                        label = { Text(if (type == ClipboardMatchType.PREFIX) AppText.get(R.string.prefix_match) else AppText.get(R.string.wildcard_match)) })
                }
            }
            OutlinedTextField(prefix, { prefix = it; error = null }, label = { Text(if (matchType == ClipboardMatchType.PREFIX) AppText.get(R.string.prefix_required) else AppText.get(R.string.wildcard_required)) }, singleLine = true,
                supportingText = { Text(if (matchType == ClipboardMatchType.PREFIX) AppText.get(R.string.prefix_help)
                    else AppText.get(R.string.wildcard_help)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(name, { name = it; error = null }, label = { Text(AppText.get(R.string.default_name)) }, singleLine = true,
                supportingText = { Text(AppText.get(R.string.default_name_hint)) }, modifier = Modifier.fillMaxWidth())
            Text(AppText.get(R.string.prefill_action), style = MaterialTheme.typography.titleSmall)
            listOf(ActionType.COPY_AND_LAUNCH, ActionType.OPEN_WEBVIEW).forEach { type ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = action == type, onClick = { action = type; error = null })
                    TextButton(onClick = { action = type; error = null }) { Text(type.label()) }
                }
            }
            if (action != ActionType.OPEN_WEBVIEW) OutlinedTextField(targets, { targets = it; error = null }, label = { Text(AppText.get(R.string.rule_targets)) }, minLines = 3,
                supportingText = { Text(AppText.get(R.string.rule_targets_help)) }, modifier = Modifier.fillMaxWidth())
            Text(if (action == ActionType.OPEN_WEBVIEW) AppText.get(R.string.rule_web_help)
                else AppText.get(R.string.rule_app_help), style = MaterialTheme.typography.bodyMedium)
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(AppText.get(R.string.discard_changes)) },
        confirmButton = { TextButton(onClick = onBack) { Text(AppText.get(R.string.discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(AppText.get(R.string.keep_editing)) } })
}
