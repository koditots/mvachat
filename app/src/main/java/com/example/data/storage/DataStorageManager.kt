package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject

class DataStorageManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mva_business_chat_production_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_GOOGLE_SIGNED_IN = "is_google_signed_in"
        private const val KEY_CURRENT_USER = "current_user_json"
        private const val KEY_CHANNELS = "channels_json"
        private const val KEY_MESSAGES = "messages_json"
        private const val KEY_DIRECTORY = "directory_users_json"
        private const val KEY_DARK_MODE = "is_dark_mode"
        private const val KEY_BATTERY_SAVER = "is_battery_saver"
        private const val KEY_BIOMETRIC_ENABLED = "is_biometric_enabled"
        private const val KEY_AUTOLOCK_TIMEOUT = "autolock_timeout_minutes"
    }

    fun isGoogleSignedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_GOOGLE_SIGNED_IN, false)
    }

    fun setGoogleSignedIn(signedIn: Boolean) {
        prefs.edit().putBoolean(KEY_IS_GOOGLE_SIGNED_IN, signedIn).apply()
    }

    fun saveCurrentUser(user: User) {
        val json = userToJson(user).toString()
        prefs.edit().putString(KEY_CURRENT_USER, json).apply()
    }

    fun loadCurrentUser(): User? {
        val jsonStr = prefs.getString(KEY_CURRENT_USER, null) ?: return null
        return try {
            userFromJson(JSONObject(jsonStr))
        } catch (e: Exception) {
            null
        }
    }

    fun saveChannels(channels: List<Channel>) {
        val arr = JSONArray()
        channels.forEach { arr.put(channelToJson(it)) }
        prefs.edit().putString(KEY_CHANNELS, arr.toString()).apply()
    }

    fun loadChannels(): List<Channel>? {
        val jsonStr = prefs.getString(KEY_CHANNELS, null) ?: return null
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Channel>()
            for (i in 0 until arr.length()) {
                list.add(channelFromJson(arr.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    fun saveMessages(messagesMap: Map<String, List<ChatMessage>>) {
        val obj = JSONObject()
        messagesMap.forEach { (channelId, msgs) ->
            val arr = JSONArray()
            msgs.forEach { arr.put(messageToJson(it)) }
            obj.put(channelId, arr)
        }
        prefs.edit().putString(KEY_MESSAGES, obj.toString()).apply()
    }

    fun loadMessages(): Map<String, List<ChatMessage>>? {
        val jsonStr = prefs.getString(KEY_MESSAGES, null) ?: return null
        return try {
            val obj = JSONObject(jsonStr)
            val map = mutableMapOf<String, List<ChatMessage>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val arr = obj.getJSONArray(key)
                val list = mutableListOf<ChatMessage>()
                for (i in 0 until arr.length()) {
                    list.add(messageFromJson(arr.getJSONObject(i)))
                }
                map[key] = list
            }
            map
        } catch (e: Exception) {
            null
        }
    }

    fun saveDirectoryUsers(users: List<User>) {
        val arr = JSONArray()
        users.forEach { arr.put(userToJson(it)) }
        prefs.edit().putString(KEY_DIRECTORY, arr.toString()).apply()
    }

    fun loadDirectoryUsers(): List<User>? {
        val jsonStr = prefs.getString(KEY_DIRECTORY, null) ?: return null
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<User>()
            for (i in 0 until arr.length()) {
                list.add(userFromJson(arr.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            null
        }
    }

    fun saveDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun loadDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    fun saveBatterySaver(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BATTERY_SAVER, enabled).apply()
    }

    fun loadBatterySaver(): Boolean = prefs.getBoolean(KEY_BATTERY_SAVER, false)

    fun saveBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun loadBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun saveAutoLockTimeout(minutes: Int) {
        prefs.edit().putInt(KEY_AUTOLOCK_TIMEOUT, minutes).apply()
    }

    fun loadAutoLockTimeout(): Int = prefs.getInt(KEY_AUTOLOCK_TIMEOUT, 1)

    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_IS_GOOGLE_SIGNED_IN, false)
            .remove(KEY_CURRENT_USER)
            .apply()
    }

    // JSON Serializers
    private fun userToJson(u: User): JSONObject {
        return JSONObject().apply {
            put("id", u.id)
            put("name", u.name)
            put("username", u.username)
            put("title", u.title)
            put("company", u.company)
            put("email", u.email)
            put("phone", u.phone)
            put("statusBio", u.statusBio)
            put("role", u.role.name)
            put("accountType", u.accountType.name)
            put("isGoogleAuthenticated", u.isGoogleAuthenticated)
            put("googleId", u.googleId)
            put("avatarInitial", u.avatarInitial)
            put("avatarBgColor", u.avatarBgColor)
            put("profilePictureUri", u.profilePictureUri ?: "")
            put("profileAvatarId", u.profileAvatarId)
            put("isOnline", u.isOnline)
            put("keyFingerprint", u.keyFingerprint)
        }
    }

    private fun userFromJson(j: JSONObject): User {
        val role = try {
            UserRole.valueOf(j.optString("role", UserRole.ADMIN.name))
        } catch (e: Exception) {
            UserRole.ADMIN
        }
        val accountType = try {
            AccountType.valueOf(j.optString("accountType", AccountType.BUSINESS.name))
        } catch (e: Exception) {
            AccountType.BUSINESS
        }
        return User(
            id = j.getString("id"),
            name = j.getString("name"),
            username = j.optString("username", "user"),
            title = j.optString("title", "Enterprise Member"),
            company = j.optString("company", "Enterprise"),
            email = j.optString("email", ""),
            phone = j.optString("phone", "+1 (555) 019-2834"),
            statusBio = j.optString("statusBio", "🔒 Encrypted via MVA Enclave • Active"),
            role = role,
            accountType = accountType,
            isGoogleAuthenticated = j.optBoolean("isGoogleAuthenticated", true),
            googleId = j.optString("googleId", ""),
            avatarInitial = j.optString("avatarInitial", "U"),
            avatarBgColor = j.optLong("avatarBgColor", 0xFFB71C1CL),
            profilePictureUri = j.optString("profilePictureUri").ifEmpty { null },
            profileAvatarId = j.optInt("profileAvatarId", 1),
            isOnline = j.optBoolean("isOnline", true),
            keyFingerprint = j.optString("keyFingerprint", "E2EE-ENTERPRISE-KEY")
        )
    }

    private fun channelToJson(c: Channel): JSONObject {
        return JSONObject().apply {
            put("id", c.id)
            put("name", c.name)
            put("description", c.description)
            put("type", c.type.name)
            val memberArr = JSONArray()
            c.memberIds.forEach { memberArr.put(it) }
            put("memberIds", memberArr)
            put("isEncrypted", c.isEncrypted)
            put("encryptionStandard", c.encryptionStandard)
            put("safetyNumber", c.safetyNumber)
            put("disappearingSeconds", c.disappearingSeconds)
            put("requiredMinRoleToSend", c.requiredMinRoleToSend.name)
            put("allowClientFiles", c.allowClientFiles)
            put("lastMessageText", c.lastMessageText)
            put("lastMessageTime", c.lastMessageTime)
            put("unreadCount", c.unreadCount)
            put("avatarInitial", c.avatarInitial)
            put("avatarBgColor", c.avatarBgColor)
            put("avatarId", c.avatarId)
            put("isPeerOnline", c.isPeerOnline)
        }
    }

    private fun channelFromJson(j: JSONObject): Channel {
        val type = try {
            ChannelType.valueOf(j.optString("type", ChannelType.DIRECT.name))
        } catch (e: Exception) {
            ChannelType.DIRECT
        }
        val minRole = try {
            UserRole.valueOf(j.optString("requiredMinRoleToSend", UserRole.CLIENT_GUEST.name))
        } catch (e: Exception) {
            UserRole.CLIENT_GUEST
        }
        val memberArr = j.optJSONArray("memberIds")
        val members = mutableListOf<String>()
        if (memberArr != null) {
            for (i in 0 until memberArr.length()) {
                members.add(memberArr.getString(i))
            }
        }
        return Channel(
            id = j.getString("id"),
            name = j.getString("name"),
            description = j.optString("description", ""),
            type = type,
            memberIds = members,
            isEncrypted = j.optBoolean("isEncrypted", true),
            encryptionStandard = j.optString("encryptionStandard", "AES-256-GCM + RSA-4096"),
            safetyNumber = j.optString("safetyNumber", "SAFETY-VERIFIED"),
            disappearingSeconds = j.optLong("disappearingSeconds", 0L),
            requiredMinRoleToSend = minRole,
            allowClientFiles = j.optBoolean("allowClientFiles", true),
            lastMessageText = j.optString("lastMessageText", ""),
            lastMessageTime = j.optString("lastMessageTime", "Just now"),
            unreadCount = j.optInt("unreadCount", 0),
            avatarInitial = j.optString("avatarInitial", "CH"),
            avatarBgColor = j.optLong("avatarBgColor", 0xFF1565C0L),
            avatarId = j.optInt("avatarId", 1),
            isPeerOnline = j.optBoolean("isPeerOnline", true)
        )
    }

    private fun messageToJson(m: ChatMessage): JSONObject {
        return JSONObject().apply {
            put("id", m.id)
            put("channelId", m.channelId)
            put("senderId", m.senderId)
            put("senderName", m.senderName)
            put("senderRole", m.senderRole.name)
            put("text", m.text)
            put("ciphertext", m.ciphertext)
            put("timestamp", m.timestamp)
            put("formattedTime", m.formattedTime)
            put("status", m.status.name)
            put("isFromMe", m.isFromMe)
            put("stickerName", m.stickerName ?: "")
            put("disappearingDurationSeconds", m.disappearingDurationSeconds)
            put("expiresAt", m.expiresAt ?: -1L)
            put("isBurned", m.isBurned)
            if (m.attachment != null) {
                val attObj = JSONObject().apply {
                    put("id", m.attachment.id)
                    put("fileName", m.attachment.fileName)
                    put("type", m.attachment.type.name)
                    put("fileSizeBytes", m.attachment.fileSizeBytes)
                    put("checksumSha256", m.attachment.checksumSha256)
                    put("isEncrypted", m.attachment.isEncrypted)
                    put("previewUrl", m.attachment.previewUrl ?: "")
                }
                put("attachment", attObj)
            }
        }
    }

    private fun messageFromJson(j: JSONObject): ChatMessage {
        val role = try {
            UserRole.valueOf(j.optString("senderRole", UserRole.MEMBER.name))
        } catch (e: Exception) {
            UserRole.MEMBER
        }
        val status = try {
            MessageStatus.valueOf(j.optString("status", MessageStatus.DELIVERED.name))
        } catch (e: Exception) {
            MessageStatus.DELIVERED
        }
        var attachment: FileAttachment? = null
        if (j.has("attachment") && !j.isNull("attachment")) {
            val aObj = j.getJSONObject("attachment")
            val attType = try {
                AttachmentType.valueOf(aObj.optString("type", AttachmentType.PDF_DOCUMENT.name))
            } catch (e: Exception) {
                AttachmentType.PDF_DOCUMENT
            }
            attachment = FileAttachment(
                id = aObj.getString("id"),
                fileName = aObj.getString("fileName"),
                type = attType,
                fileSizeBytes = aObj.optLong("fileSizeBytes", 1024L),
                checksumSha256 = aObj.optString("checksumSha256", ""),
                isEncrypted = aObj.optBoolean("isEncrypted", true),
                previewUrl = aObj.optString("previewUrl").ifEmpty { null }
            )
        }
        val exp = j.optLong("expiresAt", -1L)
        return ChatMessage(
            id = j.getString("id"),
            channelId = j.getString("channelId"),
            senderId = j.getString("senderId"),
            senderName = j.getString("senderName"),
            senderRole = role,
            text = j.getString("text"),
            ciphertext = j.optString("ciphertext", ""),
            timestamp = j.optLong("timestamp", System.currentTimeMillis()),
            formattedTime = j.optString("formattedTime", "10:00 AM"),
            status = status,
            isFromMe = j.optBoolean("isFromMe", false),
            attachment = attachment,
            stickerName = j.optString("stickerName").ifEmpty { null },
            disappearingDurationSeconds = j.optLong("disappearingDurationSeconds", 0L),
            expiresAt = if (exp > 0) exp else null,
            isBurned = j.optBoolean("isBurned", false)
        )
    }
}
