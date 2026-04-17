package com.hunterxdk.stayconnected

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.hunterxdk.stayconnected.ui.navigation.NavGraph
import com.hunterxdk.stayconnected.ui.navigation.Screen
import com.hunterxdk.stayconnected.ui.permissions.PermissionHandler
import com.hunterxdk.stayconnected.ui.settings.SettingsViewModel
import com.hunterxdk.stayconnected.ui.theme.StayConnectedTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            val settings by settingsViewModel.settings.collectAsState()
            val darkTheme = settings.appTheme == "DARK"

            StayConnectedTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionHandler {
                        val deepLinkContactId = intent.getLongExtra("contactId", -1L)
                        MainContent(deepLinkContactId = deepLinkContactId)
                    }
                }
            }
        }
    }
}

@Composable
fun MainContent(deepLinkContactId: Long = -1L) {
    val navController = rememberNavController()

    // Navigate to ContactDetail if launched from a notification tap
    LaunchedEffect(deepLinkContactId) {
        if (deepLinkContactId != -1L) {
            navController.navigate(Screen.ContactDetail.createRoute(deepLinkContactId))
        }
    }

    NavGraph(navController = navController)
}
