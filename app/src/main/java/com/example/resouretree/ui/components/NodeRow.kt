package com.example.resouretree.ui.components

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.resouretree.domain.model.*

@Composable
fun NodeRow(node: ResourceNode, onClick: () -> Unit, onMenu: () -> Unit,
    selectionMode: Boolean = false, selected: Boolean = false, enabled: Boolean = true) {
    val folder = node.type == NodeType.FOLDER
    val tint = if (folder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    ListItem(
        headlineContent = { Text(node.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
            if (node.isPinned) Text(AppText.get(R.string.pinned), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
            if (folder) Text(AppText.get(R.string.folder)) else {
                Column {
                    val preview = if (node.content.type == ContentType.TEXT) node.content.text else node.content.type.name
                    if (preview.isNotEmpty()) Text(preview, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (node.tags.isNotEmpty()) Text(node.tags.joinToString(" · "), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            }
        },
        leadingContent = {
            Canvas(Modifier.size(28.dp)) {
                if (folder) {
                    val path = Path().apply {
                        moveTo(0f, size.height * .2f); lineTo(size.width * .4f, size.height * .2f)
                        lineTo(size.width * .52f, size.height * .34f); lineTo(size.width, size.height * .34f)
                        lineTo(size.width, size.height * .85f); lineTo(0f, size.height * .85f); close()
                    }
                    drawPath(path, tint)
                } else {
                    drawRect(tint.copy(alpha = .18f), Offset(size.width * .15f, 0f), Size(size.width * .7f, size.height))
                    for (i in 1..3) drawLine(tint, Offset(size.width * .3f, size.height * i / 4), Offset(size.width * .7f, size.height * i / 4), 2.dp.toPx())
                }
            }
        },
        trailingContent = {
            if (selectionMode) Checkbox(checked = selected, enabled = enabled, onCheckedChange = { onClick() })
            else TextButton(onClick = onMenu, enabled = enabled,
                modifier = Modifier.semantics { contentDescription = AppText.get(R.string.more_named, node.name) }) { Text(AppText.get(R.string.more)) }
        },
        colors = ListItemDefaults.colors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick)
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f))
}
