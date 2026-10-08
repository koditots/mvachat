package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserRole
import com.example.data.repository.InAppNotification
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.MVABusinessChatTheme
import com.example.ui.viewmodel.AppNavTab
import com.example.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: ChatViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MVABusinessChatTheme(darkTheme = uiState.isDarkMode) {
                // Main Application Root
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when {
                        // 1. Biometric Enclave Lock Overlay
                        uiState.isAppLocked -> {
                            BiometricLockOverlay(
                                onUnlockSuccess = { viewModel.unlockApp() }
                            )
                        }

                        // 2. Active E2EE Audio Call Overlay
                        uiState.activeCall != null -> {
                            AudioCallScreen(
                                callSession = uiState.activeCall!!,
                                onToggleMute = { viewModel.toggleMute() },
                                onToggleSpeaker = { viewModel.toggleSpeaker() },
                                onEndCall = { viewModel.endCall() }
                            )
                        }

                        // 3. Channel Conversation Detail Screen
                        uiState.selectedChannelId != null -> {
                            val channel = uiState.channels.find { it.id == uiState.selectedChannelId }
                            if (channel != null) {
                                ChatDetailScreen(
                                    channel = channel,
                                    messages = uiState.currentMessages,
                                    currentUser = uiState.currentUser,
                                    onBackClick = { viewModel.closeChannel() },
                                    onSendMessage = { text, disappearingSeconds ->
                                        viewModel.sendMessage(text, disappearingSeconds)
                                    },
                                    onOpenSafetyNumber = {
                                        viewModel.showingSafetyNumbers.value = channel
                                    },
                                    onOpenSettings = {
                                        viewModel.showingChannelSettings.value = channel
                                    },
                                    onStartAudioCall = {
                                        viewModel.startCall(channel.id)
                                    },
                                    onOpenAttachSheet = {
                                        viewModel.showingAttachSheet.value = true
                                    },
                                    onOpenStickerSheet = {
                                        viewModel.showingStickerSheet.value = true
                                    },
                                    onInspectCiphertext = { msg ->
                                        viewModel.inspectingCiphertext.value = msg
                                    },
                                    onReact = { messageId, emoji ->
                                        viewModel.reactToMessage(messageId, emoji)
                                    },
                                    onOpenFile = { file ->
                                        // Trigger inspection / decryption alert
                                    }
                                )
                            }
                        }

                        // 4. Primary App View with Top Bar & Bottom Navigation
                        else -> {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                topBar = {
                                    TopNavBar(
                                        currentUser = uiState.currentUser,
                                        isOffline = uiState.isOffline,
                                        isSyncing = uiState.isSyncing,
                                        pendingSyncCount = uiState.pendingSyncCount,
                                        isDarkMode = uiState.isDarkMode,
                                        onToggleOffline = { viewModel.toggleOffline() },
                                        onTriggerSync = { viewModel.triggerSync() },
                                        onToggleTheme = { viewModel.toggleTheme() },
                                        onLockApp = { viewModel.lockApp() },
                                        onOpenRoleSwitcher = { viewModel.showingRoleSwitcher.value = true }
                                    )
                                },
                                bottomBar = {
                                    val totalUnread = uiState.channels.sumOf { it.unreadCount }
                                    BottomNavBar(
                                        currentTab = uiState.currentTab,
                                        onTabSelected = { viewModel.setNavTab(it) },
                                        unreadCount = totalUnread
                                    )
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (uiState.currentTab) {
                                        AppNavTab.CHATS -> {
                                            val searchQuery by viewModel.searchFilter.collectAsStateWithLifecycle()
                                            val typeFilter by viewModel.channelTypeFilter.collectAsStateWithLifecycle()
                                            val unreadOnly by viewModel.unreadOnlyFilter.collectAsStateWithLifecycle()

                                            ChatListScreen(
                                                channels = uiState.channels,
                                                currentUser = uiState.currentUser,
                                                selectedTypeFilter = typeFilter,
                                                searchQuery = searchQuery,
                                                unreadOnlyFilter = unreadOnly,
                                                onFilterChanged = { viewModel.channelTypeFilter.value = it },
                                                onUnreadFilterChanged = { viewModel.unreadOnlyFilter.value = it },
                                                onMarkAllReadClick = { viewModel.markAllAsRead() },
                                                onSearchChanged = { viewModel.searchFilter.value = it },
                                                onChannelClick = { channelId ->
                                                    viewModel.openChannel(channelId)
                                                },
                                                onNewChatClick = {
                                                    // Quick demo: open first available client channel
                                                    val firstChan = uiState.channels.firstOrNull()
                                                    if (firstChan != null) {
                                                        viewModel.openChannel(firstChan.id)
                                                    }
                                                },
                                                onEditProfileClick = {
                                                    viewModel.showingProfileDialog.value = true
                                                }
                                            )
                                        }

                                        AppNavTab.CALLS -> {
                                            CallsScreen(
                                                channels = uiState.channels,
                                                onStartCall = { channelId ->
                                                    viewModel.startCall(channelId)
                                                }
                                            )
                                        }

                                        AppNavTab.LINKED_WEB -> {
                                            LinkedWebScreen(
                                                pairingToken = uiState.webPairingToken,
                                                linkedDevices = uiState.linkedDevices,
                                                onLinkNewDeviceClick = {
                                                    viewModel.showingPairingScanner.value = true
                                                },
                                                onRevokeDevice = { deviceId ->
                                                    viewModel.revokeDevice(deviceId)
                                                }
                                            )
                                        }

                                        AppNavTab.SECURITY -> {
                                            SecuritySettingsScreen(
                                                currentUser = uiState.currentUser,
                                                isBiometricEnabled = uiState.isBiometricEnabled,
                                                isDarkMode = uiState.isDarkMode,
                                                isBatterySaver = uiState.isBatterySaver,
                                                cloudBackup = uiState.cloudBackup,
                                                isBackingUp = uiState.isBackingUp,
                                                onToggleBiometric = { viewModel.toggleBiometric(it) },
                                                onLockAppNow = { viewModel.lockApp() },
                                                onOpenRoleSwitcher = { viewModel.showingRoleSwitcher.value = true },
                                                onRunBackupNow = { viewModel.runBackup() },
                                                onToggleDarkMode = { viewModel.toggleTheme() },
                                                onToggleBatterySaver = { viewModel.toggleBatterySaver() }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating In-App Push Notification Alert Banner
                    PushNotificationBanner(viewModel = viewModel)

                    // Modal Dialogs & Sheets
                    val inspectingMsg by viewModel.inspectingCiphertext.collectAsStateWithLifecycle()
                    inspectingMsg?.let { msg ->
                        CiphertextInspectDialog(
                            message = msg,
                            onDismiss = { viewModel.inspectingCiphertext.value = null }
                        )
                    }

                    val safetyChan by viewModel.showingSafetyNumbers.collectAsStateWithLifecycle()
                    safetyChan?.let { chan ->
                        SafetyNumberDialog(
                            channel = chan,
                            onDismiss = { viewModel.showingSafetyNumbers.value = null }
                        )
                    }

                    val settingsChan by viewModel.showingChannelSettings.collectAsStateWithLifecycle()
                    settingsChan?.let { chan ->
                        ChannelSettingsDialog(
                            channel = chan,
                            currentUserRole = uiState.currentUser.role,
                            onSetDisappearingTimer = { seconds ->
                                viewModel.setDisappearingTimer(chan.id, seconds)
                            },
                            onDismiss = { viewModel.showingChannelSettings.value = null }
                        )
                    }

                    val showRoleSwitcher by viewModel.showingRoleSwitcher.collectAsStateWithLifecycle()
                    if (showRoleSwitcher) {
                        RoleSwitcherDialog(
                            currentRole = uiState.currentUser.role,
                            onRoleSelected = { role ->
                                viewModel.switchRole(role)
                            },
                            onDismiss = { viewModel.showingRoleSwitcher.value = false }
                        )
                    }

                    val showAttachSheet by viewModel.showingAttachSheet.collectAsStateWithLifecycle()
                    if (showAttachSheet) {
                        AttachmentSheet(
                            onFileSelected = { file ->
                                viewModel.sendFileAttachment(file)
                            },
                            onDismiss = { viewModel.showingAttachSheet.value = false }
                        )
                    }

                    val showStickerSheet by viewModel.showingStickerSheet.collectAsStateWithLifecycle()
                    if (showStickerSheet) {
                        StickerSheet(
                            onStickerSelected = { sticker ->
                                viewModel.sendSticker(sticker)
                            },
                            onDismiss = { viewModel.showingStickerSheet.value = false }
                        )
                    }

                    val showScanner by viewModel.showingPairingScanner.collectAsStateWithLifecycle()
                    if (showScanner) {
                        QrPairingDialog(
                            onPairSuccess = { name, browser, os ->
                                viewModel.pairDevice(name, browser, os)
                            },
                            onDismiss = { viewModel.showingPairingScanner.value = false }
                        )
                    }

                    val showProfileDialog by viewModel.showingProfileDialog.collectAsStateWithLifecycle()
                    if (showProfileDialog) {
                        ProfileEditDialog(
                            currentUser = uiState.currentUser,
                            onSaveProfile = { name, title, company, email, phone, statusBio, avatarId, profilePicUri ->
                                viewModel.updateProfile(name, title, company, email, phone, statusBio, avatarId, profilePicUri)
                            },
                            onDismiss = { viewModel.showingProfileDialog.value = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PushNotificationBanner(viewModel: ChatViewModel) {
    var currentAlert by remember { mutableStateOf<InAppNotification?>(null) }

    LaunchedEffect(Unit) {
        viewModel.notificationEvents.collect { alert ->
            currentAlert = alert
            delay(3800)
            if (currentAlert?.id == alert.id) {
                currentAlert = null
            }
        }
    }

    AnimatedVisibility(
        visible = currentAlert != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 36.dp)
    ) {
        currentAlert?.let { notif ->
            Surface(
                color = Color(0xFF212529),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.openChannel(notif.channelId)
                        currentAlert = null
                    }
                    .testTag("push_notification_banner")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFB71C1C), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = notif.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "E2EE",
                                fontSize = 9.sp,
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = notif.message,
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                    }

                    IconButton(onClick = { currentAlert = null }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
