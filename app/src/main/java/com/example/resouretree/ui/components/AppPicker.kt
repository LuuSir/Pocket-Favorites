package com.example.resouretree.ui.components

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.resouretree.data.apps.InstalledApp

@Composable
fun ApplicationIcon(app: InstalledApp?, modifier: Modifier = Modifier) {
    val icon = app?.icon
    val iconModifier = modifier.size(40.dp)
    if (icon != null) Image(icon.asImageBitmap(), contentDescription = null, modifier = iconModifier)
    else Surface(iconModifier, shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
        Box(contentAlignment = Alignment.Center) { Text(app?.name?.take(1) ?: AppText.get(R.string.app)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPicker(
    apps: List<InstalledApp>, loading: Boolean, error: String?, selectedPackage: String,
    onReload: () -> Unit, onSelect: (InstalledApp) -> Unit, onDismiss: () -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(apps, query) {
        val text = query.trim()
        apps.filter { it.name.contains(text, ignoreCase = true) || it.packageName.contains(text, ignoreCase = true) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(topBar = { TopAppBar(title = { Text(AppText.get(R.string.choose_app)) },
                navigationIcon = { TextButton(onClick = onDismiss) { Text(AppText.get(R.string.back)) } },
                actions = { TextButton(enabled = !loading, onClick = onReload) { Text(AppText.get(R.string.refresh)) } }) }) { padding ->
                Column(Modifier.padding(padding).imePadding()) {
                    OutlinedTextField(query, { query = it }, label = { Text(AppText.get(R.string.search_apps)) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(16.dp))
                    when {
                        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                        error != null -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(error, color = MaterialTheme.colorScheme.error)
                            Button(onClick = onReload) { Text(AppText.get(R.string.retry)) }
                        }
                        filtered.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(if (query.isNotBlank()) AppText.get(R.string.no_matching_apps) else AppText.get(R.string.no_apps))
                        }
                        else -> LazyColumn(Modifier.fillMaxSize()) {
                            items(filtered, key = { it.packageName }) { app ->
                                ListItem(headlineContent = { Text(app.name) },
                                    supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) },
                                    leadingContent = { ApplicationIcon(app) },
                                    trailingContent = { RadioButton(selected = app.packageName == selectedPackage, onClick = { onSelect(app) }) },
                                    modifier = Modifier.testTag("app-${app.packageName}").clickable { onSelect(app) })
                            }
                        }
                    }
                }
            }
        }
    }
}
