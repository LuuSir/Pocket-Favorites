package com.example.resouretree.ui.screens

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.resouretree.domain.model.*
import com.example.resouretree.ui.viewmodel.EditorViewModel
import com.example.resouretree.ui.components.AppPicker
import com.example.resouretree.ui.components.ApplicationIcon

fun ActionType.label(): String = when (this) {
    ActionType.NONE -> AppText.get(R.string.action_none); ActionType.COPY -> AppText.get(R.string.action_copy)
    ActionType.SHARE -> AppText.get(R.string.action_share)
    ActionType.OPEN_WEBVIEW -> AppText.get(R.string.action_web)
    ActionType.LAUNCH_APP -> AppText.get(R.string.action_launch); ActionType.COPY_AND_LAUNCH -> AppText.get(R.string.action_copy_launch)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: EditorViewModel, editing: Boolean, fromClipboard: Boolean = false, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    var dirty by rememberSaveable { mutableStateOf(fromClipboard) }
    var discard by remember { mutableStateOf(false) }
    var chooseApp by rememberSaveable { mutableStateOf(false) }
    val working = state.saving || state.importingMedia
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { dirty = true; vm.selectMedia(uri) }
    }
    val needsApp = state.type == NodeType.ITEM && state.actionType in listOf(ActionType.LAUNCH_APP, ActionType.COPY_AND_LAUNCH, ActionType.SHARE)
    LaunchedEffect(needsApp, state.loading, state.target) { if (needsApp && !state.loading) vm.loadTarget() }
    LaunchedEffect(chooseApp) { if (chooseApp) vm.loadApps() }
    val back = { if (dirty && !state.saved) discard = true else onBack() }
    BackHandler(enabled = !state.saved) { if (!working) back() }
    LaunchedEffect(state.saved) { if (state.saved) onBack() }
    Scaffold(topBar = {
        TopAppBar(title = { Text(AppText.get(if (editing) {
            if (state.type == NodeType.FOLDER) R.string.edit_folder else R.string.edit_item
        } else { if (state.type == NodeType.FOLDER) R.string.new_folder else R.string.new_item })) },
            navigationIcon = { TextButton(onClick = back, enabled = !working) { Text(AppText.get(R.string.back)) } },
            actions = { TextButton(onClick = vm::save, enabled = !state.loading && !working && !state.loadFailed) { Text(if (state.saving) AppText.get(R.string.saving) else AppText.get(R.string.save)) } })
    }) { padding ->
        if (state.loading) Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        else Column(Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedTextField(state.name, { dirty = true; vm.change { s -> s.copy(name = it) } }, label = { Text(AppText.get(R.string.name_required)) }, modifier = Modifier.fillMaxWidth(), enabled = !working && !state.loadFailed)
            if (state.type == NodeType.ITEM && !state.loadFailed) {
                Text(AppText.get(R.string.content_type), style = MaterialTheme.typography.titleSmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ContentType.entries.forEach { type ->
                        FilterChip(selected = state.content.type == type, enabled = !working,
                            onClick = { dirty = true; vm.selectContentType(type) }, label = { Text(type.name) })
                    }
                }
                if (state.content.type == ContentType.TEXT) {
                    OutlinedTextField(state.content.text, { value -> dirty = true; vm.change { it.copy(content = it.content.copy(text = value)) } },
                        label = { Text(AppText.get(R.string.text_content)) }, minLines = 3, modifier = Modifier.fillMaxWidth(), enabled = !working)
                } else {
                    OutlinedButton(enabled = !working, onClick = {
                        val mime = when (state.content.type) { ContentType.IMAGE -> "image/*"; ContentType.VIDEO -> "video/*"; else -> "*/*" }
                        try { mediaPicker.launch(arrayOf(mime)) } catch (_: Exception) { vm.mediaError() }
                    }) { Text(if (state.importingMedia) AppText.get(R.string.saving_file) else if (state.content.path.isBlank()) AppText.get(R.string.choose_media) else AppText.get(R.string.choose_file_again)) }
                    if (state.content.path.isNotBlank()) Text(state.content.path, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(state.content.mimeType, { value -> dirty = true; vm.change { it.copy(content = it.content.copy(mimeType = value.trim())) } },
                        label = { Text(AppText.get(R.string.mime_type)) }, supportingText = { Text(AppText.get(R.string.mime_hint)) }, enabled = !working, modifier = Modifier.fillMaxWidth())
                }
                OutlinedTextField(state.tags, { dirty = true; vm.change { s -> s.copy(tags = it) } }, label = { Text(AppText.get(R.string.tags_hint)) }, modifier = Modifier.fillMaxWidth(), enabled = !working)
                Text(AppText.get(R.string.on_item_tap), style = MaterialTheme.typography.titleSmall)
                ActionType.entries.filter { state.content.type == ContentType.TEXT || it !in listOf(ActionType.COPY, ActionType.COPY_AND_LAUNCH, ActionType.OPEN_WEBVIEW) }.forEach { action ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(selected = state.actionType == action, enabled = !working, onClick = { dirty = true; vm.change { it.copy(actionType = action) } })
                        TextButton(enabled = !working, onClick = { dirty = true; vm.change { it.copy(actionType = action) } }) { Text(action.label()) }
                    }
                }
                if (needsApp) {
                    val app = state.apps.find { it.packageName == state.target } ?: state.targetApp?.takeIf { it.packageName == state.target }
                    Text(if (state.actionType == ActionType.SHARE) AppText.get(R.string.target_optional) else AppText.get(R.string.target_required), style = MaterialTheme.typography.titleSmall)
                    OutlinedCard(onClick = { vm.loadApps(); chooseApp = true }, enabled = !working, modifier = Modifier.fillMaxWidth()) {
                        ListItem(leadingContent = { ApplicationIcon(app) },
                            headlineContent = { Text(app?.name ?: if (state.target.isBlank()) { if (state.actionType == ActionType.SHARE) AppText.get(R.string.system_sharesheet) else AppText.get(R.string.choose_app) } else if (state.targetLoading) AppText.get(R.string.loading_app) else state.target) },
                            supportingContent = { Text(if (state.target.isBlank()) AppText.get(R.string.choose_app_hint) else AppText.get(R.string.target_set)) })
                    }
                    if (state.actionType == ActionType.SHARE && state.target.isNotBlank()) {
                        TextButton(enabled = !working, onClick = { dirty = true; vm.change { it.copy(target = "") } }) { Text(AppText.get(R.string.use_sharesheet)) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (chooseApp) AppPicker(state.apps, state.appsLoading, state.appsError, state.target,
        onReload = { vm.loadApps(force = true) },
        onSelect = { dirty = true; vm.selectApp(it); chooseApp = false }, onDismiss = { chooseApp = false })
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text(AppText.get(R.string.discard_changes)) },
        confirmButton = { TextButton(onClick = { discard = false; onBack() }) { Text(AppText.get(R.string.discard)) } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text(AppText.get(R.string.keep_editing)) } })
}
