package com.example.ui.tools

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SchoolRepository
import com.example.model.*
import com.example.ui.components.DbusAddButton
import com.example.ui.components.DbusDeleteIconButton
import com.example.ui.components.DbusEditIconButton
import com.example.util.SchoolCalculatorUtil
import java.util.Calendar
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarCalculatorScreen(
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    onBack: () -> Unit,
    calendarOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Calendar, 1: Calculator

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (calendarOnly) {
                            if (isUrdu) "DBUS تعلیمی کیلنڈر" else "DBUS Academic Calendar"
                        } else {
                            if (isUrdu) "تعلیمی کیلنڈر اور کیلکولیٹر" else "Calendar & School Tools"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_calendar_tools_back")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!calendarOnly) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = if (isUrdu) "تعلیمی کیلنڈر" else "Academic Calendar",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        modifier = Modifier.testTag("tab_academic_calendar")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = if (isUrdu) "اسکول کیلکولیٹر" else "School Calculator",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = { Icon(Icons.Default.Calculate, contentDescription = null) },
                        modifier = Modifier.testTag("tab_school_calculator")
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (calendarOnly || selectedTab == 0) {
                    AcademicCalendarTab(
                        repository = repository,
                        currentUser = currentUser,
                        language = language
                    )
                } else {
                    SchoolCalculatorTab(
                        language = language
                    )
                }
            }
        }
    }
}

