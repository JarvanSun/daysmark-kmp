package com.example.daysmark.android.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.daysmark.android.R
import com.example.daysmark.android.designsystem.Daysmark
import com.example.daysmark.android.designsystem.DaysmarkType
import com.example.daysmark.android.designsystem.Dimensions

/**
 * Notifications, how to add the widget, and the version. All of it is system state, so this screen
 * has no shared ViewModel; it re-reads the permissions every time it resumes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // Bumped on resume and after a permission answer, so the rows read the system again.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        refresh++
        onPauseOrDispose {}
    }
    Scaffold(
        containerColor = Daysmark.colors.canvas,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.onboarding_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader(R.string.settings_notifications_title)
            key(refresh) {
                NotificationsRow(context) { refresh++ }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ExactAlarmsRow(context)
            }
            SectionHeader(R.string.settings_widget_title)
            Text(
                stringResource(R.string.settings_widget_steps),
                style = DaysmarkType.supporting,
                color = Daysmark.colors.textSecondary,
                modifier = Modifier.padding(horizontal = LIST_INSET),
            )
            SectionHeader(R.string.settings_about_title)
            Row(
                title = stringResource(R.string.settings_about_version),
                value = context.versionLabel(),
            )
        }
    }
}

@Composable
private fun NotificationsRow(context: Context, onAnswered: () -> Unit) {
    val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val openSettings = {
        context.startActivity(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        )
    }
    // After a refusal the system answers "no" without asking, so a "no" opens the app's
    // notification settings, where the user can still turn them on.
    val ask =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) onAnswered() else openSettings()
        }
    Row(
        title = stringResource(R.string.settings_notifications_permission),
        value =
            stringResource(
                if (enabled) R.string.settings_allowed else R.string.settings_notifications_off
            ),
        action =
            if (enabled) null
            else
                Pair(stringResource(R.string.settings_turn_on)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        openSettings()
                    }
                },
    )
}

/** Without this, reminders arrive in the system's window around their time, not on the minute. */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
private fun ExactAlarmsRow(context: Context) {
    val allowed = context.getSystemService<AlarmManager>()?.canScheduleExactAlarms() == true
    Row(
        title = stringResource(R.string.settings_exact_alarms),
        value =
            stringResource(
                if (allowed) R.string.settings_allowed else R.string.settings_exact_alarms_off
            ),
        action =
            if (allowed) null
            else
                Pair(stringResource(R.string.settings_allow)) {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData("package:${context.packageName}".toUri())
                    )
                },
    )
}

@Composable
private fun SectionHeader(title: Int) {
    Text(
        stringResource(title),
        style = DaysmarkType.filterLabelSelected,
        color = Daysmark.colors.textSecondary,
        modifier =
            Modifier.padding(
                    start = LIST_INSET,
                    end = LIST_INSET,
                    top = Dimensions.spaceXLarge,
                    bottom = Dimensions.spaceSmall,
                )
                .semantics { heading() },
    )
}

@Composable
private fun Row(title: String, value: String, action: Pair<String, () -> Unit>? = null) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(value) },
        trailingContent =
            action?.let { (label, onClick) ->
                { FilledTonalButton(onClick = onClick) { Text(label) } }
            },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/** "0.1.0 (1)". */
private fun Context.versionLabel(): String {
    val info =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION") // The only form before Android 13.
            packageManager.getPackageInfo(packageName, 0)
        }
    return "${info.versionName} (${info.longVersionCode})"
}

/** Material list items inset their text by 16dp; headers and notes line up with it. */
private val LIST_INSET = 16.dp
