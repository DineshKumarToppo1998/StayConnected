package com.hunterxdk.stayconnected.ui.settings

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // Dialog visibility states
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showGroupDialog  by remember { mutableStateOf(false) }

    // ── Default Snooze dialog ─────────────────────────────────────────────
    if (showSnoozeDialog) {
        val snoozeOptions = listOf(30 to "30 minutes", 60 to "1 hour", 120 to "2 hours", 240 to "4 hours")
        AlertDialog(
            onDismissRequest = { showSnoozeDialog = false },
            title = { Text("Default Snooze Duration") },
            text = {
                Column {
                    snoozeOptions.forEach { (minutes, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = settings.defaultSnoozeMinutes == minutes,
                                onClick = {
                                    viewModel.updateDefaultSnooze(minutes)
                                    showSnoozeDialog = false
                                }
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSnoozeDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Default Group dialog ──────────────────────────────────────────────
    if (showGroupDialog) {
        val groupOptions = listOf("FAMILY" to "Family", "FRIENDS" to "Friends", "WORK" to "Work")
        AlertDialog(
            onDismissRequest = { showGroupDialog = false },
            title = { Text("Default Group") },
            text = {
                Column {
                    groupOptions.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = settings.defaultGroup == key,
                                onClick = {
                                    viewModel.updateDefaultGroup(key)
                                    showGroupDialog = false
                                }
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showGroupDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── APPEARANCE ────────────────────────────────────────────────
            SettingsSectionHeader(title = "APPEARANCE")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (settings.appTheme == "DARK")
                                Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Dark Theme", fontWeight = FontWeight.Bold)
                            Text(
                                if (settings.appTheme == "DARK") "Dark mode is on" else "Light mode is on",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = settings.appTheme == "DARK",
                            onCheckedChange = { isDark ->
                                viewModel.updateAppTheme(if (isDark) "DARK" else "LIGHT")
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colorScheme.onPrimary,
                                checkedTrackColor = colorScheme.primary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── NOTIFICATIONS ─────────────────────────────────────────────
            SettingsSectionHeader(title = "NOTIFICATIONS")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Quiet Hours toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quiet Hours", fontWeight = FontWeight.Bold)
                            Text(
                                "Mute reminders during specific times",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = settings.quietHoursEnabled,
                            onCheckedChange = { viewModel.updateQuietHoursEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colorScheme.onPrimary,
                                checkedTrackColor = colorScheme.primary
                            )
                        )
                    }

                    // From / To sub-rows
                    if (settings.quietHoursEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))
                        TimeSettingRow(
                            label = "From",
                            time = formatTime24h(settings.quietWindowStart),
                            onEditClick = {
                                val (h, m) = parseTime(settings.quietWindowStart)
                                TimePickerDialog(context, { _, hour, minute ->
                                    viewModel.updateQuietWindowStart("%02d:%02d".format(hour, minute))
                                }, h, m, false).show()
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TimeSettingRow(
                            label = "To",
                            time = formatTime24h(settings.quietWindowEnd),
                            onEditClick = {
                                val (h, m) = parseTime(settings.quietWindowEnd)
                                TimePickerDialog(context, { _, hour, minute ->
                                    viewModel.updateQuietWindowEnd("%02d:%02d".format(hour, minute))
                                }, h, m, false).show()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Respect System DND toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Respect System DND", fontWeight = FontWeight.Bold)
                            Text(
                                "Sync with your device's Focus settings",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = settings.respectSystemDnd,
                            onCheckedChange = { viewModel.updateRespectDnd(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colorScheme.onPrimary,
                                checkedTrackColor = colorScheme.primary
                            )
                        )
                    }
                }
            }

            // ── REMINDERS ─────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionHeader(title = "REMINDERS")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Default Snooze
                    SettingsClickableRow(
                        label = "Default Snooze",
                        value = formatSnoozeMinutes(settings.defaultSnoozeMinutes),
                        onClick = { showSnoozeDialog = true }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Default Group
                    SettingsClickableRow(
                        label = "Default Group",
                        value = settings.defaultGroup
                            .lowercase()
                            .replaceFirstChar { it.uppercase() },
                        onClick = { showGroupDialog = true }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Auto-detect Calls toggle
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-detect Calls", fontWeight = FontWeight.Bold)
                            Text(
                                "Automatically log calls from your call history",
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        Switch(
                            checked = settings.autoDetectCalls,
                            onCheckedChange = { viewModel.updateAutoDetectCalls(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colorScheme.onPrimary,
                                checkedTrackColor = colorScheme.primary
                            )
                        )
                    }
                }
            }

            // ── ABOUT ──────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(24.dp))
            SettingsSectionHeader(title = "ABOUT")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    // Version
                    Column {
                        Text("Version", fontWeight = FontWeight.Bold)
                        Text(
                            "StayConnected 1.0.0",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Privacy Policy
                    SettingsIconRow(
                        icon = Icons.Default.Policy,
                        label = "Privacy Policy",
                        trailingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = { /* Phase 10: open browser */ }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Made with ❤️ by Dinesh",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun TimeSettingRow(label: String, time: String, onEditClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:  $time",
            color = colorScheme.onSurface,
            fontSize = 14.sp
        )
        IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit $label",
                tint = colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsClickableRow(label: String, value: String, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold)
            Text(
                value,
                fontSize = 12.sp,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
        IconButton(onClick = onClick) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SettingsIconRow(
    icon: ImageVector,
    label: String,
    trailingIcon: ImageVector,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onClick) {
            Icon(
                trailingIcon,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── Formatting helpers ────────────────────────────────────────────────────────

/** Parses "HH:mm" → Pair(hour, minute). */
private fun parseTime(time: String): Pair<Int, Int> {
    val parts = time.split(":")
    return Pair(parts.getOrNull(0)?.toIntOrNull() ?: 0, parts.getOrNull(1)?.toIntOrNull() ?: 0)
}

/** Converts "22:00" → "10:00 PM". */
private fun formatTime24h(time: String): String = try {
    val sdf24 = SimpleDateFormat("HH:mm", Locale.getDefault())
    val sdf12 = SimpleDateFormat("h:mm a", Locale.getDefault())
    sdf12.format(sdf24.parse(time)!!)
} catch (e: Exception) {
    time
}

/** Converts minutes → "30 minutes", "1 hour", "2 hours". */
private fun formatSnoozeMinutes(minutes: Int): String = when {
    minutes < 60         -> "$minutes minutes"
    minutes == 60        -> "1 hour"
    minutes % 60 == 0    -> "${minutes / 60} hours"
    else                 -> "${minutes / 60}h ${minutes % 60}m"
}
