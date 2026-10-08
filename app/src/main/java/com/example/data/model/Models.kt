package com.example.data.model

import java.util.UUID

enum class UserRole(val displayName: String, val badgeColor: Long) {
    ADMIN("Admin / CISO", 0xFFB71C1C),
    MANAGER("Manager", 0xFFD84315),
    MEMBER("Team Member", 0xFF1565C0),
    CLIENT_GUEST("Verified Client", 0xFF2E7D32)
}

data class User(
    val id: String,
    val name: String,
    val title: String,
    val company: String,
    val email: String = "marcus.vance@mva-enterprises.com",
    val phone: String = "+1 (555) 019-2834",
    val statusBio: String = "🔒 Encrypted via MVA Enclave • Active",
    val role: UserRole,
    val avatarInitial: String,
    val avatarBgColor: Long,
    val profilePictureUri: String? = null,
    val profileAvatarId: Int = 1, // 1..6 predefined executive avatars or custom
    val isOnline: Boolean = true,
    val keyFingerprint: String
)

enum class ChannelType {
    DIRECT,
    INTERNAL_TEAM,
    CLIENT_PORTAL,
    EXECUTIVE_BOARD
}

data class Channel(
    val id: String,
    val name: String,
    val description: String,
    val type: ChannelType,
    val memberIds: List<String>,
    val isEncrypted: Boolean = true,
    val encryptionStandard: String = "AES-256-GCM + RSA-4096",
    val safetyNumber: String,
    val disappearingSeconds: Long = 0, // 0 = disabled
    val requiredMinRoleToSend: UserRole = UserRole.CLIENT_GUEST,
    val allowClientFiles: Boolean = true,
    val lastMessageText: String = "",
    val lastMessageTime: String = "Just now",
    val unreadCount: Int = 0,
    val avatarInitial: String = "MV",
    val avatarBgColor: Long = 0xFFB71C1C,
    val avatarId: Int = 1,
    val isPeerOnline: Boolean = true
)

enum class MessageStatus {
    PENDING_OFFLINE,
    ENCRYPTING,
    SENT,
    DELIVERED,
    READ
}

enum class AttachmentType {
    PDF_DOCUMENT,
    EXCEL_SHEET,
    IMAGE,
    VOICE_NOTE,
    CONTRACT
}

data class FileAttachment(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String,
    val type: AttachmentType,
    val fileSizeBytes: Long,
    val checksumSha256: String,
    val isEncrypted: Boolean = true,
    val previewUrl: String? = null
) {
    val formattedSize: String
        get() = when {
            fileSizeBytes >= 1024 * 1024 -> String.format("%.1f MB", fileSizeBytes / (1024.0 * 1024.0))
            else -> String.format("%d KB", fileSizeBytes / 1024)
        }
}

data class MessageReaction(
    val emoji: String,
    val count: Int,
    val userReacted: Boolean = false
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val channelId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: UserRole,
    val text: String,
    val ciphertext: String,
    val timestamp: Long = System.currentTimeMillis(),
    val formattedTime: String = "10:30 AM",
    val status: MessageStatus = MessageStatus.DELIVERED,
    val isFromMe: Boolean = false,
    val attachment: FileAttachment? = null,
    val stickerName: String? = null,
    val reactions: List<MessageReaction> = emptyList(),
    val disappearingDurationSeconds: Long = 0,
    val expiresAt: Long? = null, // timestamp when it should self-destruct
    val isBurned: Boolean = false
)

data class LinkedDevice(
    val id: String,
    val name: String,
    val browser: String,
    val os: String,
    val ipAddress: String,
    val location: String,
    val linkedDate: String,
    val lastActive: String,
    val isCurrent: Boolean = false
)

data class CallSession(
    val channelId: String,
    val peerName: String,
    val peerRole: String,
    val isConnected: Boolean = true,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = true,
    val safetySasCode: String = "9482-B2A4-F09C",
    val encryptionDetails: String = "E2EE SRTP / Opus 48kHz"
)

data class CloudBackupInfo(
    val lastBackupDate: String = "Today at 09:15 AM",
    val totalSizeMb: Double = 42.8,
    val encryptedFileCount: Int = 184,
    val isAutoBackupEnabled: Boolean = true,
    val destination: String = "Enterprise Cloud Vault (Frankfurt AZ-1)"
)
