package com.drunkbatya.drunksettings.ui

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.drunkbatya.drunksettings.R
import com.drunkbatya.drunksettings.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

@Composable
fun SettingsApp() {
    val backStack = remember { mutableStateListOf<Screen>(Screen.Main) }
    val current = backStack.last()
    val onBack = rememberUpdatedState {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    BackHandler(enabled = backStack.size > 1) {
        onBack.value.invoke()
    }

    when (current) {
        Screen.Main -> MainScreen(
            onOpenNotifications = { backStack.add(Screen.Notifications) },
            onOpenDebug = { backStack.add(Screen.Debug) }
        )
        Screen.Notifications -> NotificationsScreen(
            onBack = { onBack.value.invoke() },
            onOpenGeneral = { backStack.add(Screen.General) },
            onOpenApps = { backStack.add(Screen.AppNotifications) }
        )
        Screen.Debug -> DebugScreen(
            onBack = { onBack.value.invoke() },
            onOpenNotifications = { backStack.add(Screen.DebugNotifications) }
        )
        Screen.General -> GeneralScreen(
            onBack = { onBack.value.invoke() },
            onOpenMinSound = { backStack.add(Screen.GeneralMinSound) }
        )
        Screen.GeneralMinSound -> {
            val context = LocalContext.current
            val selected = rememberGeneralMinSound()
            MinSoundOptionsScreen(
                selectedSeconds = selected.intValue,
                onBack = { onBack.value.invoke() },
                onSelect = { seconds ->
                    selected.intValue = seconds
                    SettingsStore.setGeneralMinSoundTimeout(context, seconds)
                }
            )
        }
        Screen.AppNotifications -> AppNotificationsScreen(
            onBack = { onBack.value.invoke() },
            onOpenApp = { app ->
                backStack.add(Screen.AppDetail(app.packageName, app.label))
            }
        )
        Screen.DebugNotifications -> DebugNotificationsScreen(
            onBack = { onBack.value.invoke() }
        )
        is Screen.AppDetail -> {
            val app = current
            AppDetailScreen(
                appLabel = app.label,
                appPackage = app.packageName,
                onBack = { onBack.value.invoke() },
                onOpenMinSound = { backStack.add(Screen.AppMinSound(app.packageName, app.label)) }
            )
        }
        is Screen.AppMinSound -> {
            val context = LocalContext.current
            val app = current
            val selected = rememberAppTimeout(app.packageName)
            MinSoundOptionsScreen(
                selectedSeconds = selected.intValue,
                onBack = { onBack.value.invoke() },
                onSelect = { seconds ->
                    selected.intValue = seconds
                    SettingsStore.setAppMinSoundTimeout(context, app.packageName, seconds)
                }
            )
        }
    }
}

private sealed interface Screen {
    data object Main : Screen
    data object Notifications : Screen
    data object Debug : Screen
    data object DebugNotifications : Screen
    data object General : Screen
    data object GeneralMinSound : Screen
    data object AppNotifications : Screen
    data class AppDetail(val packageName: String, val label: String) : Screen
    data class AppMinSound(val packageName: String, val label: String) : Screen
}

private data class TimeoutOption(val seconds: Int, val label: String)

private val timeoutOptions = listOf(
    TimeoutOption(0, "No restrictions"),
    TimeoutOption(10, "10 seconds"),
    TimeoutOption(30, "30 seconds"),
    TimeoutOption(60, "1 minute"),
    TimeoutOption(300, "5 minutes"),
    TimeoutOption(900, "15 minutes"),
    TimeoutOption(1800, "30 minutes")
)

private data class AppInfo(
    val packageName: String,
    val label: String,
    val isUserApp: Boolean,
    val icon: ImageBitmap?
)

private const val DEBUG_CHANNEL_ID = "debug_sound_notifications"
private const val MIN_SOUND_TITLE = "Minimum time between notification sounds"

@Composable
private fun MainScreen(
    onOpenNotifications: () -> Unit,
    onOpenDebug: () -> Unit
) {
    SettingsScaffold(title = "Settings") { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = "Notifications",
                    onClick = onOpenNotifications
                )
            }
            item {
                SettingsListItem(
                    title = "Debug",
                    onClick = onOpenDebug
                )
            }
        }
    }
}

