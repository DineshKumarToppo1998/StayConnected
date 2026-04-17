package com.hunterxdk.stayconnected.ui.addedit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.hunterxdk.stayconnected.model.ContactGroup
import com.hunterxdk.stayconnected.model.RecurringUnit
import com.hunterxdk.stayconnected.model.ScheduleType
import com.hunterxdk.stayconnected.util.RelativeTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditContactScreen(
    contactId: Long? = null,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    viewModel: AddEditContactViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    // Navigate away once save completes
    LaunchedEffect(uiState.savedSuccessfully) {
        if (uiState.savedSuccessfully) onSaveClick()
    }

    // Phonebook contact picker
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri -> uri?.let { viewModel.onPhonebookContactSelected(it) } }

    // Tag input state (local — not persisted until saved)
    var tagInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.contactId == null) "Add Reminder" else "Edit Reminder",
                        fontWeight = FontWeight.Bold
                    )
                },
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

            // ── SELECT CONTACT ────────────────────────────────────────────
            SectionLabel(text = "SELECT CONTACT")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.photoUri != null) {
                            AsyncImage(
                                model = uiState.photoUri,
                                contentDescription = uiState.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            val initials = uiState.name
                                .split(" ")
                                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                .take(2)
                                .joinToString("")
                                .ifEmpty { "?" }
                            Text(
                                text = initials,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.name.ifEmpty { "Tap to select a contact" },
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.name.isEmpty())
                                colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else
                                colorScheme.onSurface
                        )
                        if (uiState.phone.isNotEmpty()) {
                            Text(
                                text = uiState.phone,
                                fontSize = 12.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TextButton(
                        onClick = { contactPickerLauncher.launch(null) },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Text(
                            text = if (uiState.name.isEmpty()) "Select" else "Change",
                            color = colorScheme.primary
                        )
                    }
                }
            }

            // ── GROUP ─────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel(text = "GROUP")
            val groups = listOf(ContactGroup.FAMILY, ContactGroup.FRIENDS, ContactGroup.WORK)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                groups.forEachIndexed { index, group ->
                    SegmentedButton(
                        selected = uiState.group == group,
                        onClick = { viewModel.updateGroup(group) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = groups.size)
                    ) {
                        Text(text = group.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                }
            }

            // ── VIP CONTACT ─────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (uiState.isVip) Color(0xFFFFC107).copy(alpha = 0.12f)
                    else colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (uiState.isVip) Color(0xFFFFC107) else colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "VIP Contact",
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isVip) Color(0xFFFFC107) else colorScheme.onSurface
                        )
                        Text(
                            text = "Prioritise reminders and highlight in list",
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = uiState.isVip,
                        onCheckedChange = { viewModel.updateIsVip(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFFC107),
                            checkedTrackColor = Color(0xFFFFC107).copy(alpha = 0.5f)
                        )
                    )
                }
            }

            // ── CONTEXTUAL TAGS ───────────────────────────────────────────
            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel(text = "CONTEXTUAL TAGS")
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.tags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { viewModel.removeTag(tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove $tag",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    placeholder = { Text("Add tag...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = colorScheme.surface,
                        focusedContainerColor = colorScheme.surface
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = {
                        viewModel.addTag(tagInput)
                        tagInput = ""
                    },
                    enabled = tagInput.isNotBlank()
                ) {
                    Text("Add")
                }
            }

            // ── REMINDER SCHEDULE ─────────────────────────────────────────
            Spacer(modifier = Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(text = "REMINDER SCHEDULE")
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Manual card
            ScheduleOption(
                icon = Icons.Default.CalendarToday,
                title = "Manual",
                description = "Set a specific one-time date",
                selected = uiState.scheduleType == ScheduleType.MANUAL,
                onClick = { viewModel.updateScheduleType(ScheduleType.MANUAL) }
            )
            if (uiState.scheduleType == ScheduleType.MANUAL) {
                ManualScheduleSubUi(
                    currentDateTime = uiState.manualDateTime,
                    onDateTimeSelected = { viewModel.updateManualDateTime(it) },
                    context = context,
                    colorScheme = colorScheme
                )
            }

            // Recurring card
            ScheduleOption(
                icon = Icons.Default.Refresh,
                title = "Recurring",
                description = "Repeat on a fixed cadence",
                selected = uiState.scheduleType == ScheduleType.RECURRING,
                onClick = { viewModel.updateScheduleType(ScheduleType.RECURRING) }
            )
            if (uiState.scheduleType == ScheduleType.RECURRING) {
                RecurringScheduleSubUi(
                    selected = uiState.recurringUnit,
                    onSelect = { viewModel.updateRecurringUnit(it) },
                    colorScheme = colorScheme
                )
            }

            // Fixed Interval card
            ScheduleOption(
                icon = Icons.Default.History,
                title = "Fixed Interval",
                description = "Every X days",
                selected = uiState.scheduleType == ScheduleType.INTERVAL,
                onClick = { viewModel.updateScheduleType(ScheduleType.INTERVAL) }
            )
            if (uiState.scheduleType == ScheduleType.INTERVAL) {
                IntervalScheduleSubUi(
                    days = uiState.intervalDays,
                    onDaysChange = { viewModel.updateIntervalDays(it) },
                    colorScheme = colorScheme
                )
            }

            // ── NOTES ─────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel(text = "NOTES")
            OutlinedTextField(
                value = uiState.notes,
                onValueChange = { viewModel.updateNotes(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(top = 8.dp),
                placeholder = { Text("Things to talk about...") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colorScheme.surface,
                    focusedContainerColor = colorScheme.surface
                )
            )

            // ── ERROR ─────────────────────────────────────────────────────
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
            }

            // ── SAVE BUTTON ───────────────────────────────────────────────
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { viewModel.saveContact() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(28.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Reminder", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// ── Schedule sub-UIs ──────────────────────────────────────────────────────────

@Composable
private fun ManualScheduleSubUi(
    currentDateTime: Long?,
    onDateTimeSelected: (Long) -> Unit,
    context: Context,
    colorScheme: ColorScheme
) {
    // Store intermediate date so we can combine with time
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }

    fun openTimePicker(dateMillis: Long) {
        val initCal = Calendar.getInstance().apply {
            if (currentDateTime != null) timeInMillis = currentDateTime
        }
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = dateMillis
                // DatePickerDialog returns local-midnight; set hours/minutes
                val yr  = cal.get(Calendar.YEAR)
                val mo  = cal.get(Calendar.MONTH)
                val day = cal.get(Calendar.DAY_OF_MONTH)
                cal.set(yr, mo, day, hour, minute, 0)
                cal.set(Calendar.MILLISECOND, 0)
                onDateTimeSelected(cal.timeInMillis)
            },
            initCal.get(Calendar.HOUR_OF_DAY),
            initCal.get(Calendar.MINUTE),
            false
        ).show()
    }

    fun openDatePicker() {
        val initCal = Calendar.getInstance().apply {
            if (currentDateTime != null) timeInMillis = currentDateTime
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                pendingDateMillis = cal.timeInMillis
                openTimePicker(cal.timeInMillis)
            },
            initCal.get(Calendar.YEAR),
            initCal.get(Calendar.MONTH),
            initCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Quick chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val todayAt9 = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val tomorrowAt9 = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                SuggestionChip(
                    onClick = { onDateTimeSelected(todayAt9) },
                    label = { Text("Today 9 AM") }
                )
                SuggestionChip(
                    onClick = { onDateTimeSelected(tomorrowAt9) },
                    label = { Text("Tomorrow 9 AM") }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Custom date/time row
            OutlinedButton(
                onClick = { openDatePicker() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentDateTime != null)
                        RelativeTimeFormatter.formatDateTime(currentDateTime)
                    else
                        "Pick date & time"
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringScheduleSubUi(
    selected: RecurringUnit,
    onSelect: (RecurringUnit) -> Unit,
    colorScheme: ColorScheme
) {
    var expanded by remember { mutableStateOf(false) }

    val labelFor = { unit: RecurringUnit ->
        when (unit) {
            RecurringUnit.WEEKLY   -> "Every Week"
            RecurringUnit.BIWEEKLY -> "Every 2 Weeks"
            RecurringUnit.MONTHLY  -> "Every Month"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            OutlinedTextField(
                value = labelFor(selected),
                onValueChange = {},
                readOnly = true,
                label = { Text("Repeat cadence") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                shape = RoundedCornerShape(10.dp)
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                listOf(RecurringUnit.WEEKLY, RecurringUnit.BIWEEKLY, RecurringUnit.MONTHLY)
                    .forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(labelFor(unit)) },
                            onClick = {
                                onSelect(unit)
                                expanded = false
                            }
                        )
                    }
            }
        }
    }
}

@Composable
private fun IntervalScheduleSubUi(
    days: Int,
    onDaysChange: (Int) -> Unit,
    colorScheme: ColorScheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Remind me every",
                color = colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = days.toString(),
                onValueChange = { onDaysChange(it.toIntOrNull()?.coerceIn(1, 365) ?: days) },
                modifier = Modifier.width(80.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colorScheme.surface,
                    focusedContainerColor = colorScheme.surface
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("days", color = colorScheme.onSurface)
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp
    )
}

@Composable
fun ScheduleOption(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colorScheme.primaryContainer
            else colorScheme.surfaceContainerLow
        ),
        border = if (selected)
            androidx.compose.foundation.BorderStroke(2.dp, colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) colorScheme.primary else colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colorScheme.primary
                )
            }
        }
    }
}
