package com.example.ui.attendance

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SchoolRepository
import com.example.localization.AppStrings
import com.example.model.AppLanguage
import com.example.model.AttendanceAlertType
import com.example.model.AttendanceRecord
import com.example.model.AttendanceStatus
import com.example.model.StudentRecord
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.DbusDeleteIconButton
import com.example.ui.media.StudentAvatar
import java.util.Calendar
import java.util.Locale

@Composable
fun AttendanceScreen(
    repository: SchoolRepository,
    currentUser: UserProfile,
    language: AppLanguage,
    onOpenStudentPage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    // Observe Repository States
    val attendanceRecords by repository.attendanceRecords.collectAsState()
    val studentsList by repository.studentsList.collectAsState()
    val weeklyOffDays by repository.weeklyOffDays.collectAsState()
    val schoolHolidays by repository.schoolHolidays.collectAsState()
    val lowAttendanceThreshold by repository.lowAttendanceThreshold.collectAsState()
    val alerts by repository.attendanceAlerts.collectAsState()

    // Filter students by Role (Requirement 3: Who sees what)
    val accessibleStudents = remember(currentUser, studentsList) {
        repository.getAttendanceStudentsList(currentUser)
    }

    val calToday = remember { Calendar.getInstance(Locale.US) }
    val currentYear = remember { calToday.get(Calendar.YEAR) }
    val currentMonth = remember { calToday.get(Calendar.MONTH) + 1 }
    val currentMonthCalIndex = remember { calToday.get(Calendar.MONTH) }
    val currentMonthName = remember {
        java.text.SimpleDateFormat("MMMM yyyy", Locale.US).format(calToday.time)
    }
    val daysInCurrentMonth = remember { calToday.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val quickDateChips = remember {
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        (0..2).map { offset ->
            val c = Calendar.getInstance(Locale.US).apply { add(Calendar.DAY_OF_MONTH, -offset) }
            fmt.format(c.time)
        }
    }

    // Selected Date (Defaults to today)
    var selectedDate by remember { mutableStateOf(repository.todayDate()) }

    // Search query (Requirement 6: Search a student by name or Student ID)
    var searchQuery by remember { mutableStateOf("") }

    val filteredStudents = remember(accessibleStudents, searchQuery) {
        if (searchQuery.isBlank()) accessibleStudents
        else {
            val q = searchQuery.trim().lowercase()
            accessibleStudents.filter { s ->
                s.fullName.lowercase().contains(q) ||
                s.urduName.lowercase().contains(q) ||
                s.permanentStudentId.lowercase().contains(q) ||
                s.fatherName.lowercase().contains(q)
            }
        }
    }

    // Selected student for detailed summary, calendar, and chart (always synced with live list)
    var selectedStudentIdForDetail by remember {
        mutableStateOf<String?>(accessibleStudents.firstOrNull()?.permanentStudentId)
    }
    val selectedStudentForDetail = remember(accessibleStudents, selectedStudentIdForDetail) {
        accessibleStudents.find { it.permanentStudentId == selectedStudentIdForDetail }
            ?: accessibleStudents.firstOrNull()
    }

    // Dialog States
    var showMarkAttendanceDialog by remember { mutableStateOf<StudentRecord?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showAdminConfigDialog by remember { mutableStateOf(false) }
    var showChangeLogDialog by remember { mutableStateOf<AttendanceRecord?>(null) }
    var detailedBreakdownType by remember { mutableStateOf<String?>(null) }

    val relevantAlerts = remember(alerts, currentUser) {
        when (currentUser.role) {
            UserRole.Administrator -> alerts.filter { it.type == AttendanceAlertType.LOW_ATTENDANCE }
            UserRole.Parent -> alerts.filter { it.type == AttendanceAlertType.ABSENT_OR_LATE }
            else -> emptyList()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))

            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.attendance(language),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (isUrdu) "تعلیمی سیشن $currentYear-${currentYear + 1} • DBUS حاضری ماڈیول"
                        else "Academic Session $currentYear-${currentYear + 1} • DBUS Attendance Module",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Admin Config Button (Weekly off days, holidays, low attendance threshold)
                    if (currentUser.role == UserRole.Administrator) {
                        OutlinedButton(
                            onClick = { showAdminConfigDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_attendance_admin_config")
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isUrdu) "ترتیبات" else "Config", style = MaterialTheme.typography.labelSmall)
                        }

                        // Export Button (Requirement 3: Administrators see all and can export)
                        Button(
                            onClick = { showExportDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_export_attendance")
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (isUrdu) "ایکسپورٹ" else "Export", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Active Attendance Alerts Banner (Requirement 4)
        if (relevantAlerts.isNotEmpty()) {
            item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.NotificationImportant,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isUrdu) "حاضری الرٹ (نوٹیفکیشن)" else "Attendance Alert Notices",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            relevantAlerts.take(3).forEach { alert ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "• " + (if (isUrdu) alert.urduMessage else alert.message),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { repository.dismissAlert(alert.id) },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

        // Search Bar (Requirement 6: Search student by name or Student ID)
        // Teachers and Admins have search bar in attendance
        if (currentUser.role == UserRole.Teacher || currentUser.role == UserRole.Administrator) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_attendance_search"),
                    placeholder = {
                        Text(
                            text = if (isUrdu) "طالب علم کا نام یا اسٹوڈنٹ آئی ڈی تلاش کریں..."
                            else "Search by student name or Student ID...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Date Bar for Marking or Reviewing (2 clean rows: Selected Date on 1 line; quick chips on Row 2)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUrdu) "منتخب تاریخ: $selectedDate" else "Selected Date: $selectedDate",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Quick Date Switcher on separate row so chips are never squeezed
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickDateChips.forEachIndexed { idx, d ->
                            val isSel = selectedDate == d
                            val chipLabel = when (idx) {
                                0 -> if (isUrdu) "آج (${d.takeLast(5)})" else "Today (${d.takeLast(5)})"
                                1 -> if (isUrdu) "کل (${d.takeLast(5)})" else "Yesterday (${d.takeLast(5)})"
                                else -> d.takeLast(5)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .clickable { selectedDate = d }
                                    .testTag("att_date_chip_${d.takeLast(5)}")
                            ) {
                                Text(
                                    text = chipLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Roster / Student Daily Attendance Header (Stacked on 2 lines cleanly)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = when (currentUser.role) {
                        UserRole.Teacher -> if (isUrdu) "مقررہ طلباء کی روزانہ حاضری" else "Assigned Students Attendance"
                        UserRole.Parent -> if (isUrdu) "آپ کے بچوں کی حاضری" else "Your Children Attendance"
                        UserRole.Student -> if (isUrdu) "آپ کا حاضری ریکارڈ" else "Your Attendance Record"
                        UserRole.Administrator, UserRole.SuperAdmin -> if (isUrdu) "اسکول کے تمام طلباء" else "All Students Attendance"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (currentUser.role == UserRole.Teacher || currentUser.role == UserRole.Administrator || currentUser.role == UserRole.SuperAdmin) {
                    Text(
                        text = if (isUrdu) "حاضری لگانے یا ترمیم کیلئے ٹیپ کریں" else "Tap row to mark/edit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (filteredStudents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("empty_attendance_students"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = when {
                                searchQuery.isNotBlank() ->
                                    if (isUrdu) "کوئی طالب علم نہیں ملا" else "No students match your query"
                                currentUser.role == UserRole.Teacher ->
                                    if (isUrdu) "ابھی کوئی طالب علم تفویض نہیں کیا گیا" else "No students assigned yet"
                                currentUser.role == UserRole.Parent ->
                                    if (isUrdu) "آپ کے اکاؤنٹ سے ابھی کوئی بچہ منسلک نہیں ہے" else "No children linked yet"
                                currentUser.role == UserRole.Student ->
                                    if (isUrdu) "آپ کا طالب علم ریکارڈ ابھی منسلک نہیں ہے" else "Your student profile is not linked yet"
                                else ->
                                    if (isUrdu) "ابھی کوئی طالب علم موجود نہیں ہے" else "No students yet"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(filteredStudents) { student ->
                val record = repository.getDailyRecord(student.permanentPermanentIdSafe(), selectedDate)
                val summary = repository.getAttendanceSummary(student.permanentPermanentIdSafe(), currentYear, currentMonth)
                val canMark = repository.canUserMarkAttendance(currentUser, student)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedStudentIdForDetail = student.permanentStudentId
                            if (canMark) {
                                showMarkAttendanceDialog = student
                            }
                        }
                        .testTag("att_row_${student.permanentStudentId}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedStudentForDetail?.permanentStudentId == student.permanentStudentId)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (selectedStudentForDetail?.permanentStudentId == student.permanentStudentId)
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.clickable { onOpenStudentPage(student.permanentStudentId) }) {
                                    StudentAvatar(student = student, size = 44.dp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isUrdu) student.urduName else student.fullName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ID: ${student.permanentStudentId} • ${student.grade}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isUrdu) "والد: ${student.fatherName}" else "Father: ${student.fatherName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onOpenStudentPage(student.permanentStudentId) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = if (isUrdu) "طالب علم کا صفحہ" else "Student Profile",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Attendance Status Badge
                            val status = record?.status
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (status != null) Color(status.colorHex).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (status != null) Color(status.colorHex)
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = status?.getDisplayName(isUrdu)
                                            ?: if (isUrdu) "درج نہیں" else "Not Marked",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (status != null) Color(status.colorHex)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // Note / Change log info if present
                        if (record != null && record.note.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📝 ${record.note}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (record.changeLog.isNotEmpty()) {
                                        TextButton(
                                            onClick = { showChangeLogDialog = record },
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = if (isUrdu) "آڈٹ لاگ (${record.changeLog.size})" else "Log (${record.changeLog.size})",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Summary percentage ribbon
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Rate: ${String.format(Locale.US, "%.1f", summary.attendancePercentage)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.attendancePercentage < lowAttendanceThreshold) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )

                            Text(
                                text = "P: ${summary.presentDays} | A: ${summary.absentDays} | L: ${summary.lateDays} | Lv: ${summary.leaveDays}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Requirement 2: Summaries (Monthly & Yearly, Calendar View and Chart)
        selectedStudentForDetail?.let { detailStudent ->
            val summaryMonth = repository.getAttendanceSummary(detailStudent.permanentStudentId, currentYear, currentMonth)
            val summaryYear = repository.getAttendanceSummary(detailStudent.permanentStudentId, currentYear, null)
            val allStudentRecords = attendanceRecords[detailStudent.permanentStudentId] ?: emptyList()

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrdu) "خلاصہ و کیلنڈر چارٹ (${detailStudent.fullName})"
                        else "Summary & Calendar (${detailStudent.fullName})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Summary Totals: Monthly & Yearly Cards (Requirement 2)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Monthly Card (Clickable for Detailed Breakdown - Requirement 8)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { detailedBreakdownType = "monthly" }
                            .testTag("card_attendance_summary_monthly"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUrdu) "ماہانہ خلاصہ ($currentMonthName)" else "Monthly Summary",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", summaryMonth.attendancePercentage)}%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Present: ${summaryMonth.presentDays} • Absent: ${summaryMonth.absentDays}\nLate: ${summaryMonth.lateDays} • Leave: ${summaryMonth.leaveDays}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isUrdu) "تفصیل دیکھیں ←" else "View dates & remarks →",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Yearly Academic Year Card (Clickable for Detailed Breakdown - Requirement 8)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { detailedBreakdownType = "yearly" }
                            .testTag("card_attendance_summary_yearly"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUrdu) "تعلیمی سال $currentYear-${currentYear + 1}" else "Academic Year Total",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format(Locale.US, "%.1f", summaryYear.attendancePercentage)}%",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Present: ${summaryYear.presentDays} • Absent: ${summaryYear.absentDays}\nLate: ${summaryYear.lateDays} • Leave: ${summaryYear.leaveDays}",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isUrdu) "تفصیل دیکھیں ←" else "View dates & remarks →",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Simple Calendar View (Requirement 2: Show a simple calendar view and a chart)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
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
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isUrdu) "$currentMonthName کا حاضری کیلنڈر" else "$currentMonthName Attendance Calendar",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = if (isUrdu) "تعطیلات خارج ہیں" else "Off days excluded",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Grid of days for active calendar month
                        val monthPadded = currentMonth.toString().padStart(2, '0')
                        val daysGrid = (1..daysInCurrentMonth).toList()
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            daysGrid.chunked(7).forEach { week ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    week.forEach { dayNum ->
                                        val dayStr = "$currentYear-$monthPadded-" + dayNum.toString().padStart(2, '0')
                                        val rec = allStudentRecords.find { it.date == dayStr }
                                        val calCheck = Calendar.getInstance(Locale.US).apply {
                                            set(currentYear, currentMonthCalIndex, dayNum)
                                        }
                                        val isSunday = weeklyOffDays.contains(calCheck.get(Calendar.DAY_OF_WEEK))
                                        val isHoliday = schoolHolidays.any { it.date == dayStr }

                                        val bgColor = when {
                                            isHoliday -> Color(0xFFE0E0E0)
                                            isSunday -> Color(0xFFEEEEEE)
                                            rec == null -> MaterialTheme.colorScheme.surfaceVariant
                                            rec.status == AttendanceStatus.PRESENT -> Color(0xFFE8F5E9)
                                            rec.status == AttendanceStatus.ABSENT -> Color(0xFFFFEBEE)
                                            rec.status == AttendanceStatus.LATE -> Color(0xFFFFF3E0)
                                            rec.status == AttendanceStatus.LEAVE -> Color(0xFFE3F2FD)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }

                                        val textColor = when {
                                            isHoliday || isSunday -> Color.Gray
                                            rec == null -> MaterialTheme.colorScheme.onSurfaceVariant
                                            rec.status == AttendanceStatus.PRESENT -> Color(0xFF2E7D32)
                                            rec.status == AttendanceStatus.ABSENT -> Color(0xFFC62828)
                                            rec.status == AttendanceStatus.LATE -> Color(0xFFEF6C00)
                                            rec.status == AttendanceStatus.LEAVE -> Color(0xFF1565C0)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(bgColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = "$dayNum",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                                Text(
                                                    text = when {
                                                        isHoliday -> "HOL"
                                                        isSunday -> "OFF"
                                                        rec != null -> rec.status.shortCode
                                                        else -> "-"
                                                    },
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = textColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Calendar Legend
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LegendItem(label = "Present (P)", color = Color(0xFF2E7D32))
                            LegendItem(label = "Absent (A)", color = Color(0xFFD32F2F))
                            LegendItem(label = "Late (L)", color = Color(0xFFF57C00))
                            LegendItem(label = "Leave (LV)", color = Color(0xFF1976D2))
                            LegendItem(label = "Off/Hol", color = Color.Gray)
                        }
                    }
                }
            }

            // Attendance Trend Bar Chart (Requirement 2)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isUrdu) "ہفتہ وار حاضری کا رجحان (حاضر / غیر حاضر)" else "Weekly Attendance Breakdown Chart",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 4 Weekly comparison bars computed from real records for the active month
                        val monthPrefix = "$currentYear-${currentMonth.toString().padStart(2, '0')}-"
                        val monthRecords = allStudentRecords.filter { it.date.startsWith(monthPrefix) }
                        val weekRanges = listOf(
                            Triple("W1 (Days 1-7)", 1, 7),
                            Triple("W2 (Days 8-14)", 8, 14),
                            Triple("W3 (Days 15-21)", 15, 21),
                            Triple("W4 (Days 22-$daysInCurrentMonth)", 22, daysInCurrentMonth)
                        )
                        val weeklyStats = weekRanges.map { (label, startDay, endDay) ->
                            val inWeek = monthRecords.filter { rec ->
                                val d = rec.date.split("-").getOrNull(2)?.toIntOrNull() ?: 0
                                d in startDay..endDay
                            }
                            val pCount = inWeek.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
                            val aCount = inWeek.count { it.status == AttendanceStatus.ABSENT }
                            Triple(label, pCount, aCount)
                        }

                        if (monthRecords.isEmpty()) {
                            Text(
                                text = if (isUrdu) "اس مہینے میں ابھی کوئی حاضری ریکارڈ نہیں ہے۔"
                                else "No attendance records logged for this month yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            weeklyStats.forEach { (weekName, presentCount, absentCount) ->
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = weekName, style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            text = "$presentCount Present • $absentCount Absent",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        if (presentCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(presentCount.toFloat())
                                                    .fillMaxSize()
                                                    .background(Color(0xFF2E7D32))
                                            )
                                        }
                                        if (absentCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(absentCount.toFloat())
                                                    .fillMaxSize()
                                                    .background(Color(0xFFD32F2F))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // -------------------------------------------------------------
    // DIALOGS
    // -------------------------------------------------------------

    // 1. Mark Attendance Dialog (Requirement 1: Teacher marks assigned student, optional note, edit same day saved in log)
    showMarkAttendanceDialog?.let { student ->
        val existing = repository.getDailyRecord(student.permanentStudentId, selectedDate)
        var selectedStatus by remember { mutableStateOf(existing?.status ?: AttendanceStatus.PRESENT) }
        var optionalNote by remember { mutableStateOf(existing?.note ?: "") }

        AlertDialog(
            onDismissRequest = { showMarkAttendanceDialog = null },
            title = {
                Column {
                    Text(
                        text = if (isUrdu) "روزانہ کی حاضری درج کریں" else "Mark Daily Attendance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${student.fullName} • ID: ${student.permanentStudentId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Date: $selectedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isUrdu) "حاضری کی حیثیت منتخب کریں:" else "Select Attendance Status:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AttendanceStatus.values().forEach { st ->
                            val isSel = selectedStatus == st
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedStatus = st }
                                    .testTag("status_choice_${st.name.lowercase()}"),
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Color(st.colorHex) else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, if (isSel) Color(st.colorHex) else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = st.getDisplayName(isUrdu),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional Note Input (Requirement 1: e.g. reason for leave)
                    OutlinedTextField(
                        value = optionalNote,
                        onValueChange = { optionalNote = it },
                        label = { Text(if (isUrdu) "اختیاری نوٹ (مثلاً رخصت کی وجہ)" else "Optional Note (e.g. Reason for Leave)") },
                        placeholder = { Text(if (isUrdu) "وجہ درج کریں..." else "Enter reason or details...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_att_note"),
                        singleLine = true
                    )

                    if (existing != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isUrdu) "ℹ️ آپ اسی دن حاضری ایڈٹ کر رہے ہیں۔ تبدیلی آڈٹ لاگ میں محفوظ ہوگی۔"
                            else "ℹ️ Editing existing attendance for today. Change will be recorded in audit log.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.markDailyAttendance(
                            studentPermanentId = student.permanentStudentId,
                            date = selectedDate,
                            status = selectedStatus,
                            note = optionalNote,
                            teacherName = currentUser.name
                        )
                        showMarkAttendanceDialog = null
                    },
                    modifier = Modifier.testTag("btn_save_attendance")
                ) {
                    Text(if (isUrdu) "محفوظ کریں" else "Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkAttendanceDialog = null }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // 2. Export Dialog (Requirement 3: Administrators can export)
    if (showExportDialog) {
        val exportText = remember(accessibleStudents, attendanceRecords) { repository.exportAttendanceCsv(accessibleStudents) }
        var copied by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrdu) "حاضری ڈیٹا برآمد کریں (CSV Export)" else "Export Attendance Data (CSV)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = if (isUrdu) "اسکول کے تمام طلباء کی مکمل حاضری تاریخ اور مستقل اسٹوڈنٹ آئی ڈیز کے ساتھ برآمد کے لئے تیار ہے:"
                        else "All attendance records linked to permanent Student IDs are formatted as CSV below:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Text(
                            text = exportText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportText))
                        copied = true
                    }
                ) {
                    Icon(imageVector = if (copied) Icons.Default.Check else Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (copied) (if (isUrdu) "کاپی ہو گیا!" else "Copied!") else (if (isUrdu) "کلپ بورڈ پر کاپی کریں" else "Copy CSV"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(if (isUrdu) "بند کریں" else "Close")
                }
            }
        )
    }

    // 3. Admin Configuration Dialog (Requirement 4 & 5: Off days, holidays, low attendance threshold)
    if (showAdminConfigDialog) {
        var thresholdInput by remember { mutableIntStateOf(lowAttendanceThreshold) }
        var newHolName by remember { mutableStateOf("") }
        var newHolDate by remember { mutableStateOf(repository.todayDate()) }

        AlertDialog(
            onDismissRequest = { showAdminConfigDialog = false },
            title = {
                Text(
                    text = if (isUrdu) "ایڈمن حاضری ترتیبات" else "Admin Attendance Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Requirement 4: Alert threshold (default 75%, admin can change)
                    item {
                        Text(
                            text = if (isUrdu) "کم حاضری الرٹ کی حد (%)" else "Low Attendance Alert Threshold (%)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isUrdu) "جب ماہانہ حاضری اس نمبر سے کم ہو تو ایڈمن کو مطلع کریں:"
                            else "Alert admin when monthly attendance falls below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(65, 70, 75, 80, 85).forEach { th ->
                                val isSel = thresholdInput == th
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                    .clickable { thresholdInput = th },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "$th%",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Requirement 5: Weekly off days (Admin sets off days)
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = if (isUrdu) "ہفتہ وار تعطیلات (Weekly Off Days)" else "Weekly Off Days",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isUrdu) "یہ دن اسکول کے کام کے دنوں میں شمار نہیں ہوتے:"
                            else "These days are excluded from working day counts:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        val daysList = listOf(
                            Pair(Calendar.SUNDAY, "Sunday (اتوار)"),
                            Pair(Calendar.SATURDAY, "Saturday (ہفتہ)"),
                            Pair(Calendar.FRIDAY, "Friday (جمعہ)")
                        )
                        daysList.forEach { (calDay, label) ->
                            val isChecked = weeklyOffDays.contains(calDay)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val updated = if (isChecked) weeklyOffDays - calDay else weeklyOffDays + calDay
                                        repository.setWeeklyOffDays(updated)
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = {
                                        val updated = if (it) weeklyOffDays + calDay else weeklyOffDays - calDay
                                        repository.setWeeklyOffDays(updated)
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    // Requirement 5: School Holidays list & Add holiday
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = if (isUrdu) "اسکول کی سرکاری تعطیلات (School Holidays)" else "School Holidays List",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        schoolHolidays.forEach { hol ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = if (isUrdu) hol.urduName else hol.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                    Text(text = hol.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                DbusDeleteIconButton(
                                    onClick = { repository.removeSchoolHoliday(hol.id) },
                                    contentDescription = "Delete Holiday"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newHolName,
                            onValueChange = { newHolName = it },
                            label = { Text("Holiday Name (e.g. Winter Break)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newHolDate,
                                onValueChange = { newHolDate = it },
                                label = { Text("Date (YYYY-MM-DD)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    if (newHolName.isNotBlank() && newHolDate.isNotBlank()) {
                                        repository.addSchoolHoliday(newHolName, newHolName, newHolDate)
                                        newHolName = ""
                                    }
                                }
                            ) {
                                Text("Add")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.setLowAttendanceThreshold(thresholdInput)
                        showAdminConfigDialog = false
                    }
                ) {
                    Text(if (isUrdu) "محفوظ کریں" else "Save Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAdminConfigDialog = false }) {
                    Text(if (isUrdu) "بند کریں" else "Done")
                }
            }
        )
    }

    // 4. Change Log Audit Dialog (Requirement 1: Every change is saved in a log)
    showChangeLogDialog?.let { rec ->
        AlertDialog(
            onDismissRequest = { showChangeLogDialog = null },
            title = {
                Text(
                    text = if (isUrdu) "حاضری تبدیلیوں کا آڈٹ لاگ" else "Attendance Change Audit Log",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Student ID: ${rec.studentPermanentId} • Date: ${rec.date}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (rec.changeLog.isEmpty()) {
                        Text(
                            text = if (isUrdu) "اس ریکارڈ میں کوئی تبدیلی نہیں ہوئی۔" else "No modifications made to this record.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(rec.changeLog) { log ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${log.previousStatus.englishName} ➔ ${log.newStatus.englishName}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(log.newStatus.colorHex)
                                            )
                                            Text(text = log.timestamp, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                        }
                                        Text(text = "By: ${log.modifiedBy}", style = MaterialTheme.typography.labelSmall)
                                        if (log.note.isNotBlank()) {
                                            Text(text = "Reason: ${log.note}", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showChangeLogDialog = null }) {
                    Text(if (isUrdu) "ٹھیک ہے" else "Done")
                }
            }
        )
    }

    // Detailed Breakdown Dialog for Monthly and Yearly Summary Cards
    detailedBreakdownType?.let { type ->
        selectedStudentForDetail?.let { detailStudent ->
            val isYearly = type == "yearly"
            val summary = if (isYearly) repository.getAttendanceSummary(detailStudent.permanentStudentId, currentYear, null)
            else repository.getAttendanceSummary(detailStudent.permanentStudentId, currentYear, currentMonth)
            val records = attendanceRecords[detailStudent.permanentStudentId] ?: emptyList()

            DetailedAttendanceBreakdownDialog(
                student = detailStudent,
                summary = summary,
                allRecords = records,
                isYearly = isYearly,
                language = language,
                onDismiss = { detailedBreakdownType = null }
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(text = label, fontSize = 9.sp, color = Color.Gray)
    }
}

private fun StudentRecord.permanentPermanentIdSafe(): String = this.permanentStudentId
