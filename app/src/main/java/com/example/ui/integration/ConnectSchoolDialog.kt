package com.example.ui.integration

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
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.model.SyncMethod
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Requirement 4: Universal School Integration System ("CONNECT YOUR SCHOOL")
 * 6 Connection Modes:
 * 1. Connect Existing Software
 * 2. Connect Database
 * 3. DBUS Desktop Connector
 * 4. Connect Through API
 * 5. Import Excel / CSV records
 * 6. DBUS School Portal
 * Features: Automatic student synchronization, duplicate detection, and sync history.
 */
@Composable
fun ConnectSchoolDialog(
    repository: SchoolRepository,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val isUrdu = language == AppLanguage.URDU
    var selectedMethod by remember { mutableStateOf(SyncMethod.EXCEL_IMPORT) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Integration Setup, 1: Sync History & Audit

    val syncHistory by repository.syncHistoryLogs.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Form states for inputs (all start empty; placeholders guide the user)
    var existingSoftwareName by remember { mutableStateOf("") }
    var softwareUrl by remember { mutableStateOf("") }

    var dbType by remember { mutableStateOf("PostgreSQL") }
    var dbHost by remember { mutableStateOf("") }
    var dbUser by remember { mutableStateOf("") }

    var desktopIp by remember { mutableStateOf("") }
    var desktopToken by remember { mutableStateOf("") }

    var apiEndpoint by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }

    var csvInputText by remember { mutableStateOf("") }

    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var lastImportResult by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
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
                        Icon(imageVector = Icons.Default.Link, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isUrdu) "اپنا اسکول منسلک کریں (CONNECT YOUR SCHOOL)" else "CONNECT YOUR SCHOOL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = if (isUrdu) "6 رابطہ جاتی طریقے • خودکار طلباء سنک اور ڈپلیکیٹ چیکنگ" else "Universal Integration • 6 Sync Modes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, enabled = !isProcessing) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(if (isUrdu) "انضمام کے طریقے" else "Connection Modes") },
                        icon = { Icon(Icons.Default.Layers, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(if (isUrdu) "سنک ہسٹری (${syncHistory.size})" else "Sync History (${syncHistory.size})") },
                        icon = { Icon(Icons.Default.History, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (selectedTab == 0) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 6 Mode Selector Chips
                        item {
                            Text(
                                text = if (isUrdu) "رابطہ کرنے کا طریقہ منتخب کریں:" else "Select Connection Method:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        items(SyncMethod.values()) { method ->
                            val isSelected = selectedMethod == method
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isProcessing) {
                                        selectedMethod = method
                                        statusMessage = null
                                        lastImportResult = null
                                    }
                                    .testTag("mode_${method.name.lowercase()}"),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (method) {
                                            SyncMethod.EXISTING_SOFTWARE -> Icons.Default.Layers
                                            SyncMethod.DATABASE_CONNECTOR -> Icons.Default.Storage
                                            SyncMethod.DESKTOP_CONNECTOR -> Icons.Default.Computer
                                            SyncMethod.API_ONEROSTER -> Icons.Default.Api
                                            SyncMethod.EXCEL_IMPORT -> Icons.Default.UploadFile
                                            SyncMethod.CLOUD_PORTAL -> Icons.Default.Cloud
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = method.getDisplayName(isUrdu),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                                        )
                                        Text(
                                            text = method.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Form for the selected method
                        item {
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    when (selectedMethod) {
                                        SyncMethod.EXISTING_SOFTWARE -> {
                                            Text(text = "Existing Software Configuration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            OutlinedTextField(
                                                value = existingSoftwareName,
                                                onValueChange = { existingSoftwareName = it },
                                                label = { Text("Software Name (Fedena, OpenSIS, QuickSchools, etc.)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = softwareUrl,
                                                onValueChange = { softwareUrl = it },
                                                label = { Text("Server API URL / Host") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                        }
                                        SyncMethod.DATABASE_CONNECTOR -> {
                                            Text(text = "Direct Database Connector", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            OutlinedTextField(
                                                value = dbType,
                                                onValueChange = { dbType = it },
                                                label = { Text("Database Engine (MySQL / PostgreSQL / MS SQL)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = dbHost,
                                                onValueChange = { dbHost = it },
                                                label = { Text("Database Host & Port") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                        }
                                        SyncMethod.DESKTOP_CONNECTOR -> {
                                            Text(text = "DBUS Desktop Connector Agent", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "Lightweight desktop client synchronizes local school offline server with DBUS Cloud.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            OutlinedTextField(
                                                value = desktopIp,
                                                onValueChange = { desktopIp = it },
                                                label = { Text("Local School Server IP") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = desktopToken,
                                                onValueChange = { desktopToken = it },
                                                label = { Text("Desktop Agent Token") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                        }
                                        SyncMethod.API_ONEROSTER -> {
                                            Text(text = "Open Standard OneRoster / LTI Sync", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            OutlinedTextField(
                                                value = apiEndpoint,
                                                onValueChange = { apiEndpoint = it },
                                                label = { Text("OneRoster 1.2 REST Endpoint") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                            OutlinedTextField(
                                                value = apiKey,
                                                onValueChange = { apiKey = it },
                                                label = { Text("API Consumer Secret") },
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true
                                            )
                                        }
                                        SyncMethod.EXCEL_IMPORT -> {
                                            Text(text = "Spreadsheet / CSV Student Importer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = "Paste comma-separated rows: Full Name, B-Form / ID, Father Name, Grade, Section. DBUS automatically detects duplicate B-Forms & Permanent IDs.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            OutlinedTextField(
                                                value = csvInputText,
                                                onValueChange = { csvInputText = it },
                                                label = { Text("CSV / Excel Data") },
                                                placeholder = { Text("Full Name, B-Form, Father Name, Grade, Section") },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(130.dp),
                                                maxLines = 6
                                            )
                                        }
                                        SyncMethod.CLOUD_PORTAL -> {
                                            Text(text = "DBUS Cloud Native Portal", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFE8F5E9)
                                            ) {
                                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Active & Synchronized. All changes update instantly across Mobile & Web.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF1B5E20),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (statusMessage != null) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = statusMessage!!,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }

                                    if (isProcessing) {
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: SYNC HISTORY
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (syncHistory.isEmpty()) {
                            item {
                                Text(
                                    text = if (isUrdu) "ابھی کوئی سنک ہسٹری موجود نہیں ہے۔" else "No sync history yet.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                        items(syncHistory) { log ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = log.method.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(text = log.timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Status: ${log.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (log.status.contains("Success") || log.status.contains("Completed")) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Synced: ${log.recordsSynced} records", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                        Text(text = "Duplicates: ${log.duplicatesFound}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                        Text(text = "By: ${log.initiatedBy}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                Button(
                    onClick = {
                        val validationError = when (selectedMethod) {
                            SyncMethod.EXCEL_IMPORT -> if (csvInputText.isBlank()) {
                                if (isUrdu) "براہ کرم امپورٹ کرنے کے لیے CSV ڈیٹا درج کریں۔" else "Please paste CSV student rows before importing."
                            } else null
                            SyncMethod.EXISTING_SOFTWARE -> if (existingSoftwareName.isBlank()) {
                                if (isUrdu) "براہ کرم سافٹ ویئر کا نام درج کریں۔" else "Please enter the existing software name."
                            } else null
                            SyncMethod.DATABASE_CONNECTOR -> if (dbHost.isBlank()) {
                                if (isUrdu) "براہ کرم ڈیٹا بیس ہوسٹ درج کریں۔" else "Please enter the database host address."
                            } else null
                            SyncMethod.DESKTOP_CONNECTOR -> if (desktopIp.isBlank()) {
                                if (isUrdu) "براہ کرم ڈیسک ٹاپ آئی پی درج کریں۔" else "Please enter the desktop connector IP."
                            } else null
                            SyncMethod.API_ONEROSTER -> if (apiEndpoint.isBlank()) {
                                if (isUrdu) "براہ کرم API اینڈ پوائنٹ درج کریں۔" else "Please enter the OneRoster API endpoint."
                            } else null
                            SyncMethod.CLOUD_PORTAL -> null
                        }
                        if (validationError != null) {
                            statusMessage = validationError
                            return@Button
                        }
                        isProcessing = true
                        coroutineScope.launch {
                            delay(300)
                            if (selectedMethod == SyncMethod.EXCEL_IMPORT) {
                                val (imported, duplicates) = repository.importExcelStudentRecords(csvInputText)
                                lastImportResult = Pair(imported, duplicates)
                                statusMessage = "Import completed: $imported students added, $duplicates duplicates skipped."
                            } else {
                                repository.connectSchoolIntegration(
                                    selectedMethod,
                                    when (selectedMethod) {
                                        SyncMethod.EXISTING_SOFTWARE -> existingSoftwareName.trim()
                                        SyncMethod.DATABASE_CONNECTOR -> "$dbType on ${dbHost.trim()}"
                                        SyncMethod.DESKTOP_CONNECTOR -> "Desktop Agent ${desktopIp.trim()}"
                                        SyncMethod.API_ONEROSTER -> apiEndpoint.trim()
                                        else -> "DBUS Cloud Portal"
                                    }
                                )
                                statusMessage = "Integration configured successfully. Synchronization active."
                            }
                            isProcessing = false
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier.testTag("btn_connect_and_sync")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (selectedMethod == SyncMethod.EXCEL_IMPORT) (if (isUrdu) "امپورٹ و ہم آہنگ کریں" else "Import & Sync Records")
                        else (if (isUrdu) "منسلک و فعال کریں" else "Connect & Sync Now")
                    )
                }
            } else {
                Button(onClick = onDismiss) {
                    Text(if (isUrdu) "بند کریں" else "Done")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProcessing) {
                Text(if (isUrdu) "منسوخ" else "Cancel")
            }
        }
    )
}
