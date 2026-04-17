package com.hunterxdk.stayconnected.ui.permissions

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.hunterxdk.stayconnected.ui.settings.SettingsViewModel

/**
 * Multi-step permission onboarding flow:
 *
 * Step 1 (required)   — READ_CONTACTS + CALL_PHONE
 *   Permanently denied → show "Open Settings" instead of "Grant"
 *
 * Step 2 (optional)   — READ_CALL_LOG + READ_PHONE_STATE
 *   "Skip" disables auto-detect calls in settings
 *
 * Step 3 (conditional) — POST_NOTIFICATIONS, API 33+ only
 *   "Skip" is available; user can enable later in system settings
 *
 * After all steps resolved → renders [content]
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionHandler(
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // 1 = contacts/call, 2 = call log, 3 = notifications, 4 = done
    var currentStep by rememberSaveable { mutableIntStateOf(1) }

    // Track whether each step's system dialog has been launched so we can
    // distinguish "never asked" from "permanently denied".
    var step1Requested by rememberSaveable { mutableStateOf(false) }
    var step2Requested by rememberSaveable { mutableStateOf(false) }
    var step3Requested by rememberSaveable { mutableStateOf(false) }

    val step1 = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
    )

    val step2 = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE
        )
    )

    val notifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    } else null

    fun nextAfterStep2() {
        currentStep = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) 3 else 4
    }

    when (currentStep) {
        1 -> {
            if (step1.allPermissionsGranted) {
                currentStep = 2
                return
            }
            // Permanently denied = requested before AND rationale not showable AND not granted
            val permanentlyDenied = step1Requested &&
                    step1.revokedPermissions.any { !it.status.shouldShowRationale }

            PermissionStepScreen(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Contacts,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = "Contact Access Required",
                rationale = "StayConnected needs access to your contacts to let you select people to stay in touch with, and call permission to dial them directly from the app.",
                isPermanentlyDenied = permanentlyDenied,
                showSkip = false,
                onGrant = {
                    step1Requested = true
                    step1.launchMultiplePermissionRequest()
                },
                onSkip = {},
                onOpenSettings = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            )
        }

        2 -> {
            if (step2.allPermissionsGranted) {
                nextAfterStep2()
                return
            }
            PermissionStepScreen(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = "Auto-detect Calls (Optional)",
                rationale = "Allow StayConnected to automatically detect when you've called someone, so reminders update without you having to log the call manually.",
                isPermanentlyDenied = false,
                showSkip = true,
                onGrant = {
                    step2Requested = true
                    step2.launchMultiplePermissionRequest()
                },
                onSkip = {
                    settingsViewModel.updateAutoDetectCalls(false)
                    nextAfterStep2()
                },
                onOpenSettings = {}
            )
        }

        3 -> {
            val notif = notifPermission
            if (notif == null || notif.status.isGranted) {
                currentStep = 4
                return
            }
            PermissionStepScreen(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = "Enable Notifications",
                rationale = "StayConnected uses notifications to remind you when it's time to reach out. You can adjust this anytime in system Settings.",
                isPermanentlyDenied = false,
                showSkip = true,
                onGrant = {
                    step3Requested = true
                    notif.launchPermissionRequest()
                },
                onSkip = { currentStep = 4 },
                onOpenSettings = {}
            )
        }

        else -> content()
    }
}

@Composable
private fun PermissionStepScreen(
    icon: @Composable () -> Unit,
    title: String,
    rationale: String,
    isPermanentlyDenied: Boolean,
    showSkip: Boolean,
    onGrant: () -> Unit,
    onSkip: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon()

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = rationale,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (isPermanentlyDenied) {
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Settings")
            }
        } else {
            Button(
                onClick = onGrant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Grant Permission")
            }
        }

        if (showSkip) {
            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Skip")
            }
        }
    }
}
