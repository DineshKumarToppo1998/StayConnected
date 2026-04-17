package com.hunterxdk.stayconnected.ui.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hunterxdk.stayconnected.ui.components.ContactItem
import com.hunterxdk.stayconnected.ui.components.StatCard
import com.hunterxdk.stayconnected.ui.components.StreakBanner
import com.hunterxdk.stayconnected.util.NextReminderCalculator
import com.hunterxdk.stayconnected.util.RelativeTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddContactClick: () -> Unit,
    onContactClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    val listState = rememberLazyListState()
    val isScrolling by remember { derivedStateOf { listState.isScrollInProgress } }

    val filters = listOf("ALL", "FAMILY", "FRIENDS", "WORK", "OVERDUE", "VIP")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colorScheme.onSurface,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = colorScheme.surface,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "StayConnected",
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Phase 9: notifications */ }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddContactClick,
                expanded = !isScrolling,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Contact") },
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary,
                shape = RoundedCornerShape(24.dp)
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

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Streak Banner
            item {
                Spacer(modifier = Modifier.height(8.dp))
                StreakBanner(streak = uiState.streak)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Never lose touch with people you care about",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Stat Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "OVERDUE",
                        value = "${uiState.overdueCount}",
                        valueColor = colorScheme.error,
                        onClick = { viewModel.setFilter("OVERDUE") }
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "DUE TODAY",
                        value = "${uiState.dueTodayCount}",
                        valueColor = colorScheme.primary,
                        onClick = { viewModel.setFilter("ALL") }
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "DONE",
                        value = "${uiState.doneThisWeekCount}",
                        onClick = { viewModel.setFilter("ALL") }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filters) { filter ->
                        FilterChip(
                            selected = uiState.activeFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(text = filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorScheme.primaryContainer,
                                selectedLabelColor = colorScheme.onPrimaryContainer
                            ),
                            border = null,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Ghost section label
            item {
                Text(
                    text = "Contacts",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray.copy(alpha = 0.4f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Empty state
            if (uiState.filteredContacts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No contacts yet",
                                color = colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Tap + to add someone to stay in touch with",
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Contact list — real data
            itemsIndexed(
                items = uiState.filteredContacts,
                key = { _, contact -> contact.id }
            ) { index, contact ->
                val reminder = uiState.reminders[contact.id]
                val lastCall = uiState.lastCalls[contact.id]

                val isOverdue = reminder?.let {
                    NextReminderCalculator.isOverdue(it.nextReminderAt)
                } ?: false

                val nextReminderText = reminder?.let {
                    RelativeTimeFormatter.formatDate(it.nextReminderAt)
                } ?: "Not set"

                val lastCalledText = RelativeTimeFormatter.format(lastCall?.calledAt)

                ContactItem(
                    name = contact.name,
                    group = contact.primaryGroup.name,
                    lastCalled = lastCalledText,
                    nextReminder = nextReminderText,
                    isOverdue = isOverdue,
                    isVip = contact.isVip,
                    photoUri = contact.photoUri,
                    onClick = { onContactClick(contact.id) },
                    onCallClick = {
                        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:${contact.phone}"))
                        context.startActivity(intent)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}
