package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.AppNavTab

@Composable
fun BottomNavBar(
    currentTab: AppNavTab,
    onTabSelected: (AppNavTab) -> Unit,
    unreadCount: Int = 3
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = currentTab == AppNavTab.CHATS,
            onClick = { onTabSelected(AppNavTab.CHATS) },
            icon = {
                BadgedBox(badge = {
                    if (unreadCount > 0) {
                        Badge { Text(unreadCount.toString()) }
                    }
                }) {
                    Icon(Icons.Default.Chat, contentDescription = "Chats")
                }
            },
            label = { Text("Chats") },
            modifier = Modifier.testTag("nav_tab_chats"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = currentTab == AppNavTab.CALLS,
            onClick = { onTabSelected(AppNavTab.CALLS) },
            icon = {
                Icon(Icons.Default.PhoneInTalk, contentDescription = "Encrypted Calls")
            },
            label = { Text("Calls") },
            modifier = Modifier.testTag("nav_tab_calls"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = currentTab == AppNavTab.LINKED_WEB,
            onClick = { onTabSelected(AppNavTab.LINKED_WEB) },
            icon = {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Web Sync")
            },
            label = { Text("Web Sync") },
            modifier = Modifier.testTag("nav_tab_web_sync"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )

        NavigationBarItem(
            selected = currentTab == AppNavTab.SECURITY,
            onClick = { onTabSelected(AppNavTab.SECURITY) },
            icon = {
                Icon(Icons.Default.Security, contentDescription = "Security Vault")
            },
            label = { Text("Security") },
            modifier = Modifier.testTag("nav_tab_security"),
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}
