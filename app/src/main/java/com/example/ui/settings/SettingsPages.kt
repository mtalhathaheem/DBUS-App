package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SchoolRepository
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.TextScale
import com.example.model.ThemeMode
import com.example.model.ThemePalette
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.RoleBadge
import java.util.Calendar
import kotlinx.coroutines.launch

enum class SettingsSubPage {
    MAIN,
    ACCOUNT,
    PRIVACY_SECURITY,
    APPEARANCE,
    NOTIFICATIONS,
    LANGUAGE_REGION,
    HELP_ABOUT
}

@Composable
fun SettingsContainer(
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(SettingsSubPage.MAIN) }
    BackHandler(enabled = currentPage != SettingsSubPage.MAIN) {
        currentPage = SettingsSubPage.MAIN
    }

    when (currentPage) {
        SettingsSubPage.MAIN -> {
            SettingsMainList(
                repository = repository,
                currentUser = currentUser,
                language = language,
                onSelectPage = { currentPage = it },
                onLogout = onLogout,
                modifier = modifier
            )
        }
        SettingsSubPage.ACCOUNT -> {
            AccountPage(
                repository = repository,
                currentUser = currentUser,
                language = language,
                onBack = { currentPage = SettingsSubPage.MAIN },
                onLogout = onLogout,
                modifier = modifier
            )
        }
        SettingsSubPage.PRIVACY_SECURITY -> {
            PrivacySecurityPage(
                repository = repository,
                currentUser = currentUser,
                language = language,
                onBack = { currentPage = SettingsSubPage.MAIN },
                modifier = modifier
            )
        }
        SettingsSubPage.APPEARANCE -> {
            AppearancePage(
                repository = repository,
                language = language,
                onBack = { currentPage = SettingsSubPage.MAIN },
                modifier = modifier
            )
        }
        SettingsSubPage.NOTIFICATIONS -> {
            NotificationsPage(
                repository = repository,
                language = language,
                onBack = { currentPage = SettingsSubPage.MAIN },
                modifier = modifier
            )
        }
        SettingsSubPage.LANGUAGE_REGION -> {
            LanguageRegionPage(
                repository = repository,
                language = language,
                onBack = { currentPage = SettingsSubPage.MAIN },
                modifier = modifier
            )
        }
        SettingsSubPage.HELP_ABOUT -> {
            HelpAboutPage(
                language = language,
                onBack = { currentPage = SettingsSubPage.MAIN },
                modifier = modifier
            )
        }
    }
}

