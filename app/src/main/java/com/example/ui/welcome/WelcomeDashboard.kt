package com.example.ui.welcome

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PermanentIdRegistry
import com.example.data.SchoolRepository
import com.example.model.AppLanguage
import com.example.model.ApprovalStatus
import com.example.model.CoreSkill
import com.example.model.SchoolAccount
import com.example.model.SkillLevel
import com.example.model.StudentRecord
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.DbusAddButton
import com.example.ui.components.DbusDeleteIconButton
import com.example.ui.components.DbusEditIconButton
import com.example.ui.components.RoleBadge
import com.example.ui.components.StudentApprovalBadge
import com.example.ui.components.StudentRowItem
import com.example.ui.media.InsertPhotoButton
import com.example.ui.media.StudentAvatar
import com.example.ui.integration.ConnectSchoolDialog
import java.util.Locale

@Composable
fun WelcomeDashboard(
    repository: SchoolRepository,
    currentUser: UserProfile,
    language: AppLanguage,
    onOpenStudentPage: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val students by repository.studentsList.collectAsState()
    val pendingApprovals by repository.pendingApprovals.collectAsState()
    val allAssessments by repository.skillAssessments.collectAsState()
    val schoolAccounts by repository.schoolAccounts.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Search query state (Only for Teacher & Administrator & SuperAdmin)
    var searchQuery by remember { mutableStateOf("") }

    // Connect School Dialog (Universal School Integration - 6 modes)
    var showConnectSchoolDialog by remember { mutableStateOf(false) }

    // Total Students & Pending Queue dialogs (Administrator & SuperAdmin)
    var showTotalStudentsDialog by remember { mutableStateOf(false) }
    var showPendingQueueDialog by remember { mutableStateOf(false) }

    // Account Management states (Administrator & SuperAdmin)
    var accountRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var showCreateAccountDialog by remember { mutableStateOf(false) }
    var selectedAccountForDetail by remember { mutableStateOf<SchoolAccount?>(null) }
    var editingAccount by remember { mutableStateOf<SchoolAccount?>(null) }
    var deletingAccount by remember { mutableStateOf<SchoolAccount?>(null) }
    var assigningTeacherAccount by remember { mutableStateOf<SchoolAccount?>(null) }
    var linkingParentAccount by remember { mutableStateOf<SchoolAccount?>(null) }

    // Add Student dialog (Administrator & SuperAdmin)
    var showAddStudentDialog by remember { mutableStateOf(false) }
    var newStudentName by remember { mutableStateOf("") }
    var newStudentFatherName by remember { mutableStateOf("") }
    var newStudentBForm by remember { mutableStateOf("") }
    var newStudentGrade by remember { mutableStateOf("Grade 8") }
    var newStudentSection by remember { mutableStateOf("Section A") }
    var newStudentPhotoColor by remember { mutableStateOf(0xFF1B3B6F) }
    var newStudentPhotoUri by remember { mutableStateOf<String?>(null) }
    var newStudentConsent by remember { mutableStateOf(true) }

    val photoColorChoices = listOf(0xFF1B3B6F, 0xFF01411C, 0xFF1D4ED8, 0xFF006D77, 0xFF7F1D1D, 0xFFB45309)

    // Filter students by name or Student ID for Teacher, Admin & SuperAdmin
    val filteredStudents = remember(students, searchQuery, currentUser.role) {
        if (currentUser.role == UserRole.Student || currentUser.role == UserRole.Parent) {
            students
        } else if (searchQuery.isBlank()) {
            students
        } else {
            val q = searchQuery.trim().lowercase()
            students.filter { student ->
                student.fullName.lowercase().contains(q) ||
                student.urduName.lowercase().contains(q) ||
                student.permanentStudentId.lowercase().contains(q) ||
                student.fatherName.lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // -------------------------------------------------------------
        // 1. TOP HEADER (Single role icon lives in top bar only; no Cloud/RLS chips)
        // -------------------------------------------------------------
        item {
            Spacer(modifier = Modifier.height(10.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = when (currentUser.role) {
                        UserRole.SuperAdmin -> if (isUrdu) "سپر ایڈمن پورٹل (تمام تعلیمی ادارے)" else "Super Admin Portal"
                        UserRole.Student -> if (isUrdu) "طالب علم پورٹل" else "Student Portal"
                        UserRole.Parent -> if (isUrdu) "والدین پورٹل" else "Parent Portal"
                        UserRole.Teacher -> if (isUrdu) "اساتذہ پورٹل" else "Teacher Portal"
                        UserRole.Administrator -> if (isUrdu) "اسکول انتظامیہ پورٹل" else "Administrator Portal"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val currentYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
                Text(
                    text = if (isUrdu) "تعلیمی سیشن $currentYear-${currentYear + 1} • DBUS کلاؤڈ سسٹم"
                    else "Academic Session $currentYear-${currentYear + 1} • DBUS Cloud System",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // -------------------------------------------------------------
        // ROLE 1: STUDENT (Requirement 3: No search bar, no school numbers,
        // opens student's own page: photo, name, Student ID, father's name,
        // class, section, and the 10 skills [view only])
        // -------------------------------------------------------------
        if (currentUser.role == UserRole.Student) {
            val studentRecord = currentUser.permanentStudentId?.let { repository.getStudentByPermanentId(it) }
                ?: students.firstOrNull()

            if (studentRecord == null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_student_profile"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isUrdu) "آپ کا طالب علم پروفائل ابھی منسلک نہیں ہوا۔ براہ کرم اسکول ایڈمن سے رابطہ کریں۔"
                                else "Your student profile is not linked yet. Please contact your DBUS Administrator.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                val studentPermanentId = studentRecord.permanentStudentId
                val history = allAssessments[studentPermanentId] ?: emptyList()
                val latestAssessment = history.firstOrNull()
                val attSummary = repository.getAttendanceSummary(studentPermanentId)

                // 1. Student Header Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_self_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudentAvatar(student = studentRecord, size = 64.dp)

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isUrdu) studentRecord.urduName else studentRecord.fullName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        StudentApprovalBadge(status = studentRecord.approvalStatus, language = language)
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "${studentRecord.grade} • ${studentRecord.section}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Text(
                                        text = if (isUrdu) "والد کا نام: ${studentRecord.fatherName}"
                                        else "Father's Name: ${studentRecord.fatherName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Permanent Student ID Banner
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isUrdu) "مستقل اسٹوڈنٹ شناختی نمبر" else "Permanent Student ID",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        text = studentRecord.permanentStudentId,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Attendance Quick Summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isUrdu) "حاضری کا مجموعی تناسب" else "Attendance Rate",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", attSummary.attendancePercentage)}% (Present: ${attSummary.presentDays}, Late: ${attSummary.lateDays})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. The 10 Core Skills Header (View Only)
                item {
                    Text(
                        text = if (isUrdu) "دس بنیادی تعلیمی و تربیتی صلاحیتیں (10 Core Skills)" else "The 10 Core Skills (View Only)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 3. The 10 Core Skills Cards (View Only, Shared Notes Only, NO Private Notes)
                items(CoreSkill.values()) { skill ->
                    val score = latestAssessment?.scores?.get(skill)
                    val sharedNote = latestAssessment?.sharedNotes?.get(skill)
                    val level = score?.let { SkillLevel.fromScore(it) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_self_skill_${skill.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = skill.getDisplayName(isUrdu),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = skill.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (level != null) Color(level.colorHex).copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (level != null) "${level.score}/4 • ${level.getDisplayName(isUrdu)}"
                                        else if (isUrdu) "ابھی درج نہیں" else "Not assessed yet",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (level != null) Color(level.colorHex)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for (i in 1..4) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(
                                                if (score != null && level != null && i <= score) Color(level.colorHex)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                    )
                                }
                            }

                            // Shared Note ONLY (Parents and Students must NEVER see private notes)
                            if (!sharedNote.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "📝 $sharedNote",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // ROLE 2: PARENT (Requirement 4: No search bar, no school numbers.
        // Show only their own children as cards. Tap card opens child's page.)
        // -------------------------------------------------------------
        if (currentUser.role == UserRole.Parent) {
            item {
                Text(
                    text = if (isUrdu) "آپ کے بچے (${filteredStudents.size})" else "Your Children (${filteredStudents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isUrdu) "طالب علم کا صفحہ، صلاحیتیں اور حاضری دیکھنے کیلئے کارڈ پر ٹیپ کریں"
                    else "Tap a child's card to open their profile, 10 skills, and attendance record",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            if (filteredStudents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_parent_children"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isUrdu) "آپ کے اکاؤنٹ سے ابھی کوئی بچہ منسلک نہیں ہے۔" else "No children linked yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredStudents) { child ->
                    StudentRowItem(
                        student = child,
                        language = language,
                        onClick = { onOpenStudentPage(child.permanentStudentId) }
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // ROLE 3: TEACHER (Requirement 5: Search bar [name or ID].
        // Assigned students only. No school-wide numbers.)
        // -------------------------------------------------------------
        if (currentUser.role == UserRole.Teacher) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_search"),
                    placeholder = {
                        Text(
                            text = if (isUrdu) "مقررہ طالب علم کا نام یا اسٹوڈنٹ آئی ڈی تلاش کریں..."
                            else "Search assigned students by name or Student ID...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Text(
                    text = if (isUrdu) "مقررہ طلباء کی فہرست (${filteredStudents.size})" else "Assigned Students (${filteredStudents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isUrdu) "صلاحیتیں جانچنے اور تفصیلات کیلئے قطار پر ٹیپ کریں"
                    else "Tap row to evaluate 10 core skills and view details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            if (filteredStudents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(text = if (isUrdu) "کوئی مقررہ طالب علم نہیں ملا" else "No assigned students found")
                        }
                    }
                }
            } else {
                items(filteredStudents) { student ->
                    StudentRowItem(
                        student = student,
                        language = language,
                        onClick = { onOpenStudentPage(student.permanentStudentId) }
                    )
                }
            }
        }

        // -------------------------------------------------------------
        // ROLE: SUPER ADMIN (Multi-institution, Plan Management, Sync)
        // -------------------------------------------------------------
        if (currentUser.role == UserRole.SuperAdmin) {
            item {
                SuperAdminManagementView(
                    repository = repository,
                    language = language,
                    onOpenConnectSchool = { showConnectSchoolDialog = true }
                )
            }
        }

        // -------------------------------------------------------------
        // ROLE 4: ADMINISTRATOR (School Admin)
        // -------------------------------------------------------------
        if (currentUser.role == UserRole.Administrator || currentUser.role == UserRole.SuperAdmin) {
            // Connect Your School Banner (Universal School Integration)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showConnectSchoolDialog = true }
                        .testTag("btn_open_connect_school"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isUrdu) "اپنا اسکول منسلک کریں (یونیورسل انٹیگریشن)" else "Connect Your School (Universal Integration)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isUrdu) "6 طریقے: ڈیٹابیس، سافٹ ویئر، API، ایکسل/CSV، ڈیسک ٹاپ کنیکٹر"
                                else "6 Modes: Existing DB, School ERP, REST API, Excel/CSV, Desktop Connector",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Button(
                            onClick = { showConnectSchoolDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = if (isUrdu) "منسلک کریں" else "Connect",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            // Numbers cards: Total Students, Pending Approval, Issued IDs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Students Card (Tappable to view full list dialog)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTotalStudentsDialog = true }
                            .testTag("card_total_students"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUrdu) "کل طلباء" else "Total Students",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(imageVector = Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${repository.getAllStudentsList().size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isUrdu) "فہرست کیلئے ٹیپ کریں" else "Tap for full list",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Pending Approval Card (Tappable to review pending queue)
                    val totalPendingCount = pendingApprovals.size + schoolAccounts.count {
                        it.approvalStatus == ApprovalStatus.PendingApproval && it.role != UserRole.Student
                    }
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showPendingQueueDialog = true }
                            .testTag("card_pending_approvals"),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUrdu) "منظوری کے منتظر" else "Pending Approval",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$totalPendingCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (totalPendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isUrdu) "تفصیل کیلئے ٹیپ کریں" else "Tap to review",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Issued IDs Card
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUrdu) "جاری شدہ آئی ڈیز" else "Issued IDs",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${PermanentIdRegistry.getIssuedCount()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isUrdu) "مستقل آئی ڈیز" else "Immutable IDs",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Search Bar for Administrator
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_student_search"),
                    placeholder = {
                        Text(
                            text = if (isUrdu) "طلباء کا نام یا اسٹوڈنٹ آئی ڈی تلاش کریں..."
                            else "Search students by name or Student ID...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Pending Approvals Queue if any
            if (pendingApprovals.isNotEmpty()) {
                item {
                    Text(
                        text = if (isUrdu) "منظوری کے منتظر طالب علم اکاؤنٹس" else "Pending Account Approvals",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                items(pendingApprovals) { pendingStudent ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenStudentPage(pendingStudent.permanentStudentId) }
                            .testTag("pending_student_card_${pendingStudent.permanentStudentId}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudentAvatar(student = pendingStudent, size = 42.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(text = pendingStudent.fullName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = "${pendingStudent.grade} • ID: ${pendingStudent.permanentStudentId}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (isUrdu) "والد: ${pendingStudent.fatherName}" else "Father: ${pendingStudent.fatherName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Button(
                                onClick = { repository.approveStudent(pendingStudent.permanentStudentId) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_approve_${pendingStudent.permanentStudentId}")
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isUrdu) "منظور کریں" else "Approve", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }

            // All Students List Header with Add Student Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUrdu) "طلباء کی مکمل فہرست (${filteredStudents.size})" else "All Enrolled Students (${filteredStudents.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isUrdu) "طالب علم کا پیج اور 10 صلاحیتیں دیکھنے کیلئے قطار پر ٹیپ کریں"
                            else "Tap student row to open Student Page & 10 Skills",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    DbusAddButton(
                        text = if (isUrdu) "نیا طالب علم" else "Add Student",
                        onClick = {
                            newStudentName = ""
                            newStudentFatherName = ""
                            newStudentBForm = ""
                            newStudentGrade = "Grade 8"
                            newStudentSection = "Section A"
                            newStudentPhotoUri = null
                            newStudentConsent = true
                            showAddStudentDialog = true
                        },
                        modifier = Modifier.testTag("btn_add_student_form")
                    )
                }
            }

            // All Students Roster
            if (filteredStudents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_admin_students"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    if (isUrdu) "کوئی طالب علم نہیں ملا" else "No students match your query"
                                } else {
                                    if (isUrdu) "ابھی کوئی طالب علم موجود نہیں ہے۔ نیا طالب علم شامل کریں۔" else "No students yet. Tap Add Student to enroll."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredStudents) { student ->
                    StudentRowItem(
                        student = student,
                        language = language,
                        onClick = { onOpenStudentPage(student.permanentStudentId) }
                    )
                }
            }

            // -------------------------------------------------------------
            // SCHOOL ACCOUNTS & ASSIGNMENTS CRUD SECTION (Admin / SuperAdmin)
            // -------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ManageAccounts,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isUrdu) "اکاؤنٹس اور تفویض مینجمنٹ" else "Accounts & Assignments",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = if (isUrdu) "اکاؤنٹ بنائیں، ترمیم کریں، طلباء اور والدین کو لنک کریں"
                            else "Manage Admin, Teacher, Student & Parent accounts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    DbusAddButton(
                        text = if (isUrdu) "نیا اکاؤنٹ" else "Create Account",
                        onClick = { showCreateAccountDialog = true },
                        modifier = Modifier.testTag("btn_create_account")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val filterRoles = listOf(
                    null to (if (isUrdu) "تمام" else "All"),
                    UserRole.Administrator to (if (isUrdu) "ایڈمن" else "Admin"),
                    UserRole.Teacher to (if (isUrdu) "اساتذہ" else "Teacher"),
                    UserRole.Student to (if (isUrdu) "طلباء" else "Student"),
                    UserRole.Parent to (if (isUrdu) "والدین" else "Parent")
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filterRoles) { (roleOption, label) ->
                        FilterChip(
                            selected = accountRoleFilter == roleOption,
                            onClick = { accountRoleFilter = roleOption },
                            label = { Text(text = label, maxLines = 1, softWrap = false) },
                            modifier = Modifier.testTag("filter_role_${roleOption?.name?.lowercase() ?: "all"}")
                        )
                    }
                }
            }

            val filteredAccounts = schoolAccounts.filter { acc ->
                !acc.isArchived &&
                    (accountRoleFilter == null || acc.role == accountRoleFilter) &&
                    (searchQuery.isBlank() ||
                        acc.name.lowercase().contains(searchQuery.trim().lowercase()) ||
                        acc.loginId.lowercase().contains(searchQuery.trim().lowercase()) ||
                        acc.email.lowercase().contains(searchQuery.trim().lowercase()))
            }

            if (filteredAccounts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("empty_admin_accounts"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (isUrdu) "ابھی کوئی اکاؤنٹ موجود نہیں ہے۔" else "No accounts yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredAccounts) { account ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedAccountForDetail = account }
                        .testTag("account_card_${account.loginId}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = account.loginId.ifBlank { account.id.take(6) },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Text(
                                    text = account.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                RoleBadge(role = account.role, language = language)
                                StudentApprovalBadge(status = account.approvalStatus, language = language)
                            }

                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = when (account.role) {
                                    UserRole.Teacher -> if (isUrdu) "تفویض کردہ طلباء: ${account.assignedStudentIds.size}" else "Assigned Students: ${account.assignedStudentIds.size} • ${account.email}"
                                    UserRole.Parent -> if (isUrdu) "منسلک بچے: ${account.linkedChildIds.size}" else "Linked Children: ${account.linkedChildIds.size} • ${account.email}"
                                    UserRole.Student -> "${account.permanentStudentId ?: ""} • ${account.email}"
                                    else -> account.email
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DbusEditIconButton(
                                onClick = { editingAccount = account },
                                contentDescription = if (isUrdu) "اکاؤنٹ میں ترمیم" else "Edit Account",
                                modifier = Modifier.testTag("btn_edit_account_${account.loginId}")
                            )
                            if (account.id != currentUser.id) {
                                DbusDeleteIconButton(
                                    onClick = { deletingAccount = account },
                                    contentDescription = if (isUrdu) "اکاؤنٹ حذف کریں" else "Delete Account",
                                    modifier = Modifier.testTag("btn_delete_account_${account.loginId}")
                                )
                            }
                        }
                    }
                }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // -------------------------------------------------------------
    // TOTAL STUDENTS SEARCH DIALOG (Requirement 3)
    // -------------------------------------------------------------
    if (showTotalStudentsDialog) {
        var totalDialogSearch by remember { mutableStateOf("") }
        val allStudentsList = repository.getAllStudentsList()
        val totalFiltered = remember(allStudentsList, totalDialogSearch) {
            if (totalDialogSearch.isBlank()) allStudentsList
            else {
                val q = totalDialogSearch.trim().lowercase()
                allStudentsList.filter {
                    it.fullName.lowercase().contains(q) ||
                    it.urduName.lowercase().contains(q) ||
                    it.permanentStudentId.lowercase().contains(q) ||
                    it.fatherName.lowercase().contains(q)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showTotalStudentsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isUrdu) "اسکول کے تمام طلباء" else "Total Enrolled Students",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${allStudentsList.size} " + (if (isUrdu) "طالب علم رجسٹرڈ ہیں" else "students registered in DBUS"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showTotalStudentsDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = totalDialogSearch,
                        onValueChange = { totalDialogSearch = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_total_dialog_search"),
                        placeholder = {
                            Text(if (isUrdu) "نام یا آئی ڈی تلاش کریں..." else "Search by name or Student ID...")
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (totalFiltered.isEmpty()) {
                            item {
                                Text(
                                    text = if (isUrdu) "ابھی کوئی طالب علم موجود نہیں ہے۔" else "No students yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(totalFiltered) { itemStudent ->
                                StudentRowItem(
                                    student = itemStudent,
                                    language = language,
                                    onClick = {
                                        showTotalStudentsDialog = false
                                        onOpenStudentPage(itemStudent.permanentStudentId)
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTotalStudentsDialog = false }) {
                    Text(if (isUrdu) "ٹھیک ہے" else "Done")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // ADD STUDENT DIALOG (Administrator Only)
    // -------------------------------------------------------------
    if (showAddStudentDialog) {
        AlertDialog(
            onDismissRequest = { showAddStudentDialog = false },
            title = {
                Text(
                    text = if (isUrdu) "طالب علم فارم (Add Student Form)" else "Add Student Form",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Photo & Avatar selection (Requirement 1)
                    item {
                        Text(
                            text = if (isUrdu) "طالب علم کی تصویر (Student Photo):" else "Student Photo:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StudentAvatar(
                                student = StudentRecord(
                                    id = "temp_preview",
                                    permanentStudentId = "NEW",
                                    fullName = newStudentName.ifBlank { "New Student" },
                                    urduName = newStudentName.ifBlank { "نیا طالب علم" },
                                    bFormNumber = newStudentBForm,
                                    grade = newStudentGrade,
                                    section = newStudentSection,
                                    parentId = "parent_temp",
                                    parentName = newStudentFatherName,
                                    fatherName = newStudentFatherName,
                                    avatarColorHex = newStudentPhotoColor,
                                    assignedTeacherIds = emptyList(),
                                    photoUri = newStudentPhotoUri
                                ),
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                InsertPhotoButton(
                                    currentPhotoUri = newStudentPhotoUri,
                                    language = language,
                                    onPhotoSelected = { newStudentPhotoUri = it }
                                )
                                if (!newStudentPhotoUri.isNullOrBlank()) {
                                    TextButton(
                                        onClick = { newStudentPhotoUri = null },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = if (isUrdu) "تصویر ہٹائیں" else "Remove Photo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Background color choices
                    item {
                        Text(
                            text = if (isUrdu) "اواتار بیک گراؤنڈ رنگ منتخب کریں:" else "Select Avatar Color:",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            photoColorChoices.forEach { colorHex ->
                                val isSel = newStudentPhotoColor == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorHex))
                                        .clickable { newStudentPhotoColor = colorHex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = newStudentName,
                            onValueChange = { newStudentName = it },
                            label = { Text(if (isUrdu) "طالب علم کا نام" else "Student Full Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_add_student_name"),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = newStudentFatherName,
                            onValueChange = { newStudentFatherName = it },
                            label = { Text(if (isUrdu) "والد کا نام" else "Father's Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_add_student_father"),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = newStudentBForm,
                            onValueChange = { newStudentBForm = it },
                            label = { Text("NADRA B-Form Number (Admin Only)") },
                            placeholder = { Text("XXXXX-XXXXXXX-X") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_add_student_bform"),
                            singleLine = true
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newStudentGrade,
                                onValueChange = { newStudentGrade = it },
                                label = { Text("Grade / Class") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_add_student_grade"),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = newStudentSection,
                                onValueChange = { newStudentSection = it },
                                label = { Text("Section") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("input_add_student_section"),
                                singleLine = true
                            )
                        }
                    }

                    // Parent Media Consent (Requirement 5)
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { newStudentConsent = !newStudentConsent }
                                    .padding(8.dp)
                            ) {
                                Checkbox(
                                    checked = newStudentConsent,
                                    onCheckedChange = { newStudentConsent = it }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (isUrdu) "والدین کی رضامندی (Media Consent)" else "Parental Media Consent",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isUrdu) "والدین تصویر اور ویڈیو محفوظ کرنے کی اجازت دیتے ہیں۔"
                                        else "Parent agrees to allow storing photos & videos.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = if (isUrdu) "مستقل اسٹوڈنٹ آئی ڈی خودکار طریقے سے بن کر منسلک ہو جائے گی۔"
                            else "Permanent Student ID will be automatically generated and linked.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = newStudentName.isNotBlank() && newStudentFatherName.isNotBlank(),
                    onClick = {
                        val name = newStudentName.trim()
                        val bForm = newStudentBForm.trim()
                        val father = newStudentFatherName.trim()

                        val newStudent = repository.registerNewStudent(
                            name = name,
                            fatherName = father,
                            bForm = bForm,
                            grade = newStudentGrade,
                            section = newStudentSection,
                            photoColorHex = newStudentPhotoColor,
                            photoUri = newStudentPhotoUri,
                            parentMediaConsentGiven = newStudentConsent
                        )
                        showAddStudentDialog = false
                        onOpenStudentPage(newStudent.permanentStudentId)
                    },
                    modifier = Modifier.testTag("btn_submit_add_student")
                ) {
                    Text(if (isUrdu) "شامل کریں" else "Create & Issue ID")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStudentDialog = false }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Universal School Integration Dialog
    if (showConnectSchoolDialog) {
        ConnectSchoolDialog(
            repository = repository,
            language = language,
            onDismiss = { showConnectSchoolDialog = false }
        )
    }

    // Pending Queue Dialog (Tappable from Pending Approval Card)
    if (showPendingQueueDialog) {
        val pendingAccounts = schoolAccounts.filter { it.approvalStatus == ApprovalStatus.PendingApproval }
        AlertDialog(
            onDismissRequest = { showPendingQueueDialog = false },
            title = { Text(if (isUrdu) "منظوری کے منتظر ریکارڈز" else "Pending Approvals Queue") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pendingApprovals.isEmpty() && pendingAccounts.isEmpty()) {
                        item {
                            Text(
                                text = if (isUrdu) "اس وقت کوئی ریکارڈ منظوری کا منتظر نہیں ہے۔" else "No pending approvals right now.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        items(pendingApprovals) { pendingStudent ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showPendingQueueDialog = false
                                        onOpenStudentPage(pendingStudent.permanentStudentId)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = pendingStudent.fullName, fontWeight = FontWeight.Bold)
                                        Text(text = pendingStudent.permanentStudentId, style = MaterialTheme.typography.labelSmall)
                                    }
                                    Button(onClick = { repository.approveStudent(pendingStudent.permanentStudentId) }) {
                                        Text(if (isUrdu) "منظور" else "Approve", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                        items(pendingAccounts.filter { it.role != UserRole.Student }) { pendingAcc ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showPendingQueueDialog = false
                                        selectedAccountForDetail = pendingAcc
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = pendingAcc.name, fontWeight = FontWeight.Bold)
                                        Text(text = "${pendingAcc.loginId} • ${pendingAcc.role.getDisplayName(isUrdu)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Button(onClick = { repository.approveAccount(pendingAcc.id, currentUser) }) {
                                        Text(if (isUrdu) "منظور" else "Approve", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPendingQueueDialog = false }) {
                    Text(if (isUrdu) "بند کریں" else "Close")
                }
            }
        )
    }

    // Account Detail CRUD Modal
    selectedAccountForDetail?.let { detailAccount ->
        val liveAccount = schoolAccounts.find { it.id == detailAccount.id } ?: detailAccount
        AlertDialog(
            onDismissRequest = { selectedAccountForDetail = null },
            title = {
                Column {
                    Text(text = liveAccount.name, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${liveAccount.loginId} • ${liveAccount.role.getDisplayName(isUrdu)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Email: ${liveAccount.email}", style = MaterialTheme.typography.bodySmall)
                    if (liveAccount.phone.isNotBlank()) {
                        Text(text = "Phone: ${liveAccount.phone}", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = "Status: ${liveAccount.approvalStatus.getDisplayName(isUrdu)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    liveAccount.permanentStudentId?.let { permId ->
                        Text(
                            text = "Permanent ID: $permId",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (liveAccount.approvalStatus == ApprovalStatus.PendingApproval) {
                        Button(
                            onClick = {
                                repository.approveAccount(liveAccount.id, currentUser)
                                selectedAccountForDetail = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isUrdu) "اکاؤنٹ منظور کریں" else "Approve Account")
                        }
                    }

                    if (liveAccount.role == UserRole.Teacher) {
                        OutlinedButton(
                            onClick = {
                                selectedAccountForDetail = null
                                assigningTeacherAccount = liveAccount
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_assign_students_teacher_${liveAccount.loginId}")
                        ) {
                            Icon(Icons.Default.SupervisorAccount, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isUrdu) "طلباء تفویض کریں (${liveAccount.assignedStudentIds.size})" else "Assign Students (${liveAccount.assignedStudentIds.size})")
                        }
                    }

                    if (liveAccount.role == UserRole.Parent) {
                        OutlinedButton(
                            onClick = {
                                selectedAccountForDetail = null
                                linkingParentAccount = liveAccount
                            },
                            modifier = Modifier.fillMaxWidth().testTag("btn_link_children_parent_${liveAccount.loginId}")
                        ) {
                            Icon(Icons.Default.FamilyRestroom, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isUrdu) "بچے لنک کریں (${liveAccount.linkedChildIds.size})" else "Link Children (${liveAccount.linkedChildIds.size})")
                        }
                    }

                    if (liveAccount.role == UserRole.Student && !liveAccount.permanentStudentId.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                val targetId = liveAccount.permanentStudentId
                                selectedAccountForDetail = null
                                onOpenStudentPage(targetId)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isUrdu) "طالب علم کا صفحہ کھولیں" else "Open Student Page")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedAccountForDetail = null
                                editingAccount = liveAccount
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isUrdu) "ترمیم" else "Edit")
                        }

                        if (liveAccount.id != currentUser.id) {
                            OutlinedButton(
                                onClick = {
                                    selectedAccountForDetail = null
                                    deletingAccount = liveAccount
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isUrdu) "حذف" else "Delete")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAccountForDetail = null }) {
                    Text(if (isUrdu) "بند کریں" else "Close")
                }
            }
        )
    }

    // Create Account Dialog (Admin / SuperAdmin)
    if (showCreateAccountDialog) {
        var accRole by remember { mutableStateOf(UserRole.Teacher) }
        var accName by remember { mutableStateOf("") }
        var accEmail by remember { mutableStateOf("") }
        var accPhone by remember { mutableStateOf("") }
        var accPassword by remember { mutableStateOf("") }
        var accFatherName by remember { mutableStateOf("") }
        var accGrade by remember { mutableStateOf("Grade 9") }
        var accSection by remember { mutableStateOf("Section A") }
        var accBForm by remember { mutableStateOf("") }
        var accError by remember { mutableStateOf<String?>(null) }
        var accLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!accLoading) showCreateAccountDialog = false },
            title = { Text(if (isUrdu) "نیا اسکول اکاؤنٹ بنائیں" else "Create School Account") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = if (isUrdu) "کردار منتخب کریں:" else "Select Account Role:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf(UserRole.Administrator, UserRole.Teacher, UserRole.Student, UserRole.Parent)) { roleOpt ->
                                FilterChip(
                                    selected = accRole == roleOpt,
                                    onClick = { accRole = roleOpt },
                                    label = { Text(roleOpt.getDisplayName(isUrdu), maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = accName,
                            onValueChange = { accName = it },
                            label = { Text(if (isUrdu) "پورا نام" else "Full Name") },
                            modifier = Modifier.fillMaxWidth().testTag("input_create_acc_name"),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = accEmail,
                            onValueChange = { accEmail = it },
                            label = { Text(if (isUrdu) "ای میل" else "Email") },
                            modifier = Modifier.fillMaxWidth().testTag("input_create_acc_email"),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = accPhone,
                            onValueChange = { accPhone = it },
                            label = { Text(if (isUrdu) "فون نمبر" else "Phone") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = accPassword,
                            onValueChange = { accPassword = it },
                            label = { Text(if (isUrdu) "پاس ورڈ (کم از کم 6 حروف)" else "Password (min 6 chars)") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth().testTag("input_create_acc_password"),
                            singleLine = true
                        )
                    }
                    if (accRole == UserRole.Student) {
                        item {
                            OutlinedTextField(
                                value = accFatherName,
                                onValueChange = { accFatherName = it },
                                label = { Text(if (isUrdu) "والد کا نام" else "Father's Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = accGrade,
                                    onValueChange = { accGrade = it },
                                    label = { Text("Grade") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = accSection,
                                    onValueChange = { accSection = it },
                                    label = { Text("Section") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                            }
                        }
                        item {
                            OutlinedTextField(
                                value = accBForm,
                                onValueChange = { accBForm = it },
                                label = { Text("B-Form (Admin Only)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                    if (accError != null) {
                        item {
                            Text(
                                text = accError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !accLoading,
                    onClick = {
                        coroutineScope.launch {
                            accLoading = true
                            accError = null
                            val res = repository.createAccountSuspend(
                                name = accName,
                                email = accEmail,
                                phone = accPhone,
                                password = accPassword,
                                role = accRole,
                                grade = accGrade,
                                section = accSection,
                                fatherName = accFatherName,
                                bForm = accBForm,
                                actingUser = currentUser
                            )
                            accLoading = false
                            res.fold(
                                onSuccess = { showCreateAccountDialog = false },
                                onFailure = { accError = it.message ?: "Failed to create account" }
                            )
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_create_account")
                ) {
                    if (accLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(if (isUrdu) "اکاؤنٹ بنائیں" else "Create Account")
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !accLoading, onClick = { showCreateAccountDialog = false }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Edit Account Dialog
    editingAccount?.let { targetAcc ->
        var editName by remember(targetAcc.id) { mutableStateOf(targetAcc.name) }
        var editEmail by remember(targetAcc.id) { mutableStateOf(targetAcc.email) }
        var editPhone by remember(targetAcc.id) { mutableStateOf(targetAcc.phone) }
        var editPassword by remember(targetAcc.id) { mutableStateOf(targetAcc.password) }
        var editStatus by remember(targetAcc.id) { mutableStateOf(targetAcc.approvalStatus) }
        var editError by remember(targetAcc.id) { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { editingAccount = null },
            title = { Text(if (isUrdu) "اکاؤنٹ میں ترمیم (${targetAcc.loginId})" else "Edit Account (${targetAcc.loginId})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text(if (isUrdu) "پورا نام" else "Full Name") },
                        modifier = Modifier.fillMaxWidth().testTag("input_edit_acc_name"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text(if (isUrdu) "ای میل" else "Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text(if (isUrdu) "فون" else "Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = editPassword,
                        onValueChange = { editPassword = it },
                        label = { Text(if (isUrdu) "پاس ورڈ" else "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf(ApprovalStatus.Approved, ApprovalStatus.PendingApproval, ApprovalStatus.Rejected)) { st ->
                            FilterChip(
                                selected = editStatus == st,
                                onClick = { editStatus = st },
                                label = { Text(st.getDisplayName(isUrdu), maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    if (editError != null) {
                        Text(text = editError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = repository.updateAccount(
                            accountId = targetAcc.id,
                            name = editName,
                            email = editEmail,
                            phone = editPhone,
                            password = editPassword,
                            role = targetAcc.role,
                            approvalStatus = editStatus,
                            actingUser = currentUser
                        )
                        res.fold(
                            onSuccess = { editingAccount = null },
                            onFailure = { editError = it.message ?: "Failed to update account" }
                        )
                    },
                    modifier = Modifier.testTag("btn_save_edit_account")
                ) {
                    Text(if (isUrdu) "محفوظ کریں" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAccount = null }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Delete Account Confirmation Dialog
    deletingAccount?.let { targetAcc ->
        AlertDialog(
            onDismissRequest = { deletingAccount = null },
            title = { Text(if (isUrdu) "کیا آپ واقعی مطمئن ہیں؟" else "Are you sure?") },
            text = {
                Text(
                    if (isUrdu) "کیا آپ اکاؤنٹ ${targetAcc.name} (${targetAcc.loginId}) حذف کرنا چاہتے ہیں؟ طالب علم کی مستقل آئی ڈی محفوظ رہے گی۔"
                    else "Are you sure you want to delete account ${targetAcc.name} (${targetAcc.loginId})? Any Student ID remains archived and never reused."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteAccount(targetAcc.id, currentUser)
                        deletingAccount = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_account")
                ) {
                    Text(if (isUrdu) "حذف کریں" else "Delete Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAccount = null }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Assign Students to Teacher Dialog
    assigningTeacherAccount?.let { teacherAcc ->
        val allActiveStudents = repository.getAllStudentsList()
        val liveTeacher = schoolAccounts.find { it.id == teacherAcc.id } ?: teacherAcc
        AlertDialog(
            onDismissRequest = { assigningTeacherAccount = null },
            title = {
                Text(
                    text = if (isUrdu) "استاد کو طلباء تفویض کریں: ${liveTeacher.name}" else "Assign Students to ${liveTeacher.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allActiveStudents) { st ->
                        val isAssigned = st.assignedTeacherIds.contains(liveTeacher.id) || liveTeacher.assignedStudentIds.contains(st.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    repository.toggleAssignStudentToTeacher(st.id, liveTeacher.id, currentUser)
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isAssigned,
                                onCheckedChange = {
                                    repository.toggleAssignStudentToTeacher(st.id, liveTeacher.id, currentUser)
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = st.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = "${st.permanentStudentId} • ${st.grade}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { assigningTeacherAccount = null }) {
                    Text(if (isUrdu) "مکمل" else "Done")
                }
            }
        )
    }

    // Link Children to Parent Dialog
    linkingParentAccount?.let { parentAcc ->
        val allActiveStudents = repository.getAllStudentsList()
        val liveParent = schoolAccounts.find { it.id == parentAcc.id } ?: parentAcc
        AlertDialog(
            onDismissRequest = { linkingParentAccount = null },
            title = {
                Text(
                    text = if (isUrdu) "والدین کے ساتھ بچے لنک کریں: ${liveParent.name}" else "Link Children to ${liveParent.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allActiveStudents) { st ->
                        val isLinked = st.parentId == liveParent.id || liveParent.linkedChildIds.contains(st.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    repository.linkParentToChild(liveParent.id, st.id, unlink = isLinked, actingUser = currentUser)
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isLinked,
                                onCheckedChange = {
                                    repository.linkParentToChild(liveParent.id, st.id, unlink = isLinked, actingUser = currentUser)
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = st.fullName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = "${st.permanentStudentId} • ${st.grade}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { linkingParentAccount = null }) {
                    Text(if (isUrdu) "مکمل" else "Done")
                }
            }
        )
    }
}
