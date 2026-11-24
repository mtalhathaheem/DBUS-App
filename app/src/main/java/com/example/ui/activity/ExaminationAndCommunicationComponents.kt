package com.example.ui.activity

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SchoolRepository
import com.example.model.AnnouncementComment
import com.example.model.AppLanguage
import com.example.model.ExamScheduleItem
import com.example.model.ParentTeacherAnnouncement
import com.example.model.StudentExamResult
import com.example.model.StudentRecord
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.DbusAddButton
import com.example.ui.components.DbusDeleteIconButton
import com.example.ui.components.DbusEditIconButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

/**
 * Requirement 8: Examination & Academic Performance section
 * - Subject marks, exam schedules CRUD, result edit/delete, and report cards.
 */
@Composable
fun ExaminationsTabContent(
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val examSchedules by repository.examSchedules.collectAsState()
    val allResults by repository.examResults.collectAsState()
    val students by repository.studentsList.collectAsState()

    val accessibleStudents = remember(students, currentUser) {
        repository.getAttendanceStudentsList(currentUser)
    }

    var selectedStudentPermanentId by remember(currentUser, accessibleStudents) {
        mutableStateOf(
            if (currentUser?.role == UserRole.Student) {
                currentUser.permanentStudentId ?: accessibleStudents.firstOrNull()?.permanentStudentId
            } else {
                accessibleStudents.firstOrNull()?.permanentStudentId
            }
        )
    }

    val targetStudent: StudentRecord? = remember(selectedStudentPermanentId, accessibleStudents, currentUser) {
        if (currentUser?.role == UserRole.Student) {
            accessibleStudents.find { it.permanentStudentId == currentUser.permanentStudentId } ?: accessibleStudents.firstOrNull()
        } else {
            accessibleStudents.find { it.permanentStudentId == selectedStudentPermanentId } ?: accessibleStudents.firstOrNull()
        }
    }

    val studentResults = remember(targetStudent, allResults) {
        if (targetStudent == null) emptyList()
        else repository.getExamResultsForStudent(targetStudent.permanentStudentId)
    }

    var showReportCardDialog by remember { mutableStateOf(false) }
    var showRecordMarksDialog by remember { mutableStateOf(false) }
    var editingResult by remember { mutableStateOf<StudentExamResult?>(null) }
    var deletingResult by remember { mutableStateOf<StudentExamResult?>(null) }

    var showAddScheduleDialog by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ExamScheduleItem?>(null) }
    var deletingSchedule by remember { mutableStateOf<ExamScheduleItem?>(null) }

    val canRecordMarks = currentUser?.role == UserRole.Teacher ||
        currentUser?.role == UserRole.Administrator ||
        currentUser?.role == UserRole.SuperAdmin
    val isAdmin = currentUser?.role == UserRole.Administrator || currentUser?.role == UserRole.SuperAdmin

    val totalObtained = studentResults.sumOf { it.marksObtained }
    val totalMax = studentResults.sumOf { it.totalMarks }.coerceAtLeast(1)
    val percentage = (totalObtained.toFloat() / totalMax.toFloat()) * 100f
    val overallGrade = when {
        studentResults.isEmpty() -> "—"
        percentage >= 90 -> "A+"
        percentage >= 80 -> "A"
        percentage >= 70 -> "B"
        else -> "C"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUrdu) "امتحانات و تعلیمی کارکردگی" else "Examinations & Performance",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isUrdu) "نصاب، شیڈول اور امتحانی نتائج کا ریکارڈ" else "Exam schedules, subject marks & report cards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                if (canRecordMarks && targetStudent != null) {
                    DbusAddButton(
                        text = if (isUrdu) "نمبر درج کریں" else "Record Marks",
                        onClick = { showRecordMarksDialog = true },
                        modifier = Modifier.testTag("btn_record_exam_marks")
                    )
                }
            }
        }

        // Student switcher for Admin, Teacher, and Parent with multiple children
        if (currentUser?.role != UserRole.Student && accessibleStudents.size > 1) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accessibleStudents, key = { it.permanentStudentId }) { st ->
                        FilterChip(
                            selected = st.permanentStudentId == targetStudent?.permanentStudentId,
                            onClick = { selectedStudentPermanentId = st.permanentStudentId },
                            label = {
                                Text(
                                    text = "${if (isUrdu) st.urduName else st.fullName} (${st.permanentStudentId.takeLast(4)})",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }
            }
        }

        // Target Student Banner & Report Card Trigger
        if (targetStudent == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (isUrdu) "ابھی تک کوئی طالب علم موجود نہیں۔" else "No students yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isUrdu) targetStudent.urduName else targetStudent.fullName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ID: ${targetStudent.permanentStudentId} • ${targetStudent.grade}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { showReportCardDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("btn_open_report_card")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isUrdu) "رزلٹ کارڈ" else "Report Card",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = if (isUrdu) "کل نمبر" else "Total Marks", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = if (studentResults.isEmpty()) "0 / 0" else "$totalObtained / $totalMax",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = if (isUrdu) "فیصد" else "Percentage", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = if (studentResults.isEmpty()) "0.0%" else "${String.format(Locale.US, "%.1f", percentage)}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(text = if (isUrdu) "گریڈ" else "Grade", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        text = overallGrade,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Subject-Wise Marks Breakdown
        item {
            Text(
                text = if (isUrdu) "مضامین کے نمبر اور گریڈز" else "Subject Marks & Grades",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (studentResults.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = if (isUrdu) "اس طالب علم کے امتحانی نمبر ابھی درج نہیں کیے گئے۔"
                        else "No exam marks recorded for this student yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(studentResults, key = { it.id }) { res ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = canRecordMarks) { editingResult = res }
                    .testTag("result_row_${res.subject.lowercase().replace(" ", "_")}"),
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = res.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(text = res.remarks, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${res.marksObtained} / ${res.totalMarks}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (res.grade.startsWith("A")) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                            ) {
                                Text(
                                    text = res.grade,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (res.grade.startsWith("A")) Color(0xFF2E7D32) else Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            if (canRecordMarks) {
                                DbusEditIconButton(
                                    onClick = { editingResult = res },
                                    contentDescription = "Edit Marks",
                                    modifier = Modifier.testTag("btn_edit_result_${res.id}")
                                )
                                DbusDeleteIconButton(
                                    onClick = { deletingResult = res },
                                    contentDescription = "Delete Marks",
                                    modifier = Modifier.testTag("btn_delete_result_${res.id}")
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (res.marksObtained.toFloat() / res.totalMarks.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Examination Schedules Header + Add Schedule button
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.EventNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrdu) "امتحانی ڈیٹ شیٹ" else "Exam Date Sheet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (canRecordMarks) {
                    DbusAddButton(
                        text = if (isUrdu) "نیا شیڈول" else "Add Schedule",
                        onClick = { showAddScheduleDialog = true },
                        modifier = Modifier.testTag("btn_add_exam_schedule")
                    )
                }
            }
        }

        if (examSchedules.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = if (isUrdu) "ابھی تک کوئی امتحانی شیڈول شامل نہیں کیا گیا۔"
                        else "No exam schedules yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(examSchedules, key = { it.id }) { schedule ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = canRecordMarks) { editingSchedule = schedule }
                    .testTag("exam_schedule_${schedule.id}"),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = schedule.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = "${schedule.date} • ${schedule.time}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(text = "Room: ${schedule.room}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "${schedule.totalMarks} Marks",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        if (canRecordMarks) {
                            DbusEditIconButton(
                                onClick = { editingSchedule = schedule },
                                contentDescription = "Edit Schedule",
                                modifier = Modifier.testTag("btn_edit_schedule_${schedule.id}")
                            )
                        }
                        if (isAdmin) {
                            DbusDeleteIconButton(
                                onClick = { deletingSchedule = schedule },
                                contentDescription = "Delete Schedule",
                                modifier = Modifier.testTag("btn_delete_schedule_${schedule.id}")
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

    // Official Downloadable Result Report Dialog
    if (showReportCardDialog && targetStudent != null) {
        val context = LocalContext.current
        val currentYr = remember { Calendar.getInstance().get(Calendar.YEAR) }
        val exportPdfLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/pdf")
        ) { uri ->
            if (uri != null) {
                try {
                    val doc = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = doc.startPage(pageInfo)
                    val canvas = page.canvas
                    val titlePaint = Paint().apply {
                        textSize = 18f
                        isFakeBoldText = true
                        color = android.graphics.Color.rgb(27, 58, 128)
                    }
                    val headerPaint = Paint().apply {
                        textSize = 13f
                        isFakeBoldText = true
                        color = android.graphics.Color.BLACK
                    }
                    val bodyPaint = Paint().apply {
                        textSize = 11f
                        color = android.graphics.Color.DKGRAY
                    }
                    var y = 52f
                    canvas.drawText("DBUS EDUCATIONAL NETWORK", 40f, y, titlePaint)
                    y += 20f
                    canvas.drawText("EXAMINATION RESULT CARD $currentYr-${currentYr + 1}", 40f, y, headerPaint)
                    y += 28f
                    canvas.drawText("Student: ${targetStudent.fullName}", 40f, y, bodyPaint)
                    y += 18f
                    canvas.drawText("Father Name: ${targetStudent.fatherName}", 40f, y, bodyPaint)
                    y += 18f
                    canvas.drawText("Permanent Student ID: ${targetStudent.permanentStudentId}", 40f, y, bodyPaint)
                    y += 18f
                    canvas.drawText("Grade & Section: ${targetStudent.grade} - ${targetStudent.section}", 40f, y, bodyPaint)
                    y += 26f
                    canvas.drawText("Subject Results:", 40f, y, headerPaint)
                    y += 20f
                    if (studentResults.isEmpty()) {
                        canvas.drawText("No subject marks recorded yet.", 40f, y, bodyPaint)
                        y += 18f
                    } else {
                        studentResults.forEach { r ->
                            canvas.drawText("${r.subject}: ${r.marksObtained}/${r.totalMarks} (${r.grade})", 40f, y, bodyPaint)
                            y += 18f
                            if (y > 780f) return@forEach
                        }
                    }
                    y += 14f
                    canvas.drawText(
                        "Total: $totalObtained / $totalMax   |   Grade: $overallGrade (${String.format(Locale.US, "%.1f", percentage)}%)",
                        40f,
                        y,
                        headerPaint
                    )
                    doc.finishPage(page)
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        doc.writeTo(out)
                    }
                    doc.close()
                    Toast.makeText(
                        context,
                        if (isUrdu) "رپورٹ کارڈ پی ڈی ایف محفوظ ہو گیا" else "Report card PDF saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    showReportCardDialog = false
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        e.message ?: "Failed to export PDF",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showReportCardDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUrdu) "سرکاری نتیجہ نامہ (Report Card)" else "Official DBUS Academic Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showReportCardDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "DBUS EDUCATIONAL NETWORK", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Text(text = "Official Academic Performance Statement", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "EXAMINATION RESULT CARD $currentYr-${currentYr + 1}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Text(text = "Student Name: ${targetStudent.fullName}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(text = "Father Name: ${targetStudent.fatherName}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "Permanent Student ID: ${targetStudent.permanentStudentId}", style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(text = "Grade & Section: ${targetStudent.grade} • ${targetStudent.section}", style = MaterialTheme.typography.bodySmall)

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        studentResults.forEach { r ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = r.subject, style = MaterialTheme.typography.labelSmall)
                                Text(text = "${r.marksObtained}/${r.totalMarks} (${r.grade})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total: $totalObtained / $totalMax", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(
                                text = "Grade: $overallGrade (${String.format(Locale.US, "%.1f", percentage)}%)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        exportPdfLauncher.launch("DBUS_ReportCard_${targetStudent.permanentStudentId}.pdf")
                    },
                    modifier = Modifier.testTag("btn_export_report_pdf")
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isUrdu) "پی ڈی ایف ایکسپورٹ" else "Export PDF Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportCardDialog = false }) {
                    Text(if (isUrdu) "بند کریں" else "Close")
                }
            }
        )
    }

    // Record / Edit Marks Dialog (Teachers & Admin)
    if ((showRecordMarksDialog || editingResult != null) && targetStudent != null) {
        val isEditing = editingResult != null
        var subjectName by remember(editingResult) { mutableStateOf(editingResult?.subject ?: "") }
        var marksText by remember(editingResult) { mutableStateOf(editingResult?.marksObtained?.toString() ?: "") }
        var remarksText by remember(editingResult) { mutableStateOf(editingResult?.remarks ?: "") }
        val parsedMarks = marksText.trim().toIntOrNull()

        AlertDialog(
            onDismissRequest = {
                showRecordMarksDialog = false
                editingResult = null
            },
            title = { Text(if (isEditing) (if (isUrdu) "نمبر میں ترمیم" else "Edit Subject Marks") else (if (isUrdu) "نمبر درج کریں" else "Record Subject Marks")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Student: ${targetStudent.fullName} (${targetStudent.permanentStudentId})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = subjectName,
                        onValueChange = { subjectName = it },
                        label = { Text("Subject Name") },
                        placeholder = { Text("e.g. Mathematics") },
                        modifier = Modifier.fillMaxWidth().testTag("input_exam_subject"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = marksText,
                        onValueChange = { marksText = it },
                        label = { Text("Marks Obtained (0 - 100)") },
                        placeholder = { Text("e.g. 85") },
                        modifier = Modifier.fillMaxWidth().testTag("input_exam_marks"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = remarksText,
                        onValueChange = { remarksText = it },
                        label = { Text("Teacher Remarks (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = subjectName.isNotBlank() && parsedMarks != null,
                    onClick = {
                        val m = (parsedMarks ?: 0).coerceIn(0, 100)
                        val grade = if (m >= 90) "A+" else if (m >= 80) "A" else if (m >= 70) "B" else if (m >= 60) "C" else "D"
                        val updatedResult = StudentExamResult(
                            id = editingResult?.id ?: ("res_" + UUID.randomUUID().toString().take(6)),
                            studentPermanentId = targetStudent.permanentStudentId,
                            examName = editingResult?.examName ?: "Assessment",
                            term = editingResult?.term ?: "Term 1",
                            subject = subjectName.trim(),
                            marksObtained = m,
                            totalMarks = 100,
                            grade = grade,
                            remarks = remarksText.trim(),
                            date = repository.todayDate()
                        )
                        repository.recordExamResult(updatedResult)
                        showRecordMarksDialog = false
                        editingResult = null
                    },
                    modifier = Modifier.testTag("btn_save_exam_marks")
                ) {
                    Text(if (isUrdu) "محفوظ کریں" else "Save Marks")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRecordMarksDialog = false
                    editingResult = null
                }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Delete Result Confirmation
    deletingResult?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingResult = null },
            title = { Text(if (isUrdu) "نتیجہ حذف کریں؟" else "Delete Exam Result?") },
            text = { Text("Remove ${target.subject} (${target.marksObtained}/${target.totalMarks})?") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteExamResult(target.studentPermanentId, target.id, currentUser)
                        deletingResult = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isUrdu) "حذف کریں" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingResult = null }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Add / Edit Exam Schedule Dialog
    if (showAddScheduleDialog || editingSchedule != null) {
        val isEditing = editingSchedule != null
        var examName by remember(editingSchedule) { mutableStateOf(editingSchedule?.examName ?: "") }
        var subject by remember(editingSchedule) { mutableStateOf(editingSchedule?.subject ?: "") }
        var date by remember(editingSchedule) { mutableStateOf(editingSchedule?.date ?: repository.todayDate()) }
        var time by remember(editingSchedule) { mutableStateOf(editingSchedule?.time ?: "") }
        var room by remember(editingSchedule) { mutableStateOf(editingSchedule?.room ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddScheduleDialog = false
                editingSchedule = null
            },
            title = {
                Text(
                    if (isEditing) (if (isUrdu) "شیڈول میں ترمیم" else "Edit Exam Schedule")
                    else (if (isUrdu) "نیا امتحانی شیڈول" else "Add Exam Schedule")
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = examName, onValueChange = { examName = it }, label = { Text("Exam Name") }, placeholder = { Text("e.g. Mid-Term Examination") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("input_schedule_subject"))
                    OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("Date (YYYY-MM-DD)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = time, onValueChange = { time = it }, label = { Text("Time") }, placeholder = { Text("e.g. 09:00 AM - 12:00 PM") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room / Hall") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    enabled = subject.isNotBlank() && date.isNotBlank(),
                    onClick = {
                        val item = ExamScheduleItem(
                            id = editingSchedule?.id ?: ("sch_" + UUID.randomUUID().toString().take(6)),
                            examName = examName.trim().ifBlank { "Examination" },
                            subject = subject.trim(),
                            date = date.trim(),
                            time = time.trim(),
                            room = room.trim(),
                            totalMarks = editingSchedule?.totalMarks ?: 100,
                            passingMarks = editingSchedule?.passingMarks ?: 40
                        )
                        if (isEditing) repository.updateExamSchedule(item, currentUser)
                        else repository.addExamSchedule(item, currentUser)
                        showAddScheduleDialog = false
                        editingSchedule = null
                    },
                    modifier = Modifier.testTag("btn_save_exam_schedule")
                ) {
                    Text(if (isUrdu) "محفوظ کریں" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddScheduleDialog = false
                    editingSchedule = null
                }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Delete Schedule Confirmation
    deletingSchedule?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingSchedule = null },
            title = { Text(if (isUrdu) "شیڈول حذف کریں؟" else "Delete Exam Schedule?") },
            text = { Text("Remove ${target.subject} on ${target.date}?") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteExamSchedule(target.id, currentUser)
                        deletingSchedule = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isUrdu) "حذف کریں" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSchedule = null }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }
}

/**
 * Requirement 9: Parent-Teacher Communication
 * - Announcements, parent comments, questions, teacher responses.
 */
@Composable
fun CommunicationTabContent(
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val announcements by repository.parentAnnouncements.collectAsState()

    var showNewAnnouncementDialog by remember { mutableStateOf(false) }
    var activeCommentingAnnouncementId by remember { mutableStateOf<String?>(null) }
    var commentInput by remember { mutableStateOf("") }

    val canPostAnnouncement = currentUser?.role == UserRole.Teacher ||
        currentUser?.role == UserRole.Administrator ||
        currentUser?.role == UserRole.SuperAdmin

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUrdu) "اسکول اعلانات و والدین رابطہ" else "Parent-Teacher Communication",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isUrdu) "سرکاری اعلانات، سوالات اور اساتذہ کے جوابات" else "Announcements, parent questions & teacher replies",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                if (canPostAnnouncement) {
                    DbusAddButton(
                        text = if (isUrdu) "نیا اعلان" else "Announce",
                        onClick = { showNewAnnouncementDialog = true },
                        modifier = Modifier.testTag("btn_new_announcement")
                    )
                }
            }
        }

        if (announcements.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (isUrdu) "ابھی تک کوئی اعلان شائع نہیں کیا گیا۔" else "No announcements yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        items(announcements, key = { it.id }) { ann ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("announcement_${ann.id}"),
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
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = if (isUrdu) ann.urduTitle else ann.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Posted by ${ann.authorName} (${ann.authorRole.englishName}) • ${ann.date}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = ann.targetGrade,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isUrdu) ann.urduMessage else ann.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = if (isUrdu) "والدین کے سوالات و اساتذہ کے جوابات (${ann.comments.size}):" else "Parent Questions & Teacher Replies (${ann.comments.size}):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (ann.comments.isEmpty()) {
                        Text(
                            text = if (isUrdu) "اس اعلان پر ابھی کوئی سوال یا تبصرہ نہیں کیا گیا۔" else "No comments on this announcement yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        ann.comments.forEach { comment ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (comment.authorRole == UserRole.Teacher || comment.authorRole == UserRole.Administrator || comment.authorRole == UserRole.SuperAdmin)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${comment.authorName} (${comment.authorRole.englishName})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(text = comment.timestamp, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = comment.text, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = if (activeCommentingAnnouncementId == ann.id) commentInput else "",
                            onValueChange = {
                                activeCommentingAnnouncementId = ann.id
                                commentInput = it
                            },
                            placeholder = {
                                Text(
                                    text = if (isUrdu) "سوال یا تبصرہ لکھیں..." else "Ask a question or write a reply...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_comment_${ann.id}"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (commentInput.isNotBlank() && currentUser != null) {
                                    val newComment = AnnouncementComment(
                                        id = "c_" + UUID.randomUUID().toString().take(6),
                                        authorName = currentUser.name,
                                        authorRole = currentUser.role,
                                        text = commentInput.trim(),
                                        timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Calendar.getInstance().time)
                                    )
                                    repository.addAnnouncementComment(ann.id, newComment)
                                    commentInput = ""
                                    activeCommentingAnnouncementId = null
                                }
                            },
                            modifier = Modifier.testTag("btn_send_comment_${ann.id}")
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // New Announcement Dialog
    if (showNewAnnouncementDialog) {
        var titleInput by remember { mutableStateOf("") }
        var messageInput by remember { mutableStateOf("") }
        var targetGradeInput by remember { mutableStateOf("All Grades") }

        AlertDialog(
            onDismissRequest = { showNewAnnouncementDialog = false },
            title = { Text(if (isUrdu) "نیا اسکول اعلان جاری کریں" else "Broadcast School Announcement") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Title") },
                        placeholder = { Text("e.g. Winter Break Schedule") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetGradeInput,
                        onValueChange = { targetGradeInput = it },
                        label = { Text("Target Audience / Grade") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text("Announcement Message") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAnn = ParentTeacherAnnouncement(
                            id = "ann_" + UUID.randomUUID().toString().take(6),
                            title = titleInput.ifBlank { "School Notice" },
                            urduTitle = titleInput.ifBlank { "اسکول نوٹس" },
                            message = messageInput.ifBlank { "Notice for students and parents." },
                            urduMessage = messageInput.ifBlank { "طلباء اور والدین کیلئے نوٹس۔" },
                            authorName = currentUser?.name ?: "Principal Office",
                            authorRole = currentUser?.role ?: UserRole.Administrator,
                            targetGrade = targetGradeInput,
                            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time),
                            comments = emptyList()
                        )
                        repository.addAnnouncement(newAnn)
                        showNewAnnouncementDialog = false
                    }
                ) {
                    Text("Broadcast")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewAnnouncementDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
