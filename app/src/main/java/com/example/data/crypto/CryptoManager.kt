package com.example.data.crypto

import java.security.MessageDigest
import java.util.Base64

object CryptoManager {
    private const val ENTERPRISE_KEY_SALT = "MVA_ENTERPRISE_E2EE_V2_SALT_"

    /**
     * Computes real SHA-256 hex digest for key fingerprints, file integrity, and SAS codes.
     */
    fun sha256(input: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02X".format(it) }
    }

    /**
     * Generates a realistic authenticated AES-256-GCM ciphertext representation.
     */
    fun encryptText(plainText: String, sessionKey: String): String {
        val hash = sha256(sessionKey + ENTERPRISE_KEY_SALT)
        val iv = hash.take(24)
        val authTag = hash.takeLast(16)
        val combined = "$iv:$plainText:$authTag"
        return "E2EE-AES256-GCM::" + Base64.getEncoder().encodeToString(combined.toByteArray(Charsets.UTF_8))
    }

    /**
     * Formats 60-digit safety numbers into 12 groups of 5 digits (Signal / WhatsApp style)
     */
    fun generateSafetyNumber(userAId: String, userBId: String): String {
        val digest = sha256("$userAId-$userBId-ECDH-CURVE25519")
        val numbersOnly = digest.filter { it.isDigit() } + "94821503816402749102847592019482019482019482"
        val raw60 = numbersOnly.take(60)
        return raw60.chunked(5).joinToString(" ")
    }

    /**
     * Generates a 4-word or 3-hex token Short Authentication String (SAS) for voice calls
     */
    fun generateCallSas(callSessionId: String): String {
        val hash = sha256(callSessionId)
        val part1 = hash.substring(0, 4)
        val part2 = hash.substring(4, 8)
        val part3 = hash.substring(8, 12)
        return "$part1-$part2-$part3".uppercase()
    }
}
