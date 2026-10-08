package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*

@Composable
fun MessageItem(
    message: ChatMessage,
    onInspectCiphertext: (ChatMessage) -> Unit,
    onReact: (emoji: String) -> Unit,
    onOpenFile: (FileAttachment) -> Unit
) {
    var showQuickReactions by remember { mutableStateOf(false) }

    // Calculate remaining seconds if disappearing
    val remainingSeconds = remember(message.expiresAt, System.currentTimeMillis()) {
        message.expiresAt?.let { exp ->
            val diff = (exp - System.currentTimeMillis()) / 1000
            if (diff > 0) diff else 0
        }
    }

    val bubbleAlignment = if (message.isFromMe) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isFromMe) {
        if (message.isBurned) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.primary
    } else {
        if (message.isBurned) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surface
    }

    val textColor = if (message.isFromMe) {
        if (message.isBurned) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = bubbleAlignment
    ) {
        // Sender name & role badge (for received messages or group channels)
        if (!message.isFromMe) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
            ) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    color = Color(message.senderRole.badgeColor).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = message.senderRole.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(message.senderRole.badgeColor),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Main Bubble Card
        Card(
            modifier = Modifier
                .widthIn(min = 120.dp, max = 320.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                        bottomEnd = if (message.isFromMe) 4.dp else 16.dp
                    )
                )
                .clickable { showQuickReactions = !showQuickReactions }
                .testTag("message_card_${message.id}"),
            colors = CardDefaults.cardColors(containerColor = bubbleColor),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {

                // Disappearing countdown header or incinerated status
                if (message.isBurned) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Burned",
                            tint = Color(0xFFD84315),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "INCINERATED SECURELY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD84315),
                            letterSpacing = 0.5.sp
                        )
                    }
                } else if (remainingSeconds != null && remainingSeconds > 0) {
                    Surface(
                        color = if (message.isFromMe) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Timer",
                                tint = if (message.isFromMe) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Self-destructs in ${remainingSeconds}s",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (message.isFromMe) Color.White else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // File Attachment (if present and not burned)
                if (message.attachment != null && !message.isBurned) {
                    val att = message.attachment
                    Surface(
                        color = if (message.isFromMe) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { onOpenFile(att) }
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (att.type) {
                                            AttachmentType.CONTRACT -> Color(0xFF1565C0)
                                            AttachmentType.PDF_DOCUMENT -> Color(0xFFC62828)
                                            AttachmentType.EXCEL_SHEET -> Color(0xFF2E7D32)
                                            AttachmentType.VOICE_NOTE -> Color(0xFF6A1B9A)
                                            AttachmentType.IMAGE -> Color(0xFFEF6C00)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (att.type) {
                                        AttachmentType.CONTRACT -> Icons.Default.Verified
                                        AttachmentType.PDF_DOCUMENT -> Icons.Default.PictureAsPdf
                                        AttachmentType.EXCEL_SHEET -> Icons.Default.TableChart
                                        AttachmentType.VOICE_NOTE -> Icons.Default.Mic
                                        AttachmentType.IMAGE -> Icons.Default.Image
                                    },
                                    contentDescription = "File Icon",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = att.fileName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${att.formattedSize} • E2EE Encrypted",
                                        fontSize = 10.sp,
                                        color = textColor.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = textColor.copy(alpha = 0.8f),
                                        modifier = Modifier.size(9.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Business Sticker (if present and not burned)
                if (message.stickerName != null && !message.isBurned) {
                    val (stickerBg, stickerFg) = when (message.stickerName) {
                        "CONFIDENTIAL" -> Color(0xFF7F0000) to Color.White
                        "APPROVED" -> Color(0xFF1B5E20) to Color.White
                        "URGENT" -> Color(0xFFB71C1C) to Color.White
                        "REVIEW REQUIRED" -> Color(0xFFE65100) to Color.White
                        "SIGNED & VALIDATED" -> Color(0xFF0D47A1) to Color.White
                        "PAID" -> Color(0xFF004D40) to Color.White
                        else -> Color(0xFF37474F) to Color.White
                    }

                    Surface(
                        color = stickerBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = stickerFg,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = message.stickerName,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                letterSpacing = 1.sp,
                                color = stickerFg
                            )
                        }
                    }
                }

                // Message Text Content
                if (message.text.isNotBlank()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Footer: Timestamp, Ciphertext Key Icon, Delivery Status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tap to inspect AES ciphertext
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "Inspect Ciphertext",
                        tint = textColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .size(12.dp)
                            .clickable { onInspectCiphertext(message) }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = message.formattedTime,
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )

                    if (message.isFromMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        when (message.status) {
                            MessageStatus.PENDING_OFFLINE -> {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Pending Offline Sync",
                                    tint = textColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageStatus.ENCRYPTING -> {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = "Encrypting",
                                    tint = textColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageStatus.SENT -> {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Sent",
                                    tint = textColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageStatus.DELIVERED, MessageStatus.READ -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Delivered",
                                    tint = if (message.status == MessageStatus.READ) Color(0xFF64B5F6) else textColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Existing Reactions Pills
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 2.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                message.reactions.forEach { reaction ->
                    Surface(
                        color = if (reaction.userReacted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.clickable { onReact(reaction.emoji) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = reaction.emoji, fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = reaction.count.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick Emoji Reaction Bar (Revealed on click)
        if (showQuickReactions) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val emojis = listOf("👍", "🔒", "💼", "🚀", "❤️", "👏")
                    emojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            fontSize = 18.sp,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    onReact(emoji)
                                    showQuickReactions = false
                                }
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
