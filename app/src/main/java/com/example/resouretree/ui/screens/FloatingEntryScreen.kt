package com.example.resouretree.ui.screens

import com.example.resouretree.R
import com.example.resouretree.localization.AppText
import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.resouretree.overlay.FloatingEntryService
import com.example.resouretree.overlay.FloatingEntrySettings
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingEntryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember(context) { FloatingEntrySettings(context) }
    var sizeDp by remember(settings) { mutableIntStateOf(settings.sizeDp) }
    DisposableEffect(settings) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == FloatingEntrySettings.KEY_SIZE) sizeDp = settings.sizeDp
        }
        settings.preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { settings.preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val running by FloatingEntryService.running.collectAsStateWithLifecycle()
    val serviceError by FloatingEntryService.error.collectAsStateWithLifecycle()
    var permitted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var awaitingPermission by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun start() {
        try {
            FloatingEntryService.error.value = null
            ContextCompat.startForegroundService(context, Intent(context, FloatingEntryService::class.java))
        } catch (_: Exception) { error = AppText.get(R.string.overlay_retry) }
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { start() }
    fun enable() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        else start()
    }
    val overlayPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Permission settings may return before RESUMED; the lifecycle observer performs the start.
        permitted = Settings.canDrawOverlays(context)
    }
    val onResume by rememberUpdatedState(newValue = {
        permitted = Settings.canDrawOverlays(context)
        if (!permitted && running) context.stopService(Intent(context, FloatingEntryService::class.java))
        if (awaitingPermission) {
            awaitingPermission = false
            if (permitted) enable() else error = AppText.get(R.string.overlay_not_allowed)
        }
    })
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) onResume() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    Scaffold(topBar = { TopAppBar(title = { Text(AppText.get(R.string.floating_button)) }, navigationIcon = { TextButton(onClick = onBack) { Text(AppText.get(R.string.back)) } }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(AppText.get(R.string.overlay_intro))
            Text(AppText.get(R.string.tile_instructions))
            Text(if (running) AppText.get(R.string.status_on) else AppText.get(R.string.status_off))
            Text(AppText.get(R.string.bubble_size_value, sizeDp))
            Slider(value = sizeDp.toFloat(), onValueChange = { sizeDp = settings.setSize(it) },
                valueRange = FloatingEntrySettings.MIN_SIZE.toFloat()..FloatingEntrySettings.MAX_SIZE.toFloat(), steps = 14,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = AppText.get(R.string.bubble_size) })
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(AppText.get(R.string.small), style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = { sizeDp = settings.setSize(FloatingEntrySettings.DEFAULT_SIZE.toFloat()) }) { Text(AppText.get(R.string.reset_size)) }
                Text(AppText.get(R.string.large), style = MaterialTheme.typography.labelSmall)
            }
            Text(AppText.get(R.string.size_help), style = MaterialTheme.typography.bodySmall)
            (error ?: serviceError)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                error = null
                if (running) context.stopService(Intent(context, FloatingEntryService::class.java))
                else if (Settings.canDrawOverlays(context)) enable()
                else {
                    awaitingPermission = true
                    try { overlayPermission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())) }
                    catch (_: Exception) { awaitingPermission = false; error = AppText.get(R.string.overlay_settings_failed) }
                }
            }) { Text(if (running) AppText.get(R.string.disable_overlay) else AppText.get(R.string.enable_overlay)) }
            Text(AppText.get(R.string.overlay_permission_help), style = MaterialTheme.typography.bodySmall)
        }
    }
}
