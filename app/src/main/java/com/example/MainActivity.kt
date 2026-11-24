package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import android.util.Log
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.example.data.SchoolRepository
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.ui.activity.ActivityScreen
import com.example.ui.attendance.AttendanceScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.components.OfflineConnectivityBanner
import com.example.ui.components.RoleIconBadge
import com.example.ui.settings.SettingsContainer
import com.example.ui.student.StudentPageScreen
import com.example.ui.theme.DBusTheme
import com.example.ui.tools.CalendarCalculatorScreen
import com.example.ui.welcome.WelcomeDashboard

/**
 * Requirement 2:
 * Bottom bar: Dashboard, Attendance, Activity, Account.
 * Every icon and every back button works.
 */
enum class MainNavigationTab {
    DASHBOARD,
    ATTENDANCE,
    ACTIVITY,
    ACCOUNT
}

class MainActivity : ComponentActivity() {
    private val repository = SchoolRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository.initPersistence(applicationContext)

        setContent {
            val themeMode by repository.themeMode.collectAsState()
            val themePalette by repository.themePalette.collectAsState()
            val textScale by repository.textScale.collectAsState()
            val language by repository.appLanguage.collectAsState()
            val currentUser by repository.currentUser.collectAsState()

            DBusTheme(
                mode = themeMode,
                palette = themePalette,
                textScale = textScale
            ) {
                val layoutDirection = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (currentUser == null) {
                            AuthScreen(
                                repository = repository,
                                language = language,
                                onAuthSuccess = { /* Automatically switches to MainAppScreen */ }
                            )
                        } else {
                            val activeUser = currentUser!!
                            key(activeUser.id) {
                                MainAppScreen(
                                    repository = repository,
                                    language = language
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    repository: SchoolRepository,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val currentUser by repository.currentUser.collectAsState()
    val isOnline by repository.isOnline.collectAsState()
    val syncError by repository.syncError.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var selectedTab by remember { mutableStateOf(MainNavigationTab.DASHBOARD) }

    // Navigation states
    var viewingStudentPermanentId by remember { mutableStateOf<String?>(null) }
    var showingCalendarTools by remember { mutableStateOf(false) }
    var showRoleInfoDialog by remember { mutableStateOf(false) }

    // System Back Handler
    val backEnabled = viewingStudentPermanentId != null || showingCalendarTools || selectedTab != MainNavigationTab.DASHBOARD
    BackHandler(enabled = backEnabled) {
        if (viewingStudentPermanentId != null) {
            viewingStudentPermanentId = null
        } else if (showingCalendarTools) {
            showingCalendarTools = false
        } else {
            selectedTab = MainNavigationTab.DASHBOARD
        }
    }

    // If viewing dedicated student page
    if (viewingStudentPermanentId != null) {
        StudentPageScreen(
            repository = repository,
            studentPermanentId = viewingStudentPermanentId!!,
            currentUser = currentUser,
            language = language,
            onBack = { viewingStudentPermanentId = null },
            modifier = modifier
        )
        return
    }

    // If viewing Full School Calendar screen (Calendar-only outside Activity)
    if (showingCalendarTools) {
        CalendarCalculatorScreen(
            repository = repository,
            currentUser = currentUser,
            language = language,
            onBack = { showingCalendarTools = false },
            calendarOnly = true,
            modifier = modifier
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "DBUS",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isUrdu) "پاکستان اسکول کلاؤڈ" else "Pakistan School Cloud",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Calendar Icon (opens Full Interactive School Calendar)
                    IconButton(
                        onClick = { showingCalendarTools = true },
                        modifier = Modifier.testTag("btn_tools_calendar")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = AppStrings.schoolCalendar(language),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Single compact Role Icon in Top Right
                    currentUser?.let { user ->
                        RoleIconBadge(
                            role = user.role,
                            language = language,
                            onClick = { showRoleInfoDialog = true }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                // 1. Dashboard Tab
                NavigationBarItem(
                    selected = selectedTab == MainNavigationTab.DASHBOARD,
                    onClick = { selectedTab = MainNavigationTab.DASHBOARD },
                    icon = {
                        Icon(imageVector = Icons.Default.Dashboard, contentDescription = AppStrings.dashboard(language))
                    },
                    label = {
                        Text(
                            text = AppStrings.dashboard(language),
                            fontWeight = if (selectedTab == MainNavigationTab.DASHBOARD) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("nav_dashboard")
                )

                // 2. Attendance Tab
                NavigationBarItem(
                    selected = selectedTab == MainNavigationTab.ATTENDANCE,
                    onClick = { selectedTab = MainNavigationTab.ATTENDANCE },
                    icon = {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = AppStrings.attendance(language))
                    },
                    label = {
                        Text(
                            text = AppStrings.attendance(language),
                            fontWeight = if (selectedTab == MainNavigationTab.ATTENDANCE) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("nav_attendance")
                )

                // 3. Activity Tab (Subjects, Class Activity, Assignments - Requirement 6)
                NavigationBarItem(
                    selected = selectedTab == MainNavigationTab.ACTIVITY,
                    onClick = { selectedTab = MainNavigationTab.ACTIVITY },
                    icon = {
                        Icon(imageVector = Icons.Default.LocalActivity, contentDescription = if (isUrdu) "سرگرمیاں" else "Activity")
                    },
                    label = {
                        Text(
                            text = if (isUrdu) "سرگرمیاں" else "Activity",
                            fontWeight = if (selectedTab == MainNavigationTab.ACTIVITY) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("nav_activity")
                )

                // 4. Account Tab (Account, Settings, Security, Appearance - Requirement 2 & 8)
                NavigationBarItem(
                    selected = selectedTab == MainNavigationTab.ACCOUNT,
                    onClick = { selectedTab = MainNavigationTab.ACCOUNT },
                    icon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = AppStrings.account(language))
                    },
                    label = {
                        Text(
                            text = AppStrings.account(language),
                            fontWeight = if (selectedTab == MainNavigationTab.ACCOUNT) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("nav_account")
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Clear banner when internet is disconnected
            OfflineConnectivityBanner(
                isOnline = isOnline,
                language = language,
                onReconnectClick = {
                    repository.setOnline(true)
                    coroutineScope.launch { repository.syncAllFromSupabase() }
                }
            )

            // Dismissible Supabase Sync Error Banner
            syncError?.let { err ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("banner_sync_error"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                repository.clearSyncError()
                                coroutineScope.launch { repository.syncAllFromSupabase() }
                            }
                        ) {
                            Text(if (isUrdu) "دوبارہ کوشش" else "Retry")
                        }
                        IconButton(
                            onClick = { repository.clearSyncError() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isUrdu) "بند کریں" else "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    MainNavigationTab.DASHBOARD -> {
                        currentUser?.let { user ->
                            WelcomeDashboard(
                                repository = repository,
                                currentUser = user,
                                language = language,
                                onOpenStudentPage = { permanentId ->
                                    viewingStudentPermanentId = permanentId
                                },
                                onNavigateToSettings = { selectedTab = MainNavigationTab.ACCOUNT }
                            )
                        }
                    }
                    MainNavigationTab.ATTENDANCE -> {
                        currentUser?.let { user ->
                            AttendanceScreen(
                                repository = repository,
                                currentUser = user,
                                language = language,
                                onOpenStudentPage = { permanentId ->
                                    viewingStudentPermanentId = permanentId
                                }
                            )
                        }
                    }
                    MainNavigationTab.ACTIVITY -> {
                        ActivityScreen(
                            repository = repository,
                            currentUser = currentUser,
                            language = language,
                            onOpenStudentPage = { permanentId ->
                                viewingStudentPermanentId = permanentId
                            }
                        )
                    }
                    MainNavigationTab.ACCOUNT -> {
                        SettingsContainer(
                            repository = repository,
                            currentUser = currentUser,
                            language = language,
                            onLogout = {
                                repository.logout()
                                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                                    withContext(NonCancellable) {
                                        try {
                                            withTimeoutOrNull(1500L) {
                                                credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                            }
                                        } catch (e: Exception) {
                                            Log.w("Auth", "Failed to clear credential state: ${e.message}")
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showRoleInfoDialog && currentUser != null) {
        val user = currentUser!!
        AlertDialog(
            onDismissRequest = { showRoleInfoDialog = false },
            title = {
                Text(
                    text = user.role.getDisplayName(isUrdu),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (user.loginId.isNotBlank()) {
                        Text(
                            text = if (isUrdu) "لاگ ان آئی ڈی: ${user.loginId}" else "Login ID: ${user.loginId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showRoleInfoDialog = false }) {
                    Text(if (isUrdu) "بند کریں" else "Close")
                }
            }
        )
    }
}
