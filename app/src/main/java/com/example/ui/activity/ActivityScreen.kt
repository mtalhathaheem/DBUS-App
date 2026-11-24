package com.example.ui.activity

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SchoolRepository
import com.example.model.*
import com.example.ui.components.DbusAddButton
import com.example.ui.components.DbusDeleteIconButton
import com.example.ui.components.DbusEditIconButton
import com.example.ui.media.AddClassActivityMediaDialog
import com.example.ui.media.ClassActivityMediaGallery
import com.example.ui.media.StudentAssignmentsSection
import com.example.ui.tools.SchoolCalculatorTab
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    onOpenStudentPage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Subjects, 1: Class Activity, 2: Assignments, 3: Examinations, 4: Communication
    var showCalculator by remember { mutableStateOf(false) }

    if (showCalculator) {
        BackHandler { showCalculator = false }
        Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showCalculator = false },
                            modifier = Modifier.testTag("btn_activity_calc_back")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = if (isUrdu) "واپس" else "Back"
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUrdu) "DBUS اسکول کیلکولیٹر" else "DBUS School Calculator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                SchoolCalculatorTab(
                    language = language,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Compact Activity Header with dedicated Calculator button (ONLY inside Activity)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUrdu) "DBUS تعلیمی سرگرمیاں" else "DBUS Academics & Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isUrdu) "مضامین، میڈیا، اسائنمنٹس اور امتحانات"
                            else "Subjects, Media, Assignments & Exams",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    FilledTonalIconButton(
                        onClick = { showCalculator = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_activity_calculator"),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = if (isUrdu) "کیلکولیٹر" else "Calculator"
                        )
                    }
                }
            }

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = if (isUrdu) "مضامین" else "Subjects",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    icon = { Icon(Icons.Default.Book, contentDescription = null) },
                    modifier = Modifier.testTag("tab_activity_subjects")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = if (isUrdu) "کلاس سرگرمیاں" else "Class Activity",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    icon = { Icon(Icons.Default.LocalActivity, contentDescription = null) },
                    modifier = Modifier.testTag("tab_activity_media")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            text = if (isUrdu) "اسائنمنٹس" else "Assignments",
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                    modifier = Modifier.testTag("tab_activity_assignments")
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = {
                        Text(
                            text = if (isUrdu) "امتحانات" else "Examinations",
                            fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    icon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null) },
                    modifier = Modifier.testTag("tab_activity_examinations")
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = {
                        Text(
                            text = if (isUrdu) "اعلانات" else "Communication",
                            fontWeight = if (selectedTab == 4) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    icon = { Icon(Icons.Default.Campaign, contentDescription = null) },
                    modifier = Modifier.testTag("tab_activity_communication")
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    0 -> SubjectsTabContent(repository, currentUser, language)
                    1 -> ClassActivityTabContent(repository, currentUser, language, onOpenStudentPage)
                    2 -> AssignmentsTabContent(repository, currentUser, language, onOpenStudentPage)
                    3 -> ExaminationsTabContent(repository, currentUser, language)
                    4 -> CommunicationTabContent(repository, currentUser, language)
                }
            }
        }
    }
}

