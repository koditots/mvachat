package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.ChatRepository
import com.example.data.repository.InAppNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class AppNavTab {
    CHATS,
    CALLS,
    LINKED_WEB,
    SECURITY
}

data class UiState(
    val currentUser: User,
    val isOffline: Boolean,
    val isSyncing: Boolean,
    val pendingSyncCount: Int,
    val isAppLocked: Boolean,
    val isBiometricEnabled: Boolean,
    val autoLockTimeoutMinutes: Int,
    val currentTab: AppNavTab = AppNavTab.CHATS,
    val selectedChannelId: String? = null,
    val activeCall: CallSession? = null,
    val isDarkMode: Boolean = false,
    val isBatterySaver: Boolean = false,
    val isBackingUp: Boolean = false,
    val cloudBackup: CloudBackupInfo = CloudBackupInfo(),
    val channels: List<Channel> = emptyList(),
    val currentMessages: List<ChatMessage> = emptyList(),
    val linkedDevices: List<LinkedDevice> = emptyList(),
    val webPairingToken: String = "",
    val isGoogleSignedIn: Boolean = false,
    val directoryUsers: List<User> = emptyList(),
    val appUpdateInfo: AppUpdateInfo = AppUpdateInfo()
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    val repository = ChatRepository(viewModelScope, application.applicationContext)

    private val _currentTab = MutableStateFlow(AppNavTab.CHATS)
    val currentTab: StateFlow<AppNavTab> = _currentTab.asStateFlow()

    // Dialog & overlay states
    val inspectingCiphertext = MutableStateFlow<ChatMessage?>(null)
    val showingSafetyNumbers = MutableStateFlow<Channel?>(null)
    val showingChannelSettings = MutableStateFlow<Channel?>(null)
    val showingRoleSwitcher = MutableStateFlow(false)
    val showingAttachSheet = MutableStateFlow(false)
    val showingStickerSheet = MutableStateFlow(false)
    val showingPairingScanner = MutableStateFlow(false)
    val showingProfileDialog = MutableStateFlow(false)
    val showingAddContactDialog = MutableStateFlow(false)
    val showingGoogleSignInDialog = MutableStateFlow(false)
    val showingLoginScreen = MutableStateFlow(false)
    val showingUpdateDialog = MutableStateFlow(false)
    val searchFilter = MutableStateFlow("")
    val channelTypeFilter = MutableStateFlow<ChannelType?>(null)
    val unreadOnlyFilter = MutableStateFlow(false)

    // Notification banners
    val notificationEvents: SharedFlow<InAppNotification> = repository.notificationEvents

    val uiState: StateFlow<UiState> = combine(
        repository.currentUser,
        repository.isOffline,
        repository.isSyncing,
        repository.pendingSyncCount,
        repository.isAppLocked,
        repository.isBiometricEnabled,
        repository.channels,
        repository.selectedChannelId,
        repository.messages,
        _currentTab,
        repository.activeCall,
        repository.isDarkMode,
        repository.isBatterySaver,
        repository.linkedDevices,
        repository.webPairingToken,
        repository.cloudBackup,
        repository.isBackingUp,
        repository.isGoogleSignedIn,
        repository.directoryUsers,
        repository.appUpdateInfo
    ) { args: Array<Any?> ->
        val user = args[0] as User
        val offline = args[1] as Boolean
        val syncing = args[2] as Boolean
        val pendingCount = args[3] as Int
        val appLocked = args[4] as Boolean
        val bioEnabled = args[5] as Boolean
        val chanList = args[6] as List<Channel>
        val selChanId = args[7] as String?
        val msgsMap = args[8] as Map<String, List<ChatMessage>>
        val tab = args[9] as AppNavTab
        val call = args[10] as CallSession?
        val darkMode = args[11] as Boolean
        val battery = args[12] as Boolean
        val linkedDevs = args[13] as List<LinkedDevice>
        val webToken = args[14] as String
        val backup = args[15] as CloudBackupInfo
        val backingUp = args[16] as Boolean
        val googleSignedIn = args[17] as Boolean
        val directory = args[18] as List<User>
        val updateInfo = args[19] as AppUpdateInfo

        val currentMsgs = selChanId?.let { msgsMap[it] }.orEmpty()

        UiState(
            currentUser = user,
            isOffline = offline,
            isSyncing = syncing,
            pendingSyncCount = pendingCount,
            isAppLocked = appLocked,
            isBiometricEnabled = bioEnabled,
            autoLockTimeoutMinutes = 1,
            currentTab = tab,
            selectedChannelId = selChanId,
            activeCall = call,
            isDarkMode = darkMode,
            isBatterySaver = battery,
            isBackingUp = backingUp,
            cloudBackup = backup,
            channels = chanList,
            currentMessages = currentMsgs,
            linkedDevices = linkedDevs,
            webPairingToken = webToken,
            isGoogleSignedIn = googleSignedIn,
            directoryUsers = directory,
            appUpdateInfo = updateInfo
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState(
            currentUser = repository.currentUser.value,
            isOffline = false,
            isSyncing = false,
            pendingSyncCount = 0,
            isAppLocked = false,
            isBiometricEnabled = true,
            autoLockTimeoutMinutes = 1
        )
    )

    fun setNavTab(tab: AppNavTab) {
        _currentTab.value = tab
    }

    fun openChannel(channelId: String) {
        repository.selectChannel(channelId)
    }

    fun closeChannel() {
        repository.selectChannel(null)
    }

    fun sendMessage(text: String, disappearingSeconds: Long = 0) {
        val selId = uiState.value.selectedChannelId ?: return
        if (text.isNotBlank()) {
            repository.sendMessage(
                channelId = selId,
                text = text.trim(),
                disappearingSeconds = disappearingSeconds
            )
        }
    }

    fun sendFileAttachment(attachment: FileAttachment) {
        val selId = uiState.value.selectedChannelId ?: return
        repository.sendMessage(
            channelId = selId,
            text = "Shared encrypted enterprise document",
            attachment = attachment
        )
        showingAttachSheet.value = false
    }

    fun sendSticker(stickerName: String) {
        val selId = uiState.value.selectedChannelId ?: return
        repository.sendMessage(
            channelId = selId,
            text = "",
            stickerName = stickerName
        )
        showingStickerSheet.value = false
    }

    fun reactToMessage(messageId: String, emoji: String) {
        val selId = uiState.value.selectedChannelId ?: return
        repository.addReaction(selId, messageId, emoji)
    }

    fun toggleOffline() {
        repository.toggleOfflineMode()
    }

    fun triggerSync() {
        repository.triggerSyncQueue()
    }

    fun startCall(channelId: String) {
        repository.startAudioCall(channelId)
    }

    fun toggleMute() {
        repository.toggleCallMute()
    }

    fun toggleSpeaker() {
        repository.toggleCallSpeaker()
    }

    fun endCall() {
        repository.endAudioCall()
    }

    fun lockApp() {
        repository.lockApp()
    }

    fun unlockApp() {
        repository.unlockApp()
    }

    fun toggleBiometric(enabled: Boolean) {
        repository.toggleBiometricEnabled(enabled)
    }

    fun switchRole(role: UserRole) {
        repository.switchUserRole(role)
        showingRoleSwitcher.value = false
    }

    fun updateProfile(
        name: String,
        username: String = repository.currentUser.value.username,
        title: String,
        company: String,
        email: String,
        phone: String,
        statusBio: String,
        avatarId: Int,
        profilePictureUri: String? = null
    ) {
        repository.updateUserProfile(
            name = name,
            username = username,
            title = title,
            company = company,
            email = email,
            phone = phone,
            statusBio = statusBio,
            avatarId = avatarId,
            profilePictureUri = profilePictureUri
        )
        showingProfileDialog.value = false
    }

    fun setDisappearingTimer(channelId: String, seconds: Long) {
        repository.updateChannelDisappearingTimer(channelId, seconds)
        showingChannelSettings.value = null
    }

    fun pairDevice(deviceName: String, browser: String, os: String) {
        repository.pairNewDevice(deviceName, browser, os)
        showingPairingScanner.value = false
    }

    fun revokeDevice(deviceId: String) {
        repository.revokeDevice(deviceId)
    }

    fun runBackup() {
        repository.performBackupNow()
    }

    fun toggleTheme() {
        repository.toggleDarkMode()
    }

    fun toggleBatterySaver() {
        repository.toggleBatterySaver()
    }

    fun markAllAsRead() {
        repository.markAllAsRead()
    }

    fun searchDirectory(query: String): List<User> {
        return repository.searchDirectory(query)
    }

    fun addContactAndStartChat(targetUser: User, initialMessage: String = "") {
        repository.addContactAndStartChat(targetUser, initialMessage)
        showingAddContactDialog.value = false
    }

    fun addContactByEmailOrUsername(query: String, initialMessage: String = "", customDisplayName: String = "") {
        repository.addContactByEmailOrUsername(query, initialMessage, customDisplayName)
        showingAddContactDialog.value = false
    }

    fun switchUserAccount(user: User) {
        repository.switchUserAccount(user)
        showingGoogleSignInDialog.value = false
    }

    fun signInWithGoogle(
        name: String,
        email: String,
        username: String,
        accountType: AccountType,
        company: String,
        title: String
    ) {
        repository.signInWithGoogle(name, email, username, accountType, company, title)
        showingGoogleSignInDialog.value = false
        showingLoginScreen.value = false
    }

    fun signOutGoogle() {
        repository.signOutGoogle()
        showingLoginScreen.value = true
    }

    fun openLoginScreen() {
        showingLoginScreen.value = true
    }

    fun closeLoginScreen() {
        showingLoginScreen.value = false
    }

    // App Updater
    fun checkForUpdates(manual: Boolean = true) {
        repository.checkForUpdates(manual)
    }

    fun downloadAndInstallUpdate() {
        repository.downloadAndInstallUpdate()
    }

    fun dismissUpdateBanner() {
        repository.dismissUpdateBanner()
    }

    fun toggleAutoCheckUpdates(enabled: Boolean) {
        repository.toggleAutoCheckUpdates(enabled)
    }
}
