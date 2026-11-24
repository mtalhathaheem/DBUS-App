package com.example.ui.student

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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableIntStateOf
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
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.data.SchoolRepository
import com.example.model.AppLanguage
import com.example.model.CoreSkill
import com.example.model.MediaType
import com.example.model.SkillAssessment
import com.example.model.SkillEvidenceItem
import com.example.model.SkillLevel
import com.example.model.StudentRecord
import com.example.model.UserProfile
import com.example.util.MediaUtils
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Requirement 7: Dedicated Core Skills Detail Page
 * Opens when any of the 10 core skills is tapped.
 * Displays:
 * - Scores out of 10 and Level 1-4 (Needs support, Developing, Good, Excellent)
 * - Evidence media (audio recordings, handwriting photos, video speeches)
 * - Evaluation history over time
 * - Private notes (Teacher & Admin only) and Shared notes (Parent & Student viewable)
 */
@Composable
fun SkillDetailPageDialog(
    skill: CoreSkill,
    student: StudentRecord,
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val isUrdu = language == AppLanguage.URDU
    val allAssessments by repository.skillAssessments.collectAsState()
    val history = repository.getSkillHistory(student.permanentStudentId, currentUser)
    val latestAssessment = history.firstOrNull()

    val currentScoreLevel = latestAssessment?.scores?.get(skill)
    val skillLevel = currentScoreLevel?.let { SkillLevel.fromScore(it) }
    val scoreOutOfTen = when (currentScoreLevel) {
        1 -> 4
        2 -> 6
        3 -> 8
        4 -> 10
        else -> 0
    }
    val accentColor = skillLevel?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.primary

    // Evidence items for this specific skill
    val evidenceList = repository.getEvidenceForSkill(student.permanentStudentId, skill)

    // Notes
    val sharedNote = latestAssessment?.sharedNotes?.get(skill)
    val privateNote = latestAssessment?.privateNotes?.get(skill)
    val canViewPrivateNotes = repository.canUserViewPrivateNotes(currentUser, student)
    val canEdit = repository.canUserEditSkills(currentUser, student)

    var showAddEvidenceDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = skill.getDisplayName(isUrdu),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = student.fullName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Description & Criteria
                item {
                    Text(
                        text = skill.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                // Score Card (Score out of 10 and Level 1-4)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (isUrdu) "موجودہ اسکور (10 میں سے)" else "Score (Out of 10)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (currentScoreLevel != null) "$scoreOutOfTen / 10" else "0 / 10",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentColor
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = accentColor.copy(alpha = 0.2f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = if (currentScoreLevel != null) "Level $currentScoreLevel of 4" else if (isUrdu) "درج نہیں" else "Unassessed",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor
                                        )
                                        Text(
                                            text = skillLevel?.getDisplayName(isUrdu) ?: (if (isUrdu) "ابھی درج نہیں" else "Not assessed yet"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = accentColor
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { scoreOutOfTen / 10f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = accentColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }

                // Evidence & Media Artifacts Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isUrdu) "صلاحیت کے ثبوت و ریکارڈنگز" else "Evidence & Artifacts",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        if (canEdit) {
                            TextButton(
                                onClick = { showAddEvidenceDialog = true },
                                modifier = Modifier.testTag("btn_add_skill_evidence")
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isUrdu) "نیا ثبوت شامل کریں" else "Add Evidence", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Evidence list
                if (evidenceList.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = if (isUrdu) "اس صلاحیت کا کوئی الگ میڈیا ثبوت تاحال اپ لوڈ نہیں کیا گیا۔"
                                else "No specific media evidence recorded yet for this skill.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    items(evidenceList) { item ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (item.mediaType) {
                                            MediaType.AUDIO -> Icons.Default.Audiotrack
                                            MediaType.PHOTO -> Icons.Default.Image
                                            MediaType.VIDEO -> Icons.Default.Movie
                                            MediaType.DOCUMENT -> Icons.Default.Notes
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(text = item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "${item.recordedDate} • Evaluated by ${item.evaluatedBy}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = "${item.scoreOutOfTen}/10",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Shared Note
                if (!sharedNote.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = if (isUrdu) "مشترکہ نوٹ (تمام کیلئے)" else "Shared Note (Visible to Parent & Student)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = sharedNote, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                // Private Note (Strictly Teacher & Administrator Only)
                if (canViewPrivateNotes && !privateNote.isNullOrBlank()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFDE8E8),
                            border = BorderStroke(1.dp, Color(0xFFF87171)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isUrdu) "خفیہ نوٹ (صرف استاد و منتظم کیلئے)" else "Private Note (Teacher & Admin Only)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = privateNote, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7F1D1D))
                            }
                        }
                    }
                }

                // Evaluation History
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isUrdu) "تشخیصی تاریخ و ریکارڈ" else "Skill Evaluation History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (history.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ) {
                            Text(
                                text = if (isUrdu) "ابھی تک کوئی تشخیصی تاریخ موجود نہیں۔"
                                else "No evaluation history yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                } else {
                    items(history) { record ->
                        val s = record.scores[skill]
                        val lvl = s?.let { SkillLevel.fromScore(it) }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = record.date, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(text = "Assessed by ${record.assessedByTeacherName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = if (lvl != null && s != null) "Score: $s/4 (${lvl.getDisplayName(isUrdu)})" else if (isUrdu) "درج نہیں" else "Not scored",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = lvl?.let { Color(it.colorHex) } ?: MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(if (isUrdu) "بند کریں" else "Done")
            }
        }
    )

    // Add Evidence Dialog
    if (showAddEvidenceDialog) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        var newTitle by remember { mutableStateOf("") }
        var newDesc by remember { mutableStateOf("") }
        var newScore10 by remember { mutableIntStateOf(8) }
        var newMediaType by remember { mutableStateOf(MediaType.AUDIO) }
        var pickedUri by remember { mutableStateOf<Uri?>(null) }
        var pickedFileName by remember { mutableStateOf("") }
        var pickedSizeBytes by remember { mutableStateOf(0L) }
        var pickedMimeType by remember { mutableStateOf("application/octet-stream") }
        var isUploading by remember { mutableStateOf(false) }
        var uploadError by remember { mutableStateOf<String?>(null) }

        val evidenceFilePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                pickedUri = uri
                uploadError = null
                val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
                pickedMimeType = mime
                newMediaType = when {
                    mime.startsWith("image/") -> MediaType.PHOTO
                    mime.startsWith("video/") -> MediaType.VIDEO
                    mime.startsWith("audio/") -> MediaType.AUDIO
                    else -> MediaType.DOCUMENT
                }
                var resolvedName = "evidence_${student.permanentStudentId}"
                var resolvedSize = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx >= 0) resolvedName = cursor.getString(nameIdx) ?: resolvedName
                        if (sizeIdx >= 0) resolvedSize = cursor.getLong(sizeIdx)
                    }
                }
                pickedFileName = resolvedName
                pickedSizeBytes = resolvedSize
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isUploading) showAddEvidenceDialog = false },
            title = { Text(if (isUrdu) "نیا ثبوت ریکارڈ کریں" else "Record Skill Evidence") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it; uploadError = null },
                        label = { Text(if (isUrdu) "عنوان" else "Title") },
                        placeholder = { Text(if (isUrdu) "مثلاً زبانی قراءت / مشاہدہ" else "e.g. Oral Reading Recitation") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newDesc,
                        onValueChange = { newDesc = it; uploadError = null },
                        label = { Text(if (isUrdu) "مشاہدات و ریمارکس" else "Observations / Rubric Remarks") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Score (1-10): $newScore10/10", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row {
                            listOf(4, 6, 8, 10).forEach { num ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (newScore10 == num) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .clickable(enabled = !isUploading) { newScore10 = num }
                                ) {
                                    Text(
                                        text = "$num",
                                        color = if (newScore10 == num) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            evidenceFilePicker.launch("*/*")
                        },
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (pickedFileName.isNotBlank()) {
                                "${pickedFileName} (${MediaUtils.formatFileSize(pickedSizeBytes)})"
                            } else {
                                if (isUrdu) "فائل منسلک کریں (اختیاری)" else "Attach Evidence File (Optional)"
                            }
                        )
                    }

                    if (isUploading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    uploadError?.let { err ->
                        Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isUploading && newTitle.isNotBlank(),
                    onClick = {
                        coroutineScope.launch {
                            var storedUri = ""
                            if (pickedUri != null) {
                                val (ok, errMsg) = MediaUtils.validateMediaUpload(
                                    mediaType = newMediaType,
                                    fileSizeBytes = pickedSizeBytes,
                                    durationSeconds = 0,
                                    isUrdu = isUrdu
                                )
                                if (!ok) {
                                    uploadError = errMsg
                                    return@launch
                                }
                                isUploading = true
                                val bytes = try {
                                    context.contentResolver.openInputStream(pickedUri!!)?.use { it.readBytes() }
                                } catch (e: Exception) {
                                    null
                                }
                                if (bytes == null) {
                                    isUploading = false
                                    uploadError = if (isUrdu) "منتخب فائل پڑھی نہیں جا سکی۔" else "Could not read selected file."
                                    return@launch
                                }
                                val uploadRes = repository.uploadToPrivateStorage(
                                    bucket = "student-media",
                                    studentPermanentId = student.permanentStudentId,
                                    fileName = pickedFileName.ifBlank { "evidence.bin" },
                                    bytes = bytes,
                                    contentType = pickedMimeType
                                )
                                isUploading = false
                                uploadRes.fold(
                                    onSuccess = { storedUri = it },
                                    onFailure = {
                                        uploadError = it.message ?: "Upload failed."
                                        return@launch
                                    }
                                )
                            }
                            val evidence = SkillEvidenceItem(
                                id = "evi_" + UUID.randomUUID().toString().take(6),
                                skill = skill,
                                studentPermanentId = student.permanentStudentId,
                                title = newTitle.trim(),
                                description = newDesc.trim().ifBlank { "Evaluated during classroom activity." },
                                mediaType = newMediaType,
                                fileUri = storedUri,
                                scoreOutOfTen = newScore10,
                                scoreLevel = if (newScore10 >= 9) 4 else if (newScore10 >= 7) 3 else if (newScore10 >= 5) 2 else 1,
                                recordedDate = repository.todayDate(),
                                evaluatedBy = currentUser?.name ?: "Faculty"
                            )
                            repository.addSkillEvidence(evidence)
                            showAddEvidenceDialog = false
                        }
                    }
                ) {
                    Text(if (isUrdu) "ثبوت محفوظ کریں" else "Save Evidence")
                }
            },
            dismissButton = {
                TextButton(enabled = !isUploading, onClick = { showAddEvidenceDialog = false }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }
}
