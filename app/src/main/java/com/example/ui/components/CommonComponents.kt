package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SchoolRepository
import com.example.model.AppLanguage
import com.example.model.ApprovalStatus
import com.example.model.StudentRecord
import com.example.model.UserRole
import com.example.ui.media.StudentAvatar

@Composable
fun RoleIconBadge(
    role: UserRole,
    language: AppLanguage,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val (bgColor, tintColor, iconVector) = when (role) {
        UserRole.SuperAdmin -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.Security
        )
        UserRole.Administrator -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.AdminPanelSettings
        )
        UserRole.Teacher -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Default.CoPresent
        )
        UserRole.Parent -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.FamilyRestroom
        )
        UserRole.Student -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Default.School
        )
    }

    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .testTag("role_icon_badge_${role.name.lowercase()}")
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = role.getDisplayName(isUrdu),
                tint = tintColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun DbusAddButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DbusEditIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.EditNote,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun DbusDeleteIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
            contentColor = MaterialTheme.colorScheme.error
        )
    ) {
        Icon(
            imageVector = Icons.Outlined.DeleteOutline,
            contentDescription = contentDescription,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
fun RoleBadge(
    role: UserRole,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val (bgColor, textColor) = when (role) {
        UserRole.SuperAdmin -> MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary
        UserRole.Administrator -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        UserRole.Teacher -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        UserRole.Parent -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        UserRole.Student -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        modifier = modifier.testTag("role_badge_${role.name.lowercase()}"),
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = textColor
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = role.getDisplayName(isUrdu),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Requirement 7: "Approved" text must stay in one line (maxLines = 1, softWrap = false)
 */
@Composable
fun StudentApprovalBadge(
    status: ApprovalStatus,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val (bgColor, textColor, icon) = when (status) {
        ApprovalStatus.Approved -> Triple(
            Color(0xFFE8F5E9),
            Color(0xFF1B5E20),
            Icons.Default.CheckCircle
        )
        ApprovalStatus.PendingApproval -> Triple(
            Color(0xFFFFF8E1),
            Color(0xFFF57F17),
            Icons.Default.HourglassEmpty
        )
        ApprovalStatus.Rejected -> Triple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            Icons.Default.Lock
        )
        ApprovalStatus.Archived -> Triple(
            Color(0xFFECEFF1),
            Color(0xFF546E7A),
            Icons.Default.Lock
        )
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = textColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = status.getDisplayName(isUrdu),
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Requirement 2:
 * "Student list. Each row shows: small photo, name, Student ID, father's name.
 *  Tap a row to open the student page."
 */
@Composable
fun StudentRowItem(
    student: StudentRecord,
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("student_row_${student.permanentStudentId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small Photo / Avatar (Requirement 1: small round image or first letter)
            StudentAvatar(student = student, size = 46.dp)

            Spacer(modifier = Modifier.width(14.dp))

            // Student Information: Name, Student ID, Father's Name
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUrdu) student.urduName else student.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    StudentApprovalBadge(status = student.approvalStatus, language = language)
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Permanent Student ID
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = student.permanentStudentId,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${student.grade}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Father's Name
                Text(
                    text = if (isUrdu) "والد کا نام: ${student.fatherName}"
                    else "Father's Name: ${student.fatherName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Requirement 2 & 8:
 * Student Page dialog / detail sheet showing full info with strict privacy:
 * B-Form is only shown to Administrator and only last 4 digits!
 */
@Composable
fun StudentDetailDialog(
    student: StudentRecord,
    viewerRole: UserRole,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val isUrdu = language == AppLanguage.URDU
    val repository = SchoolRepository.getInstance()
    val maskedBForm = repository.getMaskedBForm(student.bFormNumber, viewerRole)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StudentAvatar(student = student, size = 42.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isUrdu) student.urduName else student.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isUrdu) "طالب علم پروفائل" else "Student Profile",
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
            Column(modifier = Modifier.fillMaxWidth()) {
                // Permanent Student ID ribbon
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isUrdu) "مستقل اسٹوڈنٹ آئی ڈی" else "Permanent Student ID",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = student.permanentStudentId,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        StudentApprovalBadge(status = student.approvalStatus, language = language)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Detail Items
                DetailRow(
                    label = if (isUrdu) "والد کا نام" else "Father's Name",
                    value = student.fatherName
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                DetailRow(
                    label = if (isUrdu) "جماعت اور سیکشن" else "Class & Section",
                    value = "${student.grade} • ${student.section}"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                DetailRow(
                    label = if (isUrdu) "حاضری کی شرح" else "Attendance Rate",
                    value = "${student.attendanceRate}% (Satisfactory)"
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Requirement 8: Show B-Form ONLY to Administrator and ONLY last 4 digits
                if ((viewerRole == UserRole.Administrator || viewerRole == UserRole.SuperAdmin) && maskedBForm != null) {
                    DetailRow(
                        label = if (isUrdu) "بے فارم (صرف ایڈمنسٹریٹر)" else "B-Form (Admin Masked)",
                        value = maskedBForm,
                        isMonospace = true
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                DetailRow(
                    label = if (isUrdu) "تاریخِ اجراء" else "Registration Date",
                    value = student.issuedDate
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Guarantee Banner
                Text(
                    text = if (isUrdu) "یہ شناختی نمبر مستقل ہے، کبھی تبدیل نہیں ہوگا اور نہ ہی دوبارہ استعمال ہوگا۔"
                    else "This ID never changes and is never reused.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isUrdu) "بند کریں" else "Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
        )
    }
}