@Composable
private fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenGeneral: () -> Unit,
    onOpenApps: () -> Unit
) {
    SettingsScaffold(title = "Notifications", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = "General",
                    onClick = onOpenGeneral
                )
            }
            item {
                SettingsListItem(
                    title = "App notifications",
                    onClick = onOpenApps
                )
            }
        }
    }
}

@Composable
private fun DebugScreen(
    onBack: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    SettingsScaffold(title = "Debug", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = "Notifications",
                    onClick = onOpenNotifications
                )
            }
        }
    }
}

@Composable
private fun DebugNotificationsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            sendTestNotification(context)
        }
    }
    val onTestNotification = {
        if (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            sendTestNotification(context)
        } else {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    SettingsScaffold(title = "Notifications", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = "Test sound notification",
                    onClick = onTestNotification
                )
            }
        }
    }
}

@Composable
private fun GeneralScreen(
    onBack: () -> Unit,
    onOpenMinSound: () -> Unit
) {
    val generalSeconds = rememberGeneralMinSound().intValue
    SettingsScaffold(title = "General", onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = MIN_SOUND_TITLE,
                    summary = timeoutLabel(generalSeconds),
                    onClick = onOpenMinSound
                )
            }
        }
    }
}

@Composable
private fun AppNotificationsScreen(
    onBack: () -> Unit,
    onOpenApp: (AppInfo) -> Unit
) {
    val apps = rememberInstalledApps()
    var showSystemApps by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    SettingsScaffold(
        title = "App notifications",
        onBack = onBack,
        actions = {
            IconButton(onClick = { menuExpanded = true }) {
                Text("...")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Show system apps") },
                    onClick = {
                        showSystemApps = !showSystemApps
                        menuExpanded = false
                    },
                    trailingIcon = {
                        Checkbox(
                            checked = showSystemApps,
                            onCheckedChange = null
                        )
                    }
                )
            }
        }
    ) { padding ->
        if (apps.value == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@SettingsScaffold
        }

        val visibleApps = apps.value.orEmpty().filter { showSystemApps || it.isUserApp }
        LazyColumn(contentPadding = padding) {
            items(visibleApps, key = { it.packageName }) { app ->
                SettingsListItem(
                    title = app.label,
                    summary = app.packageName,
                    onClick = { onOpenApp(app) },
                    leadingContent = { AppIcon(app) }
                )
            }
        }
    }
}

@Composable
private fun AppDetailScreen(
    appLabel: String,
    appPackage: String,
    onBack: () -> Unit,
    onOpenMinSound: () -> Unit
) {
    val currentSeconds = rememberAppTimeout(appPackage).intValue
    SettingsScaffold(title = appLabel, onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingsListItem(
                    title = MIN_SOUND_TITLE,
                    summary = timeoutLabel(currentSeconds),
                    onClick = onOpenMinSound
                )
            }
        }
    }
}

@Composable
private fun MinSoundOptionsScreen(
    selectedSeconds: Int,
    onBack: () -> Unit,
    onSelect: (Int) -> Unit
) {
    SettingsScaffold(title = MIN_SOUND_TITLE, onBack = onBack) { padding ->
        LazyColumn(contentPadding = padding) {
            items(timeoutOptions, key = { it.seconds }) { option ->
                OptionListItem(
                    label = option.label,
                    selected = selectedSeconds == option.seconds,
                    onClick = { onSelect(option.seconds) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Text("<", fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                actions = actions
            )
        },
        content = content
    )
}

@Composable
private fun SettingsListItem(
    title: String,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    } else {
        Modifier.fillMaxWidth()
    }

    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = leadingContent,
        modifier = modifier
    )
    HorizontalDivider()
}

@Composable
private fun OptionListItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
    HorizontalDivider()
}

