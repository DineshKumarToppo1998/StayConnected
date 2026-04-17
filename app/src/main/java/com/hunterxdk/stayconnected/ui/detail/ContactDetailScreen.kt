package com.hunterxdk.stayconnected.ui.detail

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallReceived
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.hunterxdk.stayconnected.data.local.entities.CallLogEntity
import com.hunterxdk.stayconnected.model.RecurringUnit
import com.hunterxdk.stayconnected.model.ScheduleType
import com.hunterxdk.stayconnected.ui.components.SnoozeBottomSheet
import com.hunterxdk.stayconnected.util.RelativeTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contactId: Long,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    viewModel: ContactDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var showSnoozeSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogCallSheet by remember { mutableStateOf(false) }
    var callLogToDelete by remember { mutableStateOf<CallLogEntity?>(null) }
    var callLogToEdit by remember { mutableStateOf<CallLogEntity?>(null) }

    // Navigate back once contact is deleted
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onBackClick()
    }

    // ── Delete contact confirmation dialog ───────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete contact?") },
            text = {
                Text(
                    "Remove ${uiState.contact?.name ?: "this contact"} and all their " +
                            "reminders and call history? This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteContact()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Delete call log confirmation dialog ──────────────────────────────
    callLogToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { callLogToDelete = null },
            title = { Text("Delete call log?") },
            text = {
                Text(
                    "Remove the call logged on ${RelativeTimeFormatter.formatDateTime(log.calledAt)}? " +
                            "This cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCallLog(log)
                        callLogToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { callLogToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Detail", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = colorScheme.primary)
            }
            return@Scaffold
        }

        val contact = uiState.contact
        val reminder = uiState.reminder

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Profile card ──────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    color = colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (contact?.photoUri != null) {
                                AsyncImage(
                                    model = contact.photoUri,
                                    contentDescription = contact.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(96.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                val initials = contact?.name
                                    ?.split(" ")
                                    ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    ?.take(2)
                                    ?.joinToString("") ?: "?"
                                Text(
                                    text = initials,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = contact?.name ?: "",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Group + tag badges
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (contact != null) {
                                Badge(
                                    text = contact.primaryGroup.name,
                                    color = colorScheme.secondaryContainer,
                                    textColor = colorScheme.onSecondaryContainer
                                )
                                if (contact.isVip) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(
                                        text = "\u2B50 VIP",
                                        color = Color(0xFFFFC107),
                                        textColor = Color(0xFF5D4037)
                                    )
                                }
                                contact.tags.take(3).forEach { tag ->
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Badge(
                                        text = tag,
                                        color = colorScheme.surfaceContainerLow,
                                        textColor = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Overdue banner ─────────────────────────────────────────────
            if (uiState.isOverdue && reminder != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Overdue — was due ${RelativeTimeFormatter.formatDateTime(reminder.nextReminderAt)}",
                                color = colorScheme.onErrorContainer,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // ── Action buttons ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionButton(
                        icon = Icons.Default.Phone,
                        label = "CALL NOW",
                        containerColor = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            contact?.phone?.let { phone ->
                                context.startActivity(
                                    Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone"))
                                )
                            }
                        }
                    )
                    ActionButton(
                        icon = Icons.Default.Edit,
                        label = "EDIT",
                        containerColor = colorScheme.surfaceContainerLow,
                        contentColor = colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        onClick = onEditClick
                    )
                    ActionButton(
                        icon = Icons.Default.Delete,
                        label = "DELETE",
                        containerColor = colorScheme.surfaceContainerLow,
                        contentColor = colorScheme.error,
                        modifier = Modifier.weight(1f),
                        onClick = { showDeleteDialog = true }
                    )
                }
                Spacer(modifier = Modifier.height(28.dp))
            }

            // ── Next reminder card ─────────────────────────────────────────
            if (reminder != null) {
                item {
                    SectionHeader(title = "NEXT REMINDER")
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
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = RelativeTimeFormatter.formatDateTime(reminder.nextReminderAt),
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = scheduleLabel(reminder.scheduleType, reminder.recurringUnit, reminder.intervalDays),
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            TextButton(onClick = { showSnoozeSheet = true }) {
                                Text("Edit", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // ── Notes card ─────────────────────────────────────────────────
            item {
                SectionHeader(
                    title = "NOTES",
                    action = {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        }
                    }
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
                ) {
                    val notes = contact?.notes
                    if (!notes.isNullOrBlank()) {
                        Text(
                            text = "\"$notes\"",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            color = colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No notes yet. Tap Edit to add things to talk about.",
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // ── Call history ───────────────────────────────────────────────
            item {
                SectionHeader(title = "CALL HISTORY")
            }

            if (uiState.callLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No calls logged yet.",
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(uiState.callLogs, key = { it.id }) { log ->
                    CallHistoryItem(
                        dateTime = RelativeTimeFormatter.formatDateTime(log.calledAt),
                        duration = RelativeTimeFormatter.formatDuration(log.durationSeconds),
                        isManual = log.markedManually,
                        colorScheme = colorScheme,
                        onEditClick = if (log.markedManually) ({ callLogToEdit = log }) else null,
                        onDeleteClick = { callLogToDelete = log }
                    )
                }
            }

            // ── Log call manually button ───────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showLogCallSheet = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                ) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log a call manually", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }

    // ── Snooze bottom sheet ───────────────────────────────────────────────
    if (showSnoozeSheet) {
        SnoozeBottomSheet(
            contactName = uiState.contact?.name ?: "",
            onDismiss = { showSnoozeSheet = false },
            onSetReminder = { newNextAt ->
                viewModel.snoozeReminder(newNextAt)
                showSnoozeSheet = false
            }
        )
    }

    // ── Log call bottom sheet ─────────────────────────────────────────────
    if (showLogCallSheet) {
        LogCallBottomSheet(
            contactName = uiState.contact?.name ?: "",
            onDismiss = { showLogCallSheet = false },
            onConfirm = { calledAt, durationSeconds ->
                viewModel.logCallManually(calledAt, durationSeconds)
                showLogCallSheet = false
            }
        )
    }

    // ── Edit call log bottom sheet ────────────────────────────────────────
    callLogToEdit?.let { log ->
        LogCallBottomSheet(
            contactName = uiState.contact?.name ?: "",
            initialCalledAt = log.calledAt,
            initialDurationSeconds = log.durationSeconds,
            title = "Edit call log",
            confirmLabel = "Save Changes",
            onDismiss = { callLogToEdit = null },
            onConfirm = { calledAt, durationSeconds ->
                viewModel.updateCallLog(log, calledAt, durationSeconds)
                callLogToEdit = null
            }
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun scheduleLabel(
    type: ScheduleType,
    recurringUnit: RecurringUnit?,
    intervalDays: Int?
): String = when (type) {
    ScheduleType.MANUAL    -> "One-time"
    ScheduleType.RECURRING -> when (recurringUnit) {
        RecurringUnit.WEEKLY   -> "Recurring • Weekly"
        RecurringUnit.BIWEEKLY -> "Recurring • Every 2 weeks"
        RecurringUnit.MONTHLY  -> "Recurring • Monthly"
        null                   -> "Recurring"
    }
    ScheduleType.INTERVAL  -> "Every ${intervalDays ?: "?"} days"
}

@Composable
fun Badge(text: String, color: Color, textColor: Color) {
    Surface(color = color, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = contentColor)
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        action?.invoke()
    }
}

@Composable
private fun CallHistoryItem(
    dateTime: String,
    duration: String,
    isManual: Boolean,
    colorScheme: ColorScheme,
    onEditClick: (() -> Unit)?,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CallReceived,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = dateTime, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (duration != "0 sec") {
                    Text(
                        text = duration,
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Surface(
                color = if (isManual) colorScheme.surfaceContainerLow else colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp),
                border = if (isManual) androidx.compose.foundation.BorderStroke(
                    1.dp, colorScheme.outline.copy(alpha = 0.4f)
                ) else null
            ) {
                Text(
                    text = if (isManual) "MANUAL" else "AUTO",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isManual) colorScheme.onSurfaceVariant else colorScheme.onPrimaryContainer
                )
            }
            if (onEditClick != null) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = "Edit call log",
                        tint = colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Delete call log",
                    tint = colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogCallBottomSheet(
    contactName: String,
    initialCalledAt: Long = System.currentTimeMillis(),
    initialDurationSeconds: Long = 0L,
    title: String = "Log a call",
    confirmLabel: String = "Log Call",
    onDismiss: () -> Unit,
    onConfirm: (calledAt: Long, durationSeconds: Long) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val initCal = remember { Calendar.getInstance().apply { timeInMillis = initialCalledAt } }

    var selectedDateMillis by remember { mutableLongStateOf(initialCalledAt) }
    var selectedHour by remember { mutableIntStateOf(initCal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(initCal.get(Calendar.MINUTE)) }
    val initialMinutes = if (initialDurationSeconds > 0L) (initialDurationSeconds / 60L).toString() else ""
    var durationText by remember { mutableStateOf(initialMinutes) }

    fun openTimePicker() {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                selectedHour = hour
                selectedMinute = minute
            },
            selectedHour, selectedMinute, false
        ).show()
    }

    fun openDatePicker() {
        val initCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val cal = Calendar.getInstance().apply {
                    set(year, month, day, selectedHour, selectedMinute, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                selectedDateMillis = cal.timeInMillis
            },
            initCal.get(Calendar.YEAR),
            initCal.get(Calendar.MONTH),
            initCal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    val displayDate = RelativeTimeFormatter.formatDate(selectedDateMillis)
    val displayTime = RelativeTimeFormatter.formatTime(
        Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
            set(Calendar.HOUR_OF_DAY, selectedHour)
            set(Calendar.MINUTE, selectedMinute)
        }.timeInMillis
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surfaceContainerLow.copy(alpha = 0.95f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Text(
                text = "For $contactName",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "WHEN DID YOU CALL?",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    onClick = { openDatePicker() },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(displayDate, fontSize = 13.sp, color = colorScheme.onPrimaryContainer)
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Pick date",
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Card(
                    onClick = { openTimePicker() },
                    modifier = Modifier.weight(1f).height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(displayTime, fontSize = 13.sp, color = colorScheme.onPrimaryContainer)
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Pick time",
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "DURATION (OPTIONAL)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = durationText,
                onValueChange = { durationText = it.filter { c -> c.isDigit() }.take(3) },
                placeholder = { Text("Duration in minutes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = selectedDateMillis
                        set(Calendar.HOUR_OF_DAY, selectedHour)
                        set(Calendar.MINUTE, selectedMinute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val durationSeconds = (durationText.toLongOrNull() ?: 0L) * 60L
                    onConfirm(cal.timeInMillis, durationSeconds)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(confirmLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