@Composable
fun SubjectsTabContent(
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val subjects by repository.subjectsList.collectAsState()
    val isAdmin = currentUser?.role == UserRole.Administrator || currentUser?.role == UserRole.SuperAdmin

    var showAddDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectItem?>(null) }
    var viewingSubject by remember { mutableStateOf<SubjectItem?>(null) }
    var deletingSubjectId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(subjects, searchQuery) {
        if (searchQuery.isBlank()) subjects
        else {
            val q = searchQuery.trim().lowercase()
            subjects.filter {
                it.name.lowercase().contains(q) ||
                it.urduName.lowercase().contains(q) ||
                it.code.lowercase().contains(q) ||
                it.teacherName.lowercase().contains(q)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUrdu) "اسکول کا نصاب و مضامین" else "Curriculum & Subjects",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isUrdu) "کل مضامین: ${subjects.size}" else "Total Subjects: ${subjects.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    if (isAdmin) {
                        DbusAddButton(
                            text = if (isUrdu) "نیا مضمون" else "Add Subject",
                            onClick = { showAddDialog = true },
                            modifier = Modifier.testTag("btn_add_subject")
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isUrdu) "مضمون کا نام یا کوڈ تلاش کریں..." else "Search subjects by name or code...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_subjects_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (filtered.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (isUrdu) "ابھی تک کوئی مضمون شامل نہیں کیا گیا۔" else "No subjects yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }

            items(filtered, key = { it.id }) { subject ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewingSubject = subject }
                        .testTag("subject_card_${subject.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = subject.name.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isUrdu) subject.urduName else subject.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = subject.code,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (isUrdu) "استاد: ${subject.teacherName} • ${subject.grade}"
                                else "Teacher: ${subject.teacherName} • ${subject.grade}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (subject.description.isNotBlank()) {
                                Text(
                                    text = subject.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isAdmin) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DbusEditIconButton(
                                    onClick = { editingSubject = subject },
                                    contentDescription = if (isUrdu) "ترمیم" else "Edit Subject",
                                    modifier = Modifier.testTag("btn_edit_subject_${subject.id}")
                                )
                                DbusDeleteIconButton(
                                    onClick = { deletingSubjectId = subject.id },
                                    contentDescription = if (isUrdu) "حذف" else "Delete Subject",
                                    modifier = Modifier.testTag("btn_delete_subject_${subject.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Subject Detail Dialog (Every card clickable)
    viewingSubject?.let { subject ->
        AlertDialog(
            onDismissRequest = { viewingSubject = null },
            title = {
                Text(
                    text = if (isUrdu) subject.urduName else subject.name,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Code: ${subject.code}", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(text = if (isUrdu) "جماعت: ${subject.grade}" else "Grade: ${subject.grade}")
                    Text(text = if (isUrdu) "استاد: ${subject.teacherName}" else "Assigned Teacher: ${subject.teacherName}")
                    Text(text = if (isUrdu) "ہفتہ وار گھنٹے: ${subject.weeklyHours}" else "Weekly Hours: ${subject.weeklyHours}")
                    if (subject.description.isNotBlank()) {
                        HorizontalDivider()
                        Text(text = subject.description, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isAdmin) {
                        TextButton(onClick = {
                            deletingSubjectId = subject.id
                            viewingSubject = null
                        }) {
                            Text(if (isUrdu) "حذف" else "Delete", color = MaterialTheme.colorScheme.error)
                        }
                        Button(onClick = {
                            editingSubject = subject
                            viewingSubject = null
                        }) {
                            Text(if (isUrdu) "ترمیم" else "Edit")
                        }
                    } else {
                        Button(onClick = { viewingSubject = null }) {
                            Text(if (isUrdu) "بند کریں" else "Close")
                        }
                    }
                }
            },
            dismissButton = {
                if (isAdmin) {
                    TextButton(onClick = { viewingSubject = null }) {
                        Text(if (isUrdu) "بند کریں" else "Close")
                    }
                }
            }
        )
    }

    // Add / Edit Dialog
    if (showAddDialog || editingSubject != null) {
        val isEditing = editingSubject != null
        var name by remember(editingSubject) { mutableStateOf(editingSubject?.name ?: "") }
        var urduName by remember(editingSubject) { mutableStateOf(editingSubject?.urduName ?: "") }
        var code by remember(editingSubject) { mutableStateOf(editingSubject?.code ?: "") }
        var grade by remember(editingSubject) { mutableStateOf(editingSubject?.grade ?: "") }
        var teacher by remember(editingSubject) { mutableStateOf(editingSubject?.teacherName ?: "") }
        var description by remember(editingSubject) { mutableStateOf(editingSubject?.description ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingSubject = null
            },
            title = {
                Text(
                    if (isEditing) (if (isUrdu) "مضمون میں ترمیم" else "Edit Subject")
                    else (if (isUrdu) "نیا مضمون شامل کریں" else "Add New Subject")
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Subject Name (English)") },
                        placeholder = { Text("e.g. Mathematics") },
                        modifier = Modifier.fillMaxWidth().testTag("input_subject_name"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = urduName,
                        onValueChange = { urduName = it },
                        label = { Text("Subject Name (Urdu)") },
                        modifier = Modifier.fillMaxWidth().testTag("input_subject_urdu"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("Subject Code") },
                        placeholder = { Text("e.g. MATH-101") },
                        modifier = Modifier.fillMaxWidth().testTag("input_subject_code"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it },
                        label = { Text("Grade") },
                        placeholder = { Text("e.g. Grade 9") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("Assigned Teacher") },
                        modifier = Modifier.fillMaxWidth().testTag("input_subject_teacher"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = name.isNotBlank(),
                    onClick = {
                        val sub = SubjectItem(
                            id = editingSubject?.id ?: ("sub_" + UUID.randomUUID().toString().take(6)),
                            name = name.trim(),
                            urduName = urduName.trim().ifBlank { name.trim() },
                            code = code.trim().ifBlank { "SUB" },
                            grade = grade.trim(),
                            teacherName = teacher.trim(),
                            weeklyHours = editingSubject?.weeklyHours ?: 4,
                            description = description.trim()
                        )
                        currentUser?.let { user ->
                            if (isEditing) repository.updateSubject(sub, user)
                            else repository.addSubject(sub, user)
                        }
                        showAddDialog = false
                        editingSubject = null
                    },
                    modifier = Modifier.testTag("btn_save_subject")
                ) {
                    Text(if (isUrdu) "محفوظ کریں" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    editingSubject = null
                }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Delete confirmation
    if (deletingSubjectId != null) {
        AlertDialog(
            onDismissRequest = { deletingSubjectId = null },
            title = { Text(if (isUrdu) "مضمون حذف کریں؟" else "Delete Subject?") },
            text = {
                Text(
                    if (isUrdu) "کیا آپ واقعی یہ مضمون حذف کرنا چاہتے ہیں؟"
                    else "Are you sure you want to delete this subject from curriculum?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentUser?.let { user ->
                            repository.deleteSubject(deletingSubjectId!!, user)
                        }
                        deletingSubjectId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_subject")
                ) {
                    Text(if (isUrdu) "حذف کریں" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSubjectId = null }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun ClassActivityTabContent(
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    onOpenStudentPage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val allMedia by repository.classActivityMedia.collectAsState()
    val students by repository.studentsList.collectAsState()
    val accessibleStudents = remember(students, currentUser) {
        repository.getAttendanceStudentsList(currentUser)
    }

    var addMediaDialogType by remember { mutableStateOf<MediaType?>(null) }
    var selectedStudentPermanentId by remember(accessibleStudents) {
        mutableStateOf(accessibleStudents.firstOrNull()?.permanentStudentId ?: "")
    }
    var editingMedia by remember { mutableStateOf<ClassActivityMedia?>(null) }
    var editCaptionText by remember { mutableStateOf("") }
    var deletingMediaId by remember { mutableStateOf<String?>(null) }

    val canAdd = currentUser?.role == UserRole.Administrator ||
        currentUser?.role == UserRole.SuperAdmin ||
        currentUser?.role == UserRole.Teacher

    // Filter media based on RLS access
    val visibleMedia = remember(allMedia, currentUser, students) {
        allMedia.filter { item ->
            repository.canUserAccessStorage(currentUser, item.studentPermanentId)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUrdu) "کلاس سرگرمیاں اور میڈیا" else "Class Activity & Media",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isUrdu) "تصاویر، ویڈیوز اور آڈیو ریکارڈنگز" else "Photos, Videos, and Audio Recordings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    if (canAdd) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { addMediaDialogType = MediaType.PHOTO },
                                modifier = Modifier.testTag("btn_act_add_photo"),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text(if (isUrdu) "+ تصویر" else "+ Photo", style = MaterialTheme.typography.labelSmall)
                            }
                            OutlinedButton(
                                onClick = { addMediaDialogType = MediaType.VIDEO },
                                modifier = Modifier.testTag("btn_act_add_video"),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Text(if (isUrdu) "+ ویڈیو" else "+ Video", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            if (canAdd && accessibleStudents.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isUrdu) "طالب علم منتخب کریں:" else "Target Student for New Upload:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(accessibleStudents, key = { it.permanentStudentId }) { st ->
                                val selected = st.permanentStudentId == selectedStudentPermanentId
                                FilterChip(
                                    selected = selected,
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
            }

            item {
                ClassActivityMediaGallery(
                    mediaList = visibleMedia,
                    currentUser = currentUser,
                    language = language,
                    onAddMediaClick = { type -> addMediaDialogType = type },
                    onEditMedia = { media ->
                        editingMedia = media
                        editCaptionText = media.caption
                    },
                    onDeleteMedia = { mediaId ->
                        deletingMediaId = mediaId
                    }
                )
            }
        }
    }

    // Add Media Dialog
    if (addMediaDialogType != null && currentUser != null) {
        val targetStudent = accessibleStudents.find { it.permanentStudentId == selectedStudentPermanentId }
            ?: accessibleStudents.firstOrNull()

        if (targetStudent == null) {
            AlertDialog(
                onDismissRequest = { addMediaDialogType = null },
                title = { Text(if (isUrdu) "کوئی طالب علم دستیاب نہیں" else "No Students Yet") },
                text = {
                    Text(
                        if (isUrdu) "میڈیا اپ لوڈ کرنے سے پہلے براہ کرم کسی طالب علم کو رجسٹر یا تفویض کریں۔"
                        else "No students available yet. Enroll or assign a student before uploading activity media."
                    )
                },
                confirmButton = {
                    Button(onClick = { addMediaDialogType = null }) {
                        Text(if (isUrdu) "ٹھیک ہے" else "OK")
                    }
                }
            )
        } else {
            AddClassActivityMediaDialog(
                initialMediaType = addMediaDialogType!!,
                student = targetStudent,
                currentUser = currentUser,
                language = language,
                repository = repository,
                onDismiss = { addMediaDialogType = null },
                onSaveMedia = { newMedia ->
                    repository.addClassActivityMedia(newMedia, currentUser)
                    addMediaDialogType = null
                }
            )
        }
    }

    // Edit Caption Dialog
    editingMedia?.let { target ->
        AlertDialog(
            onDismissRequest = { editingMedia = null },
            title = { Text(if (isUrdu) "کیپشن میں ترمیم" else "Edit Media Caption") },
            text = {
                OutlinedTextField(
                    value = editCaptionText,
                    onValueChange = { editCaptionText = it },
                    label = { Text(if (isUrdu) "تفصیل / کیپشن" else "Caption") },
                    modifier = Modifier.fillMaxWidth().testTag("input_edit_media_caption")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentUser?.let { user ->
                            repository.updateClassActivityMedia(target.id, editCaptionText.trim(), user)
                        }
                        editingMedia = null
                    },
                    modifier = Modifier.testTag("btn_save_media_caption")
                ) {
                    Text(if (isUrdu) "محفوظ کریں" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingMedia = null }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Delete Media Confirmation
    if (deletingMediaId != null && currentUser != null) {
        AlertDialog(
            onDismissRequest = { deletingMediaId = null },
            title = { Text(if (isUrdu) "میڈیا فائل حذف کریں؟" else "Delete Media File?") },
            text = { Text(if (isUrdu) "کیا آپ واقعی یہ فائل حذف کرنا چاہتے ہیں؟" else "Are you sure you want to delete this media record?") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteClassActivityMedia(deletingMediaId!!, currentUser)
                        deletingMediaId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isUrdu) "حذف کریں" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMediaId = null }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun AssignmentsTabContent(
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    onOpenStudentPage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val allAssignments by repository.studentAssignments.collectAsState()
    val students by repository.studentsList.collectAsState()
    val accessibleStudents = remember(students, currentUser) {
        repository.getAttendanceStudentsList(currentUser)
    }
    val canCreate = currentUser?.role == UserRole.SuperAdmin ||
        currentUser?.role == UserRole.Administrator ||
        currentUser?.role == UserRole.Teacher

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingAssignment by remember { mutableStateOf<StudentAssignment?>(null) }
    var deletingAssignmentId by remember { mutableStateOf<String?>(null) }

    // Strict RLS filtered assignments for all 4 roles
    val visibleAssignments = remember(allAssignments, currentUser, students) {
        if (currentUser == null) emptyList()
        else {
            when (currentUser.role) {
                UserRole.SuperAdmin, UserRole.Administrator -> allAssignments
                UserRole.Teacher -> {
                    allAssignments.filter { assign ->
                        val student = repository.getStudentByPermanentId(assign.studentPermanentId)
                        student != null && repository.canUserViewStudent(currentUser, student)
                    }
                }
                UserRole.Parent -> {
                    allAssignments.filter { assign ->
                        val student = repository.getStudentByPermanentId(assign.studentPermanentId)
                        student != null && repository.canUserViewStudent(currentUser, student)
                    }
                }
                UserRole.Student -> {
                    allAssignments.filter { it.studentPermanentId == currentUser.permanentStudentId }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isUrdu) "ہوم ورک اور اسائنمنٹس" else "Assignments & Homework",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isUrdu) "تصویر، پی ڈی ایف، آڈیو، اور ویڈیو اپ لوڈز"
                            else "Photo, PDF, Audio, and Video submissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    if (canCreate) {
                        DbusAddButton(
                            text = if (isUrdu) "نئی اسائنمنٹ" else "New",
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.testTag("btn_create_assignment")
                        )
                    }
                }
            }

            item {
                val targetStudent = accessibleStudents.firstOrNull()

                if (targetStudent == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = if (isUrdu) "ابھی تک کوئی طالب علم موجود نہیں۔" else "No students yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                } else {
                    StudentAssignmentsSection(
                        assignments = visibleAssignments,
                        student = targetStudent,
                        currentUser = currentUser,
                        language = language,
                        repository = repository,
                        onSubmitAssignment = { assignmentId, mediaType, uri, fileName, sizeBytes ->
                            currentUser?.let { user ->
                                repository.submitAssignment(
                                    assignmentId = assignmentId,
                                    mediaType = mediaType,
                                    fileUri = uri,
                                    fileName = fileName,
                                    fileSizeBytes = sizeBytes,
                                    submittingUser = user
                                )
                            }
                        },
                        onEditAssignment = if (canCreate) { assign -> editingAssignment = assign } else null,
                        onDeleteAssignment = if (canCreate) { assign -> deletingAssignmentId = assign.id } else null
                    )
                }
            }
        }
    }

    // Create / Edit Assignment Dialog
    if (showCreateDialog || editingAssignment != null) {
        val isEditing = editingAssignment != null
        var title by remember(editingAssignment) { mutableStateOf(editingAssignment?.title ?: "") }
        var subject by remember(editingAssignment) { mutableStateOf(editingAssignment?.subject ?: "") }
        var dueDate by remember(editingAssignment) { mutableStateOf(editingAssignment?.dueDate ?: repository.todayDate()) }
        var targetStudentId by remember(editingAssignment, accessibleStudents) {
            mutableStateOf(
                editingAssignment?.studentPermanentId
                    ?: accessibleStudents.firstOrNull()?.permanentStudentId
                    ?: ""
            )
        }

        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                editingAssignment = null
            },
            title = {
                Text(
                    if (isEditing) (if (isUrdu) "اسائنمنٹ میں ترمیم" else "Edit Assignment")
                    else (if (isUrdu) "نئی اسائنمنٹ بنائیں" else "Create New Assignment")
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isEditing && accessibleStudents.isNotEmpty()) {
                        Text(
                            text = if (isUrdu) "طالب علم منتخب کریں:" else "Assign to Student:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(accessibleStudents, key = { it.permanentStudentId }) { st ->
                                FilterChip(
                                    selected = targetStudentId == st.permanentStudentId,
                                    onClick = { targetStudentId = st.permanentStudentId },
                                    label = {
                                        Text(
                                            text = st.fullName,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (isUrdu) "عنوان" else "Title") },
                        modifier = Modifier.fillMaxWidth().testTag("input_assign_title"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text(if (isUrdu) "مضمون" else "Subject") },
                        modifier = Modifier.fillMaxWidth().testTag("input_assign_subject"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("Due Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = targetStudentId,
                        onValueChange = { targetStudentId = it },
                        label = { Text("Student Permanent ID") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                val matchedStudent = accessibleStudents.any {
                    it.permanentStudentId.equals(targetStudentId.trim(), ignoreCase = true)
                }
                Button(
                    enabled = title.isNotBlank() && targetStudentId.isNotBlank() && matchedStudent,
                    onClick = {
                        val normalizedId = accessibleStudents.first {
                            it.permanentStudentId.equals(targetStudentId.trim(), ignoreCase = true)
                        }.permanentStudentId
                        val assign = if (isEditing) {
                            editingAssignment!!.copy(
                                studentPermanentId = normalizedId,
                                title = title.trim(),
                                subject = subject.trim().ifBlank { editingAssignment!!.subject },
                                dueDate = dueDate.trim().ifBlank { editingAssignment!!.dueDate }
                            )
                        } else {
                            StudentAssignment(
                                id = "assign_" + UUID.randomUUID().toString().take(6),
                                studentPermanentId = normalizedId,
                                title = title.trim(),
                                subject = subject.trim().ifBlank { "General" },
                                dueDate = dueDate.trim().ifBlank { repository.todayDate() }
                            )
                        }
                        currentUser?.let { user ->
                            if (isEditing) repository.updateAssignment(assign, user)
                            else repository.addAssignment(assign, user)
                        }
                        showCreateDialog = false
                        editingAssignment = null
                    },
                    modifier = Modifier.testTag("btn_save_new_assignment")
                ) {
                    Text(if (isEditing) (if (isUrdu) "محفوظ کریں" else "Save") else (if (isUrdu) "شامل کریں" else "Create"))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    editingAssignment = null
                }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Delete Assignment Confirmation
    if (deletingAssignmentId != null && currentUser != null) {
        AlertDialog(
            onDismissRequest = { deletingAssignmentId = null },
            title = { Text(if (isUrdu) "اسائنمنٹ حذف کریں؟" else "Delete Assignment?") },
            text = {
                Text(
                    if (isUrdu) "کیا آپ واقعی یہ اسائنمنٹ حذف کرنا چاہتے ہیں؟"
                    else "Are you sure you want to delete this assignment?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteAssignment(deletingAssignmentId!!, currentUser)
                        deletingAssignmentId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_assignment")
                ) {
                    Text(if (isUrdu) "حذف کریں" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingAssignmentId = null }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }
}