@Composable
fun AcademicCalendarTab(
    repository: SchoolRepository,
    currentUser: UserProfile?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    val calendarEvents by repository.schoolCalendarEvents.collectAsState()
    var selectedFilter by remember { mutableStateOf<CalendarEventType?>(null) }

    // Interactive Month & Date Grid state (Defaults to current month/day)
    val todayCal = remember { Calendar.getInstance(Locale.US) }
    val currentYear = remember { todayCal.get(Calendar.YEAR) }
    val currentMonth = remember { todayCal.get(Calendar.MONTH) + 1 }
    var displayYear by remember { mutableIntStateOf(currentYear) }
    var displayMonth by remember { mutableIntStateOf(currentMonth) } // 1..12
    var selectedDate by remember { mutableStateOf(repository.todayDate()) }
    var filterBySelectedDate by remember { mutableStateOf(false) }

    var showAddEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<SchoolCalendarEvent?>(null) }
    var viewingEvent by remember { mutableStateOf<SchoolCalendarEvent?>(null) }
    var deletingEvent by remember { mutableStateOf<SchoolCalendarEvent?>(null) }

    val canEditCalendar = currentUser == null || currentUser.role in listOf(
        UserRole.Administrator,
        UserRole.SuperAdmin,
        UserRole.Teacher
    )

    val monthPrefix = remember(displayYear, displayMonth) {
        String.format(Locale.US, "%04d-%02d", displayYear, displayMonth)
    }

    val monthFullNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val monthShortNames = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")

    // Compute calendar grid cells (Mon..Sun)
    val calendarCells = remember(displayYear, displayMonth) {
        val cal = Calendar.getInstance(Locale.US)
        cal.set(displayYear, displayMonth - 1, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sun=1, Mon=2 .. Sat=7
        val leadingEmpty = (firstDayOfWeek + 5) % 7 // Mon=0 .. Sun=6
        val list = mutableListOf<Int?>()
        repeat(leadingEmpty) { list.add(null) }
        for (d in 1..daysInMonth) list.add(d)
        while (list.size % 7 != 0) list.add(null)
        list.toList()
    }

    val filteredEvents = remember(calendarEvents, selectedFilter, filterBySelectedDate, selectedDate) {
        calendarEvents.filter { ev ->
            (selectedFilter == null || ev.type == selectedFilter) &&
                (!filterBySelectedDate || ev.date == selectedDate)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Academic Session Banner + Add Event CTA
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isUrdu) "DBUS تعلیمی سیشن $currentYear-${currentYear + 1}" else "DBUS Academic Session $currentYear-${currentYear + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isUrdu) "منتخب تاریخ: $selectedDate" else "Selected Date: $selectedDate",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    if (canEditCalendar) {
                        Spacer(modifier = Modifier.width(8.dp))
                        DbusAddButton(
                            text = if (isUrdu) "نیا ایونٹ" else "Add Event",
                            onClick = { showAddEventDialog = true },
                            modifier = Modifier.testTag("btn_add_calendar_event")
                        )
                    }
                }
            }
        }

        // 2. Full Interactive Monthly Calendar Grid
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_month_grid_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Month Navigation Row (< November 2026 >)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (displayMonth > 1) {
                                    displayMonth -= 1
                                } else if (displayYear > currentYear - 1) {
                                    displayYear -= 1
                                    displayMonth = 12
                                }
                            },
                            modifier = Modifier.testTag("btn_calendar_prev_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Month"
                            )
                        }

                        Text(
                            text = "${monthFullNames[displayMonth - 1]} $displayYear",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = {
                                if (displayMonth < 12) {
                                    displayMonth += 1
                                } else if (displayYear < currentYear + 2) {
                                    displayYear += 1
                                    displayMonth = 1
                                }
                            },
                            modifier = Modifier.testTag("btn_calendar_next_month")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Weekday headers (Mon .. Sun)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { dayName ->
                            Text(
                                text = dayName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 7-column clickable date cells
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        calendarCells.chunked(7).forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                week.forEach { dayNum ->
                                    if (dayNum == null) {
                                        Box(modifier = Modifier.weight(1f).height(42.dp))
                                    } else {
                                        val dateStr = String.format(Locale.US, "%s-%02d", monthPrefix, dayNum)
                                        val dayEvents = calendarEvents.filter { it.date == dateStr }
                                        val isSelected = selectedDate == dateStr

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(42.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.primary
                                                    else if (dayEvents.isNotEmpty()) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                )
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.5.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    selectedDate = dateStr
                                                }
                                                .testTag("calendar_day_$dateStr"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected || dayEvents.isNotEmpty()) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (dayEvents.isNotEmpty()) {
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        dayEvents.take(3).forEach { ev ->
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(5.dp)
                                                                    .clip(CircleShape)
                                                                    .background(
                                                                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                                        else Color(ev.type.colorHex)
                                                                    )
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
                    }
                }
            }
        }

        // 3. Filters Row (All + Selected Date toggle + Event Types)
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = !filterBySelectedDate,
                        onClick = { filterBySelectedDate = false },
                        label = { Text(if (isUrdu) "تمام تاریخیں" else "All Dates", maxLines = 1, softWrap = false) }
                    )
                }
                item {
                    FilterChip(
                        selected = filterBySelectedDate,
                        onClick = { filterBySelectedDate = !filterBySelectedDate },
                        label = { Text(selectedDate, maxLines = 1, softWrap = false) }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text(if (isUrdu) "تمام اقسام" else "All Types", maxLines = 1, softWrap = false) }
                    )
                }
                items(CalendarEventType.values()) { type ->
                    FilterChip(
                        selected = selectedFilter == type,
                        onClick = { selectedFilter = if (selectedFilter == type) null else type },
                        label = {
                            Text(
                                text = if (isUrdu) type.urduLabel else type.label,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    )
                }
            }
        }

        // 4. Events List (Every card clickable; Add/Edit/Delete for authorized roles)
        if (filteredEvents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isUrdu) "اس فلٹر یا تاریخ کیلئے کوئی ایونٹ موجود نہیں"
                            else "No calendar events for this selection",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (canEditCalendar) {
                            Spacer(modifier = Modifier.height(8.dp))
                            DbusAddButton(
                                text = if (isUrdu) "$selectedDate پر ایونٹ شامل کریں" else "Add Event on $selectedDate",
                                onClick = { showAddEventDialog = true }
                            )
                        }
                    }
                }
            }
        } else {
            items(filteredEvents) { event ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewingEvent = event }
                        .testTag("event_item_${event.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (event.date == selectedDate) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Date Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(event.type.colorHex).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(event.type.colorHex).copy(alpha = 0.4f)),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val parts = event.date.split("-")
                                val day = parts.getOrNull(2) ?: "01"
                                val monthNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
                                val month = monthShortNames.getOrElse(monthNum - 1) { "NOV" }

                                Text(
                                    text = day,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(event.type.colorHex)
                                )
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 9.sp,
                                    color = Color(event.type.colorHex)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isUrdu) event.urduTitle else event.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(event.type.colorHex).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isUrdu) event.type.urduLabel else event.type.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(event.type.colorHex),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            if (event.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (canEditCalendar) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                DbusEditIconButton(
                                    onClick = { editingEvent = event },
                                    contentDescription = if (isUrdu) "ترمیم" else "Edit Event",
                                    modifier = Modifier.testTag("btn_edit_event_${event.id}")
                                )
                                DbusDeleteIconButton(
                                    onClick = { deletingEvent = event },
                                    contentDescription = if (isUrdu) "حذف" else "Delete Event",
                                    modifier = Modifier.testTag("btn_delete_event_${event.id}")
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Event Detail Dialog (on tap)
    viewingEvent?.let { ev ->
        AlertDialog(
            onDismissRequest = { viewingEvent = null },
            title = {
                Column {
                    Text(text = if (isUrdu) ev.urduTitle else ev.title, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${ev.date} • ${if (isUrdu) ev.type.urduLabel else ev.type.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(ev.type.colorHex)
                    )
                }
            },
            text = {
                Text(
                    text = ev.description.ifBlank { if (isUrdu) "کوئی تفصیل درج نہیں" else "No additional description." },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                if (canEditCalendar) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                viewingEvent = null
                                editingEvent = ev
                            }
                        ) {
                            Text(if (isUrdu) "ترمیم" else "Edit")
                        }
                        Button(onClick = { viewingEvent = null }) {
                            Text(if (isUrdu) "بند کریں" else "Close")
                        }
                    }
                } else {
                    Button(onClick = { viewingEvent = null }) {
                        Text(if (isUrdu) "بند کریں" else "Close")
                    }
                }
            }
        )
    }

    // Add / Edit Calendar Event Dialog
    if (showAddEventDialog || editingEvent != null) {
        val target = editingEvent
        var titleInput by remember(target?.id) { mutableStateOf(target?.title ?: "") }
        var urduTitleInput by remember(target?.id) { mutableStateOf(target?.urduTitle ?: "") }
        var dateInput by remember(target?.id) { mutableStateOf(target?.date ?: selectedDate) }
        var typeInput by remember(target?.id) { mutableStateOf(target?.type ?: CalendarEventType.ASSIGNMENT) }
        var descInput by remember(target?.id) { mutableStateOf(target?.description ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddEventDialog = false
                editingEvent = null
            },
            title = {
                Text(
                    text = if (target != null) {
                        if (isUrdu) "کیلنڈر ایونٹ میں ترمیم" else "Edit Calendar Event"
                    } else {
                        if (isUrdu) "نیا کیلنڈر ایونٹ شامل کریں" else "Add Calendar Event"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text(if (isUrdu) "عنوان (انگریزی)" else "Event Title") },
                            modifier = Modifier.fillMaxWidth().testTag("input_event_title"),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = urduTitleInput,
                            onValueChange = { urduTitleInput = it },
                            label = { Text(if (isUrdu) "عنوان (اردو - اختیاری)" else "Urdu Title (Optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = dateInput,
                            onValueChange = { dateInput = it },
                            label = { Text(if (isUrdu) "تاریخ (YYYY-MM-DD)" else "Date (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth().testTag("input_event_date"),
                            singleLine = true
                        )
                    }
                    item {
                        Text(
                            text = if (isUrdu) "ایونٹ کی قسم:" else "Event Category:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(CalendarEventType.values()) { cat ->
                                FilterChip(
                                    selected = typeInput == cat,
                                    onClick = { typeInput = cat },
                                    label = {
                                        Text(
                                            text = if (isUrdu) cat.urduLabel else cat.label,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    modifier = Modifier.testTag("chip_event_type_${cat.name.lowercase()}")
                                )
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = descInput,
                            onValueChange = { descInput = it },
                            label = { Text(if (isUrdu) "تفصیل" else "Description") },
                            modifier = Modifier.fillMaxWidth().testTag("input_event_desc"),
                            maxLines = 3
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val eventItem = SchoolCalendarEvent(
                            id = target?.id ?: ("cal_" + UUID.randomUUID().toString().take(6)),
                            title = titleInput.ifBlank { typeInput.label },
                            urduTitle = urduTitleInput.ifBlank { titleInput.ifBlank { typeInput.urduLabel } },
                            date = dateInput.ifBlank { selectedDate },
                            type = typeInput,
                            description = descInput.trim()
                        )
                        if (target != null) {
                            repository.updateCalendarEvent(eventItem, currentUser)
                        } else {
                            repository.addCalendarEvent(eventItem, currentUser)
                        }
                        selectedDate = eventItem.date
                        showAddEventDialog = false
                        editingEvent = null
                    },
                    modifier = Modifier.testTag("btn_save_calendar_event")
                ) {
                    Text(if (isUrdu) "محفوظ کریں" else "Save Event")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddEventDialog = false
                        editingEvent = null
                    }
                ) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }

    // Delete Event Confirmation Dialog
    deletingEvent?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingEvent = null },
            title = { Text(if (isUrdu) "ایونٹ حذف کریں؟" else "Delete Event?") },
            text = {
                Text(
                    if (isUrdu) "کیا آپ واقعی '${target.urduTitle}' (${target.date}) حذف کرنا چاہتے ہیں؟"
                    else "Are you sure you want to delete '${target.title}' (${target.date})?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteCalendarEvent(target.id, currentUser)
                        deletingEvent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_delete_event")
                ) {
                    Text(if (isUrdu) "حذف کریں" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEvent = null }) {
                    Text(if (isUrdu) "منسوخ" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun SchoolCalculatorTab(
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val isUrdu = language == AppLanguage.URDU
    var calcSubTab by remember { mutableIntStateOf(0) } // 0: Math, 1: Grades/GPA, 2: Attendance %

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sub-tabs row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(0, "Standard Math", "ریاضی کیلکولیٹر"),
                Triple(1, "Marks & GPA", "گریڈ و جی پی اے"),
                Triple(2, "Attendance %", "حاضری فیصد")
            ).forEach { (idx, en, ur) ->
                val isSel = calcSubTab == idx
                OutlinedButton(
                    onClick = { calcSubTab = idx },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (isUrdu) ur else en,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        when (calcSubTab) {
            0 -> MathCalculatorView(isUrdu)
            1 -> GradeGpaCalculatorView(isUrdu)
            2 -> AttendanceCalculatorView(isUrdu)
        }
    }
}

@Composable
fun MathCalculatorView(isUrdu: Boolean) {
    var expression by remember { mutableStateOf("") }
    var displayResult by remember { mutableStateOf("0") }
    var hasCalculated by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("math_calculator_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Display Screen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = expression.ifBlank { "0" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
                    )
                    Text(
                        text = displayResult,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Keypad
            val buttons = listOf(
                listOf("C", "(", ")", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "-"),
                listOf("1", "2", "3", "+"),
                listOf("0", ".", "%", "=")
            )

            buttons.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { label ->
                        val isOp = label in listOf("÷", "×", "-", "+", "=")
                        val isClear = label == "C"

                        Button(
                            onClick = {
                                when (label) {
                                    "C" -> {
                                        expression = ""
                                        displayResult = "0"
                                        hasCalculated = false
                                    }
                                    "=" -> {
                                        val res = SchoolCalculatorUtil.evaluateExpression(expression)
                                        res.fold(
                                            onSuccess = {
                                                displayResult = if (it % 1.0 == 0.0) it.toLong().toString() else String.format(Locale.US, "%.2f", it)
                                                hasCalculated = true
                                            },
                                            onFailure = {
                                                displayResult = "Error"
                                            }
                                        )
                                    }
                                    else -> {
                                        if (hasCalculated && !isOp) {
                                            expression = label
                                            hasCalculated = false
                                        } else {
                                            expression += label
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("btn_calc_$label"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (label == "=") MaterialTheme.colorScheme.primary
                                else if (isOp) MaterialTheme.colorScheme.primaryContainer
                                else if (isClear) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (label == "=") MaterialTheme.colorScheme.onPrimary
                                else if (isOp) MaterialTheme.colorScheme.onPrimaryContainer
                                else if (isClear) MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GradeGpaCalculatorView(isUrdu: Boolean) {
    var obtainedMarksInput by remember { mutableStateOf("") }
    var totalMarksInput by remember { mutableStateOf("") }
    val obtainedNum = obtainedMarksInput.toDoubleOrNull()
    val totalNum = totalMarksInput.toDoubleOrNull()
    val hasValidInputs = obtainedNum != null && totalNum != null && totalNum > 0.0
    val gradeResult = remember(obtainedNum, totalNum, hasValidInputs) {
        if (hasValidInputs) SchoolCalculatorUtil.calculateGrade(obtainedNum!!, totalNum!!)
        else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grade_calculator_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isUrdu) "پاکستانی بورڈ گریڈنگ و جی پی اے" else "Pakistan Board Grading & GPA Formula",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = obtainedMarksInput,
                    onValueChange = { obtainedMarksInput = it },
                    label = { Text(if (isUrdu) "حاصل کردہ نمبر" else "Obtained Marks") },
                    placeholder = { Text("0") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_calc_obtained"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = totalMarksInput,
                    onValueChange = { totalMarksInput = it },
                    label = { Text(if (isUrdu) "کل نمبر" else "Total Marks") },
                    placeholder = { Text("100") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_calc_total"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            // Result Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = when {
                    gradeResult == null -> MaterialTheme.colorScheme.surfaceVariant
                    gradeResult.isPass -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (gradeResult == null) {
                        Column {
                            Text(
                                text = if (isUrdu) "فیصد: --%" else "Score: --%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isUrdu) "حاصل کردہ اور کل نمبر درج کریں" else "Enter obtained and total marks above",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = if (isUrdu) "فیصد: ${String.format(Locale.US, "%.1f", gradeResult.percentage)}%"
                                else "Score: ${String.format(Locale.US, "%.1f", gradeResult.percentage)}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isUrdu) gradeResult.urduGrade else gradeResult.remarks,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Grade ${gradeResult.gradeLetter}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "GPA: ${gradeResult.gpa}",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceCalculatorView(isUrdu: Boolean) {
    var presentDaysInput by remember { mutableStateOf("") }
    var totalWorkingDaysInput by remember { mutableStateOf("") }
    val presentNum = presentDaysInput.toIntOrNull()
    val totalNum = totalWorkingDaysInput.toIntOrNull()
    val hasValidInputs = presentNum != null && totalNum != null && totalNum > 0
    val calcResult = remember(presentNum, totalNum, hasValidInputs) {
        if (hasValidInputs) SchoolCalculatorUtil.calculateAttendancePercentage(presentNum!!, totalNum!!)
        else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attendance_calculator_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isUrdu) "حاضری تناسب کیلکولیٹر (75% انتباہ)" else "Attendance Rate Calculator (75% Alert Trigger)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = presentDaysInput,
                    onValueChange = { presentDaysInput = it },
                    label = { Text(if (isUrdu) "حاضر ایام" else "Present Days") },
                    placeholder = { Text("0") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = totalWorkingDaysInput,
                    onValueChange = { totalWorkingDaysInput = it },
                    label = { Text(if (isUrdu) "کل تعلیمی ایام" else "Total Days") },
                    placeholder = { Text("0") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = when {
                    calcResult == null -> MaterialTheme.colorScheme.surfaceVariant
                    calcResult.second -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (calcResult == null) {
                        Column {
                            Text(
                                text = if (isUrdu) "حاضری کا تناسب" else "Attendance Rate",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = if (isUrdu) "ایام کی تعداد درج کریں" else "Enter days above",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val (rate, isLow) = calcResult
                        Column {
                            Text(
                                text = if (isUrdu) "حاضری کا تناسب" else "Attendance Rate",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.1f", rate)}%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (isLow) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isUrdu) "انتباہ: 75% سے کم" else "Low (< 75%) Alert",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isUrdu) "تسلی بخش" else "Eligible (>=75%)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
