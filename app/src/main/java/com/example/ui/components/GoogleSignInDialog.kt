package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountType
import com.example.data.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSignInDialog(
    currentUser: User,
    directoryUsers: List<User> = emptyList(),
    onSignInSuccess: (
        name: String,
        email: String,
        username: String,
        accountType: AccountType,
        company: String,
        title: String
    ) -> Unit,
    onSwitchAccount: (User) -> Unit = {},
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: Google Account Picker, 2: Business Profile & Username Setup
    var subTab by remember { mutableStateOf(0) } // 0: Google Sign In / Sign Up, 1: Switch Account

    // Verified Accounts from Directory & Current Profile
    val suggestedAccounts = remember(directoryUsers, currentUser) {
        if (directoryUsers.isNotEmpty()) {
            directoryUsers.map { Triple(it.name, it.email, it.avatarInitial) }
        } else {
            listOf(Triple(currentUser.name, currentUser.email, currentUser.avatarInitial))
        }
    }

    var selectedGoogleName by remember { mutableStateOf(currentUser.name) }
    var selectedGoogleEmail by remember { mutableStateOf(currentUser.email) }
    var customEmailInput by remember { mutableStateOf("") }
    var useCustomEmail by remember { mutableStateOf(false) }

    var usernameInput by remember {
        mutableStateOf(currentUser.username)
    }
    var fullNameInput by remember { mutableStateOf(currentUser.name) }
    var companyInput by remember { mutableStateOf(currentUser.company) }
    var titleInput by remember { mutableStateOf(currentUser.title) }
    var selectedAccountType by remember { mutableStateOf(currentUser.accountType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "G",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = Color(0xFFEA4335)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (step == 1) "Google Sign In & Registration" else "Create Unique Chat @Username",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (step == 1) "Sign up using your Google or Workspace account" else "How clients & partners will find and chat you up",
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
                if (step == 1) {
                    if (directoryUsers.isNotEmpty()) {
                        TabRow(
                            selectedTabIndex = subTab,
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            Tab(
                                selected = subTab == 0,
                                onClick = { subTab = 0 },
                                text = { Text("Google Sign In/Up", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                            Tab(
                                selected = subTab == 1,
                                onClick = { subTab = 1 },
                                text = { Text("Switch Account (${directoryUsers.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (subTab == 0) {
                        // Account Type selector upfront
                        Text(
                            text = "Choose Registration Classification:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AccountType.values().forEach { type ->
                                val isSelected = selectedAccountType == type
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedAccountType = type }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(10.dp)
                                        ),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = if (type == AccountType.BUSINESS) Icons.Default.Business else Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (type == AccountType.BUSINESS) "Business" else "Client",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Select a Google account to continue:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        suggestedAccounts.forEach { (name, email, initial) ->
                            val isSelected = !useCustomEmail && selectedGoogleEmail == email
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        useCustomEmail = false
                                        selectedGoogleName = name
                                        selectedGoogleEmail = email
                                        fullNameInput = name
                                        usernameInput = email.substringBefore("@").replace(".", "_")
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF5F6368)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initial,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = email,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (!useCustomEmail) {
                            TextButton(
                                onClick = { useCustomEmail = true },
                                modifier = Modifier.align(Alignment.Start)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Use another Google or Workspace email")
                            }
                        } else {
                            OutlinedTextField(
                                value = customEmailInput,
                                onValueChange = {
                                    customEmailInput = it
                                    selectedGoogleEmail = it
                                    if (it.contains("@")) {
                                        usernameInput = it.substringBefore("@").replace(".", "_")
                                    }
                                },
                                label = { Text("Google Workspace Email") },
                                placeholder = { Text("your.name@company.com") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    } else {
                        // Switch active user
                        Text(
                            text = "Currently Active: ${currentUser.name} (@${currentUser.username})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap any profile to switch identity and test receiving/finding messages:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        directoryUsers.forEach { user ->
                            val isCurrent = user.id == currentUser.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        onSwitchAccount(user)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = if (isCurrent) 2.dp else 1.dp,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(user.avatarBgColor)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = user.avatarInitial,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = user.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "@${user.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(text = user.email, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isCurrent) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("Active", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    } else {
                                        TextButton(onClick = { onSwitchAccount(user) }) {
                                            Text("Switch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Step 2: Username & Profile Customization
                    val activeEmail = if (useCustomEmail) customEmailInput else selectedGoogleEmail
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Google Account: $activeEmail", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Unique Username Field
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it.removePrefix("@").filter { char -> char.isLetterOrDigit() || char == '_' } },
                        label = { Text("Unique Username (for other users to find you)") },
                        prefix = { Text("@", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                        supportingText = {
                            Text("Other users or businesses can type @$usernameInput or your email to add and chat with you.", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        },
                        trailingIcon = {
                            if (usernameInput.isNotBlank()) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32))
                            }
                        },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_google_username"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = fullNameInput,
                        onValueChange = { fullNameInput = it },
                        label = { Text("Display Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = companyInput,
                        onValueChange = { companyInput = it },
                        label = { Text("Company / Enterprise") },
                        leadingIcon = { Icon(Icons.Default.CorporateFare, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Role Title") },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                if (subTab == 0) {
                    Button(
                        onClick = {
                            val email = if (useCustomEmail) customEmailInput.trim() else selectedGoogleEmail
                            if (email.isNotBlank()) {
                                step = 2
                            }
                        },
                        modifier = Modifier.testTag("btn_google_continue"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Continue to Username Setup")
                    }
                }
            } else {
                Button(
                    onClick = {
                        val email = if (useCustomEmail) customEmailInput.trim() else selectedGoogleEmail
                        val cleanUsername = usernameInput.trim().ifEmpty { email.substringBefore("@") }
                        val name = fullNameInput.trim().ifEmpty { selectedGoogleName }

                        onSignInSuccess(
                            name,
                            email,
                            cleanUsername,
                            selectedAccountType,
                            companyInput.trim(),
                            titleInput.trim()
                        )
                    },
                    modifier = Modifier.testTag("btn_complete_google_signup"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Launch Chat")
                }
            }
        },
        dismissButton = {
            if (step == 2) {
                TextButton(onClick = { step = 1 }) { Text("Back") }
            } else {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
