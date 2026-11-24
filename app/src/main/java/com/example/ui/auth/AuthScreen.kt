package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.SchoolRepository
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Composable
fun AuthScreen(
    repository: SchoolRepository,
    language: AppLanguage,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val isAuthLoading by repository.isAuthLoading.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isGoogleSignInLoading by remember { mutableStateOf(false) }

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Error states
    var generalError by remember { mutableStateOf<String?>(null) }
    var identifierError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Phone OTP dialog states
    var showPhoneOtpDialog by remember { mutableStateOf(false) }
    var phoneNumberInput by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }
    var otpStep by remember { mutableIntStateOf(1) } // 1: Enter Phone, 2: Enter OTP
    var otpError by remember { mutableStateOf<String?>(null) }
    var otpLoading by remember { mutableStateOf(false) }

    // Forgot Password dialog states
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordEmail by remember { mutableStateOf("") }
    var forgotPasswordError by remember { mutableStateOf<String?>(null) }
    var forgotPasswordMessage by remember { mutableStateOf<String?>(null) }
    var forgotPasswordLoading by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // DBUS School Logo & Branding
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "DBUS",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = AppStrings.appTitle(language),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = AppStrings.appSubtitle(language),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Error Banner
            generalError?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                        .testTag("auth_error_banner"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Info card explaining real Supabase Login & automatic role routing
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrdu) {
                            "اپنے DBUS ایڈمنسٹریٹر کی جانب سے جاری کردہ اسکول آئی ڈی یا ای میل اور پاس ورڈ سے سائن ان کریں۔ آپ کا پورٹل خودکار طور پر کھل جائے گا۔"
                        } else {
                            "Sign in with your School ID or Email & Password issued by your DBUS Administrator. Your portal opens automatically."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = identifier,
                onValueChange = {
                    identifier = it
                    identifierError = null
                    generalError = null
                },
                enabled = !isAuthLoading,
                label = { Text(AppStrings.schoolIdOrEmailLabel(language)) },
                placeholder = {
                    Text(if (isUrdu) "اسکول آئی ڈی یا ای میل درج کریں" else "Enter School ID or Email")
                },
                isError = identifierError != null,
                supportingText = identifierError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_email"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                    generalError = null
                },
                enabled = !isAuthLoading,
                label = { Text(AppStrings.passwordLabel(language)) },
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                },
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (isUrdu) "پاس ورڈ دکھائیں یا چھپائیں" else "Toggle password visibility"
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_password"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Forgot Password link
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                TextButton(
                    enabled = !isAuthLoading,
                    onClick = {
                        forgotPasswordEmail = if (identifier.contains("@")) identifier else ""
                        forgotPasswordError = null
                        forgotPasswordMessage = null
                        showForgotPasswordDialog = true
                    },
                    modifier = Modifier.testTag("btn_forgot_password")
                ) {
                    Text(
                        text = AppStrings.forgotPassword(language),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Sign In Button (Supabase Authentication only)
            Button(
                enabled = !isAuthLoading,
                onClick = {
                    generalError = null
                    identifierError = null
                    passwordError = null

                    if (identifier.isBlank()) {
                        identifierError = if (isUrdu) "براہ کرم اسکول آئی ڈی یا ای میل درج کریں" else "Please enter your School ID or Email."
                        return@Button
                    }
                    if (password.isBlank()) {
                        passwordError = if (isUrdu) "براہ کرم پاس ورڈ درج کریں" else "Please enter your password."
                        return@Button
                    }

                    coroutineScope.launch {
                        val result = repository.loginWithSchoolIdOrEmail(identifier, password)
                        result.fold(
                            onSuccess = { onAuthSuccess() },
                            onFailure = { err ->
                                val msg = err.message ?: if (isUrdu) "سائن ان ناکام ہو گیا" else "Sign in failed."
                                if (msg.contains("password", ignoreCase = true) && !msg.contains("email", ignoreCase = true)) {
                                    passwordError = msg
                                } else {
                                    generalError = msg
                                }
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_auth_submit"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isAuthLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = AppStrings.login(language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real Phone OTP Sign In
            OutlinedButton(
                enabled = !isAuthLoading && !isGoogleSignInLoading,
                onClick = {
                    otpStep = 1
                    otpCodeInput = ""
                    otpError = null
                    showPhoneOtpDialog = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_phone_otp"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.phoneLogin(language),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Firebase Auth + Jetpack Credential Manager Google Sign-In
            OutlinedButton(
                enabled = !isAuthLoading && !isGoogleSignInLoading,
                onClick = {
                    generalError = null
                    identifierError = null
                    passwordError = null
                    isGoogleSignInLoading = true
                    signInWithGoogleCredentialManager(
                        context = context,
                        credentialManager = credentialManager,
                        scope = coroutineScope,
                        onTokenObtained = { idToken ->
                            coroutineScope.launch {
                                val res = repository.loginWithGoogleIdToken(idToken)
                                isGoogleSignInLoading = false
                                res.fold(
                                    onSuccess = { onAuthSuccess() },
                                    onFailure = { err ->
                                        generalError = err.message ?: if (isUrdu) "گوگل سائن ان ناکام ہو گیا" else "Google Sign-In failed."
                                    }
                                )
                            }
                        },
                        onAuthCancelled = {
                            isGoogleSignInLoading = false
                        },
                        onAuthError = { msg ->
                            isGoogleSignInLoading = false
                            generalError = msg
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_google_credential_signin"),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isGoogleSignInLoading) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = if (isUrdu) "گوگل کے ساتھ سائن ان کریں" else "Sign in with Google",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (isUrdu) "نئے اکاؤنٹس صرف DBUS ایڈمنسٹریٹر کی طرف سے بنائے جاتے ہیں۔" else "New accounts are created exclusively by your DBUS School Administrator.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }
    }

    // Real Supabase Phone OTP Dialog
    if (showPhoneOtpDialog) {
        AlertDialog(
            onDismissRequest = { if (!otpLoading) showPhoneOtpDialog = false },
            title = {
                Text(
                    text = if (otpStep == 1) (if (isUrdu) "فون نمبر سے لاگ ان" else "Phone Login (OTP)")
                    else (if (isUrdu) "او ٹی پی کوڈ درج کریں" else "Verify OTP Code"),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    otpError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (otpStep == 1) {
                        Text(
                            text = if (isUrdu) "اپنا رجسٹرڈ موبائل نمبر بین الاقوامی فارمیٹ میں درج کریں:" else "Enter your registered mobile number in international format:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = phoneNumberInput,
                            onValueChange = {
                                phoneNumberInput = it
                                otpError = null
                            },
                            enabled = !otpLoading,
                            label = { Text(if (isUrdu) "فون نمبر" else "Phone Number") },
                            placeholder = { Text("+92...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_phone_dialog"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true
                        )
                    } else {
                        Text(
                            text = if (isUrdu) "موصول ہونے والا 6 ہندسوں کا تصدیقی کوڈ درج کریں ($phoneNumberInput):" else "Enter the verification code sent to $phoneNumberInput:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = otpCodeInput,
                            onValueChange = {
                                otpCodeInput = it
                                otpError = null
                            },
                            enabled = !otpLoading,
                            label = { Text(if (isUrdu) "تصدیقی کوڈ" else "Verification Code") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_otp_code"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !otpLoading,
                    onClick = {
                        coroutineScope.launch {
                            otpLoading = true
                            otpError = null
                            if (otpStep == 1) {
                                val res = repository.requestPhoneOtp(phoneNumberInput)
                                res.fold(
                                    onSuccess = { otpStep = 2 },
                                    onFailure = { otpError = it.message ?: "Failed to send OTP." }
                                )
                            } else {
                                val res = repository.verifyPhoneOtp(phoneNumberInput, otpCodeInput)
                                res.fold(
                                    onSuccess = {
                                        showPhoneOtpDialog = false
                                        onAuthSuccess()
                                    },
                                    onFailure = { otpError = it.message ?: "OTP verification failed." }
                                )
                            }
                            otpLoading = false
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_otp")
                ) {
                    if (otpLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            if (otpStep == 1) (if (isUrdu) "کوڈ بھیجیں" else "Send OTP")
                            else (if (isUrdu) "تصدیق کریں" else "Verify & Sign In")
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !otpLoading,
                    onClick = { showPhoneOtpDialog = false }
                ) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Real Supabase Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!forgotPasswordLoading) showForgotPasswordDialog = false },
            title = {
                Text(
                    text = AppStrings.forgotPassword(language),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    forgotPasswordError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (forgotPasswordMessage == null) {
                        Text(
                            text = if (isUrdu) "اپنا رجسٹرڈ ای میل درج کریں۔ پاس ورڈ بحالی کا لنک بھیجا جائے گا:" else "Enter your registered email address to receive password reset instructions:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        OutlinedTextField(
                            value = forgotPasswordEmail,
                            onValueChange = {
                                forgotPasswordEmail = it
                                forgotPasswordError = null
                            },
                            enabled = !forgotPasswordLoading,
                            label = { Text(if (isUrdu) "ای میل ایڈریس" else "Email Address") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_forgot_email"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true
                        )
                    } else {
                        Text(
                            text = forgotPasswordMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !forgotPasswordLoading,
                    onClick = {
                        if (forgotPasswordMessage == null) {
                            coroutineScope.launch {
                                forgotPasswordLoading = true
                                forgotPasswordError = null
                                val res = repository.resetPassword(forgotPasswordEmail)
                                res.fold(
                                    onSuccess = { msg -> forgotPasswordMessage = msg },
                                    onFailure = { err -> forgotPasswordError = err.message ?: "Failed to send reset link." }
                                )
                                forgotPasswordLoading = false
                            }
                        } else {
                            showForgotPasswordDialog = false
                            forgotPasswordMessage = null
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_forgot_password")
                ) {
                    if (forgotPasswordLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            if (forgotPasswordMessage == null) (if (isUrdu) "لنک بھیجیں" else "Send Reset Link")
                            else (if (isUrdu) "ٹھیک ہے" else "Done")
                        )
                    }
                }
            },
            dismissButton = {
                if (forgotPasswordMessage == null) {
                    TextButton(
                        enabled = !forgotPasswordLoading,
                        onClick = { showForgotPasswordDialog = false }
                    ) {
                        Text(if (isUrdu) "منسوخ" else "Cancel")
                    }
                }
            }
        )
    }
}

private tailrec fun Context.findActivityOrNull(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivityOrNull()
    else -> null
}

private fun signInWithGoogleCredentialManager(
    context: Context,
    credentialManager: CredentialManager,
    scope: kotlinx.coroutines.CoroutineScope,
    onTokenObtained: (String) -> Unit,
    onAuthCancelled: () -> Unit,
    onAuthError: (String) -> Unit
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found.")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext: Context = context.findActivityOrNull() ?: context
            val credential = credentialManager.getCredential(activityContext, request).credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                awaitFirebaseGoogleSignIn(googleIdToken)
                onTokenObtained(googleIdToken)
            } else {
                onAuthError("Unexpected credential type returned by Google Sign-In.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Google Sign-In failed.")
        }
    }
}

private suspend fun awaitFirebaseGoogleSignIn(idToken: String) {
    suspendCancellableCoroutine<Unit> { cont ->
        val firebaseCred = GoogleAuthProvider.getCredential(idToken, null)
        Firebase.auth.signInWithCredential(firebaseCred)
            .addOnSuccessListener {
                if (cont.isActive) cont.resume(Unit)
            }
            .addOnFailureListener { exc ->
                if (cont.isActive) cont.resumeWithException(exc)
            }
    }
}