@Composable
private fun AppIcon(app: AppInfo) {
    val icon = app.icon
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = app.label,
            modifier = Modifier.size(40.dp)
        )
        return
    }
    val color = remember(app.packageName) { avatarColor(app.packageName) }
    val initial = remember(app.label) {
        val trimmed = app.label.trim()
        if (trimmed.isEmpty()) "?" else trimmed.substring(0, 1).uppercase(Locale.getDefault())
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun rememberInstalledApps() = run {
    val context = LocalContext.current
    produceState<List<AppInfo>?>(initialValue = null, context) {
        value = withContext(Dispatchers.IO) {
            loadInstalledApps(context)
        }
    }
}

private fun loadInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
    val collator = Collator.getInstance()
    val iconSizePx = (40f * context.resources.displayMetrics.density + 0.5f).toInt()
    return apps
        .map { info ->
            val label = pm.getApplicationLabel(info).toString()
            val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystem = (info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            val isUserApp = !isSystem && !isUpdatedSystem
            val icon = runCatching {
                pm.getApplicationIcon(info)
                    .toBitmap(iconSizePx, iconSizePx)
                    .asImageBitmap()
            }.getOrNull()
            AppInfo(info.packageName, label, isUserApp, icon)
        }
        .sortedWith { a, b -> collator.compare(a.label, b.label) }
}

@Composable
private fun rememberGeneralMinSound(): MutableIntState {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(SettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val state = remember {
        mutableIntStateOf(
            prefs.getInt(SettingsStore.KEY_GENERAL_MIN_SOUND, SettingsStore.DEFAULT_MIN_SOUND)
        )
    }
    DisposableEffect(SettingsStore.KEY_GENERAL_MIN_SOUND) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == SettingsStore.KEY_GENERAL_MIN_SOUND) {
                state.intValue = prefs.getInt(
                    SettingsStore.KEY_GENERAL_MIN_SOUND,
                    SettingsStore.DEFAULT_MIN_SOUND
                )
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

@Composable
private fun rememberAppTimeout(packageName: String): MutableIntState {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(SettingsStore.PREFS_NAME, Context.MODE_PRIVATE)
    }
    val state = remember { mutableIntStateOf(SettingsStore.getAppOrGeneralTimeout(context, packageName)) }
    DisposableEffect(packageName) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
            if (changedKey == SettingsStore.KEY_GENERAL_MIN_SOUND ||
                changedKey == SettingsStore.appKey(packageName)
            ) {
                state.intValue = SettingsStore.getAppOrGeneralTimeout(context, packageName)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return state
}

private fun sendTestNotification(context: Context) {
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val existing = manager.getNotificationChannel(DEBUG_CHANNEL_ID)
        if (existing == null) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                DEBUG_CHANNEL_ID,
                "Debug Sound",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Debug notification channel with sound"
                setSound(soundUri, audioAttributes)
            }
            manager.createNotificationChannel(channel)
        }
    }

    val notification = NotificationCompat.Builder(context, DEBUG_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Test sound notification")
        .setContentText("DrunkSettings debug notification")
        .setAutoCancel(true)
        .setSound(soundUri)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()

    val notificationId = (System.currentTimeMillis() and 0xFFFFFF).toInt()
    manager.notify(notificationId, notification)
}

private fun timeoutLabel(seconds: Int): String {
    return timeoutOptions.firstOrNull { it.seconds == seconds }?.label ?: "$seconds seconds"
}

private fun avatarColor(seed: String): Color {
    val hash = seed.hashCode()
    val hue = (hash and 0xFF) / 255f * 360f
    return Color.hsv(hue, 0.35f, 0.85f)
}
