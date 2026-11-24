package com.example.ui.attendance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.AppLanguage
import com.example.model.AttendanceRecord
import com.example.model.AttendanceStatus
import com.example.model.AttendanceSummary
import com.example.model.StudentRecord

/**
 * Requirement 8: Clickable Attendance Summary Cards
 * Detailed breakdown dialog listing:
 * - Specific absence dates & remarks
 * - Late arrival dates with notes
 * - Leave dates with parent reasons
 * - Total working days & rate
 */
@Composable
fun DetailedAttendanceBreakdownDialog(
    student: StudentRecord,
    summary: AttendanceSummary,
    allRecords: List<AttendanceRecord>,
    isYearly: Boolean,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val isUrdu = language == AppLanguage.URDU
    var selectedTab by remember { mutableIntStateOf(0) } // 0: All Absences/Late, 1: Complete Ledger

    val absenceList = remember(allRecords) { allRecords.filter { it.status == AttendanceStatus.ABSENT } }
    val lateList = remember(allRecords) { allRecords.filter { it.status == AttendanceStatus.LATE } }
    val leaveList = remember(allRecords) { allRecords.filter { it.status == AttendanceStatus.LEAVE } }

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
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isYearly) (if (isUrdu) "سالانہ حاضری کا تفصیلی جائزہ" else "Academic Year Attendance Breakdown")
                            else (if (isUrdu) "ماہانہ حاضری کا تفصیلی جائزہ" else "Monthly Attendance Breakdown"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${student.fullName} • ${student.permanentStudentId}",
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
                // Metric Summary Ribbon
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = if (isUrdu) "حاضری شرح" else "Attendance Rate", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "${String.format(java.util.Locale.US, "%.1f", summary.attendancePercentage)}%",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (summary.attendancePercentage < 75) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = Color(AttendanceStatus.PRESENT.colorHex).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text(text = "P: ${summary.presentDays}", color = Color(AttendanceStatus.PRESENT.colorHex), fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall)
                            }
                            Surface(color = Color(AttendanceStatus.ABSENT.colorHex).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text(text = "A: ${summary.absentDays}", color = Color(AttendanceStatus.ABSENT.colorHex), fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall)
                            }
                            Surface(color = Color(AttendanceStatus.LATE.colorHex).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text(text = "L: ${summary.lateDays}", color = Color(AttendanceStatus.LATE.colorHex), fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall)
                            }
                            Surface(color = Color(AttendanceStatus.LEAVE.colorHex).copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text(text = "Lv: ${summary.leaveDays}", color = Color(AttendanceStatus.LEAVE.colorHex), fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(if (isUrdu) "غیر حاضری و تاخیر (${absenceList.size + lateList.size})" else "Absences & Late (${absenceList.size + lateList.size})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(if (isUrdu) "تمام ایام (${allRecords.size})" else "All Dates (${allRecords.size})") }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val displayedList = if (selectedTab == 0) (absenceList + lateList + leaveList).sortedByDescending { it.date } else allRecords.sortedByDescending { it.date }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (displayedList.isEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ) {
                                Text(
                                    text = if (isUrdu) "کوئی غیر حاضری یا تاخیر کا ریکارڈ نہیں ملا۔ شاندار ریکارڈ!"
                                    else "No absence or late records found. 100% attendance!",
                                    modifier = Modifier.padding(14.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(displayedList) { record ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = record.date, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        if (record.note.isNotBlank()) {
                                            Text(text = "Remark: ${record.note}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text(text = "Marked by: ${record.markedBy}", style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(record.status.colorHex).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = record.status.getDisplayName(isUrdu),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(record.status.colorHex),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.testTag("btn_close_attendance_breakdown")) {
                Text(if (isUrdu) "بند کریں" else "Done")
            }
        }
    )
}
