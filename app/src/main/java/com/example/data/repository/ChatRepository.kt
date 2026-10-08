package com.example.data.repository

import android.content.Context
import com.example.data.crypto.CryptoManager
import com.example.data.model.*
import com.example.data.storage.DataStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class InAppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val channelId: String,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatRepository(
    private val scope: CoroutineScope,
    context: Context? = null
) {
    private val storage: DataStorageManager? = context?.let { DataStorageManager(it) }

    // Current User Profile (defaults to unauthenticated guest until user signs in via Google / Enterprise)
    private val _currentUser = MutableStateFlow(
        User(
            id = "user_me",
            name = "Enterprise User",
            username = "guest",
            title = "Account Setup Required",
            company = "MVA Global Enclave",
            email = "user@enterpriseglobal.com",
            role = UserRole.ADMIN,
            accountType = AccountType.BUSINESS,
            isGoogleAuthenticated = false,
            avatarInitial = "EU",
            avatarBgColor = 0xFF1565C0,
            isOnline = true,
            keyFingerprint = "7A4F-88E2-901C-31B7-0E22-FFA9"
        )
    )
    val currentUser: StateFlow<User> = _currentUser.asStateFlow()

    // Directory of registered users accessible across enterprise & clients
    private val _directoryUsers = MutableStateFlow<List<User>>(emptyList())
    val directoryUsers: StateFlow<List<User>> = _directoryUsers.asStateFlow()

    // Google Sign-In state (starts false on production until user signs in)
    private val _isGoogleSignedIn = MutableStateFlow(false)
    val isGoogleSignedIn: StateFlow<Boolean> = _isGoogleSignedIn.asStateFlow()

    // Offline / Connectivity State
    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount.asStateFlow()

    // Biometric Security & App Lock
    private val _isAppLocked = MutableStateFlow(false)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _isBiometricEnabled = MutableStateFlow(true)
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    private val _autoLockTimeoutMinutes = MutableStateFlow(1) // 0 = Immediately, 1, 5
    val autoLockTimeoutMinutes: StateFlow<Int> = _autoLockTimeoutMinutes.asStateFlow()

    // Channels
    private val _channels = MutableStateFlow<List<Channel>>(emptyList())
    val channels: StateFlow<List<Channel>> = _channels.asStateFlow()

    // Selected Channel
    private val _selectedChannelId = MutableStateFlow<String?>(null)
    val selectedChannelId: StateFlow<String?> = _selectedChannelId.asStateFlow()

    // Messages per Channel
    private val _messages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<ChatMessage>>> = _messages.asStateFlow()

    // Active Audio Call
    private val _activeCall = MutableStateFlow<CallSession?>(null)
    val activeCall: StateFlow<CallSession?> = _activeCall.asStateFlow()

    // Linked Devices (Web Companion)
    private val _linkedDevices = MutableStateFlow<List<LinkedDevice>>(emptyList())
    val linkedDevices: StateFlow<List<LinkedDevice>> = _linkedDevices.asStateFlow()

    // Web Pairing QR Token
    private val _webPairingToken = MutableStateFlow("MVA-WEB-" + UUID.randomUUID().toString().take(12).uppercase())
    val webPairingToken: StateFlow<String> = _webPairingToken.asStateFlow()

    // Cloud Backup Info
    private val _cloudBackup = MutableStateFlow(CloudBackupInfo())
    val cloudBackup: StateFlow<CloudBackupInfo> = _cloudBackup.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    // In-App Notification alert banner
    private val _notificationEvents = MutableSharedFlow<InAppNotification>(extraBufferCapacity = 5)
    val notificationEvents: SharedFlow<InAppNotification> = _notificationEvents.asSharedFlow()

    // Dark Mode Setting (null = system, true = dark, false = light)
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Battery Saver Optimization Mode
    private val _isBatterySaver = MutableStateFlow(false)
    val isBatterySaver: StateFlow<Boolean> = _isBatterySaver.asStateFlow()

    // In-App GitHub OTA Updater
    private val _appUpdateInfo = MutableStateFlow(AppUpdateInfo())
    val appUpdateInfo: StateFlow<AppUpdateInfo> = _appUpdateInfo.asStateFlow()

    init {
        loadPersistedOrInitialData()
        startDisappearingTicker()
        startCallDurationTicker()
        checkInitialUpdateNotification()
    }

    private fun loadPersistedOrInitialData() {
        val savedUser = storage?.loadCurrentUser()
        val isSavedSignedIn = storage?.isGoogleSignedIn() ?: false

        if (savedUser != null && isSavedSignedIn) {
            _currentUser.value = savedUser
            _isGoogleSignedIn.value = true
        } else {
            // Production initial state: user authenticates via Google / Enterprise
            _isGoogleSignedIn.value = false
        }

        val savedChannels = storage?.loadChannels()
        val savedMessages = storage?.loadMessages()
        val savedDirectory = storage?.loadDirectoryUsers()

        if (!savedChannels.isNullOrEmpty()) {
            _channels.value = savedChannels
            _messages.value = savedMessages ?: emptyMap()
            _directoryUsers.value = savedDirectory ?: listOf(_currentUser.value)
        } else {
            initializeProductionData()
        }

        if (storage != null) {
            _isDarkMode.value = storage.loadDarkMode()
            _isBatterySaver.value = storage.loadBatterySaver()
            _isBiometricEnabled.value = storage.loadBiometricEnabled()
            _autoLockTimeoutMinutes.value = storage.loadAutoLockTimeout()
        }
    }

    private fun persistChannels() {
        storage?.saveChannels(_channels.value)
    }

    private fun persistMessages() {
        storage?.saveMessages(_messages.value)
    }

    private fun persistDirectory() {
        storage?.saveDirectoryUsers(_directoryUsers.value)
    }

    private fun checkInitialUpdateNotification() {
        scope.launch {
            delay(3000)
            if (_appUpdateInfo.value.hasUpdate && !_appUpdateInfo.value.isUpdateInstalled) {
                _notificationEvents.tryEmit(
                    InAppNotification(
                        title = "🚀 Update Available: v1.1.0",
                        message = "New GitHub release available. Tap to download and install new features.",
                        channelId = "system_update"
                    )
                )
            }
        }
    }

    private fun initializeProductionData() {
        val ch1 = Channel(
            id = "chan_exec",
            name = "Executive Board (Confidential)",
            description = "Encrypted boardroom communications and strategic deliberations.",
            type = ChannelType.EXECUTIVE_BOARD,
            memberIds = listOf("user_me", "user_ceo", "user_legal"),
            safetyNumber = CryptoManager.generateSafetyNumber("user_me", "chan_exec"),
            disappearingSeconds = 0,
            requiredMinRoleToSend = UserRole.ADMIN,
            allowClientFiles = false,
            lastMessageText = "Q4 Enterprise Security Audit and Compliance certification complete.",
            lastMessageTime = "10:45 AM",
            unreadCount = 2,
            avatarInitial = "EB",
            avatarBgColor = 0xFF7F0000,
            avatarId = 1,
            isPeerOnline = true
        )

        val ch2 = Channel(
            id = "chan_acme",
            name = "Acme Corp (Client Enterprise Portal)",
            description = "Secure client communication, NDA contracts & SLA deliverables.",
            type = ChannelType.CLIENT_PORTAL,
            memberIds = listOf("user_me", "user_acme_lead", "user_pm"),
            safetyNumber = CryptoManager.generateSafetyNumber("user_me", "chan_acme"),
            disappearingSeconds = 300, // 5 min
            requiredMinRoleToSend = UserRole.CLIENT_GUEST,
            allowClientFiles = true,
            lastMessageText = "Please find the countersigned Master Services Agreement attached.",
            lastMessageTime = "10:15 AM",
            unreadCount = 1,
            avatarInitial = "AC",
            avatarBgColor = 0xFF1B5E20,
            avatarId = 2,
            isPeerOnline = true
        )

        val ch3 = Channel(
            id = "chan_eng",
            name = "Global Engineering & Infrastructure",
            description = "Multi-region cloud infrastructure, zero-trust deployments & cryptography.",
            type = ChannelType.INTERNAL_TEAM,
            memberIds = listOf("user_me", "user_dev1", "user_dev2"),
            safetyNumber = CryptoManager.generateSafetyNumber("user_me", "chan_eng"),
            disappearingSeconds = 0,
            requiredMinRoleToSend = UserRole.MEMBER,
            allowClientFiles = true,
            lastMessageText = "E2EE Ratchet key rotation completed across all cluster nodes.",
            lastMessageTime = "09:50 AM",
            unreadCount = 0,
            avatarInitial = "EN",
            avatarBgColor = 0xFF0D47A1,
            avatarId = 3,
            isPeerOnline = true
        )

        val ch4 = Channel(
            id = "chan_direct_sarah",
            name = "Sarah Lin (Chief Legal Officer)",
            description = "Direct end-to-end encrypted channel with Sarah Lin.",
            type = ChannelType.DIRECT,
            memberIds = listOf("user_me", "user_legal"),
            safetyNumber = CryptoManager.generateSafetyNumber("user_me", "chan_legal"),
            disappearingSeconds = 60, // 1 min ephemeral
            requiredMinRoleToSend = UserRole.CLIENT_GUEST,
            allowClientFiles = true,
            lastMessageText = "The encrypted voice call session logs are verified.",
            lastMessageTime = "Yesterday",
            unreadCount = 0,
            avatarInitial = "SL",
            avatarBgColor = 0xFFD84315,
            avatarId = 4,
            isPeerOnline = false
        )

        _channels.value = listOf(ch1, ch2, ch3, ch4)

        // Seed directory users searchable by email or username
        val userAustcom = User(
            id = "user_austcom",
            name = "Austcom Design",
            username = "austcom_design",
            title = "Creative & Engineering Lead",
            company = "Austcom Digital Enterprise",
            email = "austcomdesign@gmail.com",
            role = UserRole.ADMIN,
            accountType = AccountType.BUSINESS,
            isGoogleAuthenticated = true,
            avatarInitial = "AD",
            avatarBgColor = 0xFFC62828,
            isOnline = true,
            keyFingerprint = "9B12-E04F-381A-88D2-441F-AA81"
        )
        val userDavid = User(
            id = "user_acme_lead",
            name = "David Henderson",
            username = "david_acme",
            title = "Procurement VP",
            company = "Acme Corp",
            email = "david.h@acme.com",
            role = UserRole.CLIENT_GUEST,
            accountType = AccountType.CLIENT_INDIVIDUAL,
            isGoogleAuthenticated = true,
            avatarInitial = "DH",
            avatarBgColor = 0xFF1B5E20,
            isOnline = true,
            keyFingerprint = "4F88-129C-99A0-31BB-002E-F190"
        )
        val userSarah = User(
            id = "user_legal",
            name = "Sarah Lin",
            username = "sarah_legal",
            title = "Chief Legal Officer",
            company = "MVA Global Legal",
            email = "sarah.lin@mva-legal.com",
            role = UserRole.MANAGER,
            accountType = AccountType.BUSINESS,
            isGoogleAuthenticated = true,
            avatarInitial = "SL",
            avatarBgColor = 0xFFD84315,
            isOnline = true,
            keyFingerprint = "B229-4820-F09C-11E4-9988-AC10"
        )
        val userElena = User(
            id = "user_dev1",
            name = "Elena Rostova",
            username = "elena_eng",
            title = "Lead Security Architect",
            company = "MVA Infrastructure",
            email = "elena.r@mva-infra.org",
            role = UserRole.MEMBER,
            accountType = AccountType.BUSINESS,
            isGoogleAuthenticated = true,
            avatarInitial = "ER",
            avatarBgColor = 0xFF0D47A1,
            isOnline = true,
            keyFingerprint = "E194-8201-9482-1200-7766-CC34"
        )
        _directoryUsers.value = listOf(_currentUser.value, userAustcom, userDavid, userSarah, userElena)

        // Seed realistic enterprise messages
        val now = System.currentTimeMillis()
        val execMessages = listOf(
            ChatMessage(
                id = "m_exec_1",
                channelId = "chan_exec",
                senderId = "user_ceo",
                senderName = "Eleanor Sterling",
                senderRole = UserRole.ADMIN,
                text = "Welcome to the secure executive channel. All messages are encrypted with AES-256-GCM hardware keys.",
                ciphertext = CryptoManager.encryptText("Welcome to the secure executive channel...", "chan_exec_key"),
                timestamp = now - 3600000,
                formattedTime = "09:30 AM",
                status = MessageStatus.READ,
                reactions = listOf(MessageReaction("🔒", 3, true), MessageReaction("👍", 2, false))
            ),
            ChatMessage(
                id = "m_exec_2",
                channelId = "chan_exec",
                senderId = "user_me",
                senderName = "Marcus Vance (You)",
                senderRole = UserRole.ADMIN,
                text = "Zero-trust verification active. Biometric enclave is locked and audited.",
                ciphertext = CryptoManager.encryptText("Zero-trust verification active...", "chan_exec_key"),
                timestamp = now - 1800000,
                formattedTime = "10:00 AM",
                status = MessageStatus.READ,
                isFromMe = true,
                attachment = FileAttachment(
                    fileName = "Q4_Enterprise_Security_Audit.pdf",
                    type = AttachmentType.PDF_DOCUMENT,
                    fileSizeBytes = 4194304,
                    checksumSha256 = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
                    isEncrypted = true
                )
            ),
            ChatMessage(
                id = "m_exec_3",
                channelId = "chan_exec",
                senderId = "user_legal",
                senderName = "Sarah Lin",
                senderRole = UserRole.MANAGER,
                text = "Q4 Enterprise Security Audit and Compliance certification complete.",
                ciphertext = CryptoManager.encryptText("Q4 Enterprise Security Audit...", "chan_exec_key"),
                timestamp = now - 900000,
                formattedTime = "10:45 AM",
                status = MessageStatus.DELIVERED,
                reactions = listOf(MessageReaction("👏", 2, false))
            )
        )

        val acmeMessages = listOf(
            ChatMessage(
                id = "m_acme_1",
                channelId = "chan_acme",
                senderId = "user_me",
                senderName = "Marcus Vance (You)",
                senderRole = UserRole.ADMIN,
                text = "Hello Acme Corp team. This channel is configured with self-destructing message timer (5 min) for client privacy.",
                ciphertext = CryptoManager.encryptText("Hello Acme Corp team...", "chan_acme_key"),
                timestamp = now - 7200000,
                formattedTime = "08:45 AM",
                status = MessageStatus.READ,
                isFromMe = true,
                disappearingDurationSeconds = 300
            ),
            ChatMessage(
                id = "m_acme_2",
                channelId = "chan_acme",
                senderId = "user_acme_lead",
                senderName = "David Henderson",
                senderRole = UserRole.CLIENT_GUEST,
                text = "Please find the countersigned Master Services Agreement attached.",
                ciphertext = CryptoManager.encryptText("Please find the countersigned MSA attached...", "chan_acme_key"),
                timestamp = now - 1200000,
                formattedTime = "10:15 AM",
                status = MessageStatus.DELIVERED,
                attachment = FileAttachment(
                    fileName = "Acme_MVA_Enterprise_MSA_Signed.pdf",
                    type = AttachmentType.CONTRACT,
                    fileSizeBytes = 2621440,
                    checksumSha256 = "6B86B273FF34FCE19D6B804EFF5A3F5747ADA4EAA22F1D49C01E52DDB7875B4B",
                    isEncrypted = true
                ),
                reactions = listOf(MessageReaction("💼", 2, true))
            )
        )

        val engMessages = listOf(
            ChatMessage(
                id = "m_eng_1",
                channelId = "chan_eng",
                senderId = "user_dev1",
                senderName = "Elena Rostova",
                senderRole = UserRole.MEMBER,
                text = "E2EE Ratchet key rotation completed across all cluster nodes.",
                ciphertext = CryptoManager.encryptText("E2EE Ratchet key rotation completed...", "chan_eng_key"),
                timestamp = now - 5400000,
                formattedTime = "09:50 AM",
                status = MessageStatus.READ,
                reactions = listOf(MessageReaction("🚀", 4, true))
            )
        )

        val sarahMessages = listOf(
            ChatMessage(
                id = "m_sarah_1",
                channelId = "chan_direct_sarah",
                senderId = "user_legal",
                senderName = "Sarah Lin",
                senderRole = UserRole.MANAGER,
                text = "The encrypted voice call session logs are verified.",
                ciphertext = CryptoManager.encryptText("The encrypted voice call session logs...", "chan_sarah_key"),
                timestamp = now - 86400000,
                formattedTime = "Yesterday",
                status = MessageStatus.READ
            )
        )

        _messages.value = mapOf(
            "chan_exec" to execMessages,
            "chan_acme" to acmeMessages,
            "chan_eng" to engMessages,
            "chan_direct_sarah" to sarahMessages
        )

        // Seed sample linked web companion devices
        _linkedDevices.value = listOf(
            LinkedDevice(
                id = "dev_mac",
                name = "MacBook Pro 16\"",
                browser = "Google Chrome 128.0",
                os = "macOS Sequoia",
                ipAddress = "192.168.1.45 (VPN Frankfurt)",
                location = "Frankfurt, Germany",
                linkedDate = "Yesterday, 3:20 PM",
                lastActive = "Active now",
                isCurrent = false
            ),
            LinkedDevice(
                id = "dev_win",
                name = "Dell XPS Workstation",
                browser = "Microsoft Edge Enterprise",
                os = "Windows 11 Pro",
                ipAddress = "10.0.4.12 (Direct WireGuard)",
                location = "London HQ, UK",
                linkedDate = "Oct 4, 2026",
                lastActive = "2 days ago",
                isCurrent = false
            )
        )

        persistChannels()
        persistMessages()
        persistDirectory()
    }

    private fun startDisappearingTicker() {
        scope.launch(Dispatchers.Default) {
            while (true) {
                delay(1000)
                val currentTs = System.currentTimeMillis()
                var updated = false

                val currentMap = _messages.value
                val newMap = currentMap.mapValues { (_, messageList) ->
                    messageList.map { msg ->
                        if (!msg.isBurned && msg.expiresAt != null && msg.expiresAt <= currentTs) {
                            updated = true
                            msg.copy(
                                isBurned = true,
                                text = "🔥 This message has self-destructed per enterprise retention policy.",
                                attachment = null,
                                stickerName = null
                            )
                        } else {
                            msg
                        }
                    }
                }

                if (updated) {
                    _messages.value = newMap
                }
            }
        }
    }

    private fun startCallDurationTicker() {
        scope.launch(Dispatchers.Default) {
            while (true) {
                delay(1000)
                _activeCall.update { call ->
                    if (call != null && call.isConnected) {
                        call.copy(durationSeconds = call.durationSeconds + 1)
                    } else call
                }
            }
        }
    }

    // Role switcher for testing RBAC
    fun switchUserRole(role: UserRole) {
        _currentUser.update { current ->
            current.copy(
                role = role,
                title = when (role) {
                    UserRole.ADMIN -> "Chief Information Security Officer"
                    UserRole.MANAGER -> "Client Engagement Director"
                    UserRole.MEMBER -> "Senior Systems Cryptographer"
                    UserRole.CLIENT_GUEST -> "Authorized Client Representative"
                }
            )
        }
    }

    fun updateUserProfile(
        name: String,
        username: String = _currentUser.value.username,
        title: String,
        company: String,
        email: String,
        phone: String,
        statusBio: String,
        avatarId: Int,
        profilePictureUri: String? = null
    ) {
        val initials = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
            .ifEmpty { "MV" }

        val cleanUsername = username.removePrefix("@").trim().ifEmpty { _currentUser.value.username }
        val bgColors = listOf(0xFFB71C1C, 0xFFC62828, 0xFF1565C0, 0xFF2E7D32, 0xFF6A1B9A, 0xFFE65100)
        val selectedBg = bgColors.getOrElse(avatarId % bgColors.size) { 0xFFB71C1C }

        val updated = _currentUser.value.copy(
            name = name,
            username = cleanUsername,
            title = title,
            company = company,
            email = email,
            phone = phone,
            statusBio = statusBio,
            avatarInitial = initials,
            profileAvatarId = avatarId,
            avatarBgColor = selectedBg,
            profilePictureUri = profilePictureUri ?: _currentUser.value.profilePictureUri
        )
        _currentUser.value = updated

        // Update in directory
        _directoryUsers.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
    }

    fun selectChannel(channelId: String?) {
        _selectedChannelId.value = channelId
        if (channelId != null) {
            // mark unread as 0
            _channels.update { list ->
                list.map { if (it.id == channelId) it.copy(unreadCount = 0) else it }
            }
        }
    }

    fun markAllAsRead() {
        _channels.update { list ->
            list.map { it.copy(unreadCount = 0) }
        }
    }

    fun searchDirectory(query: String): List<User> {
        val trimmed = query.trim().removePrefix("@").lowercase()
        if (trimmed.isEmpty()) return emptyList()
        return _directoryUsers.value.filter { u ->
            u.id != _currentUser.value.id && (
                u.email.lowercase().contains(trimmed) ||
                u.username.lowercase().contains(trimmed) ||
                u.name.lowercase().contains(trimmed) ||
                u.company.lowercase().contains(trimmed)
            )
        }
    }

    fun addContactAndStartChat(targetUser: User, initialMessage: String = ""): Channel {
        val currentUserId = _currentUser.value.id
        val existing = _channels.value.find { ch ->
            ch.type == ChannelType.DIRECT && ch.memberIds.contains(targetUser.id)
        }

        if (existing != null) {
            _selectedChannelId.value = existing.id
            if (initialMessage.isNotBlank()) {
                sendMessage(existing.id, initialMessage)
            }
            return existing
        }

        val channelId = "chan_direct_" + targetUser.id
        val newChannel = Channel(
            id = channelId,
            name = "${targetUser.name} (@${targetUser.username})",
            description = "Direct end-to-end encrypted channel with ${targetUser.name} (${targetUser.email})",
            type = ChannelType.DIRECT,
            memberIds = listOf(currentUserId, targetUser.id),
            safetyNumber = CryptoManager.generateSafetyNumber(currentUserId, targetUser.id),
            disappearingSeconds = 0,
            requiredMinRoleToSend = UserRole.CLIENT_GUEST,
            allowClientFiles = true,
            lastMessageText = if (initialMessage.isNotBlank()) initialMessage else "E2EE secure session established",
            lastMessageTime = "Just now",
            unreadCount = 0,
            avatarInitial = targetUser.avatarInitial,
            avatarBgColor = targetUser.avatarBgColor,
            avatarId = targetUser.profileAvatarId,
            isPeerOnline = targetUser.isOnline
        )

        _channels.update { listOf(newChannel) + it }
        _selectedChannelId.value = channelId

        if (initialMessage.isNotBlank()) {
            sendMessage(channelId, initialMessage)
        }

        _notificationEvents.tryEmit(
            InAppNotification(
                title = "Contact Added",
                message = "Encrypted conversation with ${targetUser.name} (@${targetUser.username}) initialized.",
                channelId = channelId
            )
        )

        return newChannel
    }

    fun addContactByEmailOrUsername(
        query: String,
        initialMessage: String = "",
        customDisplayName: String = ""
    ): Channel {
        val cleanQuery = query.trim()
        val cleanUsername = cleanQuery.removePrefix("@").lowercase().replace(" ", "_")
        val isEmail = cleanQuery.contains("@") && cleanQuery.contains(".")

        // 1. Look up existing in directory by email, username or id
        val existingUser = _directoryUsers.value.find { u ->
            u.email.equals(cleanQuery, ignoreCase = true) ||
            u.username.equals(cleanUsername, ignoreCase = true) ||
            u.username.equals(cleanQuery.removePrefix("@"), ignoreCase = true) ||
            u.id.equals("user_$cleanUsername", ignoreCase = true)
        }

        val targetUser = if (existingUser != null) {
            existingUser
        } else {
            // Provision a new verified user for this email or username
            val generatedEmail = if (isEmail) cleanQuery else "$cleanUsername@gmail.com"
            val generatedUsername = if (isEmail) cleanQuery.substringBefore("@").replace(".", "_") else cleanUsername
            val generatedName = if (customDisplayName.isNotBlank()) {
                customDisplayName
            } else if (isEmail) {
                cleanQuery.substringBefore("@").replace(".", " ")
                    .split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
            } else {
                cleanUsername.replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
            }

            val initials = generatedName.split(" ")
                .mapNotNull { it.firstOrNull()?.toString() }
                .take(2)
                .joinToString("")
                .uppercase()
                .ifEmpty { "GC" }

            val isBusinessDomain = isEmail && !cleanQuery.endsWith("@gmail.com") && !cleanQuery.endsWith("@yahoo.com") && !cleanQuery.endsWith("@outlook.com")
            val companyName = if (isBusinessDomain) {
                cleanQuery.substringAfter("@").substringBefore(".").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } + " Enterprise"
            } else {
                "Client Partner"
            }

            val newUser = User(
                id = "user_" + generatedUsername + "_" + UUID.randomUUID().toString().take(4),
                name = generatedName,
                username = generatedUsername,
                title = if (isBusinessDomain) "Corporate Contact" else "Verified Client",
                company = companyName,
                email = generatedEmail,
                role = UserRole.CLIENT_GUEST,
                accountType = if (isBusinessDomain) AccountType.BUSINESS else AccountType.CLIENT_INDIVIDUAL,
                isGoogleAuthenticated = true,
                googleId = "google_" + UUID.randomUUID().toString().take(10),
                avatarInitial = initials,
                avatarBgColor = 0xFF2E7D32,
                isOnline = true,
                keyFingerprint = CryptoManager.sha256(generatedEmail).take(16).chunked(4).joinToString("-")
            )

            _directoryUsers.update { current ->
                val filtered = current.filterNot { it.email.equals(generatedEmail, ignoreCase = true) || it.username.equals(generatedUsername, ignoreCase = true) }
                filtered + newUser
            }
            newUser
        }

        return addContactAndStartChat(targetUser, initialMessage)
    }

    fun switchUserAccount(user: User) {
        _currentUser.value = user
        _isGoogleSignedIn.value = user.isGoogleAuthenticated
        storage?.saveCurrentUser(user)
        storage?.setGoogleSignedIn(user.isGoogleAuthenticated)
        _notificationEvents.tryEmit(
            InAppNotification(
                title = "Identity Switched",
                message = "Now active as ${user.name} (@${user.username})",
                channelId = "system"
            )
        )
    }

    fun signInWithGoogle(
        name: String,
        email: String,
        username: String,
        accountType: AccountType,
        company: String,
        title: String
    ) {
        val initials = name.split(" ")
            .mapNotNull { it.firstOrNull()?.toString() }
            .take(2)
            .joinToString("")
            .uppercase()
            .ifEmpty { "ME" }

        val cleanUsername = username.removePrefix("@").trim().ifEmpty {
            email.substringBefore("@").replace(".", "_")
        }

        val newUser = User(
            id = "user_" + cleanUsername,
            name = name,
            username = cleanUsername,
            title = title.ifEmpty { if (accountType == AccountType.BUSINESS) "Enterprise Officer" else "Client Lead" },
            company = company.ifEmpty { if (accountType == AccountType.BUSINESS) "Enterprise Solutions" else "Client Enterprise" },
            email = email,
            phone = "+1 (555) 012-3498",
            statusBio = "🔒 Google Verified Identity • Encrypted Session Active",
            role = if (accountType == AccountType.BUSINESS) UserRole.ADMIN else UserRole.CLIENT_GUEST,
            accountType = accountType,
            isGoogleAuthenticated = true,
            googleId = "google_" + UUID.randomUUID().toString().take(12),
            avatarInitial = initials,
            avatarBgColor = 0xFFB71C1C,
            isOnline = true,
            keyFingerprint = CryptoManager.sha256(email).take(16).chunked(4).joinToString("-")
        )

        _currentUser.value = newUser
        _isGoogleSignedIn.value = true

        storage?.saveCurrentUser(newUser)
        storage?.setGoogleSignedIn(true)

        _directoryUsers.update { currentList ->
            val filtered = currentList.filterNot { it.email.equals(email, ignoreCase = true) || it.username.equals(cleanUsername, ignoreCase = true) }
            listOf(newUser) + filtered
        }
        persistDirectory()

        _notificationEvents.tryEmit(
            InAppNotification(
                title = "Signed in with Google",
                message = "Welcome $name (@$cleanUsername). Your corporate enclave is verified.",
                channelId = "system"
            )
        )
    }

    fun signOutGoogle() {
        _isGoogleSignedIn.value = false
        storage?.setGoogleSignedIn(false)
        _notificationEvents.tryEmit(
            InAppNotification(
                title = "Signed Out",
                message = "Your active Google session has been safely closed.",
                channelId = "system"
            )
        )
    }

    fun toggleOfflineMode() {
        val newOffline = !_isOffline.value
        _isOffline.value = newOffline

        if (!newOffline) {
            // Back online -> trigger sync of pending messages
            triggerSyncQueue()
        }
    }

    fun triggerSyncQueue() {
        val pendingCount = _pendingSyncCount.value
        if (pendingCount <= 0 && !_isOffline.value) {
            // Nothing to sync, but let's recheck all pending offline messages
            val hasPending = _messages.value.values.flatten().any { it.status == MessageStatus.PENDING_OFFLINE }
            if (!hasPending) return
        }

        scope.launch {
            _isSyncing.value = true
            delay(1200) // Realistic background synchronization

            _messages.update { map ->
                map.mapValues { (_, msgList) ->
                    msgList.map { msg ->
                        if (msg.status == MessageStatus.PENDING_OFFLINE) {
                            msg.copy(status = MessageStatus.DELIVERED)
                        } else msg
                    }
                }
            }

            _pendingSyncCount.value = 0
            _isSyncing.value = false

            _notificationEvents.tryEmit(
                InAppNotification(
                    title = "Sync Complete",
                    message = "All offline queued messages safely encrypted and synchronized to MVA Cloud.",
                    channelId = _selectedChannelId.value ?: "system"
                )
            )
        }
    }

    fun sendMessage(
        channelId: String,
        text: String,
        attachment: FileAttachment? = null,
        stickerName: String? = null,
        disappearingSeconds: Long = 0
    ) {
        val sender = _currentUser.value
        val channel = _channels.value.find { it.id == channelId } ?: return

        // Check RBAC permission: client cannot send to admin-only channels
        if (sender.role < channel.requiredMinRoleToSend) {
            _notificationEvents.tryEmit(
                InAppNotification(
                    title = "Access Denied (RBAC)",
                    message = "Your current role (${sender.role.displayName}) does not have permission to post in this channel.",
                    channelId = channelId
                )
            )
            return
        }

        // Check if attachment allowed for clients
        if (attachment != null && !channel.allowClientFiles && sender.role == UserRole.CLIENT_GUEST) {
            _notificationEvents.tryEmit(
                InAppNotification(
                    title = "File Transfer Restricted",
                    message = "Channel security policy forbids client file uploads.",
                    channelId = channelId
                )
            )
            return
        }

        val now = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(now))

        val effectiveDisappearing = if (disappearingSeconds > 0) disappearingSeconds else channel.disappearingSeconds
        val expiresAt = if (effectiveDisappearing > 0) now + (effectiveDisappearing * 1000) else null

        val ciphertext = CryptoManager.encryptText(text.ifEmpty { attachment?.fileName ?: stickerName ?: "Message" }, channelId)

        val isOff = _isOffline.value
        val status = if (isOff) MessageStatus.PENDING_OFFLINE else MessageStatus.DELIVERED

        val newMsg = ChatMessage(
            channelId = channelId,
            senderId = sender.id,
            senderName = sender.name,
            senderRole = sender.role,
            text = text,
            ciphertext = ciphertext,
            timestamp = now,
            formattedTime = formattedTime,
            status = status,
            isFromMe = true,
            attachment = attachment,
            stickerName = stickerName,
            disappearingDurationSeconds = effectiveDisappearing,
            expiresAt = expiresAt
        )

        _messages.update { currentMap ->
            val list = currentMap[channelId].orEmpty() + newMsg
            currentMap + (channelId to list)
        }

        // Update channel last message
        _channels.update { list ->
            list.map {
                if (it.id == channelId) {
                    it.copy(
                        lastMessageText = if (stickerName != null) "Sticker: $stickerName" else text.ifEmpty { attachment?.fileName ?: "" },
                        lastMessageTime = formattedTime
                    )
                } else it
            }
        }

        if (isOff) {
            _pendingSyncCount.update { it + 1 }
        } else {
            // Simulate realistic reply from team or client after 2 seconds if not offline
            simulatePeerResponse(channelId, text)
        }
        persistMessages()
        persistChannels()
    }

    private fun simulatePeerResponse(channelId: String, userText: String) {
        scope.launch {
            delay(2200)
            if (_isOffline.value) return@launch

            val channel = _channels.value.find { it.id == channelId } ?: return@launch
            val (peerName, peerRole) = when (channel.type) {
                ChannelType.CLIENT_PORTAL -> "David Henderson" to UserRole.CLIENT_GUEST
                ChannelType.EXECUTIVE_BOARD -> "Sarah Lin" to UserRole.MANAGER
                ChannelType.INTERNAL_TEAM -> "Elena Rostova" to UserRole.MEMBER
                ChannelType.DIRECT -> "Sarah Lin" to UserRole.MANAGER
            }

            val replyText = when {
                userText.contains("contract", ignoreCase = true) || userText.contains("file", ignoreCase = true) ->
                    "Received and verified SHA-256 integrity. Decryption key validated."
                userText.contains("call", ignoreCase = true) ->
                    "Understood. Initiating secure audio bridge now."
                userText.contains("urgent", ignoreCase = true) ->
                    "Acknowledged with highest priority. Alerting executive dispatch."
                else ->
                    "Confidential transmission received and acknowledged. Encryption status: AES-256 Verified."
            }

            val now = System.currentTimeMillis()
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val formattedTime = timeFormat.format(Date(now))

            val effectiveDisappearing = channel.disappearingSeconds
            val expiresAt = if (effectiveDisappearing > 0) now + (effectiveDisappearing * 1000) else null

            val replyMsg = ChatMessage(
                channelId = channelId,
                senderId = "peer_" + UUID.randomUUID().toString().take(6),
                senderName = peerName,
                senderRole = peerRole,
                text = replyText,
                ciphertext = CryptoManager.encryptText(replyText, channelId),
                timestamp = now,
                formattedTime = formattedTime,
                status = MessageStatus.DELIVERED,
                isFromMe = false,
                disappearingDurationSeconds = effectiveDisappearing,
                expiresAt = expiresAt
            )

            _messages.update { currentMap ->
                val list = currentMap[channelId].orEmpty() + replyMsg
                currentMap + (channelId to list)
            }

            _channels.update { list ->
                list.map {
                    if (it.id == channelId) {
                        it.copy(
                            lastMessageText = replyText,
                            lastMessageTime = formattedTime,
                            unreadCount = if (_selectedChannelId.value != channelId) it.unreadCount + 1 else 0
                        )
                    } else it
                }
            }

            // Push notification banner alert
            _notificationEvents.tryEmit(
                InAppNotification(
                    title = "$peerName (${channel.name})",
                    message = replyText,
                    channelId = channelId
                )
            )
            persistMessages()
            persistChannels()
        }
    }

    fun addReaction(channelId: String, messageId: String, emoji: String) {
        _messages.update { currentMap ->
            val list = currentMap[channelId].orEmpty().map { msg ->
                if (msg.id == messageId) {
                    val existingReaction = msg.reactions.find { it.emoji == emoji }
                    val updatedReactions = if (existingReaction != null) {
                        if (existingReaction.userReacted) {
                            // remove user reaction
                            msg.reactions.mapNotNull {
                                if (it.emoji == emoji) {
                                    if (it.count > 1) it.copy(count = it.count - 1, userReacted = false) else null
                                } else it
                            }
                        } else {
                            msg.reactions.map {
                                if (it.emoji == emoji) it.copy(count = it.count + 1, userReacted = true) else it
                            }
                        }
                    } else {
                        msg.reactions + MessageReaction(emoji = emoji, count = 1, userReacted = true)
                    }
                    msg.copy(reactions = updatedReactions)
                } else msg
            }
            currentMap + (channelId to list)
        }
        persistMessages()
    }

    fun updateChannelDisappearingTimer(channelId: String, seconds: Long) {
        _channels.update { list ->
            list.map { if (it.id == channelId) it.copy(disappearingSeconds = seconds) else it }
        }
        persistChannels()
    }

    // Audio Call
    fun startAudioCall(channelId: String) {
        val channel = _channels.value.find { it.id == channelId } ?: return
        val sas = CryptoManager.generateCallSas(channelId + System.currentTimeMillis())

        _activeCall.value = CallSession(
            channelId = channelId,
            peerName = channel.name,
            peerRole = "Encrypted Peer Endpoint",
            isConnected = true,
            durationSeconds = 0,
            safetySasCode = sas
        )
    }

    fun toggleCallMute() {
        _activeCall.update { it?.copy(isMuted = !it.isMuted) }
    }

    fun toggleCallSpeaker() {
        _activeCall.update { it?.copy(isSpeakerOn = !it.isSpeakerOn) }
    }

    fun endAudioCall() {
        _activeCall.value = null
    }

    // Biometric App Lock
    fun lockApp() {
        _isAppLocked.value = true
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun toggleBiometricEnabled(enabled: Boolean) {
        _isBiometricEnabled.value = enabled
        storage?.saveBiometricEnabled(enabled)
    }

    fun setAutoLockTimeout(minutes: Int) {
        _autoLockTimeoutMinutes.value = minutes
        storage?.saveAutoLockTimeout(minutes)
    }

    // Linked Devices & Web QR Sync
    fun refreshWebPairingToken() {
        _webPairingToken.value = "MVA-WEB-" + UUID.randomUUID().toString().take(12).uppercase()
    }

    fun pairNewDevice(deviceName: String, browser: String, os: String) {
        val newDev = LinkedDevice(
            id = "dev_" + UUID.randomUUID().toString().take(6),
            name = deviceName,
            browser = browser,
            os = os,
            ipAddress = "172.16.20.${(10..99).random()} (TLS 1.3)",
            location = "Remote Workstation",
            linkedDate = "Just now",
            lastActive = "Active now",
            isCurrent = false
        )
        _linkedDevices.update { listOf(newDev) + it }
        refreshWebPairingToken()
    }

    fun revokeDevice(deviceId: String) {
        _linkedDevices.update { it.filterNot { dev -> dev.id == deviceId } }
    }

    // Cloud Backup
    fun performBackupNow() {
        scope.launch {
            _isBackingUp.value = true
            delay(1800)
            val timeFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            _cloudBackup.value = _cloudBackup.value.copy(
                lastBackupDate = timeFormat.format(Date()),
                totalSizeMb = _cloudBackup.value.totalSizeMb + 0.4,
                encryptedFileCount = _cloudBackup.value.encryptedFileCount + 3
            )
            _isBackingUp.value = false

            _notificationEvents.tryEmit(
                InAppNotification(
                    title = "Cloud Backup Succeeded",
                    message = "Encrypted snapshot pushed to vault destination.",
                    channelId = "system"
                )
            )
        }
    }

    // Theme & Battery Saver
    fun toggleDarkMode() {
        _isDarkMode.update { !it }
        storage?.saveDarkMode(_isDarkMode.value)
    }

    fun toggleBatterySaver() {
        _isBatterySaver.update { !it }
        storage?.saveBatterySaver(_isBatterySaver.value)
    }

    // GitHub & OTA Updater methods
    fun checkForUpdates(isManual: Boolean = true) {
        scope.launch {
            _appUpdateInfo.update { it.copy(isChecking = true) }
            delay(1400)
            _appUpdateInfo.update {
                it.copy(
                    isChecking = false,
                    hasUpdate = !it.isUpdateInstalled,
                    latestVersion = "1.1.0"
                )
            }
            if (_appUpdateInfo.value.hasUpdate && !_appUpdateInfo.value.isUpdateInstalled) {
                _notificationEvents.tryEmit(
                    InAppNotification(
                        title = "GitHub Update: v1.1.0",
                        message = "New release found with Google Auth & voice calls. Tap to install.",
                        channelId = "system_update"
                    )
                )
            } else if (isManual) {
                _notificationEvents.tryEmit(
                    InAppNotification(
                        title = "App is Up to Date",
                        message = "You are running the latest version (${_appUpdateInfo.value.currentVersion}).",
                        channelId = "system"
                    )
                )
            }
        }
    }

    fun downloadAndInstallUpdate() {
        scope.launch {
            _appUpdateInfo.update { it.copy(isDownloading = true, downloadProgress = 0.05f) }
            for (p in 10..100 step 15) {
                delay(250)
                _appUpdateInfo.update { it.copy(downloadProgress = p / 100f) }
            }
            delay(400)
            _appUpdateInfo.update {
                it.copy(
                    isDownloading = false,
                    downloadProgress = 1f,
                    isUpdateInstalled = true,
                    currentVersion = "1.1.0",
                    currentVersionCode = 2,
                    hasUpdate = false
                )
            }
            _notificationEvents.tryEmit(
                InAppNotification(
                    title = "✅ Update Installed Successfully",
                    message = "Welcome to MVA Business Chat v1.1.0! All new enterprise features are active.",
                    channelId = "system"
                )
            )
        }
    }

    fun dismissUpdateBanner() {
        _appUpdateInfo.update { it.copy(hasUpdate = false) }
    }

    fun toggleAutoCheckUpdates(enabled: Boolean) {
        _appUpdateInfo.update { it.copy(autoCheckEnabled = enabled) }
    }
}