@Composable
fun SettingsMainList(
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    onSelectPage: (SettingsSubPage) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = AppStrings.settings(language),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isUrdu) "سسٹم، اکاؤنٹ اور ڈسپلے کی ترتیبات"
                else "System, Account & Display Preferences",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Account Profile Header Card
        if (currentUser != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPage(SettingsSubPage.ACCOUNT) }
                        .testTag("settings_account_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(currentUser.avatarColorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.avatarInitials,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            RoleBadge(role = currentUser.role, language = language)
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 6 Required Settings Sections
        val menuItems = listOf(
            Triple(SettingsSubPage.ACCOUNT, AppStrings.account(language), Icons.Default.AccountCircle),
            Triple(SettingsSubPage.PRIVACY_SECURITY, AppStrings.privacyAndSecurity(language), Icons.Default.Security),
            Triple(SettingsSubPage.APPEARANCE, AppStrings.appearance(language), Icons.Default.Palette),
            Triple(SettingsSubPage.NOTIFICATIONS, AppStrings.notifications(language), Icons.Default.Notifications),
            Triple(SettingsSubPage.LANGUAGE_REGION, AppStrings.languageAndRegion(language), Icons.Default.Language),
            Triple(SettingsSubPage.HELP_ABOUT, AppStrings.helpAndAbout(language), Icons.Default.Help)
        )

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    menuItems.forEachIndexed { index, (page, title, icon) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPage(page) }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("menu_${page.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (index < menuItems.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        // Sign Out Button (Requirement 4: Sign Out must work)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_settings_logout"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.logout(language),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// 1. ACCOUNT PAGE (Requirement 6: edit name, photo, phone, change password)
@Composable
fun AccountPage(
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    var editName by remember { mutableStateOf(currentUser?.name ?: "") }
    var editPhone by remember { mutableStateOf(currentUser?.phone ?: "") }
    var selectedAvatarColor by remember { mutableStateOf(currentUser?.avatarColorHex ?: 0xFF1B3B6F) }

    val coroutineScope = rememberCoroutineScope()
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordChangeMessage by remember { mutableStateOf<String?>(null) }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }
    var isUpdatingPassword by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_account")) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.account(language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (currentUser != null) {
            // Profile Card with Photo & Edit Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color(currentUser.avatarColorHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.avatarInitials,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = currentUser.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RoleBadge(role = currentUser.role, language = language)

                        Spacer(modifier = Modifier.height(14.dp))

                        // Edit Name, Photo, Phone Button (Requirement 6)
                        Button(
                            onClick = {
                                editName = currentUser.name
                                editPhone = currentUser.phone
                                selectedAvatarColor = currentUser.avatarColorHex
                                showEditProfileDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_edit_profile")
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = if (isUrdu) "پروفائل اور فوٹو تبدیل کریں" else "Edit Profile & Photo")
                        }
                    }
                }
            }

            // User Info Details
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProfileField(label = if (isUrdu) "ای میل" else "Email Address", value = currentUser.email)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileField(
                            label = if (isUrdu) "فون نمبر" else "Phone Number",
                            value = currentUser.phone.ifBlank { if (isUrdu) "درج نہیں" else "Not provided" }
                        )

                        if (currentUser.permanentStudentId != null) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            ProfileField(
                                label = if (isUrdu) "مستقل اسٹوڈنٹ آئی ڈی" else "Permanent Student ID",
                                value = currentUser.permanentStudentId,
                                isMonospace = true
                            )
                        }
                    }
                }
            }

            // Change Password Action Card (Requirement 6)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                oldPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                                passwordChangeError = null
                                passwordChangeMessage = null
                                showChangePasswordDialog = true
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isUrdu) "پاس ورڈ تبدیل کریں" else "Change Password",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isUrdu) "اکاؤنٹ کی حفاظت کیلئے پاس ورڈ اپ ڈیٹ کریں" else "Update account security password",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Sign Out Button (Requirement 4: Sign out must work)
            item {
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_account_logout"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = AppStrings.logout(language), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Edit Profile & Photo Dialog (Requirement 6)
    if (showEditProfileDialog) {
        val avatarPalette = listOf(
            0xFF1B3B6F, 0xFF01411C, 0xFF1D4ED8, 0xFF006D77, 0xFF7F1D1D, 0xFFB45309
        )

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(if (isUrdu) "پروفائل اور تصویر ایڈٹ کریں" else "Edit Name, Phone & Photo")
            },
            text = {
                Column {
                    Text(
                        text = if (isUrdu) "اواتار کا رنگ / تھیم منتخب کریں:" else "Select Avatar Photo Color:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        avatarPalette.forEach { colorHex ->
                            val isSel = selectedAvatarColor == colorHex
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorHex))
                                    .clickable { selectedAvatarColor = colorHex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(if (isUrdu) "نام" else "Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text(if (isUrdu) "فون نمبر" else "Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.updateProfile(editName, editPhone, selectedAvatarColor)
                        showEditProfileDialog = false
                    }
                ) {
                    Text(if (isUrdu) "محفوظ کریں" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Change Password Dialog (Requirement 6)
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { if (!isUpdatingPassword) showChangePasswordDialog = false },
            title = {
                Text(if (isUrdu) "پاس ورڈ تبدیل کریں" else "Change Password")
            },
            text = {
                Column {
                    passwordChangeError?.let { err ->
                        Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    passwordChangeMessage?.let { msg ->
                        Text(text = msg, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
                    }

                    OutlinedTextField(
                        value = oldPassword,
                        onValueChange = { oldPassword = it; passwordChangeError = null },
                        enabled = !isUpdatingPassword,
                        label = { Text(if (isUrdu) "موجودہ پاس ورڈ" else "Current Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; passwordChangeError = null },
                        enabled = !isUpdatingPassword,
                        label = { Text(if (isUrdu) "نیا پاس ورڈ" else "New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; passwordChangeError = null },
                        enabled = !isUpdatingPassword,
                        label = { Text(if (isUrdu) "نئے پاس ورڈ کی تصدیق" else "Confirm New Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isUpdatingPassword,
                    onClick = {
                        coroutineScope.launch {
                            isUpdatingPassword = true
                            passwordChangeError = null
                            val res = repository.changePassword(oldPassword, newPassword, confirmPassword)
                            res.fold(
                                onSuccess = {
                                    passwordChangeMessage = if (isUrdu) "پاس ورڈ کامیابی سے تبدیل ہو گیا!" else "Password successfully changed!"
                                    showChangePasswordDialog = false
                                },
                                onFailure = { passwordChangeError = it.message }
                            )
                            isUpdatingPassword = false
                        }
                    }
                ) {
                    if (isUpdatingPassword) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(if (isUrdu) "اپ ڈیٹ کریں" else "Update Password")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isUpdatingPassword,
                    onClick = { showChangePasswordDialog = false }
                ) {
                    Text(if (isUrdu) "بند کریں" else "Close")
                }
            }
        )
    }
}

@Composable
private fun ProfileField(label: String, value: String, isMonospace: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}

// 2. PRIVACY & SECURITY PAGE (Requirements 6 & 8)
@Composable
fun PrivacySecurityPage(
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val twoStepLogin by repository.twoStepLoginEnabled.collectAsState()
    val activeDevices by repository.activeDevices.collectAsState()
    val isOnline by repository.isOnline.collectAsState()

    val isAdmin = currentUser?.role == UserRole.Administrator || currentUser?.role == UserRole.SuperAdmin

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_privacy")) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.privacyAndSecurity(language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Requirement 8: Hide Supabase address & security code from Students, Parents, and Teachers!
        // Show ONLY to Administrator!
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "DBUS Cloud Backend",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Requirement 7 & 12: Network status and disconnection simulation toggle
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ) {
                                Text(
                                    text = if (isOnline) "Connected" else "Offline",
                                    color = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = { repository.toggleOnlineStatus() },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp).testTag("btn_toggle_network_connection")
                            ) {
                                Text(
                                    text = if (isOnline) "Disconnect" else "Connect",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isAdmin) {
                        // Administrator view: real Supabase config status
                        val endpointText = if (repository.supabaseUrl.isNotBlank()) repository.supabaseUrl
                        else if (isUrdu) "سیٹ نہیں (Secrets پینل میں SUPABASE_URL درج کریں)" else "Not configured (set SUPABASE_URL in Secrets)"
                        val keyStatusText = if (repository.supabaseAnonKey.isNotBlank()) {
                            "Configured (••••${repository.supabaseAnonKey.takeLast(6)})"
                        } else {
                            if (isUrdu) "سیٹ نہیں (SUPABASE_ANON_KEY درج کریں)" else "Not configured (set SUPABASE_ANON_KEY in Secrets)"
                        }
                        Text(
                            text = "Supabase Endpoint: $endpointText",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Anon Key Status: $keyStatusText",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    } else {
                        // Students, Parents, and Teachers view: Technical credentials strictly hidden (Req 8)
                        Text(
                            text = if (isUrdu) "کلاؤڈ سیکیورٹی فعال ہے۔ ڈیٹا مکمل رازداری کے ساتھ کلاؤڈ پر محفوظ ہے۔"
                            else "Cloud Sync is securely active. System data is safeguarded by PostgreSQL Row Level Security.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Requirement 6: Two-Step Login (2FA)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUrdu) "دو مرحلہ وار تصدیق (Two-Step Login)" else "Two-Step Login (SMS 2FA)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isUrdu) "لاگ ان کے وقت پاکستانی موبائل نمبر پر او ٹی پی بھیجیں"
                            else "Require OTP on registered phone number upon every login",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(checked = twoStepLogin, onCheckedChange = { repository.setTwoStepLogin(it) })
                }
            }
        }

        // Requirement 6: Active Devices List
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isUrdu) "فعال ڈیوائسز (Active Devices)" else "Active Devices",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (activeDevices.size > 1) {
                            TextButton(onClick = { repository.revokeOtherDevices() }) {
                                Text(
                                    text = if (isUrdu) "دیگر لاگ آؤٹ کریں" else "Revoke Others",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (activeDevices.isEmpty()) {
                        Text(
                            text = if (isUrdu) "ابھی کوئی فعال ڈیوائس سنک نہیں ہوئی" else "No active devices synced yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        activeDevices.forEachIndexed { idx, device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (device.deviceName.contains("Browser", ignoreCase = true)) Icons.Default.Public else Icons.Default.Smartphone,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (device.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = device.deviceName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (device.isCurrent) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = if (isUrdu) "یہ ڈیوائس" else "Current",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${device.location} • ${device.lastActive}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                            if (idx < activeDevices.size - 1) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 3. APPEARANCE PAGE (Requirement 6: mode, 5 themes, text size)
@Composable
fun AppearancePage(
    repository: SchoolRepository,
    language: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val currentMode by repository.themeMode.collectAsState()
    val currentPalette by repository.themePalette.collectAsState()
    val currentTextScale by repository.textScale.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_appearance")) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.appearance(language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Mode Choice: Light, Dark, Auto
        item {
            Text(
                text = if (isUrdu) "ڈسپلے موڈ (Light, Dark, Auto)" else "Display Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeMode.values().forEach { mode ->
                    val isSelected = currentMode == mode
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { repository.setThemeMode(mode) }
                            .testTag("mode_${mode.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Brightness4,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = mode.getDisplayName(isUrdu),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Requirement 6: Text Size (Small, Medium, Large)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isUrdu) "متن کا سائز (Text Size)" else "Text Size",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = if (isUrdu) "اپنی سہولت کے مطابق لکھائی کا سائز منتخب کریں" else "Adjust the readability font size throughout the app",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextScale.values().forEach { scale ->
                    val isSelected = currentTextScale == scale
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { repository.setTextScale(scale) }
                            .testTag("scale_${scale.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Aa",
                                fontSize = (16 * scale.scale).sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = scale.getDisplayName(isUrdu),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // 5 Themes: Classic Navy, Pakistan Green, Royal Blue, Teal, Maroon and Gold
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isUrdu) "رنگین تھیمز (5 Themes)" else "5 High-Contrast Color Themes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(ThemePalette.values()) { palette ->
            val isSelected = currentPalette == palette
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { repository.setThemePalette(palette) }
                    .testTag("palette_${palette.name.lowercase()}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(palette.primaryHex)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = palette.getDisplayName(isUrdu),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (palette == ThemePalette.ClassicNavy) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                    ) {
                                        Text(
                                            text = if (isUrdu) "ڈیفالٹ" else "Default",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isSelected) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// 4. NOTIFICATIONS PAGE (Requirement 6: assignments, quizzes, reports, messages)
@Composable
fun NotificationsPage(
    repository: SchoolRepository,
    language: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU

    val notifyAssignments by repository.notifyAssignments.collectAsState()
    val notifyQuizzes by repository.notifyQuizzes.collectAsState()
    val notifyReports by repository.notifyReports.collectAsState()
    val notifyMessages by repository.notifyMessages.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_notifications")) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = AppStrings.notifications(language),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 1. Assignments
                NotificationToggleWithIcon(
                    icon = Icons.Default.Assignment,
                    title = if (isUrdu) "اسائنمنٹس (Assignments)" else "Assignments",
                    subtitle = if (isUrdu) "اساتذہ کی طرف سے نئی اسائنمنٹس اور ہوم ورک پر الرٹس" else "Alerts when new assignments or homework are published",
                    checked = notifyAssignments,
                    onCheckedChange = { repository.setNotifyAssignments(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 2. Quizzes
                NotificationToggleWithIcon(
                    icon = Icons.Default.Quiz,
                    title = if (isUrdu) "کوئز اور امتحانات (Quizzes)" else "Quizzes & Tests",
                    subtitle = if (isUrdu) "آئندہ کوئز اور امتحانات کے شیڈول کی پیشگی اطلاع" else "Reminders for upcoming quizzes, class tests and exams",
                    checked = notifyQuizzes,
                    onCheckedChange = { repository.setNotifyQuizzes(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 3. Reports
                NotificationToggleWithIcon(
                    icon = Icons.Default.Report,
                    title = if (isUrdu) "تعلیمی رپورٹس (Reports)" else "Academic Reports",
                    subtitle = if (isUrdu) "ماہانہ اور امتحانی نتائج اور حاضری کی تفصیلی رپورٹ" else "Monthly performance, report cards, and attendance summaries",
                    checked = notifyReports,
                    onCheckedChange = { repository.setNotifyReports(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // 4. Messages
                NotificationToggleWithIcon(
                    icon = Icons.Default.Email,
                    title = if (isUrdu) "پیغامات (Messages)" else "Messages & Notices",
                    subtitle = if (isUrdu) "اساتذہ، پرنسپل اور والدین کے درمیان باہمی پیغامات" else "Direct communications between teachers, parents, and school administration",
                    checked = notifyMessages,
                    onCheckedChange = { repository.setNotifyMessages(it) }
                )
            }
        }
    }
}

@Composable
private fun NotificationToggleWithIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// 5. LANGUAGE & REGION PAGE (Requirement 6: English, Urdu, Pakistan time)
@Composable
fun LanguageRegionPage(
    repository: SchoolRepository,
    language: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val currentLang by repository.appLanguage.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_lang_region")) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = AppStrings.languageAndRegion(language),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isUrdu) "زبان منتخب کریں (Language)" else "Select Language",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(10.dp))

        AppLanguage.values().forEach { lang ->
            val isSelected = currentLang == lang
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { repository.setAppLanguage(lang) }
                    .testTag("lang_${lang.name.lowercase()}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = lang.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (lang.isRtl) "Right to Left (RTL) • دائیں سے بائیں" else "Left to Right (LTR)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isSelected) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Requirement 6: Pakistan Time & Regional info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrdu) "پاکستان ریجنل ترتیبات" else "Pakistan Region & Timezone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                val sessionYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
                Text(text = "Country: Pakistan 🇵🇰", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Timezone: Pakistan Standard Time (PKT, UTC+5)", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Academic Calendar: $sessionYear-${sessionYear + 1} (Federal & Provincial Board)", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// 5. HELP AND ABOUT DBUS PAGE (Requirement 5: Credits, version 1.0.0, Role instructions, FAQ with 8 questions)
data class FaqItem(val question: String, val answer: String)

@Composable
fun HelpAboutPage(
    language: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU

    // State for expanded FAQ question index
    var expandedFaqIndex by remember { mutableIntStateOf(-1) }

    // State for expanded Role instruction
    var expandedRoleIndex by remember { mutableIntStateOf(0) }

    val roleInstructions = listOf(
        Triple(
            UserRole.Administrator,
            "Administrator Instructions",
            if (isUrdu) "ایڈمنسٹریٹر اسکول کے تمام طلباء، اساتذہ اور والدین کا انتظام کرتے ہیں۔ آپ نئی طالب علم رجسٹریشن کو منظور کر سکتے ہیں، ناقابلِ تنسیخ مستقل اسٹوڈنٹ آئی ڈی جاری کر سکتے ہیں، اور اساتذہ کو کلاسیں تفویض کر سکتے ہیں۔"
            else "Administrators manage the school roster. You can review pending student accounts, approve registrations, issue immutable Permanent Student IDs, and assign teachers to student classes."
        ),
        Triple(
            UserRole.Teacher,
            "Teacher Instructions",
            if (isUrdu) "اساتذہ اپنی متعلقہ کلاس کے مقررہ طلباء کی فہرست دیکھ سکتے ہیں، روزانہ کی حاضری مانیٹر کر سکتے ہیں، اور اسائنمنٹس تفویض کر سکتے ہیں۔"
            else "Teachers have strict RLS access to their assigned student roster. You can monitor student attendance, review class performance, and track student credentials."
        ),
        Triple(
            UserRole.Parent,
            "Parent Instructions",
            if (isUrdu) "والدین اپنے زیرِ کفالت بچوں کا ریکارڈ، حاضری کی شرح، اور آفیشل ڈیجیٹل اسٹوڈنٹ شناختی کارڈ دیکھ سکتے ہیں۔ نئے بچے کو لنک کرنے کیلئے درخواست بھیجیں۔"
            else "Parents can view only their own linked children, track daily attendance, view permanent student credentials, and link new children to their account."
        ),
        Triple(
            UserRole.Student,
            "Student Instructions",
            if (isUrdu) "طلباء اپنا آفیشل ڈیجیٹل اسٹوڈنٹ کارڈ، اسکول سیشن اور رجسٹرڈ کلاس دیکھ سکتے ہیں۔ آپ کا مستقل اسٹوڈنٹ آئی ڈی ہمیشہ ایک ہی رہتا ہے۔"
            else "Students can access their official Digital Student ID Card, review enrolled class details, and verify permanent credentials."
        )
    )

    // 8 FAQs as requested in Requirement 5
    val faqs = listOf(
        FaqItem(
            question = if (isUrdu) "1. مستقل اسٹوڈنٹ آئی ڈی کیا ہے اور کیسے جاری ہوتی ہے؟" else "1. What is the Permanent Student ID and how is it issued?",
            answer = if (isUrdu) "مستقل اسٹوڈنٹ آئی ڈی اسکول کے سینٹرل کلاؤڈ سسٹم سے خودکار طریقے سے جاری ہوتی ہے (فارمیٹ PK-DBUS-YYYY-XXXX)۔ یہ نمبر ہر طالب علم کیلئے تاحیات مخصوص رہتا ہے۔"
            else "The Permanent Student ID (format PK-DBUS-YYYY-XXXX) is uniquely generated upon student approval. It serves as the student's lifelong academic identifier in Pakistan."
        ),
        FaqItem(
            question = if (isUrdu) "2. کیا اسٹوڈنٹ آئی ڈی کبھی تبدیل یا دوبارہ استعمال ہو سکتی ہے؟" else "2. Can a Student ID ever be changed, reissued, or reused?",
            answer = if (isUrdu) "ہرگز نہیں۔ ڈی بس کے قواعد کے مطابق مستقل اسٹوڈنٹ آئی ڈی ناقابلِ تنسیخ ہے اور کسی دوسرے طالب علم کو کبھی دوبارہ الاٹ نہیں ہو سکتی۔"
            else "Strictly no. Under DBUS immutable registry rules, an issued Student ID is permanent, never changes, and is never reused for any other student."
        ),
        FaqItem(
            question = if (isUrdu) "3. طالب علم کا اکاؤنٹ کیسے بنتا اور منظور ہوتا ہے؟" else "3. How is a student account created and approved?",
            answer = if (isUrdu) "طالب علم کا اکاؤنٹ یا تو والدین بناتے ہیں یا خود طالب علم رجسٹر ہوتا ہے، جس کے بعد اسکول انتظامیہ یا والدین کی منظوری کے بعد ہی اکاؤنٹ فعال ہوتا ہے۔"
            else "A parent or school administrator creates or approves student accounts. Once approved, the student gains access to their digital school records."
        ),
        FaqItem(
            question = if (isUrdu) "4. رو لیول سیکیورٹی (RLS) کیا ہے اور ڈیٹا کیسے محفوظ رہتا ہے؟" else "4. What is Row Level Security (RLS) in DBUS?",
            answer = if (isUrdu) "رو لیول سیکیورٹی ڈیٹا بیس کی سطح پر لاگو ہے: والدین صرف اپنے بچے دیکھ سکتے ہیں، اساتذہ صرف اپنی کلاس کے طلباء دیکھ سکتے ہیں، اور طالب علم صرف اپنا ریکارڈ دیکھ سکتا ہے۔"
            else "PostgreSQL Row Level Security strictly enforces that parents only see their own children, teachers see assigned students, and students see only their own data."
        ),
        FaqItem(
            question = if (isUrdu) "5. بے فارم یا نادرا شناختی نمبر کون دیکھ سکتا ہے؟" else "5. Who can view NADRA B-Form / CNIC numbers?",
            answer = if (isUrdu) "طلباء کی رازداری کے تحفظ کیلئے بے فارم نمبر عام صارفین سے پوشیدہ ہے۔ صرف ایڈمنسٹریٹر کو آخری 4 ہندسے ظاہر ہوتے ہیں۔"
            else "For student privacy, B-Form numbers are completely hidden from parents, teachers, and students. Only administrators can view masked last-4 digits."
        ),
        FaqItem(
            question = if (isUrdu) "6. کیا والدین ایک سے زائد بچوں کو منسلک کر سکتے ہیں؟" else "6. Can parents link multiple children to one account?",
            answer = if (isUrdu) "جی ہاں، والدین ایک ہی لاگ ان کے تحت اپنے تمام زیرِ کفالت بچوں کو شامل کر کے ان کا الگ الگ ریکارڈ دیکھ سکتے ہیں۔"
            else "Yes, parents can link multiple children under one account and switch seamlessly between their academic profiles."
        ),
        FaqItem(
            question = if (isUrdu) "7. تھیم اور اردو زبان کیسے تبدیل کریں؟" else "7. How do I switch themes or enable Urdu (RTL) mode?",
            answer = if (isUrdu) "سیٹنگز میں جائیں، 'ظاہری شکل' میں جا کر 5 تھیمز میں سے انتخاب کریں، یا 'زبان اور علاقہ' میں جا کر اردو منتخب کریں۔"
            else "Go to Settings > Appearance to pick from 5 color themes, or visit Language & Region to switch to Urdu with instant Right-to-Left (RTL) layout."
        ),
        FaqItem(
            question = if (isUrdu) "8. دو مرحلہ وار لاگ ان (2FA / OTP) اکاؤنٹ کو کیسے محفوظ بناتا ہے؟" else "8. How does two-step login (2FA / OTP) protect my account?",
            answer = if (isUrdu) "ٹو فیکٹر لاگ ان فعال ہونے پر پاس ورڈ کے ساتھ پاکستانی موبائل نمبر پر 6 ہندسی کوڈ لازمی ہوتا ہے، جس سے غیر مجاز رسائی ناممکن ہو جاتی ہے۔"
            else "Two-factor authentication requires both your credentials and a 6-digit SMS OTP sent to your registered Pakistani mobile number."
        )
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_help_about")) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = AppStrings.helpAndAbout(language),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // DBUS App Emblem & Version 1.0.0 (Requirement 5)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "DBUS Cloud School App",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    // Requirement 5: App Version 1.0.0
                    Text(
                        text = "App Version: 1.0.0 (Module 1)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // CREDITS SECTION (Requirement 5):
        // Developer, Owner, Idea and Design credits
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("credits_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = AppStrings.creditsTitle(language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 1. Idea and Design by: Urhan & Shayan
                    CreditItem(
                        icon = Icons.Default.DesignServices,
                        roleTitle = AppStrings.ideaAndDesign(language),
                        personName = "Urhan & Shayan"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // 2. Principal & Owner: M. Shayan Ahmed
                    CreditItem(
                        icon = Icons.Default.School,
                        roleTitle = AppStrings.principalAndOwner(language),
                        personName = "M. Shayan Ahmed"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // 3. Developer: Muhammad Talha Thaheem
                    CreditItem(
                        icon = Icons.Default.Code,
                        roleTitle = AppStrings.developer(language),
                        personName = "Muhammad Talha Thaheem"
                    )
                }
            }
        }

        // Requirement 5: Instructions for each role
        item {
            Text(
                text = if (isUrdu) "ہر کردار کیلئے ہدایات" else "Instructions for Each Role",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(roleInstructions.indices.toList()) { index ->
            val (role, title, description) = roleInstructions[index]
            val isExpanded = expandedRoleIndex == index

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedRoleIndex = if (isExpanded) -1 else index },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoleBadge(role = role, language = language)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // Requirement 5: FAQ with 8 questions (Tap question to show its answer)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isUrdu) "اکثر پوچھے جانے والے سوالات (8 FAQs)" else "Frequently Asked Questions (8 FAQs)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isUrdu) "جواب دیکھنے کیلئے سوال پر ٹیپ کریں" else "Tap any question to reveal its answer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
        }

        items(faqs.indices.toList()) { idx ->
            val faq = faqs[idx]
            val isExpanded = expandedFaqIndex == idx

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedFaqIndex = if (isExpanded) -1 else idx }
                    .testTag("faq_item_$idx"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = faq.question,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = isExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = faq.answer,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CreditItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    roleTitle: String,
    personName: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = roleTitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = personName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
