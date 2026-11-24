package com.example.ui.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.SchoolRepository
import com.example.model.AppLanguage
import com.example.model.ApprovalStatus
import com.example.model.InstitutionRecord
import com.example.model.InstitutionType
import com.example.model.SubscriptionPlan
import com.example.model.SyncMethod
import com.example.ui.components.DbusAddButton
import com.example.ui.components.StudentApprovalBadge
import java.util.UUID

/**
 * Requirement 3: Super Admin & Multi-Institution Support
 * - Manage multiple schools & colleges
 * - Approve institutions
 * - Manage subscriptions (Free Trial, Basic, Standard, Premium, Custom)
 */
@Composable
fun SuperAdminManagementView(
    repository: SchoolRepository,
    language: AppLanguage,
    onOpenConnectSchool: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val institutions by repository.institutions.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingPlanInstitution by remember { mutableStateOf<InstitutionRecord?>(null) }

    val totalStudents = institutions.sumOf { it.studentCount }
    val totalRevenue = institutions.sumOf { it.plan.pricePerMonthPkr }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Multi-Campus Governance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isUrdu) "سپر ایڈمن پلیٹ فارم گورننس" else "Super Admin Platform Governance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = if (isUrdu) "ملک بھر کے تعلیمی اداروں کا نظم و نسق" else "Multi-Institution & Subscription Control",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = if (isUrdu) "کل ادارے" else "Institutions", style = MaterialTheme.typography.labelSmall)
                            Text(text = "${institutions.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = if (isUrdu) "کل طلباء" else "Total Students", style = MaterialTheme.typography.labelSmall)
                            Text(text = "$totalStudents", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = if (isUrdu) "ماہانہ آمدنی" else "MRR (PKR)", style = MaterialTheme.typography.labelSmall)
                            Text(text = "Rs. ${(totalRevenue / 1000)}k", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        }
                    }
                }
            }
        }

        // Section Title & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUrdu) "منسلک اسکولز اور کالجز" else "Connected Institutions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isUrdu) "سبسکرپشن اور منظوری کا نظم" else "Manage subscriptions, approvals & sync",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onOpenConnectSchool,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_superadmin_connect_school")
                ) {
                    Icon(imageVector = Icons.Default.Domain, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isUrdu) "کنیکٹر" else "Connector", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                }

                DbusAddButton(
                    text = if (isUrdu) "نیا اسکول" else "Add School",
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("btn_superadmin_add_school")
                )
            }

        // Institution Cards List
        if (institutions.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("empty_institutions_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isUrdu) "ابھی کوئی تعلیمی ادارہ رجسٹرڈ نہیں ہے۔" else "No institutions yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        institutions.forEach { inst ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("institution_card_${inst.id}"),
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
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (inst.institutionType) {
                                        InstitutionType.COLLEGE -> Icons.Default.School
                                        InstitutionType.HIGHER_SECONDARY -> Icons.Default.Domain
                                        InstitutionType.SCHOOL -> Icons.Default.Business
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isUrdu) inst.urduName else inst.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${inst.institutionType.getDisplayName(isUrdu)} • ${inst.city}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Approval Status Badge
                        StudentApprovalBadge(status = inst.status, language = language)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Admin: ${inst.adminContactName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Students: ${inst.studentCount} • Faculty: ${inst.teacherCount}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Subscription Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(inst.plan.colorHex).copy(alpha = 0.15f),
                            modifier = Modifier.clickable { editingPlanInstitution = inst }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = inst.plan.getDisplayName(isUrdu),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(inst.plan.colorHex)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Outlined.EditNote, contentDescription = "Edit Plan", tint = Color(inst.plan.colorHex), modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // Bottom Action Bar: Approve button if Pending
                    if (inst.status == ApprovalStatus.PendingApproval) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = { repository.approveInstitution(inst.id) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_approve_institution_${inst.id}")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = if (isUrdu) "ادارہ منظور کریں" else "Approve Institution", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Institution Dialog
    if (showAddDialog) {
        var nameInput by remember { mutableStateOf("") }
        var cityInput by remember { mutableStateOf("") }
        var adminName by remember { mutableStateOf("") }
        var adminEmail by remember { mutableStateOf("") }
        var formError by remember { mutableStateOf<String?>(null) }
        var selectedType by remember { mutableStateOf(InstitutionType.SCHOOL) }
        var selectedPlan by remember { mutableStateOf(SubscriptionPlan.STANDARD) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (isUrdu) "نیا اسکول / کالج رجسٹر کریں" else "Register New Institution") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it; formError = null },
                        label = { Text(if (isUrdu) "ادارے کا نام" else "Institution Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cityInput,
                        onValueChange = { cityInput = it; formError = null },
                        label = { Text(if (isUrdu) "شہر / صوبہ" else "City / Province") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = adminName,
                        onValueChange = { adminName = it; formError = null },
                        label = { Text(if (isUrdu) "پرنسپل / ایڈمنسٹریٹر کا نام" else "Principal / Administrator Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = adminEmail,
                        onValueChange = { adminEmail = it; formError = null },
                        label = { Text(if (isUrdu) "ایڈمن آفیشل ای میل" else "Admin Official Email") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    formError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.isBlank() || adminEmail.isBlank()) {
                            formError = if (isUrdu) "براہ کرم ادارے کا نام اور ای میل درج کریں" else "Please enter institution name and admin email."
                            return@Button
                        }
                        val newInst = InstitutionRecord(
                            id = "inst_" + UUID.randomUUID().toString().take(6),
                            name = nameInput.trim(),
                            urduName = nameInput.trim(),
                            institutionType = selectedType,
                            city = cityInput.trim(),
                            adminContactName = adminName.trim(),
                            adminEmail = adminEmail.trim(),
                            studentCount = 0,
                            teacherCount = 0,
                            plan = selectedPlan,
                            status = ApprovalStatus.Approved,
                            registeredDate = repository.todayDate(),
                            isIntegrationConnected = true,
                            activeSyncMethod = SyncMethod.CLOUD_PORTAL
                        )
                        repository.addInstitution(newInst)
                        showAddDialog = false
                    }
                ) {
                    Text(if (isUrdu) "رجسٹر اور فعال کریں" else "Register & Activate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Change Subscription Plan Dialog
    editingPlanInstitution?.let { inst ->
        AlertDialog(
            onDismissRequest = { editingPlanInstitution = null },
            title = { Text("Change Plan: ${inst.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubscriptionPlan.values().forEach { plan ->
                        val isCurrent = inst.plan == plan
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.updateInstitutionPlan(inst.id, plan)
                                    editingPlanInstitution = null
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isCurrent) MaterialTheme.colorScheme.primary else Color.Transparent)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = plan.getDisplayName(isUrdu), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(text = "Up to ${plan.maxStudents} students", style = MaterialTheme.typography.labelSmall)
                                }
                                Text(
                                    text = if (plan.pricePerMonthPkr == 0) "Free" else "Rs. ${plan.pricePerMonthPkr}/mo",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(plan.colorHex)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { editingPlanInstitution = null }) {
                    Text("Close")
                }
            }
        )
    }
}
