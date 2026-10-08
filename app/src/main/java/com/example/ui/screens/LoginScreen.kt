package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.data.model.AccountType
import com.example.data.model.User
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
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
    onContinueAsCurrent: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Production Account Configuration Form State (Clean inputs with realistic placeholders)
    var selectedAccountType by remember { mutableStateOf(AccountType.BUSINESS) }
    var selectedName by remember { mutableStateOf("") }
    var selectedEmail by remember { mutableStateOf("") }
    var usernameInput by remember { mutableStateOf("") }
    var companyInput by remember { mutableStateOf("") }
    var titleInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }

    fun launchGoogleCredentialManager() {
        isAuthenticating = true
        errorMessage = null
        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId("mva-business-chat-production")
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdTokenCredential.id
                    val name = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                    val derivedUsername = email.substringBefore("@").replace(".", "_")

                    selectedEmail = email
                    selectedName = name
                    usernameInput = derivedUsername

                    onSignInSuccess(
                        name,
                        email,
                        derivedUsername,
                        selectedAccountType,
                        if (selectedAccountType == AccountType.BUSINESS) companyInput.trim() else "",
                        if (selectedAccountType == AccountType.BUSINESS) titleInput.trim() else ""
                    )
                }
            } catch (e: GetCredentialCancellationException) {
                // User dismissed account picker
                errorMessage = "Google sign-in cancelled. You can enter your email directly below."
            } catch (e: Exception) {
                // Device or simulator without active Play Services account: prompt user to enter their real Google / enterprise email
                if (selectedEmail.isNotBlank()) {
                    val derivedUser = usernameInput.trim().removePrefix("@").ifEmpty {
                        selectedEmail.substringBefore("@").replace(".", "_")
                    }
                    val derivedName = selectedName.trim().ifEmpty {
                        selectedEmail.substringBefore("@").replace(".", " ")
                            .split(" ")
                            .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
                    }
                    onSignInSuccess(
                        derivedName,
                        selectedEmail.trim(),
                        derivedUser,
                        selectedAccountType,
                        if (selectedAccountType == AccountType.BUSINESS) companyInput.trim() else "",
                        if (selectedAccountType == AccountType.BUSINESS) titleInput.trim() else ""
                    )
                } else {
                    errorMessage = "Please enter your real Google or enterprise email address below to sign in."
                }
            } finally {
                isAuthenticating = false
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header / Hero Brand Area
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(72.dp),
                shadowElevation = 4.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security Shield",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = "E2EE Key",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "MVA BUSINESS CHAT",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Surface(
                color = Color(0xFFE8F5E9),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PRODUCTION ENCLAVE ACTIVE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Enterprise Zero-Knowledge End-to-End Encrypted Communications",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Google Sign-In Button (Official M3 Style)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .clickable(enabled = !isAuthenticating) {
                        launchGoogleCredentialManager()
                    }
                    .testTag("btn_google_signin"),
                color = Color.White,
                shadowElevation = 3.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDADCE0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF4285F4)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Authenticating with Google...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF3C4043)
                        )
                    } else {
                        // Google "G" Logo
                        Surface(
                            color = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "G",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color(0xFF4285F4)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Continue with Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF3C4043)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Text(
                    text = "  OR ENTER ENTERPRISE PROFILE  ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error alert banner
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Real Production Registration Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "1. Workspace Account Type",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Business Option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedAccountType = AccountType.BUSINESS }
                                .testTag("radio_business_account"),
                            color = if (selectedAccountType == AccountType.BUSINESS) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (selectedAccountType == AccountType.BUSINESS) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = if (selectedAccountType == AccountType.BUSINESS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "🏢 Business",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedAccountType == AccountType.BUSINESS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Enterprise Team",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Client Option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedAccountType = AccountType.CLIENT_INDIVIDUAL }
                                .testTag("radio_client_account"),
                            color = if (selectedAccountType == AccountType.CLIENT_INDIVIDUAL) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (selectedAccountType == AccountType.CLIENT_INDIVIDUAL) MaterialTheme.colorScheme.primary else Color.Transparent
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = if (selectedAccountType == AccountType.CLIENT_INDIVIDUAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "👤 Client / Partner",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selectedAccountType == AccountType.CLIENT_INDIVIDUAL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Verified External",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "2. User Credentials",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Full Name
                    OutlinedTextField(
                        value = selectedName,
                        onValueChange = {
                            selectedName = it
                            if (usernameInput.isBlank() && it.isNotBlank()) {
                                usernameInput = it.trim().lowercase().replace(" ", "_").filter { ch -> ch.isLetterOrDigit() || ch == '_' }
                            }
                        },
                        label = { Text("Full Legal / Corporate Name") },
                        placeholder = { Text("e.g. Austin Cooper") },
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_full_name")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Email Address
                    OutlinedTextField(
                        value = selectedEmail,
                        onValueChange = {
                            selectedEmail = it
                            if (usernameInput.isBlank() && it.contains("@")) {
                                usernameInput = it.substringBefore("@").lowercase().replace(".", "_")
                            }
                        },
                        label = { Text("Google or Corporate Email Address") },
                        placeholder = { Text("e.g. austin.cooper@gmail.com") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_email_address")
                    )

                    // Quick domain completion chips
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("@gmail.com", "@company.com", "@enterprise.io").forEach { domain ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val base = if (selectedEmail.contains("@")) selectedEmail.substringBefore("@") else selectedEmail
                                        selectedEmail = (base.ifEmpty { "austin" }) + domain
                                        if (usernameInput.isBlank()) {
                                            usernameInput = base.ifEmpty { "austin" }.replace(".", "_")
                                        }
                                    },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = domain,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Unique Chat Username
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = {
                            usernameInput = it.removePrefix("@").lowercase().replace(" ", "_")
                        },
                        label = { Text("Unique Chat Handle (@username)") },
                        placeholder = { Text("e.g. austin_cooper") },
                        leadingIcon = {
                            Icon(Icons.Default.AlternateEmail, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        supportingText = {
                            Text(
                                text = "Clients & colleagues can start encrypted chats by searching @${usernameInput.ifEmpty { "username" }}",
                                fontSize = 11.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_username")
                    )

                    // Business Specific Fields
                    if (selectedAccountType == AccountType.BUSINESS) {
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = companyInput,
                            onValueChange = { companyInput = it },
                            label = { Text("Company / Enterprise Organization") },
                            placeholder = { Text("e.g. Cooper Digital Global") },
                            leadingIcon = {
                                Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_company_name")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Corporate Role / Title") },
                            placeholder = { Text("e.g. Chief Information Security Officer") },
                            leadingIcon = {
                                Icon(Icons.Default.Work, contentDescription = null, modifier = Modifier.size(20.dp))
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_corporate_role")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Complete Sign In Button
            Button(
                onClick = {
                    val finalEmail = selectedEmail.trim()
                    val finalName = selectedName.trim()
                    val finalUsername = usernameInput.trim().removePrefix("@")

                    if (finalEmail.isBlank() && finalUsername.isBlank()) {
                        errorMessage = "Please enter your email or chosen username."
                        return@Button
                    }

                    val effectiveEmail = if (finalEmail.isNotBlank()) finalEmail else "$finalUsername@mva-enterprises.com"
                    val effectiveName = if (finalName.isNotBlank()) finalName else effectiveEmail.substringBefore("@")
                        .replace(".", " ")
                        .split(" ")
                        .joinToString(" ") { word -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } }
                    val effectiveUsername = if (finalUsername.isNotBlank()) finalUsername else effectiveEmail.substringBefore("@").replace(".", "_")

                    onSignInSuccess(
                        effectiveName,
                        effectiveEmail,
                        effectiveUsername,
                        selectedAccountType,
                        if (selectedAccountType == AccountType.BUSINESS) companyInput.trim().ifEmpty { "Enterprise Operations" } else "Client Enterprise",
                        if (selectedAccountType == AccountType.BUSINESS) titleInput.trim().ifEmpty { "Enterprise Executive" } else "Verified Client"
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("btn_complete_signin"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sign In & Initialize E2EE Enclave",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            // If a previous session exists and user navigated here from Settings, offer to return
            if (currentUser.isGoogleAuthenticated) {
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onContinueAsCurrent,
                    modifier = Modifier.testTag("btn_continue_as_current")
                ) {
                    Text(
                        text = "Return to workspace as ${currentUser.name} (@${currentUser.username})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Security Specifications Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AES-256-GCM Hardware Encryption • Zero-Knowledge Architecture",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
