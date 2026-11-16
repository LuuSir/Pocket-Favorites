package com.example.resouretree.ui.screens

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.resouretree.domain.model.*
import com.example.resouretree.ui.components.ReorderableNodeList
import com.example.resouretree.ui.components.DestinationPicker
import com.example.resouretree.ui.components.ResourceGraphView
import com.example.resouretree.ui.viewmodel.BrowserViewModel

private data class TransferRequest(val ids: Set<String>, val copy: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(vm: BrowserViewModel, onCreate: (NodeType, String?) -> Unit, onEdit: (ResourceNode) -> Unit, onClipboardRules: () -> Unit = {}, onFloatingEntry: () -> Unit = {}, onLanguage: () -> Unit = {}) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var menu by remember { mutableStateOf(false) }
    var create by remember { mutableStateOf(false) }
    var searching by rememberSaveable { mutableStateOf(false) }
    var graphMode by rememberSaveable { mutableStateOf(false) }
    var expandAll by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<ResourceNode?>(null) }
    var deleting by remember { mutableStateOf<Set<String>?>(null) }
    var transfer by remember { mutableStateOf<TransferRequest?>(null) }
    var selectionMode by rememberSaveable(state.currentId, searching, state.query) { mutableStateOf(false) }
    var selectedIds by rememberSaveable(state.currentId, searching, state.query) { mutableStateOf(arrayListOf<String>()) }
    val rows = if (searching) { if (state.query.isBlank()) emptyList() else state.results } else state.children
    val checked = selectedIds.toSet().intersect(rows.map { it.id }.toSet())
    val clearSelection = { selectedIds = arrayListOf<String>(); selectionMode = false }
    val toggle: (String) -> Unit = { id ->
        if (!state.busy) selectedIds = ArrayList(if (id in checked) checked - id else checked + id)
    }
    var about by remember { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::importDocument) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let(vm::exportDocument) }
    LaunchedEffect(vm) { vm.messages.collect { snackbar.showSnackbar(it) } }
    BackHandler(selectionMode || searching || state.currentId != null || graphMode) {
        if (!state.busy) {
            if (selectionMode) clearSelection()
            else if (searching) { searching = false; vm.search("") }
            else if (state.currentId != null) vm.up() else graphMode = false
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(if (selectionMode) AppText.quantity(R.plurals.selected_count, checked.size) else if (searching) AppText.get(R.string.global_search) else AppText.get(R.string.app_name),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = if (selectionMode || searching) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    modifier = if (selectionMode || searching) Modifier else Modifier
                        .clickable(enabled = !state.busy) { graphMode = !graphMode }
                        .semantics { contentDescription = if (graphMode) AppText.get(R.string.switch_list) else AppText.get(R.string.switch_graph) })
            },
                navigationIcon = { if (selectionMode || searching || state.currentId != null || graphMode) TextButton(enabled = !state.busy, onClick = {
                    if (selectionMode) clearSelection()
                    else if (searching) { searching = false; vm.search("") }
                    else if (state.currentId != null) vm.up() else graphMode = false
                }) { Text(if (selectionMode) AppText.get(R.string.cancel) else AppText.get(R.string.back)) } },
                actions = {
                    if (selectionMode) {
                        val allSelected = rows.isNotEmpty() && checked.size == rows.size
                        TextButton(enabled = !state.busy && rows.isNotEmpty(), onClick = {
                            selectedIds = if (allSelected) arrayListOf() else ArrayList(rows.map { it.id })
                        }) { Text(if (allSelected) AppText.get(R.string.deselect_all) else AppText.get(R.string.select_all)) }
                    } else {
                    if (!searching) TextButton(enabled = !state.busy, onClick = { searching = true }) { Text(AppText.get(R.string.search)) }
                    Box {
                        TextButton(onClick = { menu = true }) { Text(AppText.get(R.string.menu)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text(AppText.get(R.string.import_json)) }, enabled = !state.busy && !state.loading, onClick = {
                                menu = false
                                try { importer.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }
                                catch (_: Exception) { vm.notify(AppText.get(R.string.document_picker_missing)) }
                            })
                            DropdownMenuItem(text = { Text(AppText.get(R.string.export_json)) }, enabled = !state.busy && !state.loading, onClick = {
                                menu = false
                                try { exporter.launch("ResourceTree-${java.time.LocalDate.now()}.json") }
                                catch (_: Exception) { vm.notify(AppText.get(R.string.document_picker_missing)) }
                            })
                            DropdownMenuItem(text = { Text(AppText.get(R.string.clipboard_rules)) }, onClick = { menu = false; onClipboardRules() })
                            DropdownMenuItem(text = { Text(AppText.get(R.string.floating_button)) }, onClick = { menu = false; onFloatingEntry() })
                            DropdownMenuItem(text = { Text(AppText.get(R.string.language_title)) }, onClick = { menu = false; onLanguage() })
                            DropdownMenuItem(text = { Text(AppText.get(R.string.about)) }, onClick = { menu = false; about = true })
                        }
                    }
                    }
                })
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (selectionMode) BottomAppBar {
                TextButton(modifier = Modifier.weight(1f), enabled = checked.isNotEmpty() && !state.busy,
                    onClick = { transfer = TransferRequest(checked, false) }) { Text(AppText.get(R.string.move)) }
                TextButton(modifier = Modifier.weight(1f), enabled = checked.isNotEmpty() && !state.busy,
                    onClick = { transfer = TransferRequest(checked, true) }) { Text(AppText.get(R.string.copy)) }
                TextButton(modifier = Modifier.weight(1f), enabled = checked.isNotEmpty() && !state.busy,
                    onClick = { deleting = checked }) { Text(AppText.get(R.string.delete)) }
            }
        },
        floatingActionButton = { if (!graphMode && !selectionMode && !searching && !state.loading && state.error == null && !state.busy) ExtendedFloatingActionButton(onClick = { create = true }) { Text(AppText.get(R.string.create)) } }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (searching) OutlinedTextField(state.query, vm::search, enabled = !selectionMode && !state.busy, label = { Text(AppText.get(R.string.search_hint)) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(16.dp))
            else Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !selectionMode && !state.busy, onClick = { vm.open(null) }) { Text(AppText.get(R.string.home)) }
                state.breadcrumb.forEach { node -> Text("›"); TextButton(enabled = !selectionMode && !state.busy, onClick = { vm.open(node.id) }) { Text(node.name) } }
            }
            if (graphMode && !searching) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(AppText.get(R.string.graph_view), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(AppText.get(R.string.expand_all), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(8.dp))
                Switch(checked = expandAll, onCheckedChange = { expandAll = it }, enabled = !state.busy,
                    modifier = Modifier.semantics { contentDescription = AppText.get(R.string.expand_all) })
            }
            when {
                state.error != null -> CenterMessage(state.error.orEmpty(), AppText.get(R.string.retry), vm::initialize)
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                graphMode && !searching -> ResourceGraphView(state.all, state.currentId, expandAll, !state.busy) { node ->
                    if (node.type == NodeType.FOLDER) vm.open(node.id) else vm.execute(node)
                }
                searching && state.query.isBlank() -> CenterMessage(AppText.get(R.string.search_prompt))
                rows.isEmpty() -> CenterMessage(if (searching) AppText.get(R.string.no_results) else AppText.get(R.string.empty_folder))
                else -> key(state.currentId, searching) {
                    ReorderableNodeList(rows, selectionMode, checked, state.busy, canReorder = !searching,
                        onClick = { node -> if (selectionMode) toggle(node.id) else if (node.type == NodeType.FOLDER) vm.open(node.id) else vm.execute(node) },
                        onMenu = { node -> selected = node },
                        onReorder = { vm.reorder(state.currentId, it) })
                }
            }
        }
    }
    if (create) AlertDialog(onDismissRequest = { create = false }, title = { Text(AppText.get(R.string.create_here)) }, text = {
        Column { NodeType.entries.forEach { type -> TextButton(onClick = { create = false; onCreate(type, state.currentId) }) { Text(if (type == NodeType.FOLDER) AppText.get(R.string.new_folder) else AppText.get(R.string.new_item)) } } }
    }, confirmButton = { TextButton(onClick = { create = false }) { Text(AppText.get(R.string.cancel)) } })
    selected?.let { node -> AlertDialog(onDismissRequest = { selected = null }, title = { Text(node.name) }, text = {
        Column {
            TextButton(enabled = !state.busy, onClick = { selected = null; vm.setPinned(node) }) { Text(if (node.isPinned) AppText.get(R.string.unpin) else AppText.get(R.string.pin)) }
            TextButton(enabled = !state.busy, onClick = { selected = null; onEdit(node) }) { Text(AppText.get(R.string.edit)) }
            TextButton(enabled = !state.busy, onClick = { selected = null; selectionMode = true; selectedIds = arrayListOf(node.id) }) { Text(AppText.get(R.string.select_multiple)) }
            TextButton(enabled = !state.busy, onClick = { selected = null; transfer = TransferRequest(setOf(node.id), false) }) { Text(AppText.get(R.string.move)) }
            TextButton(enabled = !state.busy, onClick = { selected = null; transfer = TransferRequest(setOf(node.id), true) }) { Text(AppText.get(R.string.copy)) }
            TextButton(enabled = !state.busy, onClick = { selected = null; deleting = setOf(node.id) }) { Text(AppText.get(R.string.delete), color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = { selected = null }) { Text(AppText.get(R.string.cancel)) } }) }
    deleting?.let { ids ->
        val total = ids.flatMap { Tree.descendants(state.all, it) }.toSet().size
        val single = state.all.find { it.id == ids.singleOrNull() }
        AlertDialog(onDismissRequest = { deleting = null }, title = { Text(if (single != null) AppText.get(R.string.delete_named, single.name) else AppText.quantity(R.plurals.delete_selected, ids.size)) },
        text = { Text(AppText.quantity(R.plurals.delete_warning, total)) },
        confirmButton = { TextButton(enabled = !state.busy, onClick = { deleting = null; vm.deleteMany(ids, clearSelection) }) { Text(AppText.get(R.string.delete), color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text(AppText.get(R.string.cancel)) } }) }
    transfer?.let { request ->
        val excluded = remember(state.all, request) {
            if (request.copy) emptySet() else request.ids.flatMap { Tree.descendants(state.all, it) }.toSet()
        }
        val destination: (String?) -> Unit = { parent ->
            transfer = null; vm.transferMany(request.ids, parent, request.copy, clearSelection)
        }
        DestinationPicker(state.all, state.currentId, excluded, request.copy, request.ids.size, state.busy,
            onSelect = destination, onDismiss = { transfer = null })
    }
    if (about) AlertDialog(onDismissRequest = { about = false }, title = { Text(AppText.get(R.string.about_title, AppText.get(R.string.app_name))) },
        text = { Text(AppText.get(R.string.about_description)) },
        confirmButton = { TextButton(onClick = { about = false }) { Text(AppText.get(R.string.got_it)) } })
}

@Composable
private fun CenterMessage(text: String, action: String? = null, onClick: () -> Unit = {}) {
    Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (action != null) Button(onClick = onClick) { Text(action) }
        }
    }
}
