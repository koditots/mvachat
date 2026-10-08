package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Channel
import com.example.data.model.ChannelType
import com.example.data.model.User

@Composable
fun ChatListScreen(
    channels: List<Channel>,
    currentUser: User,
    selectedTypeFilter: ChannelType?,
    searchQuery: String,
    unreadOnlyFilter: Boolean = false,
    onFilterChanged: (ChannelType?) -> Unit,
    onUnreadFilterChanged: (Boolean) -> Unit = {},
    onMarkAllReadClick: () -> Unit = {},
    onSearchChanged: (String) -> Unit,
    onChannelClick: (String) -> Unit,
    onNewChatClick: () -> Unit,
    onEditProfileClick: () -> Unit
) {
    val totalUnreadCount = remember(channels) { channels.sumOf { it.unreadCount } }
    val unreadConversationsCount = remember(channels) { channels.count { it.unreadCount > 0 } }

    val filteredChannels = remember(channels, selectedTypeFilter, searchQuery, unreadOnlyFilter) {
        channels.filter { ch ->
            val matchesUnread = !unreadOnlyFilter || ch.unreadCount > 0
            val matchesType = selectedTypeFilter == null || ch.type == selectedTypeFilter
            val matchesSearch = searchQuery.isBlank() ||
                    ch.name.contains(searchQuery, ignoreCase = true) ||
                    ch.lastMessageText.contains(searchQuery, ignoreCase = true)
            matchesUnread && matchesType && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewChatClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_new_chat")
            ) {
                Icon(Icons.Default.EditNote, contentDescription = "New Encrypted Channel")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // User Business Profile Header Card
            UserProfileHeaderCard(
                user = currentUser,
                unreadTotalCount = totalUnreadCount,
                onEditProfileClick = onEditProfileClick
            )

            // Search Bar (Red & White accented)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChanged,
                placeholder = { Text("Search conversations, clients, encrypted files...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChanged("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("chat_search_field")
            )

            // Category & Unread Filter Pills Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Dedicated Unread Filter Pill
                item {
                    FilterChip(
                        selected = unreadOnlyFilter,
                        onClick = { onUnreadFilterChanged(!unreadOnlyFilter) },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Unread")
                                if (unreadConversationsCount > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        color = if (unreadOnlyFilter) Color.White else MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp),
                                        contentColor = if (unreadOnlyFilter) MaterialTheme.colorScheme.primary else Color.White
                                    ) {
                                        Text(
                                            text = "$unreadConversationsCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (unreadOnlyFilter) Icons.Default.MarkChatUnread else Icons.Default.NotificationsActive,
                                contentDescription = "Unread Filter",
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("filter_chip_unread")
                    )
                }

                item {
                    FilterChip(
                        selected = !unreadOnlyFilter && selectedTypeFilter == null,
                        onClick = {
                            onUnreadFilterChanged(false)
                            onFilterChanged(null)
                        },
                        label = { Text("All (${channels.size})") },
                        leadingIcon = if (!unreadOnlyFilter && selectedTypeFilter == null) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedTypeFilter == ChannelType.CLIENT_PORTAL,
                        onClick = {
                            onUnreadFilterChanged(false)
                            onFilterChanged(ChannelType.CLIENT_PORTAL)
                        },
                        label = { Text("Client Portals") },
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedTypeFilter == ChannelType.INTERNAL_TEAM,
                        onClick = {
                            onUnreadFilterChanged(false)
                            onFilterChanged(ChannelType.INTERNAL_TEAM)
                        },
                        label = { Text("Internal Teams") },
                        leadingIcon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedTypeFilter == ChannelType.EXECUTIVE_BOARD,
                        onClick = {
                            onUnreadFilterChanged(false)
                            onFilterChanged(ChannelType.EXECUTIVE_BOARD)
                        },
                        label = { Text("Executive Board") },
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }

                item {
                    FilterChip(
                        selected = selectedTypeFilter == ChannelType.DIRECT,
                        onClick = {
                            onUnreadFilterChanged(false)
                            onFilterChanged(ChannelType.DIRECT)
                        },
                        label = { Text("Direct E2EE") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Recent Conversations Header with Unread Stats & Mark Read Action
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (unreadOnlyFilter) "Unread Conversations" else "Recent Conversations",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (totalUnreadCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "$totalUnreadCount new",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (totalUnreadCount > 0) {
                        TextButton(
                            onClick = onMarkAllReadClick,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp).testTag("btn_mark_all_read")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Mark All Read",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        Text(
                            text = "${filteredChannels.size} Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Conversations List
            if (filteredChannels.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (unreadOnlyFilter) Icons.Default.CheckCircleOutline else Icons.Default.ChatBubbleOutline,
                            contentDescription = null,
                            tint = if (unreadOnlyFilter) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (unreadOnlyFilter) "All caught up! No unread messages."
                            else "No conversations matching \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredChannels) { channel ->
                        ChannelListItem(
                            channel = channel,
                            onClick = { onChannelClick(channel.id) }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserProfileHeaderCard(
    user: User,
    unreadTotalCount: Int = 0,
    onEditProfileClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onEditProfileClick)
            .testTag("user_profile_header_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: User Avatar with edit indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(user.avatarBgColor))
                            .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.profilePictureUri != null) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Profile Photo",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Text(
                                text = user.avatarInitial,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Online green badge
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = user.role.displayName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "${user.title} • ${user.company}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.statusBio,
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (unreadTotalCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "$unreadTotalCount new",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right: Edit Profile Action Button
            IconButton(
                onClick = onEditProfileClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .testTag("btn_edit_profile")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Profile",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ChannelListItem(
    channel: Channel,
    onClick: () -> Unit
) {
    val isUnread = channel.unreadCount > 0
    val (avatarColor, initialText) = when (channel.type) {
        ChannelType.EXECUTIVE_BOARD -> Color(0xFF7F0000) to channel.avatarInitial.ifEmpty { "EB" }
        ChannelType.CLIENT_PORTAL -> Color(0xFF1B5E20) to channel.avatarInitial.ifEmpty { "AC" }
        ChannelType.INTERNAL_TEAM -> Color(0xFF0D47A1) to channel.avatarInitial.ifEmpty { "EN" }
        ChannelType.DIRECT -> Color(0xFFD84315) to channel.avatarInitial.ifEmpty { "SL" }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (isUnread) MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                else Color.Transparent
            )
            .testTag("channel_item_${channel.id}")
    ) {
        // Vertical Left Crimson Accent Bar for unread items
        if (isUnread) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(56.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("unread_accent_bar_${channel.id}")
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Channel Avatar with security badge, online dot, AND prominent unread dot indicator
            Box {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(avatarColor)
                        .border(
                            width = if (isUnread) 2.dp else 1.5.dp,
                            color = if (isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                // Small E2EE padlock indicator at bottom-left corner
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.BottomStart)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32))
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "E2EE",
                        tint = Color.White,
                        modifier = Modifier.size(9.dp)
                    )
                }

                // Peer Online status dot at bottom-right
                if (channel.isPeerOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    )
                }

                // Prominent Avatar Unread Indicator Dot on Top-Right
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Center: Title, Role Badge, Disappearing Timer, Last Message
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // Unread Dot next to conversation name
                        if (isUnread) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }

                        Text(
                            text = channel.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = channel.lastMessageTime,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                        color = if (isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Sub-tags row: Category pill & Disappearing timer badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Surface(
                        color = if (isUnread) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when (channel.type) {
                                ChannelType.EXECUTIVE_BOARD -> "Board Only"
                                ChannelType.CLIENT_PORTAL -> "Client Portal"
                                ChannelType.INTERNAL_TEAM -> "Internal Team"
                                ChannelType.DIRECT -> "Direct E2EE"
                            },
                            fontSize = 9.sp,
                            fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isUnread) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    if (channel.disappearingSeconds > 0) {
                        val timerLabel = when (channel.disappearingSeconds) {
                            5L -> "5s"
                            30L -> "30s"
                            60L -> "1m"
                            300L -> "5m"
                            3600L -> "1h"
                            else -> "24h"
                        }
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = Color(0xFFE65100),
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = timerLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }

                // Message preview row with status check & Prominent Red Unread Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!isUnread) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = "New message",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = channel.lastMessageText.ifEmpty { "Encrypted channel established" },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Prominent Crimson Red Unread Badge Pill
                    if (isUnread) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 2.dp,
                            modifier = Modifier.testTag("unread_badge_${channel.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (channel.unreadCount > 99) "99+" else "${channel.unreadCount}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
