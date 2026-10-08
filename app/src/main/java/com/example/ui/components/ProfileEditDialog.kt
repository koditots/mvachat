package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User

data class AvatarPreset(
    val id: Int,
    val name: String,
    val bgColor: Long,
    val iconVector: androidx.compose.ui.graphics.vector.ImageVector? = null
)

val AVATAR_PRESETS = listOf(
    AvatarPreset(1, "Executive Crimson", 0xFFB71C1C, Icons.Default.Shield),
    AvatarPreset(2, "Dark Slate Security", 0xFF263238, Icons.Default.Security),
    AvatarPreset(3, "Enterprise Sapphire", 0xFF0D47A1, Icons.Default.Business),
    AvatarPreset(4, "Verified Emerald", 0xFF1B5E20, Icons.Default.Verified),
    AvatarPreset(5, "Amber Operations", 0xFFD84315, Icons.Default.Engineering),
    AvatarPreset(6, "Deep Purple Enclave", 0xFF4A148C, Icons.Default.VpnKey)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditDialog(
    currentUser: User,
    onSaveProfile: (
        name: String,
        username: String,
        title: String,
        company: String,
        email: String,
        phone: String,
        statusBio: String,
        avatarId: Int,
        profilePicUri: String?
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(currentUser.name) }
    var username by remember { mutableStateOf(currentUser.username) }
    var title by remember { mutableStateOf(currentUser.title) }
    var company by remember { mutableStateOf(currentUser.company) }
    var email by remember { mutableStateOf(currentUser.email) }
    var phone by remember { mutableStateOf(currentUser.phone) }
    var statusBio by remember { mutableStateOf(currentUser.statusBio) }
    var selectedAvatarId by remember { mutableStateOf(currentUser.profileAvatarId) }
    var selectedPhotoName by remember { mutableStateOf<String?>(currentUser.profilePictureUri) }

    val currentPreset = AVATAR_PRESETS.find { it.id == selectedAvatarId } ?: AVATAR_PRESETS.first()
    val initials = name.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .uppercase()
        .ifEmpty { "MV" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Edit Business Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Update profile details & enterprise avatar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Live Avatar Preview
                Box(
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(Color(currentPreset.bgColor))
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedPhotoName != null) {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Profile Photo",
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                        } else {
                            Text(
                                text = initials,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 30.sp,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Online indicator badge
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Online",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                Text(
                    text = if (selectedPhotoName != null) "Photo: $selectedPhotoName" else "Avatar Style: ${currentPreset.name}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Avatar Presets / Color Palettes
                Text(
                    text = "Choose Business Avatar Palette",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(AVATAR_PRESETS) { preset ->
                        val isSelected = selectedAvatarId == preset.id && selectedPhotoName == null
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(preset.bgColor))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedAvatarId = preset.id
                                    selectedPhotoName = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (preset.iconVector != null) {
                                Icon(
                                    imageVector = preset.iconVector,
                                    contentDescription = preset.name,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated Photo Picker / Headshot Upload Button
                OutlinedButton(
                    onClick = {
                        selectedPhotoName = if (selectedPhotoName == null) "Executive_Headshot.jpg" else null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (selectedPhotoName != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedPhotoName != null) "Using Uploaded Headshot (Tap to Reset)" else "Upload Corporate Photo")
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Text Fields for Details
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Job Title") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_title_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company / Organization") },
                    leadingIcon = { Icon(Icons.Default.Business, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_company_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Corporate Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Secure Mobile / Extension") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = statusBio,
                    onValueChange = { statusBio = it },
                    label = { Text("Status / Enclave Bio") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveProfile(
                        name.trim().ifEmpty { currentUser.name },
                        username.removePrefix("@").trim().ifEmpty { currentUser.username },
                        title.trim().ifEmpty { currentUser.title },
                        company.trim().ifEmpty { currentUser.company },
                        email.trim().ifEmpty { currentUser.email },
                        phone.trim().ifEmpty { currentUser.phone },
                        statusBio.trim().ifEmpty { currentUser.statusBio },
                        selectedAvatarId,
                        selectedPhotoName
                    )
                },
                modifier = Modifier.testTag("btn_save_profile"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Profile")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
