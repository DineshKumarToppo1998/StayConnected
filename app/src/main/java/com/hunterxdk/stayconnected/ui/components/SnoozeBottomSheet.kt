package com.hunterxdk.stayconnected.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hunterxdk.stayconnected.util.RelativeTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeBottomSheet(
    contactName: String,
    onDismiss: () -> Unit,
    onSetReminder: (Long) -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var selectedSnooze by remember { mutableStateOf("2 Hours") }

    var customDateMillis by remember { mutableStateOf<Long?>(null) }
    var customHour by remember { mutableStateOf(9) }
    var customMinute by remember { mutableStateOf(0) }
    var hasCustomTime by remember { mutableStateOf(false) }

    fun openTimePicker(dateMs: Long) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = dateMs
                val yr  = cal.get(Calendar.YEAR)
                val mo  = cal.get(Calendar.MONTH)
                val day = cal.get(Calendar.DAY_OF_MONTH)
                cal.set(yr, mo, day, hour, minute, 0)
                cal.set(Calendar.MILLISECOND, 0)
                customDateMillis = cal.timeInMillis
                customHour = hour
                customMinute = minute
                hasCustomTime = true
                selectedSnooze = ""
            },
            customHour,
            customMinute,
            false
        ).show()
    }

    fun openDatePicker() {
        val initCal = Calendar.getInstance().apply {
            customDateMillis?.let { timeInMillis = it }
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val cal = Calendar.getInstance()
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                openTimePicker(cal.timeInMillis)
            },
            initCal.get(Calendar.YEAR),
            initCal.get(Calendar.MONTH),
            initCal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Glassmorphism: semi-transparent container
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
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
                text = "Remind me again...",
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

            // Quick snooze chips — 2×2 grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SnoozeOption(
                    label = "1 Hour",
                    selected = selectedSnooze == "1 Hour",
                    onClick = { selectedSnooze = "1 Hour"; hasCustomTime = false },
                    modifier = Modifier.weight(1f),
                    colorScheme = colorScheme
                )
                SnoozeOption(
                    label = "2 Hours",
                    selected = selectedSnooze == "2 Hours",
                    onClick = { selectedSnooze = "2 Hours"; hasCustomTime = false },
                    modifier = Modifier.weight(1f),
                    colorScheme = colorScheme
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SnoozeOption(
                    label = "4 Hours",
                    selected = selectedSnooze == "4 Hours",
                    onClick = { selectedSnooze = "4 Hours"; hasCustomTime = false },
                    modifier = Modifier.weight(1f),
                    colorScheme = colorScheme
                )
                SnoozeOption(
                    label = "Tomorrow",
                    selected = selectedSnooze == "Tomorrow",
                    onClick = { selectedSnooze = "Tomorrow"; hasCustomTime = false },
                    modifier = Modifier.weight(1f),
                    colorScheme = colorScheme
                )
            }

            // 24dp tonal spacing instead of a 1px divider
            Spacer(modifier = Modifier.height(28.dp))

            // Custom date/time picker
            Text(
                text = "PICK A SPECIFIC TIME",
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
                // Date card
                Card(
                    onClick = { openDatePicker() },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasCustomTime)
                            colorScheme.primaryContainer
                        else
                            colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (customDateMillis != null)
                                RelativeTimeFormatter.formatDate(customDateMillis!!)
                            else
                                "Pick date",
                            fontSize = 13.sp,
                            color = if (hasCustomTime)
                                colorScheme.onPrimaryContainer
                            else
                                colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = "Pick date",
                            tint = if (hasCustomTime)
                                colorScheme.onPrimaryContainer
                            else
                                colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Time card
                Card(
                    onClick = {
                        val base = customDateMillis ?: Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                        }.timeInMillis
                        openTimePicker(base)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasCustomTime)
                            colorScheme.primaryContainer
                        else
                            colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (hasCustomTime)
                                RelativeTimeFormatter.formatTime(
                                    Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, customHour)
                                        set(Calendar.MINUTE, customMinute)
                                    }.timeInMillis
                                )
                            else
                                "Pick time",
                            fontSize = 13.sp,
                            color = if (hasCustomTime)
                                colorScheme.onPrimaryContainer
                            else
                                colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = "Pick time",
                            tint = if (hasCustomTime)
                                colorScheme.onPrimaryContainer
                            else
                                colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val timestamp: Long = if (hasCustomTime && customDateMillis != null) {
                        customDateMillis!!
                    } else {
                        when (selectedSnooze) {
                            "1 Hour"   -> now + 3_600_000L
                            "2 Hours"  -> now + 7_200_000L
                            "4 Hours"  -> now + 14_400_000L
                            "Tomorrow" -> Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            else       -> now + 7_200_000L
                        }
                    }
                    onSetReminder(timestamp)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text("Set Reminder", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SnoozeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    colorScheme: ColorScheme
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colorScheme.primary else colorScheme.surfaceContainerLow
        )
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                color = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
            )
        }
    }
}
