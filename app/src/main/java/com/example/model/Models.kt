package com.example.model

enum class UserRole(val englishName: String, val urduName: String) {
    SuperAdmin("Super Admin", "سپر ایڈمن"),
    Administrator("School Admin", "اسکول ایڈمن"),
    Teacher("Teacher", "استاد"),
    Parent("Parent", "والدین"),
    Student("Student", "طالب علم");

    fun getDisplayName(isUrdu: Boolean): String {
        return if (isUrdu) urduName else englishName
    }
}

enum class ApprovalStatus(val englishName: String, val urduName: String) {
    Approved("Approved", "منظور شدہ"),
    PendingApproval("Pending Approval", "منظوری کے منتظر"),
    Rejected("Rejected", "مسترد"),
    Archived("Archived", "محفوظ شدہ (آرکائیو)");

    fun getDisplayName(isUrdu: Boolean): String {
        return if (isUrdu) urduName else englishName
    }

    companion object {
        fun fromDbString(value: String): ApprovalStatus = when (value.trim().lowercase()) {
            "approved" -> Approved
            "pending approval", "pendingapproval", "pending" -> PendingApproval
            "rejected" -> Rejected
            "archived" -> Archived
            else -> PendingApproval
        }
    }
}

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val role: UserRole,
    val permanentStudentId: String? = null,
    val approvalStatus: ApprovalStatus = ApprovalStatus.Approved,
    val linkedParentId: String? = null,
    val linkedChildIds: List<String> = emptyList(),
    val assignedStudentIds: List<String> = emptyList(),
    val avatarInitials: String = name.take(2).uppercase(),
    val avatarColorHex: Long = 0xFF1B3B6F,
    val loginId: String = ""
)

data class SchoolAccount(
    val id: String,
    val loginId: String,
    val name: String,
    val email: String,
    val phone: String = "",
    val password: String,
    val role: UserRole,
    val permanentStudentId: String? = null,
    val linkedParentId: String? = null,
    val linkedChildIds: List<String> = emptyList(),
    val assignedStudentIds: List<String> = emptyList(),
    val approvalStatus: ApprovalStatus = ApprovalStatus.Approved,
    val avatarColorHex: Long = 0xFF1B3B6F,
    val isArchived: Boolean = false
) {
    fun toUserProfile(): UserProfile = UserProfile(
        id = id,
        name = name,
        email = email,
        phone = phone,
        role = role,
        permanentStudentId = permanentStudentId,
        approvalStatus = approvalStatus,
        linkedParentId = linkedParentId,
        linkedChildIds = linkedChildIds,
        assignedStudentIds = assignedStudentIds,
        avatarColorHex = avatarColorHex,
        loginId = loginId
    )
}

data class StudentRecord(
    val id: String,
    val permanentStudentId: String, // Immutable, never reused
    val fullName: String,
    val urduName: String,
    val bFormNumber: String, // Pakistan B-Form ID (Sensitive)
    val grade: String,
    val section: String,
    val parentId: String,
    val parentName: String,
    val fatherName: String = parentName, // Father's Name
    val assignedTeacherIds: List<String> = emptyList(),
    val attendanceRate: Int = 0,
    val approvalStatus: ApprovalStatus = ApprovalStatus.Approved,
    val issuedDate: String = "",
    val avatarColorHex: Long = 0xFF1B3B6F,
    val isArchived: Boolean = false, // Archived rather than deleted
    val photoUri: String? = null, // Student photo URI (or null for first-letter avatar)
    val parentMediaConsentGiven: Boolean = true // Requirement 5: Parent consent before child's media is stored
)

/**
 * Requirement 3: The 10 core skills
 * reading, writing, listening, speaking, presentation,
 * confidence, communication, cooperation, creativity, pressure handling.
 */
enum class CoreSkill(
    val englishName: String,
    val urduName: String,
    val description: String
) {
    READING("Reading", "پڑھنا", "Comprehension & text analysis"),
    WRITING("Writing", "لکھنا", "Grammar, structuring & expression"),
    LISTENING("Listening", "سننا", "Active listening & attention"),
    SPEAKING("Speaking", "بولنا", "Pronunciation & verbal clarity"),
    PRESENTATION("Presentation", "پریزنٹیشن", "Public speaking & presentation visuals"),
    CONFIDENCE("Confidence", "خود اعتمادی", "Self-assurance & classroom participation"),
    COMMUNICATION("Communication", "مواصلات", "Clear idea exchange & dialogue"),
    COOPERATION("Cooperation", "تعاون", "Teamwork, empathy & group collaboration"),
    CREATIVITY("Creativity", "تخلیقی صلاحیت", "Original thinking & innovative problem solving"),
    PRESSURE_HANDLING("Pressure Handling", "دباؤ کا سامنا", "Composure during exams & deadlines");

    fun getDisplayName(isUrdu: Boolean): String = if (isUrdu) urduName else englishName
}

