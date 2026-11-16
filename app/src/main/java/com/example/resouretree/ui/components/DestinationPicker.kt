package com.example.resouretree.ui.components

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.resouretree.domain.model.*

@Composable
fun DestinationPicker(
    nodes: List<ResourceNode>, initialId: String?, excluded: Set<String>, copy: Boolean, count: Int,
    busy: Boolean, onSelect: (String?) -> Unit, onDismiss: () -> Unit
) {
    var location by rememberSaveable { mutableStateOf(initialId) }
    val current = location?.takeIf { id -> nodes.any { it.id == id && it.type == NodeType.FOLDER && id !in excluded } }
    val breadcrumb = remember(nodes, current) { Tree.breadcrumb(nodes, current) }
    val folders = remember(nodes, current, excluded) {
        nodes.filter { it.type == NodeType.FOLDER && it.parentId == current && it.id !in excluded }.sortedWith(Tree.order)
    }
    val breadcrumbScroll = rememberScrollState()
    LaunchedEffect(current, breadcrumbScroll.maxValue) { breadcrumbScroll.scrollTo(breadcrumbScroll.maxValue) }
    AlertDialog(
        modifier = Modifier.fillMaxWidth().testTag("destination-dialog"),
        onDismissRequest = onDismiss,
        title = { Text(AppText.quantity(if (copy) R.plurals.copy_count_to else R.plurals.move_count_to, count)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(breadcrumbScroll).testTag("destination-breadcrumb")) {
                    TextButton(enabled = !busy, onClick = { location = null }) { Text(AppText.get(R.string.home)) }
                    breadcrumb.forEach { folder ->
                        Text("›", modifier = Modifier.padding(top = 12.dp))
                        TextButton(enabled = !busy, onClick = { location = folder.id }) {
                            Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 180.dp))
                        }
                    }
                }
                HorizontalDivider()
                if (current != null) TextButton(enabled = !busy, onClick = { location = breadcrumb.dropLast(1).lastOrNull()?.id }) { Text(AppText.get(R.string.parent_folder)) }
                LazyColumn(Modifier.fillMaxWidth().heightIn(min = 160.dp, max = 320.dp).testTag("destination-folders")) {
                    items(folders, key = { it.id }) { folder ->
                        TextButton(enabled = !busy, onClick = { location = folder.id }, modifier = Modifier.fillMaxWidth().testTag("destination-folder-${folder.id}")) {
                            Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("›")
                        }
                    }
                    if (folders.isEmpty()) item { Text(AppText.get(R.string.no_subfolders), modifier = Modifier.padding(vertical = 20.dp)) }
                }
            }
        },
        confirmButton = { TextButton(enabled = !busy, onClick = { onSelect(current) }) { Text(if (copy) AppText.get(R.string.copy_here) else AppText.get(R.string.move_here)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(AppText.get(R.string.cancel)) } }
    )
}
