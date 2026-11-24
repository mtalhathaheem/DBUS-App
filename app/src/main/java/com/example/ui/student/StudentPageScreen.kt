package com.example.ui.student

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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SchoolRepository
import com.example.model.AppLanguage
import com.example.model.CoreSkill
import com.example.model.SkillAssessment
import com.example.model.SkillLevel
import com.example.model.StudentRecord
import com.example.model.UserProfile
import com.example.model.UserRole
import com.example.ui.components.DbusDeleteIconButton
import com.example.ui.components.DbusEditIconButton
import com.example.ui.components.StudentApprovalBadge
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Checkbox
import com.example.model.ClassActivityMedia
import com.example.model.MediaType
import com.example.ui.media.AddClassActivityMediaDialog
import com.example.ui.media.ClassActivityMediaGallery
import com.example.ui.media.InsertPhotoButton
import com.example.ui.media.StudentAssignmentsSection
import com.example.ui.media.StudentAvatar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentPageScreen(
    repository: SchoolRepository,
    studentPermanentId: String,
    currentUser: UserProfile?,
    language: AppLanguage,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    val isUrdu = language == AppLanguage.URDU
    val studentsList by repository.studentsList.collectAsState()
    val student = repository.getStudentByPermanentId(studentPermanentId)

    // Skill history & assessments (Requirement 3 & 6)
    val allAssessments by repository.skillAssessments.collectAsState()
    val history = allAssessments[studentPermanentId] ?: emptyList()
    val latestAssessment = history.firstOrNull()

    // Activity media and assignments flows (Requirement 2 & 3)
    val allMedia by repository.classActivityMedia.collectAsState()
    val allAssignments by repository.studentAssignments.collectAsState()
    val studentMedia = repository.getMediaForStudent(studentPermanentId, currentUser)
    val studentAssignments = repository.getAssignmentsForStudent(studentPermanentId, currentUser)

    // Dialog States
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showSkillEvaluateDialog by remember { mutableStateOf(false) }
    var editingAssessment by remember { mutableStateOf<SkillAssessment?>(null) }
    var deletingAssessmentId by remember { mutableStateOf<String?>(null) }
    var addMediaDialogType by remember { mutableStateOf<MediaType?>(null) }
    var selectedSkillForDetail by remember { mutableStateOf<CoreSkill?>(null) }

    if (student == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isUrdu) "طالب علم کا ریکارڈ نہیں ملا یا آرکائیو ہو چکا ہے" else "Student record not found or has been archived.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBack) {
                    Text(if (isUrdu) "واپس جائیں" else "Go Back")
                }
            }
        }
        return
    }

    val isAuthorized = repository.canUserViewStudent(currentUser, student)
    if (!isAuthorized) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .testTag("rls_access_denied_screen"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Access Denied",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (isUrdu) "رسائی ممنوع ہے (Row Level Security)" else "Access Blocked (Row Level Security)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isUrdu)
                        "رو لیول سیکیورٹی (RLS) کے تحت آپ کو دوسرے طالب علم کا ڈیٹا دیکھنے کی اجازت نہیں ہے۔ صرف متعلقہ طالب علم، والدین، تفویض کردہ استاد یا منتظم اس ریکارڈ تک رسائی حاصل کر سکتے ہیں۔"
                    else
                        "Under Row Level Security (RLS), access to another student's profile is strictly prohibited. Only the assigned teacher, linked parent, the student themselves, or an administrator can access this record.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_rls_go_back")
                ) {
                    Text(if (isUrdu) "واپس جائیں" else "Go Back")
                }
            }
        }
        return
    }

    val canEdit = repository.canUserEditStudent(currentUser, student)
    val canUpdatePhoto = repository.canUserUpdateStudentPhoto(currentUser, student)
    val canDelete = repository.canUserDeleteStudent(currentUser) // Admin only
    val canEditSkills = repository.canUserEditSkills(currentUser, student)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isUrdu) student.urduName else student.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "ID: ${student.permanentStudentId}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_student_page_back")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Edit Button (Requirement 2: name, father's name, photo, class, section)
                    if (canEdit) {
                        DbusEditIconButton(
                            onClick = { showEditDialog = true },
                            contentDescription = "Edit Student",
                            modifier = Modifier.testTag("btn_edit_student")
                        )
                    }

                    // Delete Button: Administrator only (Requirement 2)
                    if (canDelete) {
                        DbusDeleteIconButton(
                            onClick = { showDeleteConfirmDialog = true },
                            contentDescription = "Delete Student",
                            modifier = Modifier.testTag("btn_delete_student")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
                .testTag("student_page_lazy_column"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Student Header Card (photo, name, Student ID, father's name, class, section)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_profile_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Student Photo / Avatar (Requirement 1: small round image or first letter)
                            StudentAvatar(student = student, size = 64.dp)

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isUrdu) student.urduName else student.fullName,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    StudentApprovalBadge(status = student.approvalStatus, language = language)
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "${student.grade} • ${student.section}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = if (isUrdu) "والد کا نام: ${student.fatherName}"
                                    else "Father's Name: ${student.fatherName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Permanent Student ID Ribbon
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
                                    text = student.permanentStudentId,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Sensitive B-Form (Administrator ONLY - last 4 digits)
                        val maskedBForm = repository.getMaskedBForm(student.bFormNumber, currentUser?.role ?: UserRole.Parent)
                        if (maskedBForm != null && (currentUser?.role == UserRole.Administrator || currentUser?.role == UserRole.SuperAdmin)) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "NADRA B-Form (Admin Masked):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = maskedBForm,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2. The 10 Core Skills Section Header (Requirement 3 & 4)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isUrdu) "دس بنیادی تعلیمی و تربیتی صلاحیتیں (10 Core Skills)"
                            else "The 10 Core Skills",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isUrdu) "اسکور 1 تا 4: 1 مدد درکار، 2 بہتری کی جانب، 3 اچھا، 4 شاندار"
                            else "Scored 1-4: (1 Needs support, 2 Developing, 3 Good, 4 Excellent)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    // Button to record / edit skills assessment (Teachers of assigned student or Admin only)
                    if (canEditSkills) {
                        Button(
                            onClick = { showSkillEvaluateDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_evaluate_skills")
                        ) {
                            Icon(imageVector = Icons.Default.RateReview, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isUrdu) "صلاحیتیں درج کریں" else "Evaluate",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // 3. The 10 Core Skills Cards (reading, writing, listening, speaking, presentation, confidence, communication, cooperation, creativity, pressure handling)
            val canViewPrivateNotes = currentUser?.role == UserRole.Teacher ||
                currentUser?.role == UserRole.Administrator ||
                currentUser?.role == UserRole.SuperAdmin

            items(CoreSkill.values()) { skill ->
                val score = latestAssessment?.scores?.get(skill)
                val sharedNote = latestAssessment?.sharedNotes?.get(skill)
                val privateNote = latestAssessment?.privateNotes?.get(skill)
                val level = score?.let { SkillLevel.fromScore(it) }
                val barColor = level?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.surfaceVariant

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedSkillForDetail = skill }
                        .testTag("skill_card_${skill.name.lowercase()}"),
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
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = skill.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            // Score level badge (or Not assessed yet when no assessment exists)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (level != null) Color(level.colorHex).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (level != null) "${level.score}/4 • ${level.getDisplayName(isUrdu)}"
                                        else if (isUrdu) "ابھی درج نہیں" else "Not assessed yet",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (level != null) Color(level.colorHex)
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        // Score Visualizer: 4 segmented progress bars
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
                                            if (score != null && i <= score) barColor
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                            }
                        }

                        // Shared Note: visible to Parent, Student, Teacher, Administrator
                        if (!sharedNote.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = if (isUrdu) "📝 مشترکہ نوٹ (والدین اور طالب علم):" else "📝 Shared Note (Parent & Student):",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = sharedNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // Private Note: Teacher and Administrator ONLY!
                        // Parents and students must NEVER see the private note.
                        if (canViewPrivateNotes && !privateNote.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isUrdu) "🔒 خفیہ نوٹ (صرف استاد اور منتظم کیلئے):" else "🔒 Private Note (Faculty & Admin Only):",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Text(
                                        text = privateNote,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isUrdu) "تفصیلی شواہد، اسکور اور تاریخ دیکھیں ←" else "View detailed evidence & history →",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 4. Skills Assessment History (Requirement 3: Save with the date and show the history)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isUrdu) "صلاحیتوں کی جانچ کی تاریخ (${history.size})"
                        else "Skill Assessment History (${history.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (history.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = if (isUrdu) "ابھی تک کوئی صلاحیت کی جانچ ریکارڈ نہیں کی گئی۔"
                            else "No skill assessments recorded yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(history) { record ->
                    HistoryAssessmentCard(
                        assessment = record,
                        canViewPrivateNotes = canViewPrivateNotes,
                        canEdit = canEditSkills,
                        language = language,
                        onEdit = { editingAssessment = record },
                        onDelete = { deletingAssessmentId = record.id }
                    )
                }
            }

            // 5. Class Activity Media Gallery (Requirement 2 & 5)
            // Tap to open full screen. Videos play inside the app. Private to student/parents/assigned teachers/admin.
            item {
                Spacer(modifier = Modifier.height(10.dp))
                ClassActivityMediaGallery(
                    mediaList = studentMedia,
                    currentUser = currentUser,
                    language = language,
                    onAddMediaClick = { type -> addMediaDialogType = type },
                    onDeleteMedia = { mediaId ->
                        if (currentUser != null) {
                            repository.deleteClassActivityMedia(mediaId, currentUser)
                        }
                    }
                )
            }

            // 6. Student Assignments Section (Requirement 3: Photo, PDF, Audio, Video upload)
            item {
                Spacer(modifier = Modifier.height(10.dp))
                StudentAssignmentsSection(
                    assignments = studentAssignments,
                    student = student,
                    currentUser = currentUser,
                    language = language,
                    repository = repository,
                    onSubmitAssignment = { assignmentId, mediaType, fileUri, fileName, sizeBytes ->
                        if (currentUser != null) {
                            repository.submitAssignment(assignmentId, mediaType, fileUri, fileName, sizeBytes, currentUser)
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    // Edit Student Dialog (Requirement 1 & 2: name, father's name, photo, class, section, parent consent)
    if (showEditDialog) {
        var editName by remember { mutableStateOf(student.fullName) }
        var editFatherName by remember { mutableStateOf(student.fatherName) }
        var editGrade by remember { mutableStateOf(student.grade) }
        var editSection by remember { mutableStateOf(student.section) }
        var editAvatarColor by remember { mutableStateOf(student.avatarColorHex) }
        var editPhotoUri by remember { mutableStateOf(student.photoUri) }
        var editConsent by remember { mutableStateOf(student.parentMediaConsentGiven) }

        val paletteOptions = listOf(0xFF1B3B6F, 0xFF01411C, 0xFF1D4ED8, 0xFF006D77, 0xFF7F1D1D, 0xFFB45309)

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(if (isUrdu) "طالب علم کی تفصیلات ایڈٹ کریں" else "Edit Student Details")
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Photo & Avatar section
                    item {
                        Text(
                            text = if (isUrdu) "طالب علم کی تصویر (Student Photo):" else "Student Photo:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StudentAvatar(
                                student = student.copy(
                                    photoUri = editPhotoUri,
                                    avatarColorHex = editAvatarColor,
                                    fullName = editName.ifBlank { "Student" }
                                ),
                                size = 52.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                InsertPhotoButton(
                                    currentPhotoUri = editPhotoUri,
                                    language = language,
                                    onPhotoSelected = { newUri -> editPhotoUri = newUri }
                                )
                                if (!editPhotoUri.isNullOrBlank()) {
                                    TextButton(
                                        onClick = { editPhotoUri = null },
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Text(
                                            text = if (isUrdu) "تصویر ہٹائیں (Remove Photo)" else "Remove Photo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Background Color Palette
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
                            paletteOptions.forEach { colorHex ->
                                val isSel = editAvatarColor == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorHex))
                                        .clickable { editAvatarColor = colorHex },
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
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text(if (isUrdu) "طالب علم کا نام" else "Student Full Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = editFatherName,
                            onValueChange = { editFatherName = it },
                            label = { Text(if (isUrdu) "والد کا نام" else "Father's Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editGrade,
                                onValueChange = { editGrade = it },
                                label = { Text("Class") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editSection,
                                onValueChange = { editSection = it },
                                label = { Text("Section") },
                                modifier = Modifier.weight(1f),
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
                                    .clickable { editConsent = !editConsent }
                                    .padding(8.dp)
                            ) {
                                Checkbox(
                                    checked = editConsent,
                                    onCheckedChange = { editConsent = it }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (isUrdu) "والدین کی رضامندی (Media Consent)" else "Parental Media Consent",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isUrdu) "والدین کی طرف سے طالب علم کی تصویر اور سرگرمی کی ویڈیوز محفوظ کرنے کی اجازت ہے۔"
                                        else "Parent agrees to allow storing student photos and activity media.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.updateStudent(
                            permanentId = student.permanentStudentId,
                            name = editName,
                            fatherName = editFatherName,
                            grade = editGrade,
                            section = editSection,
                            avatarColorHex = editAvatarColor,
                            photoUri = editPhotoUri,
                            parentMediaConsentGiven = editConsent
                        )
                        showEditDialog = false
                    }
                ) {
                    Text(if (isUrdu) "محفوظ کریں" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog (Requirement 2: Ask "Are you sure?" first. Archive the record, do not erase it, and never reuse the Student ID.)
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isUrdu) "کیا آپ واقعی مطمئن ہیں؟" else "Are you sure?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isUrdu) "طالب علم ${student.fullName} کے ریکارڈ کو آرکائیو کر دیا جائے گا۔"
                        else "Are you sure you want to delete ${student.fullName}?",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isUrdu) "یہ ریکارڈ سسٹم سے مٹایا نہیں جائے گا بلکہ مستقل محفوظ (Archive) رہے گا۔ مستقل اسٹوڈنٹ آئی ڈی (${student.permanentStudentId}) کو کبھی دوبارہ استعمال نہیں کیا جائے گا۔"
                        else "Under DBUS integrity rules: The record will be archived, not erased. The Permanent Student ID (${student.permanentStudentId}) will never be reused.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.archiveStudent(student.permanentStudentId)
                        showDeleteConfirmDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isUrdu) "ہاں، آرکائیو کریں" else "Archive Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Evaluate Skills Dialog (Requirement 3: score 1 to 4 + optional shared and private notes)
    // Requirement 2: When a teacher records marks, show these buttons: Add photo, Add video, Add audio.
    if (showSkillEvaluateDialog) {
        EvaluateSkillsDialog(
            student = student,
            currentScores = latestAssessment?.scores ?: CoreSkill.values().associateWith { 3 },
            currentSharedNotes = latestAssessment?.sharedNotes ?: emptyMap(),
            currentPrivateNotes = latestAssessment?.privateNotes ?: emptyMap(),
            language = language,
            onDismiss = { showSkillEvaluateDialog = false },
            onAddMedia = { type -> addMediaDialogType = type },
            onSave = { scores, sharedNotes, privateNotes ->
                repository.saveSkillAssessment(
                    studentPermanentId = student.permanentStudentId,
                    teacherName = currentUser?.name ?: "Faculty Examiner",
                    scores = scores,
                    sharedNotes = sharedNotes,
                    privateNotes = privateNotes
                )
                showSkillEvaluateDialog = false
            }
        )
    }

    // Edit Skill Assessment Dialog (Requirement 4: add/edit/delete skill)
    editingAssessment?.let { targetAssessment ->
        EvaluateSkillsDialog(
            student = student,
            currentScores = targetAssessment.scores,
            currentSharedNotes = targetAssessment.sharedNotes,
            currentPrivateNotes = targetAssessment.privateNotes,
            language = language,
            onDismiss = { editingAssessment = null },
            onAddMedia = { type -> addMediaDialogType = type },
            onSave = { scores, sharedNotes, privateNotes ->
                if (currentUser != null) {
                    repository.editSkillAssessment(
                        assessmentId = targetAssessment.id,
                        studentPermanentId = student.permanentStudentId,
                        scores = scores,
                        sharedNotes = sharedNotes,
                        privateNotes = privateNotes,
                        editor = currentUser
                    )
                }
                editingAssessment = null
            }
        )
    }

    // Delete Skill Assessment Confirmation Dialog
    if (deletingAssessmentId != null && currentUser != null) {
        AlertDialog(
            onDismissRequest = { deletingAssessmentId = null },
            title = { Text(if (isUrdu) "جانچ کا ریکارڈ حذف کریں؟" else "Delete Skill Assessment?") },
            text = {
                Text(
                    if (isUrdu) "کیا آپ واقعی یہ جانچ کا ریکارڈ حذف کرنا چاہتے ہیں؟"
                    else "Are you sure you want to delete this skill assessment evaluation?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteSkillAssessment(
                            assessmentId = deletingAssessmentId!!,
                            studentPermanentId = student.permanentStudentId,
                            user = currentUser
                        )
                        deletingAssessmentId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_assessment")
                ) {
                    Text(if (isUrdu) "حذف کریں" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAssessmentId = null }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Add Class Activity Media Dialog (Requirement 2: Photo, Video, Audio)
    addMediaDialogType?.let { mediaType ->
        if (currentUser != null) {
            AddClassActivityMediaDialog(
                initialMediaType = mediaType,
                student = student,
                currentUser = currentUser,
                language = language,
                repository = repository,
                onDismiss = { addMediaDialogType = null },
                onSaveMedia = { mediaItem ->
                    repository.addClassActivityMedia(mediaItem, currentUser)
                    addMediaDialogType = null
                }
            )
        }
    }

    // Requirement 7: Dedicated Core Skills Detail Page
    selectedSkillForDetail?.let { skill ->
        SkillDetailPageDialog(
            skill = skill,
            student = student,
            repository = repository,
            currentUser = currentUser,
            language = language,
            onDismiss = { selectedSkillForDetail = null }
        )
    }
}

/**
 * Dialog to score each of the 10 core skills (1 to 4) with two optional notes:
 * - Shared note: parent and student can read
 * - Private note: teacher and administrator only
 * Parents and students must never see the private note.
 *
 * Requirement 2:
 * When a teacher records marks, show these buttons: Add photo, Add video, Add audio.
 */
@Composable
fun EvaluateSkillsDialog(
    student: StudentRecord,
    currentScores: Map<CoreSkill, Int>,
    currentSharedNotes: Map<CoreSkill, String>,
    currentPrivateNotes: Map<CoreSkill, String>,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onAddMedia: (MediaType) -> Unit,
    onSave: (Map<CoreSkill, Int>, Map<CoreSkill, String>, Map<CoreSkill, String>) -> Unit
) {
    val isUrdu = language == AppLanguage.URDU
    val scoresState = remember { mutableStateOf(currentScores.toMutableMap()) }
    val sharedNotesState = remember { mutableStateOf(currentSharedNotes.toMutableMap()) }
    val privateNotesState = remember { mutableStateOf(currentPrivateNotes.toMutableMap()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (isUrdu) "دس بنیادی صلاحیتوں کی جانچ" else "Evaluate 10 Core Skills",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${student.fullName} • ${student.permanentStudentId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column {
                // Requirement 2: "When a teacher records marks, show these buttons: Add photo, Add video, Add audio."
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = if (isUrdu) "کلاس سرگرمی میڈیا منسلک کریں:" else "Attach Class Activity Media:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onAddMedia(MediaType.PHOTO) },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (isUrdu) "تصویر" else "Photo", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onAddMedia(MediaType.VIDEO) },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (isUrdu) "ویڈیو" else "Video", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { onAddMedia(MediaType.AUDIO) },
                                modifier = Modifier.weight(1f),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(if (isUrdu) "آڈیو" else "Audio", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(CoreSkill.values()) { skill ->
                        val currentScore = scoresState.value[skill] ?: 3
                        val currentShared = sharedNotesState.value[skill] ?: ""
                        val currentPrivate = privateNotesState.value[skill] ?: ""

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = skill.getDisplayName(isUrdu),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            // Score selector buttons 1, 2, 3, 4
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (score in 1..4) {
                                    val level = SkillLevel.fromScore(score)
                                    val isSelected = currentScore == score
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable {
                                                scoresState.value = scoresState.value.toMutableMap().apply { put(skill, score) }
                                            },
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isSelected) Color(level.colorHex) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (isSelected) Color(level.colorHex) else MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "$score",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = when (score) {
                                                    1 -> if (isUrdu) "مدد" else "Support"
                                                    2 -> if (isUrdu) "بہتر" else "Dev"
                                                    3 -> if (isUrdu) "اچھا" else "Good"
                                                    4 -> if (isUrdu) "شاندار" else "Excel"
                                                    else -> ""
                                                },
                                                fontSize = 9.sp,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // 1. Shared Note Input (Visible to Parent & Student)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = currentShared,
                                onValueChange = { text ->
                                    sharedNotesState.value = sharedNotesState.value.toMutableMap().apply { put(skill, text) }
                                },
                                label = { Text(if (isUrdu) "مشترکہ نوٹ (والدین اور طالب علم کیلئے)" else "Shared Note (Parent & Student)") },
                                placeholder = { Text(if (isUrdu) "والدین کے پڑھنے کیلئے حوصلہ افزائی کا نوٹ" else "Note for parent & student") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // 2. Private Note Input (Teacher & Administrator ONLY)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = currentPrivate,
                                onValueChange = { text ->
                                    privateNotesState.value = privateNotesState.value.toMutableMap().apply { put(skill, text) }
                                },
                                label = { Text(if (isUrdu) "🔒 خفیہ نوٹ (صرف استاد اور ایڈمن)" else "🔒 Private Note (Teacher & Admin Only)") },
                                placeholder = { Text(if (isUrdu) "اندرونی تعلیمی مشاورت و تجزیہ" else "Internal faculty observation") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }
        }
    },
        confirmButton = {
            Button(
                onClick = { onSave(scoresState.value, sharedNotesState.value, privateNotesState.value) }
            ) {
                Text(if (isUrdu) "تاریخ کے ساتھ محفوظ کریں" else "Save Assessment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isUrdu) "منسوخ" else "Cancel")
            }
        }
    )
}

@Composable
fun HistoryAssessmentCard(
    assessment: SkillAssessment,
    canViewPrivateNotes: Boolean,
    language: AppLanguage,
    canEdit: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val isUrdu = language == AppLanguage.URDU
    var expanded by remember { mutableStateOf(false) }

    val avgScore = if (assessment.scores.isEmpty()) 0.0 else assessment.scores.values.average()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
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
                Column {
                    Text(
                        text = "📅 ${assessment.date}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isUrdu) "جانچ کار: ${assessment.assessedByTeacherName}"
                        else "Evaluator: ${assessment.assessedByTeacherName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = String.format(Locale.US, "Avg: %.1f / 4", avgScore),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (canEdit && onEdit != null) {
                        DbusEditIconButton(
                            onClick = onEdit,
                            contentDescription = "Edit Assessment",
                            modifier = Modifier.testTag("btn_edit_assessment_${assessment.id}")
                        )
                    }

                    if (canEdit && onDelete != null) {
                        DbusDeleteIconButton(
                            onClick = onDelete,
                            contentDescription = "Delete Assessment",
                            modifier = Modifier.testTag("btn_delete_assessment_${assessment.id}")
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    CoreSkill.values().forEach { skill ->
                        val s = assessment.scores[skill]
                        val sharedNote = assessment.sharedNotes[skill]
                        val privateNote = assessment.privateNotes[skill]

                        if (s != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = skill.getDisplayName(isUrdu), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "${SkillLevel.fromScore(s).getDisplayName(isUrdu)} ($s/4)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(SkillLevel.fromScore(s).colorHex)
                                )
                            }
                        }

                        // Shared Note: visible to all
                        if (!sharedNote.isNullOrBlank()) {
                            Text(
                                text = "📝 $sharedNote",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)
                            )
                        }

                        // Private Note: Teacher and Administrator ONLY!
                        if (canViewPrivateNotes && !privateNote.isNullOrBlank()) {
                            Text(
                                text = "🔒 Private: $privateNote",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