/**
 * Requirement 3: Skill Score from 1 to 4:
 * 1 Needs support, 2 Developing, 3 Good, 4 Excellent
 */
enum class SkillLevel(
    val score: Int,
    val englishName: String,
    val urduName: String,
    val colorHex: Long
) {
    NEEDS_SUPPORT(1, "Needs support", "مدد کی ضرورت ہے", 0xFFE53935),
    DEVELOPING(2, "Developing", "بہتری کی جانب", 0xFFFB8C00),
    GOOD(3, "Good", "اچھا", 0xFF1E88E5),
    EXCELLENT(4, "Excellent", "شاندار", 0xFF43A047);

    fun getDisplayName(isUrdu: Boolean): String = if (isUrdu) urduName else englishName

    companion object {
        fun fromScore(score: Int): SkillLevel = when (score) {
            1 -> NEEDS_SUPPORT
            2 -> DEVELOPING
            3 -> GOOD
            4 -> EXCELLENT
            else -> if (score <= 1) NEEDS_SUPPORT else EXCELLENT
        }
    }
}

data class SkillAssessment(
    val id: String,
    val studentPermanentId: String,
    val assessedByTeacherName: String,
    val date: String,
    val scores: Map<CoreSkill, Int>,
    val sharedNotes: Map<CoreSkill, String> = emptyMap(), // Visible to parent and student
    val privateNotes: Map<CoreSkill, String> = emptyMap() // Teacher and Administrator ONLY
) {
    // Backward compatibility helper
    val notes: Map<CoreSkill, String> get() = sharedNotes
}

/**
 * Module 3 of DBUS: Attendance
 * Statuses: Present, Absent, Late, Leave
 */
enum class AttendanceStatus(
    val englishName: String,
    val urduName: String,
    val shortCode: String,
    val colorHex: Long
) {
    PRESENT("Present", "حاضر", "P", 0xFF2E7D32),
    ABSENT("Absent", "غیر حاضر", "A", 0xFFD32F2F),
    LATE("Late", "دیر سے", "L", 0xFFF57C00),
    LEAVE("Leave", "رخصت", "LV", 0xFF1976D2);

    fun getDisplayName(isUrdu: Boolean): String = if (isUrdu) urduName else englishName
}

data class AttendanceChangeLog(
    val timestamp: String,
    val modifiedBy: String,
    val previousStatus: AttendanceStatus,
    val newStatus: AttendanceStatus,
    val note: String = ""
)

data class AttendanceRecord(
    val id: String,
    val studentPermanentId: String, // Linked to permanent Student ID
    val date: String, // YYYY-MM-DD
    val status: AttendanceStatus,
    val note: String = "", // Optional note e.g. reason for leave
    val markedBy: String,
    val lastModified: String = date,
    val changeLog: List<AttendanceChangeLog> = emptyList()
)

data class AttendanceSummary(
    val presentDays: Int,
    val absentDays: Int,
    val lateDays: Int,
    val leaveDays: Int,
    val totalWorkingDays: Int,
    val attendancePercentage: Float
)

data class SchoolHoliday(
    val id: String,
    val name: String,
    val urduName: String,
    val date: String // YYYY-MM-DD
)

enum class AttendanceAlertType {
    ABSENT_OR_LATE,
    LOW_ATTENDANCE
}

data class AttendanceAlert(
    val id: String,
    val studentPermanentId: String,
    val studentName: String,
    val type: AttendanceAlertType,
    val message: String,
    val urduMessage: String,
    val date: String,
    val isRead: Boolean = false
)

enum class ThemeMode(val englishName: String, val urduName: String) {
    Light("Light Mode", "لائٹ موڈ"),
    Dark("Dark Mode", "ڈارک موڈ"),
    Auto("Auto (System)", "خودکار (سسٹم)");

