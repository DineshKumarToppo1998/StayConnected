package com.hunterxdk.stayconnected.ui.contacts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hunterxdk.stayconnected.ui.components.ContactItem
import com.hunterxdk.stayconnected.ui.dashboard.DashboardViewModel
import com.hunterxdk.stayconnected.util.NextReminderCalculator
import com.hunterxdk.stayconnected.util.RelativeTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onContactClick: (Long) -> Unit,
    onAddContactClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    var searchQuery by remember { mutableStateOf("") }

    val displayContacts = remember(uiState.allContacts, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.allContacts
        } else {
            uiState.allContacts.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.phone.contains(searchQuery)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contacts", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddContactClick,
                containerColor = colorScheme.primary,
                contentColor = colorScheme.onPrimary
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search contacts...") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = colorScheme.surfaceContainerLow,
                    focusedContainerColor = colorScheme.surfaceContainerLow,
                    unfocusedBorderColor = colorScheme.outlineVariant,
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colorScheme.primary)
                }
                return@Scaffold
            }

            if (displayContacts.isEmpty()) {
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
                            text = if (searchQuery.isNotEmpty())
                                "No contacts match \"$searchQuery\""
                            else
                                "No contacts yet",
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Tap + to add someone to stay in touch with",
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = displayContacts,
                        key = { contact -> contact.id }
                    ) { contact ->
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
                            photoUri = contact.photoUri,
                            onClick = { onContactClick(contact.id) },
                            onCallClick = {
                                val callIntent = Intent(
                                    Intent.ACTION_CALL, Uri.parse("tel:${contact.phone}")
                                )
                                context.startActivity(callIntent)
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}
