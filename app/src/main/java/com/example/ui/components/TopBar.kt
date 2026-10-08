package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.ui.theme.*

@Composable
fun TopNavBar(
    currentUser: User,
    isOffline: Boolean,
    isSyncing: Boolean,
    pendingSyncCount: Int,
    isDarkMode: Boolean,
    onToggleOffline: () -> Unit,
    onTriggerSync: () -> Unit,
    onToggleTheme: () -> Unit,
    onLockApp: () -> Unit,
    onOpenRoleSwitcher: () -> Unit,
    onOpenGoogleSignIn: () -> Unit = {},
    hasUpdate: Boolean = false,
    onOpenUpdater: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Brand and E2EE Lock Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "E2EE Shield",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MVA Business Chat",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Encrypted",
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "E2EE 256-Bit Authenticated",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                }

                // Action Controls: Offline Switch, Theme, Lock, Role Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Offline / Online Indicator Pill
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onToggleOffline)
                            .testTag("offline_toggle_button"),
                        color = if (isOffline) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                        contentColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.CloudDone,
                                contentDescription = if (isOffline) "Offline" else "Online",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isOffline) "Offline" else "Online",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Role Pill (Clickable to switch for RBAC testing)
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenRoleSwitcher)
                            .testTag("role_switcher_pill"),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = "Role",
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentUser.role.name.take(5),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Google Identity Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenGoogleSignIn)
                            .testTag("google_identity_button"),
                        color = Color.White,
                        contentColor = Color(0xFFEA4335)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "G",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFEA4335)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Sign In",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3C4043)
                            )
                        }
                    }

                    // Theme Toggle
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Biometric Lock App Icon
                    IconButton(
                        onClick = onLockApp,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("lock_app_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Lock App",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // GitHub OTA Updater Icon button
                    IconButton(
                        onClick = onOpenUpdater,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("updater_top_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (hasUpdate) {
                                    Badge(
                                        containerColor = Color(0xFFFFD600),
                                        contentColor = Color.Black
                                    ) {
                                        Text("!", fontWeight = FontWeight.Black, fontSize = 9.sp)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = "Check Updates",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Offline Sync Pending Banner
            AnimatedVisibility(visible = isOffline || pendingSyncCount > 0) {
                Surface(
                    color = if (isOffline) Color(0xFFFFF3CD) else Color(0xFFD1E7DD),
                    contentColor = if (isOffline) Color(0xFF664D03) else Color(0xFF0F5132)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isOffline) Icons.Default.WifiOff else Icons.Default.Sync,
                                contentDescription = "Sync State",
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOffline) {
                                    if (pendingSyncCount > 0) "Offline: $pendingSyncCount outgoing messages queued"
                                    else "Offline mode: local encrypted cache active"
                                } else {
                                    "Connected: $pendingSyncCount items ready to sync"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (!isOffline && pendingSyncCount > 0) {
                            TextButton(
                                onClick = onTriggerSync,
                                modifier = Modifier.height(26.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