    fun getDisplayName(isUrdu: Boolean): String = if (isUrdu) urduName else englishName
}

enum class ThemePalette(val englishName: String, val urduName: String, val primaryHex: Long) {
    ClassicNavy("Classic Navy", "کلاسک نیوی", 0xFF1B3B6F),
    PakistanGreen("Pakistan Green", "پاکستان گرین", 0xFF01411C),
    RoyalBlue("Royal Blue", "رائل بلیو", 0xFF1D4ED8),
    Teal("Teal", "ٹیل", 0xFF006D77),
    MaroonAndGold("Maroon & Gold", "مرون اور گولڈ", 0xFF7F1D1D);

    fun getDisplayName(isUrdu: Boolean): String = if (isUrdu) urduName else englishName
}

enum class TextScale(val englishName: String, val urduName: String, val scale: Float) {
    Small("Small (85%)", "چھوٹا", 0.85f),
    Medium("Medium (100%)", "درمیانہ (ڈیفالٹ)", 1.0f),
    Large("Large (115%)", "بڑا", 1.15f);

    fun getDisplayName(isUrdu: Boolean): String = if (isUrdu) urduName else englishName
}

enum class AppLanguage(val code: String, val displayName: String, val isRtl: Boolean) {
    ENGLISH("en", "English", false),
    URDU("ur", "اردو", true)
}

enum class MediaType(val englishName: String, val urduName: String) {
    PHOTO("Photo", "تصویر"),
    VIDEO("Video", "ویڈیو"),
    AUDIO("Audio", "صوتی ریکارڈنگ"),
    DOCUMENT("PDF / Document", "دستاویز");

    fun getDisplayName(isUrdu: Boolean): String = if (isUrdu) urduName else englishName
}

data class ClassActivityMedia(
    val id: String,
    val studentPermanentId: String,
    val mediaType: MediaType,
    val fileUri: String,
    val caption: String,
    val activityName: String,
    val teacherName: String,
    val teacherId: String,
    val date: String,
    val fileSizeBytes: Long = 0L,
    val durationSeconds: Int = 0,
    val isCloudStored: Boolean = true
)

data class StudentAssignment(
    val id: String,
    val studentPermanentId: String,
    val title: String,
    val subject: String,
    val dueDate: String,
    val submissionDate: String? = null,
    val mediaType: MediaType? = null,
    val fileUri: String? = null,
    val fileName: String? = null,
    val fileSizeBytes: Long = 0L,
    val isSubmitted: Boolean = false,
    val grade: String? = null
)

/**
 * Activity Module: Subjects Model
 */
data class SubjectItem(
    val id: String,
    val name: String,
    val urduName: String,
    val code: String,
    val grade: String,
    val teacherName: String,
    val weeklyHours: Int = 4,
    val description: String = ""
)

/**
 * Academic Calendar Event Model
 */
data class SchoolCalendarEvent(
    val id: String,
    val title: String,
    val urduTitle: String,
    val date: String,
    val type: CalendarEventType,
    val description: String
)

enum class CalendarEventType(val label: String, val urduLabel: String, val colorHex: Long) {
    ASSIGNMENT("Assignment Due Date", "اسائنمنٹ کی آخری تاریخ", 0xFF1976D2),
    QUIZ("Quiz", "کوئز / ٹیسٹ", 0xFF7B1FA2),
    EXAM("Exam", "امتحان", 0xFFF57C00),
    HOLIDAY("Holiday", "تعطیل", 0xFFD32F2F),
    OTHER("Other", "دیگر", 0xFF388E3C)
}

/**
 * Super Admin & Multi-Institution Support
 */
enum class InstitutionType(val englishName: String, val urduName: String) {
    SCHOOL("School", "اسکول"),
    COLLEGE("College", "کالج"),
    HIGHER_SECONDARY("Higher Secondary", "ہائر سیکنڈری");

    fun getDisplayName(isUrdu: Boolean): String = if (isUrdu) urduName else englishName
}

