package com.example.resouretree.ui.screens

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.resouretree.R
import com.example.resouretree.localization.AppLanguage
import com.example.resouretree.localization.AppLanguages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(AppLanguages.selected(context)) }
    Scaffold(topBar = {
        TopAppBar(title = { Text(stringResource(R.string.language_title)) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.language_description))
            AppLanguage.entries.forEach { language ->
                val choose = {
                    if (language != selected) {
                        selected = language
                        AppLanguages.set(context, language)
                        if (Build.VERSION.SDK_INT < 33) (context as? Activity)?.recreate()
                    }
                }
                ListItem(
                    headlineContent = { Text(when (language) {
                        AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                        AppLanguage.CHINESE -> "简体中文"
                        AppLanguage.ENGLISH -> "English"
                    }) },
                    leadingContent = { RadioButton(selected == language, onClick = choose) },
                    modifier = Modifier.fillMaxWidth().clickable(onClick = choose))
            }
        }
    }
}
