package com.hunterxdk.stayconnected.ui.navigation

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Contacts : Screen("contacts")
    object Schedule : Screen("schedule")
    object Settings : Screen("settings")
    object ContactDetail : Screen("contact_detail/{contactId}") {
        fun createRoute(contactId: Long) = "contact_detail/$contactId"
    }
    object AddEditContact : Screen("add_edit_contact?contactId={contactId}") {
        fun createRoute(contactId: Long? = null) =
            if (contactId != null) "add_edit_contact?contactId=$contactId" else "add_edit_contact"
    }
}