enum class SubscriptionPlan(
    val englishName: String,
    val urduName: String,
    val maxStudents: Int,
    val pricePerMonthPkr: Int,
    val colorHex: Long
) {
    FREE_TRIAL("Free Trial (30 Days)", "مفت آزمائش", 100, 0, 0xFF0284C7),
    BASIC("Basic Tier", "بنیادی پلان", 500, 15000, 0xFF16A34A),
    STANDARD("Standard Tier", "معیاری پلان", 1500, 35000, 0xFF2563EB),
    PREMIUM("Premium Tier", "پریمیم پلان", 5000, 75000, 0xFF7C3AED),
    CUSTOM("Custom Enterprise", "کسٹم انٹرپرائز", 20000, 150000, 0xFFDC2626);

    fun getDisplayName(isUrdu: Boolean): String = if (isUrdu) urduName else englishName
}

data class InstitutionRecord(
    val id: String,
    val name: String,
    val urduName: String,
    val institutionType: InstitutionType,
    val city: String,
    val adminContactName: String,
    val adminEmail: String,
    val studentCount: Int,
    val teacherCount: Int,
    val plan: SubscriptionPlan,
    val status: ApprovalStatus,
    val registeredDate: String,
    val isIntegrationConnected: Boolean = true,
    val activeSyncMethod: SyncMethod = SyncMethod.CLOUD_PORTAL
)

/**
 * Universal School Integration System ("CONNECT YOUR SCHOOL")
 * 6 Connection Modes:
 * 1. Connect Existing Software
 * 2. Connect Database
 * 3. DBUS Desktop Connector
 * 4. Connect Through API
 * 5. Import Excel / CSV records
 * 6. DBUS School Portal
 */
enum class SyncMethod(val title: String, val urduTitle: String, val description: String) {
    EXISTING_SOFTWARE("Connect Existing Software", "موجودہ سافٹ ویئر منسلک کریں", "Sync with existing school ERP (Fedena, OpenSIS, QuickSchools, etc.)"),
    DATABASE_CONNECTOR("Connect Database", "ڈیٹا بیس سے رابطہ کریں", "Direct secure link to MySQL, PostgreSQL or MS SQL Server"),
    DESKTOP_CONNECTOR("DBUS Desktop Connector", "ڈی بس ڈیسک ٹاپ کنیکٹر", "Lightweight desktop agent for on-premise local school servers"),
    API_ONEROSTER("Connect Through API", "اے پی آئی (API) رابطہ", "Open standard OneRoster / LTI / RESTful endpoint sync"),
    EXCEL_IMPORT("Import Excel / CSV Records", "ایکسل یا سی ایس وی امپورٹ", "Upload spreadsheets with automated column mapping & duplicate checks"),
    CLOUD_PORTAL("DBUS School Portal", "ڈی بس کلاؤڈ پورٹل", "All-in-one native cloud management with real-time sync");

    fun getDisplayName(isUrdu: Boolean): String = if (isUrdu) urduTitle else title
}

data class SyncHistoryLog(
    val id: String,
    val timestamp: String,
    val method: SyncMethod,
    val recordsSynced: Int,
    val duplicatesFound: Int,
    val status: String,
    val initiatedBy: String
)

/**
 * Examination & Academic Performance Models
 */
data class ExamScheduleItem(
    val id: String,
    val examName: String,
    val subject: String,
    val date: String,
    val time: String,
    val room: String = "Hall 1",
    val totalMarks: Int = 100,
    val passingMarks: Int = 40
)

data class StudentExamResult(
    val id: String,
    val studentPermanentId: String,
    val examName: String,
    val term: String,
    val subject: String,
    val marksObtained: Int,
    val totalMarks: Int = 100,
    val grade: String,
    val remarks: String,
    val date: String
)

/**
 * Parent-Teacher Communication Models
 */
data class ParentTeacherAnnouncement(
    val id: String,
    val title: String,
    val urduTitle: String,
    val message: String,
    val urduMessage: String,
    val authorName: String,
    val authorRole: UserRole,
    val targetGrade: String = "All Grades",
    val date: String,
    val comments: List<AnnouncementComment> = emptyList()
)

data class AnnouncementComment(
    val id: String,
    val authorName: String,
    val authorRole: UserRole,
    val text: String,
    val timestamp: String
)

/**
 * Detailed Core Skill Evidence Model
 */
data class SkillEvidenceItem(
    val id: String,
    val skill: CoreSkill,
    val studentPermanentId: String,
    val title: String,
    val description: String,
    val mediaType: MediaType,
    val fileUri: String,
    val scoreOutOfTen: Int,
    val scoreLevel: Int, // 1 to 4
    val recordedDate: String,
    val evaluatedBy: String
)



