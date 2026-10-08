package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import java.util.UUID

@Composable
fun CiphertextInspectDialog(
    message: ChatMessage,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("E2EE Cryptographic Payload", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column {
                Text(
                    text = "Raw Zero-Knowledge Encrypted Packet as transmitted across the wire:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SelectionContainer {
                        Text(
                            text = message.ciphertext,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(10.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Algorithm: AES-256-GCM + Double Ratchet Protocol",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun SafetyNumberDialog(
    channel: Channel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Verify Safety Numbers", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Compare this 60-digit security fingerprint with ${channel.name} to confirm end-to-end encryption integrity.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Simulated QR Code Matrix
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val gridSize = 14
                        val step = size.width / gridSize
                        for (i in 0 until gridSize) {
                            for (j in 0 until gridSize) {
                                // Deterministic pattern for security fingerprint QR
                                val fill = ((i * 7 + j * 13 + (channel.name.hashCode())) % 3) != 0
                                if (fill) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(i * step, j * step),
                                        size = Size(step * 0.9f, step * 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = channel.safetyNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mark as Verified")
            }
        }
    )
}

@Composable
fun ChannelSettingsDialog(
    channel: Channel,
    currentUserRole: UserRole,
    onSetDisappearingTimer: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTimer by remember { mutableStateOf(channel.disappearingSeconds) }
    val canConfigure = currentUserRole <= UserRole.MANAGER

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${channel.name} Security", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column {
                Text(
                    text = "Self-Destructing / Ephemeral Timer",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Messages in this channel burn automatically once read or expired.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                val timerOptions = listOf(
                    0L to "Off (Retain)",
                    5L to "5 seconds",
                    30L to "30 seconds",
                    60L to "1 minute",
                    300L to "5 minutes",
                    3600L to "1 hour",
                    86400L to "24 hours"
                )

                timerOptions.forEach { (seconds, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = canConfigure) { selectedTimer = seconds }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTimer == seconds,
                            onClick = { if (canConfigure) selectedTimer = seconds },
                            enabled = canConfigure
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (!canConfigure) {
                    Text(
                        text = "Notice: Only Managers and Admins can modify channel retention policies.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "RBAC Policy: Minimum role '${channel.requiredMinRoleToSend.displayName}' required to publish.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSetDisappearingTimer(selectedTimer) },
                enabled = canConfigure
            ) {
                Text("Save Policy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RoleSwitcherDialog(
    currentRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Simulate RBAC User Role", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column {
                Text(
                    text = "Select a business role to test permissions and access policies:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                UserRole.values().forEach { role ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onRoleSelected(role) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (role == currentRole) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = role == currentRole,
                                onClick = { onRoleSelected(role) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = role.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = when (role) {
                                        UserRole.ADMIN -> "Full access, audit logs, board channels, key revoke"
                                        UserRole.MANAGER -> "Manage channels, retention policy, file shares"
                                        UserRole.MEMBER -> "Internal team messaging, collaborate, react"
                                        UserRole.CLIENT_GUEST -> "Client portal only, restricted policies"
                                    },
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun AttachmentSheet(
    onFileSelected: (FileAttachment) -> Unit,
    onDismiss: () -> Unit
) {
    val enterpriseDocuments = listOf(
        FileAttachment(
            fileName = "Master_Services_Agreement_2026.pdf",
            type = AttachmentType.CONTRACT,
            fileSizeBytes = 2840000,
            checksumSha256 = "A7C49182DE10948BA0C716E982FBC019283748A9B817290123"
        ),
        FileAttachment(
            fileName = "Q3_Enterprise_Financial_Audit.xlsx",
            type = AttachmentType.EXCEL_SHEET,
            fileSizeBytes = 1450000,
            checksumSha256 = "182B09F48291A7C20183749204918239018475928173645019"
        ),
        FileAttachment(
            fileName = "ZeroTrust_Architecture_Blueprint.pdf",
            type = AttachmentType.PDF_DOCUMENT,
            fileSizeBytes = 5230000,
            checksumSha256 = "F0948172654398201948271049281726394850182736451928"
        ),
        FileAttachment(
            fileName = "Client_Brief_Confidential_VoiceNote.m4a",
            type = AttachmentType.VOICE_NOTE,
            fileSizeBytes = 890000,
            checksumSha256 = "65491827394850192837465019283746591029384756102938"
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Encrypted Enterprise File", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column {
                Text(
                    text = "Select a document to encrypt and transfer with SHA-256 integrity check:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(10.dp))
                enterpriseDocuments.forEach { file ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onFileSelected(file) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (file.type) {
                                    AttachmentType.CONTRACT -> Icons.Default.Verified
                                    AttachmentType.EXCEL_SHEET -> Icons.Default.TableChart
                                    AttachmentType.VOICE_NOTE -> Icons.Default.Mic
                                    else -> Icons.Default.PictureAsPdf
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = file.fileName, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(text = "${file.formattedSize} • E2EE Protected", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun StickerSheet(
    onStickerSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val stickers = listOf(
        "CONFIDENTIAL" to Color(0xFF7F0000),
        "APPROVED" to Color(0xFF1B5E20),
        "URGENT" to Color(0xFFB71C1C),
        "REVIEW REQUIRED" to Color(0xFFE65100),
        "SIGNED & VALIDATED" to Color(0xFF0D47A1),
        "PAID" to Color(0xFF004D40)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Business Stickers & Stamps", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(200.dp)
            ) {
                items(stickers) { (name, color) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStickerSelected(name) },
                        colors = CardDefaults.cardColors(containerColor = color)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun QrPairingDialog(
    onPairSuccess: (name: String, browser: String, os: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan Desktop QR Code", style = MaterialTheme.typography.titleMedium)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Point your camera at https://chat.mva-enterprise.com to link your PC browser securely.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera viewfinder",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Zero-Knowledge Session Handshake ready",
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onPairSuccess("Desktop Browser Client", "Chrome 129", "Windows 11")
                }
            ) {
                Text("Simulate Pairing Scan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
