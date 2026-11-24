package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.BuildConfig
import com.example.model.*
import com.example.util.MediaUtils
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

data class ActiveDevice(
    val id: String,
    val deviceName: String,
    val location: String,
    val lastActive: String,
    val isCurrent: Boolean
)

class SchoolRepository private constructor() {

    private var prefs: SharedPreferences? = null
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Supabase HTTP Client: uses BuildConfig.SUPABASE_URL & BuildConfig.SUPABASE_ANON_KEY only.
    // Never stores or uses service_role keys. RLS isolates data per authenticated user on server.
    internal val supabaseClient = SupabaseClient()

    val supabaseUrl: String
        get() = supabaseClient.baseUrl

    val supabaseAnonKey: String
        get() = supabaseClient.anonKey

    fun isSupabaseConfigured(): Boolean = supabaseClient.isConfigured()

    fun configureSupabaseForTest(baseUrl: String, anonKey: String) {
        supabaseClient.baseUrl = baseUrl.trim().trimEnd('/')
        supabaseClient.anonKey = anonKey.trim()
    }

    private var currentAccessToken: String = ""
    private var currentRefreshToken: String = ""

    // Auth & Sync UI State
    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    fun clearSyncError() {
        _syncError.value = null
    }

    // Network connectivity state
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }

    fun toggleOnlineStatus() {
        _isOnline.value = !_isOnline.value
    }

    // App Preferences State (persisted locally via SharedPreferences)
    private val _themeMode = MutableStateFlow(ThemeMode.Auto)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _themePalette = MutableStateFlow(ThemePalette.ClassicNavy)
    val themePalette: StateFlow<ThemePalette> = _themePalette.asStateFlow()

    private val _textScale = MutableStateFlow(TextScale.Medium)
    val textScale: StateFlow<TextScale> = _textScale.asStateFlow()

    private val _appLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    // Notifications Preferences
    private val _notifyAssignments = MutableStateFlow(true)
    val notifyAssignments: StateFlow<Boolean> = _notifyAssignments.asStateFlow()

    private val _notifyQuizzes = MutableStateFlow(true)
    val notifyQuizzes: StateFlow<Boolean> = _notifyQuizzes.asStateFlow()

    private val _notifyReports = MutableStateFlow(true)
    val notifyReports: StateFlow<Boolean> = _notifyReports.asStateFlow()

    private val _notifyMessages = MutableStateFlow(true)
    val notifyMessages: StateFlow<Boolean> = _notifyMessages.asStateFlow()

    // Privacy & Security
    private val _twoStepLoginEnabled = MutableStateFlow(false)
    val twoStepLoginEnabled: StateFlow<Boolean> = _twoStepLoginEnabled.asStateFlow()

    private val _activeDevices = MutableStateFlow<List<ActiveDevice>>(emptyList())
    val activeDevices: StateFlow<List<ActiveDevice>> = _activeDevices.asStateFlow()

    // Live Cloud-Synced State (All collections start strictly empty - zero dummy/fake data)
    private val allStudents = mutableListOf<StudentRecord>()

    private val _schoolAccounts = MutableStateFlow<List<SchoolAccount>>(emptyList())
    val schoolAccounts: StateFlow<List<SchoolAccount>> = _schoolAccounts.asStateFlow()

    private val _skillAssessments = MutableStateFlow<Map<String, List<SkillAssessment>>>(emptyMap())
    val skillAssessments: StateFlow<Map<String, List<SkillAssessment>>> = _skillAssessments.asStateFlow()

    private val _weeklyOffDays = MutableStateFlow<Set<Int>>(setOf(Calendar.SUNDAY))
    val weeklyOffDays: StateFlow<Set<Int>> = _weeklyOffDays.asStateFlow()

    private val _schoolHolidays = MutableStateFlow<List<SchoolHoliday>>(emptyList())
    val schoolHolidays: StateFlow<List<SchoolHoliday>> = _schoolHolidays.asStateFlow()

    private val _lowAttendanceThreshold = MutableStateFlow(75)
    val lowAttendanceThreshold: StateFlow<Int> = _lowAttendanceThreshold.asStateFlow()

    private val _attendanceAlerts = MutableStateFlow<List<AttendanceAlert>>(emptyList())
    val attendanceAlerts: StateFlow<List<AttendanceAlert>> = _attendanceAlerts.asStateFlow()

    private val _attendanceRecords = MutableStateFlow<Map<String, List<AttendanceRecord>>>(emptyMap())
    val attendanceRecords: StateFlow<Map<String, List<AttendanceRecord>>> = _attendanceRecords.asStateFlow()

    // Always starts logged out -> Login screen is strictly the first screen on launch
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _studentsList = MutableStateFlow<List<StudentRecord>>(emptyList())
    val studentsList: StateFlow<List<StudentRecord>> = _studentsList.asStateFlow()

    private val _pendingApprovals = MutableStateFlow<List<StudentRecord>>(emptyList())
    val pendingApprovals: StateFlow<List<StudentRecord>> = _pendingApprovals.asStateFlow()

    private val _classActivityMedia = MutableStateFlow<List<ClassActivityMedia>>(emptyList())
    val classActivityMedia: StateFlow<List<ClassActivityMedia>> = _classActivityMedia.asStateFlow()

    private val _studentAssignments = MutableStateFlow<List<StudentAssignment>>(emptyList())
    val studentAssignments: StateFlow<List<StudentAssignment>> = _studentAssignments.asStateFlow()

    private val _subjectsList = MutableStateFlow<List<SubjectItem>>(emptyList())
    val subjectsList: StateFlow<List<SubjectItem>> = _subjectsList.asStateFlow()

    private val _schoolCalendarEvents = MutableStateFlow<List<SchoolCalendarEvent>>(emptyList())
    val schoolCalendarEvents: StateFlow<List<SchoolCalendarEvent>> = _schoolCalendarEvents.asStateFlow()

    private val _institutions = MutableStateFlow<List<InstitutionRecord>>(emptyList())
    val institutions: StateFlow<List<InstitutionRecord>> = _institutions.asStateFlow()

    private val _syncHistoryLogs = MutableStateFlow<List<SyncHistoryLog>>(emptyList())
    val syncHistoryLogs: StateFlow<List<SyncHistoryLog>> = _syncHistoryLogs.asStateFlow()

    private val _examSchedules = MutableStateFlow<List<ExamScheduleItem>>(emptyList())
    val examSchedules: StateFlow<List<ExamScheduleItem>> = _examSchedules.asStateFlow()

    private val _examResults = MutableStateFlow<Map<String, List<StudentExamResult>>>(emptyMap())
    val examResults: StateFlow<Map<String, List<StudentExamResult>>> = _examResults.asStateFlow()

    private val _parentAnnouncements = MutableStateFlow<List<ParentTeacherAnnouncement>>(emptyList())
    val parentAnnouncements: StateFlow<List<ParentTeacherAnnouncement>> = _parentAnnouncements.asStateFlow()

    private val _skillEvidence = MutableStateFlow<Map<String, List<SkillEvidenceItem>>>(emptyMap())
    val skillEvidence: StateFlow<Map<String, List<SkillEvidenceItem>>> = _skillEvidence.asStateFlow()

    // ============================================================
    // DATE UTILITIES
    // ============================================================

    fun todayDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
    fun currentTimestamp(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Calendar.getInstance().time)

    // ============================================================
    // SUPABASE AUTHENTICATION (REAL DATABASE-BACKED)
    // ============================================================

    /**
     * Authenticates using School ID (e.g. A-001, T-001, S-001, P-001), Permanent Student ID,
     * or Email + Password against Supabase Auth & public.profiles.
     * Role is strictly read from public.profiles in Supabase.
     */
    suspend fun loginWithSchoolIdOrEmail(identifier: String, password: String): Result<UserProfile> {
        val trimmed = identifier.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter your School ID or Email."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (!supabaseClient.isConfigured()) {
            return Result.failure(IllegalStateException(supabaseClient.configErrorMessage(_appLanguage.value == AppLanguage.URDU)))
        }
        if (!_isOnline.value) {
            return Result.failure(IllegalStateException("Device is offline. Connect to the internet to sign in."))
        }

        _isAuthLoading.value = true
        return try {
            val resolvedEmail = if (trimmed.contains("@")) {
                trimmed
            } else {
                val emailRes = supabaseClient.resolveLoginEmail(trimmed)
                emailRes.getOrElse { err -> return Result.failure(err) }
            }

            val session = supabaseClient.signInWithPassword(resolvedEmail, password)
                .getOrElse { err -> return Result.failure(err) }

            completeSignInWithSession(session)
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.message ?: "Authentication failed"))
        } finally {
            _isAuthLoading.value = false
        }
    }

    /**
     * Exchanges a Google ID Token (obtained via Jetpack Credential Manager & verified with Firebase Auth)
     * for a Supabase Auth session and loads the user's database role from public.profiles.
     */
    suspend fun loginWithGoogleIdToken(idToken: String): Result<UserProfile> {
        val trimmedToken = idToken.trim()
        if (trimmedToken.isEmpty()) {
            return Result.failure(IllegalArgumentException("Google credential token is empty."))
        }
        if (!supabaseClient.isConfigured()) {
            runCatching { Firebase.auth.signOut() }
            return Result.failure(IllegalStateException(supabaseClient.configErrorMessage(_appLanguage.value == AppLanguage.URDU)))
        }
        if (!_isOnline.value) {
            return Result.failure(IllegalStateException("Device is offline. Connect to the internet to sign in."))
        }

        _isAuthLoading.value = true
        return try {
            val session = supabaseClient.signInWithGoogleIdToken(trimmedToken)
                .getOrElse { err ->
                    runCatching { Firebase.auth.signOut() }
                    return Result.failure(err)
                }
            completeSignInWithSession(session)
        } catch (e: Exception) {
            runCatching { Firebase.auth.signOut() }
            Result.failure(IllegalStateException(e.message ?: "Google Sign-In authentication failed"))
        } finally {
            _isAuthLoading.value = false
        }
    }

    suspend fun requestPhoneOtp(phone: String): Result<Unit> {
        val clean = phone.trim().replace(" ", "").replace("-", "")
        if (clean.length < 10) {
            return Result.failure(IllegalArgumentException("Please enter a valid mobile phone number."))
        }
        if (!supabaseClient.isConfigured()) {
            return Result.failure(IllegalStateException(supabaseClient.configErrorMessage(_appLanguage.value == AppLanguage.URDU)))
        }
        _isAuthLoading.value = true
        return try {
            supabaseClient.requestPhoneOtp(phone)
        } finally {
            _isAuthLoading.value = false
        }
    }

    suspend fun verifyPhoneOtp(phone: String, otp: String): Result<UserProfile> {
        val clean = phone.trim().replace(" ", "").replace("-", "")
        if (clean.length < 10) {
            return Result.failure(IllegalArgumentException("Please enter a valid mobile phone number."))
        }
        if (otp.trim().length != 6) {
            return Result.failure(IllegalArgumentException("Please enter the 6-digit verification code."))
        }
        if (!supabaseClient.isConfigured()) {
            return Result.failure(IllegalStateException(supabaseClient.configErrorMessage(_appLanguage.value == AppLanguage.URDU)))
        }

        _isAuthLoading.value = true
        return try {
            val session = supabaseClient.verifyPhoneOtp(phone, otp)
                .getOrElse { err -> return Result.failure(err) }
            completeSignInWithSession(session)
        } finally {
            _isAuthLoading.value = false
        }
    }

    suspend fun resetPassword(email: String): Result<String> {
        val trimmed = email.trim()
        if (trimmed.isEmpty() || !trimmed.contains("@") || !trimmed.contains(".")) {
            return Result.failure(IllegalArgumentException("Please enter a valid registered email address."))
        }
        if (!supabaseClient.isConfigured()) {
            return Result.failure(IllegalStateException(supabaseClient.configErrorMessage(_appLanguage.value == AppLanguage.URDU)))
        }
        val res = supabaseClient.recoverPassword(trimmed)
        return res.map { "Password reset instructions sent to $trimmed" }
    }

    suspend fun changePassword(oldPass: String, newPass: String, confirmPass: String): Result<Unit> {
        if (oldPass.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter your current password"))
        }
        if (newPass.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters"))
        }
        if (newPass != confirmPass) {
            return Result.failure(IllegalArgumentException("New passwords do not match"))
        }
        val token = currentAccessToken
        val currentEmail = _currentUser.value?.email.orEmpty()
        if (!supabaseClient.isConfigured() || token.isBlank() || currentEmail.isBlank()) {
            return Result.failure(IllegalStateException("Active Supabase session required to change password."))
        }
        // Verify old password cleanly before updating
        val reAuth = supabaseClient.signInWithPassword(currentEmail, oldPass)
        if (reAuth.isFailure) {
            return Result.failure(IllegalArgumentException("Current password is incorrect."))
        }
        val freshToken = reAuth.getOrNull()?.accessToken ?: token
        currentAccessToken = freshToken
        return supabaseClient.updatePassword(freshToken, newPass)
    }

    private suspend fun completeSignInWithSession(session: SupabaseAuthSession): Result<UserProfile> {
        currentAccessToken = session.accessToken
        currentRefreshToken = session.refreshToken

        val profileArr = supabaseClient.restSelect(
            table = "profiles",
            query = "id=eq.${SupabaseClient.encodeQueryParam(session.userId)}&select=*",
            accessToken = session.accessToken
        ).getOrElse { err ->
            currentAccessToken = ""
            currentRefreshToken = ""
            return Result.failure(IllegalStateException("Could not load profile from database: ${err.message}"))
        }

        if (profileArr.length() == 0) {
            currentAccessToken = ""
            currentRefreshToken = ""
            return Result.failure(
                IllegalStateException(
                    "Your login exists in Supabase Auth, but no matching row exists in public.profiles. Contact your DBUS Administrator."
                )
            )
        }

        val obj = profileArr.getJSONObject(0)
        val statusStr = obj.optString("approval_status", "Approved")
        val approvalStatus = ApprovalStatus.fromDbString(statusStr)

        if (approvalStatus == ApprovalStatus.Archived || approvalStatus == ApprovalStatus.Rejected) {
            currentAccessToken = ""
            currentRefreshToken = ""
            return Result.failure(IllegalStateException("This account is ${approvalStatus.englishName} and cannot access DBUS."))
        }
        if (approvalStatus == ApprovalStatus.PendingApproval) {
            currentAccessToken = ""
            currentRefreshToken = ""
            return Result.failure(IllegalStateException("Your account is pending Administrator approval."))
        }

        val role = parseUserRole(obj.optString("role"))
            ?: run {
                currentAccessToken = ""
                currentRefreshToken = ""
                return Result.failure(IllegalStateException("Invalid role assigned in database profile."))
            }

        val userName = obj.optString("name").ifBlank { session.email.substringBefore("@") }
        val userProfile = UserProfile(
            id = obj.optString("id", session.userId),
            loginId = obj.optString("login_id", ""),
            name = userName,
            email = obj.optString("email", session.email),
            phone = obj.optString("phone", ""),
            role = role,
            permanentStudentId = obj.optString("permanent_student_id").takeIf { it.isNotBlank() && it != "null" },
            approvalStatus = approvalStatus,
            linkedParentId = obj.optString("linked_parent_id").takeIf { it.isNotBlank() && it != "null" },
            avatarColorHex = obj.optLong("avatar_color_hex", 0xFF1B3B6F),
            avatarInitials = userName.take(2).uppercase().ifBlank { "DB" }
        )

        _currentUser.value = userProfile
        _activeDevices.value = listOf(
            ActiveDevice(
                id = "current_device",
                deviceName = "${Build.MANUFACTURER.orEmpty()} ${Build.MODEL.orEmpty()}".trim().ifBlank { "Android Device" },
                location = "Active Session",
                lastActive = "Active Now",
                isCurrent = true
            )
        )

        syncAllFromSupabase()
        return Result.success(userProfile)
    }

    fun logout() {
        val tokenToRevoke = currentAccessToken
        currentAccessToken = ""
        currentRefreshToken = ""
        clearAllInMemoryData()
        _currentUser.value = null
        runCatching { Firebase.auth.signOut() }
        if (tokenToRevoke.isNotBlank() && supabaseClient.isConfigured()) {
            repoScope.launch { supabaseClient.signOut(tokenToRevoke) }
        }
    }

    private fun clearAllInMemoryData() {
        allStudents.clear()
        _studentsList.value = emptyList()
        _pendingApprovals.value = emptyList()
        _schoolAccounts.value = emptyList()
        _skillAssessments.value = emptyMap()
        _schoolHolidays.value = emptyList()
        _attendanceAlerts.value = emptyList()
        _attendanceRecords.value = emptyMap()
        _classActivityMedia.value = emptyList()
        _studentAssignments.value = emptyList()
        _subjectsList.value = emptyList()
        _schoolCalendarEvents.value = emptyList()
        _institutions.value = emptyList()
        _syncHistoryLogs.value = emptyList()
        _examSchedules.value = emptyList()
        _examResults.value = emptyMap()
        _parentAnnouncements.value = emptyList()
        _skillEvidence.value = emptyMap()
        _activeDevices.value = emptyList()
        _syncError.value = null
    }

    // ============================================================
    // SUPABASE FULL SYNC (RLS-SCOPED QUERIES)
    // ============================================================

    suspend fun syncAllFromSupabase() {
        val user = _currentUser.value ?: return
        val token = currentAccessToken
        if (!supabaseClient.isConfigured() || token.isBlank() || !_isOnline.value) return

        _isSyncing.value = true
        try {
            // 1. Fetch teacher-student assignments
            val stRows = supabaseClient.restSelect("student_teachers", "select=*", token).getOrNull() ?: JSONArray()
            val teacherByStudent = mutableMapOf<String, MutableList<String>>()
            val studentsByTeacher = mutableMapOf<String, MutableList<String>>()
            for (i in 0 until stRows.length()) {
                val r = stRows.getJSONObject(i)
                val sid = r.optString("student_permanent_id")
                val tid = r.optString("teacher_id")
                if (sid.isNotBlank() && tid.isNotBlank()) {
                    teacherByStudent.getOrPut(sid) { mutableListOf() }.add(tid)
                    studentsByTeacher.getOrPut(tid) { mutableListOf() }.add(sid)
                }
            }

            // 2. Fetch Attendance Records & Logs first so we can compute real attendanceRate
            val attRows = supabaseClient.restSelect("attendance_records", "select=*&order=date.desc", token).getOrNull() ?: JSONArray()
            val logRows = supabaseClient.restSelect("attendance_change_logs", "select=*&order=timestamp.desc", token).getOrNull() ?: JSONArray()
            val logsByRecord = mutableMapOf<String, MutableList<AttendanceChangeLog>>()
            for (i in 0 until logRows.length()) {
                val l = logRows.getJSONObject(i)
                val recId = l.optString("attendance_record_id")
                if (recId.isNotBlank()) {
                    logsByRecord.getOrPut(recId) { mutableListOf() }.add(
                        AttendanceChangeLog(
                            timestamp = l.optString("timestamp").take(16).replace("T", " "),
                            modifiedBy = l.optString("modified_by"),
                            previousStatus = parseAttendanceStatus(l.optString("previous_status")),
                            newStatus = parseAttendanceStatus(l.optString("new_status")),
                            note = l.optString("note")
                        )
                    )
                }
            }

            val attMap = mutableMapOf<String, MutableList<AttendanceRecord>>()
            for (i in 0 until attRows.length()) {
                val r = attRows.getJSONObject(i)
                val recId = r.optString("id")
                val permId = r.optString("student_permanent_id")
                if (recId.isNotBlank() && permId.isNotBlank()) {
                    attMap.getOrPut(permId) { mutableListOf() }.add(
                        AttendanceRecord(
                            id = recId,
                            studentPermanentId = permId,
                            date = r.optString("date"),
                            status = parseAttendanceStatus(r.optString("status")),
                            note = r.optString("note"),
                            markedBy = r.optString("marked_by"),
                            lastModified = r.optString("last_modified"),
                            changeLog = logsByRecord[recId] ?: emptyList()
                        )
                    )
                }
            }
            _attendanceRecords.value = attMap

            // 3. Fetch Students
            val studentRows = supabaseClient.restSelect("students", "select=*&order=full_name.asc", token).getOrNull() ?: JSONArray()
            val loadedStudents = mutableListOf<StudentRecord>()
            val childrenByParent = mutableMapOf<String, MutableList<String>>()
            val permIds = mutableListOf<String>()

            for (i in 0 until studentRows.length()) {
                val s = studentRows.getJSONObject(i)
                val permId = s.optString("permanent_student_id")
                if (permId.isBlank()) continue
                permIds.add(permId)
                val authUid = s.optString("auth_user_id").takeIf { it.isNotBlank() && it != "null" }
                val recordId = authUid ?: permId
                val pId = s.optString("parent_id").takeIf { it.isNotBlank() && it != "null" }.orEmpty()
                if (pId.isNotBlank()) {
                    childrenByParent.getOrPut(pId) { mutableListOf() }.add(recordId)
                }

                val attList = attMap[permId].orEmpty()
                val realRate = if (attList.isEmpty()) 0 else {
                    val presentOrLate = attList.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
                    ((presentOrLate.toFloat() / attList.size.toFloat()) * 100f).toInt().coerceIn(0, 100)
                }

                val rawPhotoUri = s.optString("photo_uri").takeIf { it.isNotBlank() && it != "null" }
                val displayPhoto = if (rawPhotoUri != null && !rawPhotoUri.startsWith("http") && !rawPhotoUri.startsWith("data:") && !rawPhotoUri.startsWith("content:")) {
                    supabaseClient.createSignedStorageUrl("student-media", rawPhotoUri, 3600, token).getOrNull() ?: rawPhotoUri
                } else {
                    rawPhotoUri
                }

                loadedStudents.add(
                    StudentRecord(
                        id = recordId,
                        permanentStudentId = permId,
                        fullName = s.optString("full_name"),
                        urduName = s.optString("urdu_name").ifBlank { s.optString("full_name") },
                        bFormNumber = s.optString("b_form_number"),
                        grade = s.optString("grade"),
                        section = s.optString("section"),
                        parentId = pId,
                        parentName = "",
                        fatherName = s.optString("father_name"),
                        assignedTeacherIds = teacherByStudent[permId] ?: emptyList(),
                        attendanceRate = realRate,
                        approvalStatus = if (s.optBoolean("is_archived", false)) ApprovalStatus.Archived else ApprovalStatus.Approved,
                        issuedDate = s.optString("issued_date", ""),
                        avatarColorHex = s.optLong("avatar_color_hex", 0xFF1B3B6F),
                        isArchived = s.optBoolean("is_archived", false),
                        photoUri = displayPhoto,
                        parentMediaConsentGiven = s.optBoolean("parent_media_consent_given", true)
                    )
                )
            }
            PermanentIdRegistry.syncFromExistingIds(permIds)

            // 4. Fetch Profiles
            val profRows = supabaseClient.restSelect("profiles", "select=*&order=created_at.asc", token).getOrNull() ?: JSONArray()
            val accountsList = mutableListOf<SchoolAccount>()
            val namesById = mutableMapOf<String, String>()
            for (i in 0 until profRows.length()) {
                val p = profRows.getJSONObject(i)
                val pid = p.optString("id")
                val pname = p.optString("name")
                namesById[pid] = pname
                val prole = parseUserRole(p.optString("role")) ?: UserRole.Student
                val permId = p.optString("permanent_student_id").takeIf { it.isNotBlank() && it != "null" }
                val approval = ApprovalStatus.fromDbString(p.optString("approval_status", "Approved"))
                accountsList.add(
                    SchoolAccount(
                        id = pid,
                        loginId = p.optString("login_id", ""),
                        name = pname,
                        email = p.optString("email"),
                        phone = p.optString("phone", ""),
                        password = "", // Passwords stay exclusively inside Supabase Auth
                        role = prole,
                        permanentStudentId = permId,
                        linkedParentId = p.optString("linked_parent_id").takeIf { it.isNotBlank() && it != "null" },
                        linkedChildIds = childrenByParent[pid] ?: emptyList(),
                        assignedStudentIds = studentsByTeacher[pid] ?: emptyList(),
                        approvalStatus = approval,
                        isArchived = approval == ApprovalStatus.Archived,
                        avatarColorHex = p.optLong("avatar_color_hex", 0xFF1B3B6F)
                    )
                )
            }
            _schoolAccounts.value = accountsList

            // Enrich student parent names from profiles & approvalStatus if linked
            val enrichedStudents = loadedStudents.map { st ->
                val pName = namesById[st.parentId] ?: st.fatherName
                val linkedAccount = accountsList.find { it.permanentStudentId == st.permanentStudentId || it.id == st.id }
                st.copy(
                    parentName = pName,
                    approvalStatus = if (st.isArchived) ApprovalStatus.Archived else (linkedAccount?.approvalStatus ?: st.approvalStatus)
                )
            }
            allStudents.clear()
            allStudents.addAll(enrichedStudents)

            // Refresh signed-in user links
            accountsList.find { it.id == user.id }?.let { ownAcc ->
                _currentUser.value = ownAcc.toUserProfile()
            }

            // 5. Skill Assessments + Private Notes (RLS automatically hides private notes from Parents/Students)
            val skillRows = supabaseClient.restSelect("skill_assessments", "select=*&order=assessment_date.desc", token).getOrNull() ?: JSONArray()
            val privRows = supabaseClient.restSelect("skill_assessment_private_notes", "select=*", token).getOrNull() ?: JSONArray()
            val privByAssessment = mutableMapOf<String, MutableMap<CoreSkill, String>>()
            for (i in 0 until privRows.length()) {
                val pn = privRows.getJSONObject(i)
                val aId = pn.optString("assessment_id")
                val skill = runCatching { CoreSkill.valueOf(pn.optString("skill_name")) }.getOrNull()
                if (aId.isNotBlank() && skill != null) {
                    privByAssessment.getOrPut(aId) { mutableMapOf() }[skill] = pn.optString("private_note")
                }
            }

            val skillMap = mutableMapOf<String, MutableList<SkillAssessment>>()
            for (i in 0 until skillRows.length()) {
                val sa = skillRows.getJSONObject(i)
                val aId = sa.optString("id")
                val permId = sa.optString("student_permanent_id")
                val scoresJson = sa.optJSONObject("scores") ?: JSONObject()
                val sharedJson = sa.optJSONObject("shared_notes") ?: JSONObject()

                val scoresMap = mutableMapOf<CoreSkill, Int>()
                val sharedMap = mutableMapOf<CoreSkill, String>()
                CoreSkill.values().forEach { skill ->
                    if (scoresJson.has(skill.name)) {
                        scoresMap[skill] = scoresJson.optInt(skill.name, 0)
                    }
                    val note = sharedJson.optString(skill.name, "")
                    if (note.isNotBlank()) sharedMap[skill] = note
                }

                skillMap.getOrPut(permId) { mutableListOf() }.add(
                    SkillAssessment(
                        id = aId,
                        studentPermanentId = permId,
                        assessedByTeacherName = sa.optString("assessed_by_teacher_name"),
                        date = sa.optString("assessment_date"),
                        scores = scoresMap,
                        sharedNotes = sharedMap,
                        privateNotes = privByAssessment[aId] ?: emptyMap()
                    )
                )
            }
            _skillAssessments.value = skillMap

            // 6. Class Activity Media
            val mediaRows = supabaseClient.restSelect("class_activity_media", "select=*&order=created_at.desc", token).getOrNull() ?: JSONArray()
            val mediaList = mutableListOf<ClassActivityMedia>()
            for (i in 0 until mediaRows.length()) {
                val m = mediaRows.getJSONObject(i)
                val rawPath = m.optString("file_path")
                val displayUri = if (rawPath.isNotBlank() && !rawPath.startsWith("http") && !rawPath.startsWith("data:") && !rawPath.startsWith("content:")) {
                    supabaseClient.createSignedStorageUrl("student-media", rawPath, 3600, token).getOrNull() ?: rawPath
                } else rawPath
                mediaList.add(
                    ClassActivityMedia(
                        id = m.optString("id"),
                        studentPermanentId = m.optString("student_permanent_id"),
                        mediaType = runCatching { MediaType.valueOf(m.optString("media_type")) }.getOrDefault(MediaType.PHOTO),
                        fileUri = displayUri,
                        caption = m.optString("caption"),
                        activityName = m.optString("activity_name"),
                        teacherName = m.optString("teacher_name"),
                        teacherId = m.optString("teacher_id"),
                        date = m.optString("date").ifBlank { m.optString("created_at").take(10) },
                        fileSizeBytes = m.optLong("file_size_bytes", 0L),
                        durationSeconds = m.optInt("duration_seconds", 0)
                    )
                )
            }
            _classActivityMedia.value = mediaList

            // 7. Student Assignments
            val assignRows = supabaseClient.restSelect("student_assignments", "select=*&order=due_date.asc", token).getOrNull() ?: JSONArray()
            val assignList = mutableListOf<StudentAssignment>()
            for (i in 0 until assignRows.length()) {
                val a = assignRows.getJSONObject(i)
                val rawPath = a.optString("file_path").takeIf { it.isNotBlank() && it != "null" }
                val displayUri = if (rawPath != null && !rawPath.startsWith("http") && !rawPath.startsWith("data:") && !rawPath.startsWith("content:")) {
                    supabaseClient.createSignedStorageUrl("assignments", rawPath, 3600, token).getOrNull() ?: rawPath
                } else rawPath
                assignList.add(
                    StudentAssignment(
                        id = a.optString("id"),
                        studentPermanentId = a.optString("student_permanent_id"),
                        title = a.optString("title"),
                        subject = a.optString("subject"),
                        dueDate = a.optString("due_date"),
                        submissionDate = a.optString("submission_date").takeIf { it.isNotBlank() && it != "null" },
                        mediaType = a.optString("media_type").takeIf { it.isNotBlank() && it != "null" }?.let { runCatching { MediaType.valueOf(it) }.getOrNull() },
                        fileUri = displayUri,
                        fileName = a.optString("file_name").takeIf { it.isNotBlank() && it != "null" },
                        fileSizeBytes = a.optLong("file_size_bytes", 0L),
                        isSubmitted = a.optBoolean("is_submitted", false),
                        grade = a.optString("grade").takeIf { it.isNotBlank() && it != "null" }
                    )
                )
            }
            _studentAssignments.value = assignList

            // 8. Subjects
            val subRows = supabaseClient.restSelect("subjects", "select=*&order=name.asc", token).getOrNull() ?: JSONArray()
            val subjects = mutableListOf<SubjectItem>()
            for (i in 0 until subRows.length()) {
                val sb = subRows.getJSONObject(i)
                subjects.add(
                    SubjectItem(
                        id = sb.optString("id"),
                        name = sb.optString("name"),
                        urduName = sb.optString("urdu_name").ifBlank { sb.optString("name") },
                        code = sb.optString("code"),
                        grade = sb.optString("grade"),
                        teacherName = sb.optString("teacher_name"),
                        weeklyHours = sb.optInt("weekly_hours", 4),
                        description = sb.optString("description")
                    )
                )
            }
            _subjectsList.value = subjects

            // 9. Calendar Events
            val calRows = supabaseClient.restSelect("school_calendar_events", "select=*&order=date.asc", token).getOrNull() ?: JSONArray()
            val events = mutableListOf<SchoolCalendarEvent>()
            for (i in 0 until calRows.length()) {
                val ev = calRows.getJSONObject(i)
                events.add(
                    SchoolCalendarEvent(
                        id = ev.optString("id"),
                        title = ev.optString("title"),
                        urduTitle = ev.optString("urdu_title").ifBlank { ev.optString("title") },
                        date = ev.optString("date"),
                        type = runCatching { CalendarEventType.valueOf(ev.optString("event_type")) }.getOrDefault(CalendarEventType.OTHER),
                        description = ev.optString("description")
                    )
                )
            }
            _schoolCalendarEvents.value = events

            // 10. Holidays & Settings & Alerts
            val holRows = supabaseClient.restSelect("school_holidays", "select=*&order=date.asc", token).getOrNull() ?: JSONArray()
            val holidays = mutableListOf<SchoolHoliday>()
            for (i in 0 until holRows.length()) {
                val h = holRows.getJSONObject(i)
                holidays.add(SchoolHoliday(h.optString("id"), h.optString("name"), h.optString("urdu_name"), h.optString("date")))
            }
            _schoolHolidays.value = holidays

            val setRows = supabaseClient.restSelect("school_settings", "id=eq.default&select=*", token).getOrNull() ?: JSONArray()
            if (setRows.length() > 0) {
                val st = setRows.getJSONObject(0)
                _lowAttendanceThreshold.value = st.optInt("low_attendance_threshold", 75)
                val offArr = st.optJSONArray("weekly_off_days")
                if (offArr != null && offArr.length() > 0) {
                    val days = mutableSetOf<Int>()
                    for (j in 0 until offArr.length()) days.add(offArr.getInt(j))
                    _weeklyOffDays.value = days
                }
            }

            val alertRows = supabaseClient.restSelect("attendance_alerts", "select=*&order=date.desc", token).getOrNull() ?: JSONArray()
            val alerts = mutableListOf<AttendanceAlert>()
            for (i in 0 until alertRows.length()) {
                val al = alertRows.getJSONObject(i)
                alerts.add(
                    AttendanceAlert(
                        id = al.optString("id"),
                        studentPermanentId = al.optString("student_permanent_id"),
                        studentName = al.optString("student_name"),
                        type = runCatching { AttendanceAlertType.valueOf(al.optString("alert_type")) }.getOrDefault(AttendanceAlertType.ABSENT_OR_LATE),
                        message = al.optString("message"),
                        urduMessage = al.optString("urdu_message"),
                        date = al.optString("date"),
                        isRead = al.optBoolean("is_read", false)
                    )
                )
            }
            _attendanceAlerts.value = alerts

            // 11. Exam Schedules & Results
            val schRows = supabaseClient.restSelect("exam_schedules", "select=*&order=date.asc", token).getOrNull() ?: JSONArray()
            val schedules = mutableListOf<ExamScheduleItem>()
            for (i in 0 until schRows.length()) {
                val sc = schRows.getJSONObject(i)
                schedules.add(
                    ExamScheduleItem(
                        id = sc.optString("id"),
                        examName = sc.optString("exam_name"),
                        subject = sc.optString("subject"),
                        date = sc.optString("date"),
                        time = sc.optString("time"),
                        room = sc.optString("room"),
                        totalMarks = sc.optInt("total_marks", 100),
                        passingMarks = sc.optInt("passing_marks", 40)
                    )
                )
            }
            _examSchedules.value = schedules

            val resRows = supabaseClient.restSelect("student_exam_results", "select=*&order=date.desc", token).getOrNull() ?: JSONArray()
            val resMap = mutableMapOf<String, MutableList<StudentExamResult>>()
            for (i in 0 until resRows.length()) {
                val r = resRows.getJSONObject(i)
                val pid = r.optString("student_permanent_id")
                resMap.getOrPut(pid) { mutableListOf() }.add(
                    StudentExamResult(
                        id = r.optString("id"),
                        studentPermanentId = pid,
                        examName = r.optString("exam_name"),
                        term = r.optString("term"),
                        subject = r.optString("subject"),
                        marksObtained = r.optInt("marks_obtained", 0),
                        totalMarks = r.optInt("total_marks", 100),
                        grade = r.optString("grade"),
                        remarks = r.optString("remarks"),
                        date = r.optString("date")
                    )
                )
            }
            _examResults.value = resMap

            // 12. Announcements & Comments
            val annRows = supabaseClient.restSelect("parent_announcements", "select=*&order=date.desc", token).getOrNull() ?: JSONArray()
            val comRows = supabaseClient.restSelect("announcement_comments", "select=*&order=created_at.asc", token).getOrNull() ?: JSONArray()
            val commentsByAnn = mutableMapOf<String, MutableList<AnnouncementComment>>()
            for (i in 0 until comRows.length()) {
                val c = comRows.getJSONObject(i)
                val aid = c.optString("announcement_id")
                commentsByAnn.getOrPut(aid) { mutableListOf() }.add(
                    AnnouncementComment(
                        id = c.optString("id"),
                        authorName = c.optString("author_name"),
                        authorRole = parseUserRole(c.optString("author_role")) ?: UserRole.Parent,
                        text = c.optString("text"),
                        timestamp = c.optString("timestamp")
                    )
                )
            }
            val announcements = mutableListOf<ParentTeacherAnnouncement>()
            for (i in 0 until annRows.length()) {
                val a = annRows.getJSONObject(i)
                val aid = a.optString("id")
                announcements.add(
                    ParentTeacherAnnouncement(
                        id = aid,
                        title = a.optString("title"),
                        urduTitle = a.optString("urdu_title").ifBlank { a.optString("title") },
                        message = a.optString("message"),
                        urduMessage = a.optString("urdu_message").ifBlank { a.optString("message") },
                        authorName = a.optString("author_name"),
                        authorRole = parseUserRole(a.optString("author_role")) ?: UserRole.Administrator,
                        targetGrade = a.optString("target_grade", "All Grades"),
                        date = a.optString("date"),
                        comments = commentsByAnn[aid] ?: emptyList()
                    )
                )
            }
            _parentAnnouncements.value = announcements

            // 13. Skill Evidence
            val eviRows = supabaseClient.restSelect("skill_evidence", "select=*&order=recorded_date.desc", token).getOrNull() ?: JSONArray()
            val eviMap = mutableMapOf<String, MutableList<SkillEvidenceItem>>()
            for (i in 0 until eviRows.length()) {
                val e = eviRows.getJSONObject(i)
                val pid = e.optString("student_permanent_id")
                eviMap.getOrPut(pid) { mutableListOf() }.add(
                    SkillEvidenceItem(
                        id = e.optString("id"),
                        skill = runCatching { CoreSkill.valueOf(e.optString("skill")) }.getOrDefault(CoreSkill.READING),
                        studentPermanentId = pid,
                        title = e.optString("title"),
                        description = e.optString("description"),
                        mediaType = runCatching { MediaType.valueOf(e.optString("media_type")) }.getOrDefault(MediaType.DOCUMENT),
                        fileUri = e.optString("file_path"),
                        scoreOutOfTen = e.optInt("score_out_of_ten", 0),
                        scoreLevel = e.optInt("score_level", 0),
                        recordedDate = e.optString("recorded_date"),
                        evaluatedBy = e.optString("evaluated_by")
                    )
                )
            }
            _skillEvidence.value = eviMap

            // 14. Institutions & Sync Logs (Admin/SuperAdmin only; RLS returns 0 rows for others)
            val instRows = supabaseClient.restSelect("institutions", "select=*&order=created_at.desc", token).getOrNull() ?: JSONArray()
            val instList = mutableListOf<InstitutionRecord>()
            for (i in 0 until instRows.length()) {
                val ins = instRows.getJSONObject(i)
                instList.add(
                    InstitutionRecord(
                        id = ins.optString("id"),
                        name = ins.optString("name"),
                        urduName = ins.optString("urdu_name").ifBlank { ins.optString("name") },
                        institutionType = runCatching { InstitutionType.valueOf(ins.optString("institution_type")) }.getOrDefault(InstitutionType.SCHOOL),
                        city = ins.optString("city"),
                        adminContactName = ins.optString("admin_contact_name"),
                        adminEmail = ins.optString("admin_email"),
                        studentCount = ins.optInt("student_count", 0),
                        teacherCount = ins.optInt("teacher_count", 0),
                        plan = runCatching { SubscriptionPlan.valueOf(ins.optString("plan")) }.getOrDefault(SubscriptionPlan.BASIC),
                        status = ApprovalStatus.fromDbString(ins.optString("status", "Approved")),
                        registeredDate = ins.optString("registered_date"),
                        isIntegrationConnected = ins.optBoolean("is_integration_connected", false),
                        activeSyncMethod = runCatching { SyncMethod.valueOf(ins.optString("active_sync_method")) }.getOrDefault(SyncMethod.CLOUD_PORTAL)
                    )
                )
            }
            _institutions.value = instList

            val syncRows = supabaseClient.restSelect("sync_history_logs", "select=*&order=created_at.desc", token).getOrNull() ?: JSONArray()
            val syncList = mutableListOf<SyncHistoryLog>()
            for (i in 0 until syncRows.length()) {
                val sl = syncRows.getJSONObject(i)
                syncList.add(
                    SyncHistoryLog(
                        id = sl.optString("id"),
                        timestamp = sl.optString("timestamp"),
                        method = runCatching { SyncMethod.valueOf(sl.optString("method")) }.getOrDefault(SyncMethod.CLOUD_PORTAL),
                        recordsSynced = sl.optInt("records_synced", 0),
                        duplicatesFound = sl.optInt("duplicates_found", 0),
                        status = sl.optString("status"),
                        initiatedBy = sl.optString("initiated_by")
                    )
                )
            }
            _syncHistoryLogs.value = syncList

            refreshDataForCurrentUser()
        } catch (e: Exception) {
            _syncError.value = "Sync failed: ${e.message ?: "Unknown error"}"
        } finally {
            _isSyncing.value = false
        }
    }

    // ============================================================
    // STORAGE UPLOAD HELPER
    // ============================================================

    suspend fun uploadToPrivateStorage(
        bucket: String,
        studentPermanentId: String,
        fileName: String,
        bytes: ByteArray,
        contentType: String
    ): Result<String> {
        val token = currentAccessToken
        if (!supabaseClient.isConfigured() || token.isBlank()) {
            return Result.failure(IllegalStateException(supabaseClient.configErrorMessage()))
        }
        val safeFileName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val objectPath = "$studentPermanentId/${System.currentTimeMillis()}_$safeFileName"
        val uploaded = supabaseClient.uploadStorageObject(bucket, objectPath, bytes, contentType, token)
        return uploaded.fold(
            onSuccess = { path ->
                val signedUrl = supabaseClient.createSignedStorageUrl(bucket, path, 3600, token)
                Result.success(signedUrl.getOrElse { path })
            },
            onFailure = { err -> Result.failure(err) }
        )
    }

    // ============================================================
    // ROLE-BASED STUDENT VISIBILITY & AUTHORIZATION
    // ============================================================

    fun refreshDataForCurrentUser() {
        val user = _currentUser.value
        if (user == null) {
            _studentsList.value = emptyList()
            _pendingApprovals.value = emptyList()
            return
        }

        val liveAccount = _schoolAccounts.value.find { it.id == user.id }

        when (user.role) {
            UserRole.Parent -> {
                val linkedChildren = ((liveAccount?.linkedChildIds ?: emptyList()) + user.linkedChildIds).toSet()
                _studentsList.value = allStudents.filter {
                    !it.isArchived && (it.parentId == user.id || linkedChildren.contains(it.id) || linkedChildren.contains(it.permanentStudentId))
                }
                _pendingApprovals.value = _studentsList.value.filter {
                    it.approvalStatus == ApprovalStatus.PendingApproval
                }
            }
            UserRole.Teacher -> {
                val assigned = ((liveAccount?.assignedStudentIds ?: emptyList()) + user.assignedStudentIds).toSet()
                _studentsList.value = allStudents.filter {
                    !it.isArchived && (it.assignedTeacherIds.contains(user.id) || assigned.contains(it.id) || assigned.contains(it.permanentStudentId))
                }
                _pendingApprovals.value = emptyList()
            }
            UserRole.Student -> {
                val permId = liveAccount?.permanentStudentId ?: user.permanentStudentId
                _studentsList.value = allStudents.filter {
                    !it.isArchived && (it.id == user.id || (permId != null && it.permanentStudentId == permId))
                }
                _pendingApprovals.value = emptyList()
            }
            UserRole.Administrator, UserRole.SuperAdmin -> {
                _studentsList.value = allStudents.filter { !it.isArchived }
                _pendingApprovals.value = allStudents.filter { it.approvalStatus == ApprovalStatus.PendingApproval && !it.isArchived }
            }
        }
    }

    fun getAllStudentsList(): List<StudentRecord> = allStudents.filter { !it.isArchived }

    fun canUserViewStudent(user: UserProfile?, student: StudentRecord?): Boolean {
        if (user == null || student == null) return false
        if (student.isArchived && user.role != UserRole.Administrator && user.role != UserRole.SuperAdmin) return false
        val liveAccount = _schoolAccounts.value.find { it.id == user.id }
        return when (user.role) {
            UserRole.SuperAdmin, UserRole.Administrator -> true
            UserRole.Teacher -> student.assignedTeacherIds.contains(user.id) ||
                user.assignedStudentIds.contains(student.id) ||
                user.assignedStudentIds.contains(student.permanentStudentId) ||
                liveAccount?.assignedStudentIds?.contains(student.id) == true ||
                liveAccount?.assignedStudentIds?.contains(student.permanentStudentId) == true
            UserRole.Parent -> student.parentId == user.id ||
                user.linkedChildIds.contains(student.id) ||
                user.linkedChildIds.contains(student.permanentStudentId) ||
                liveAccount?.linkedChildIds?.contains(student.id) == true
            UserRole.Student -> (user.id == student.id) ||
                (user.permanentStudentId != null && user.permanentStudentId == student.permanentStudentId)
        }
    }

    fun canUserViewPrivateNotes(user: UserProfile?, student: StudentRecord?): Boolean {
        if (user == null || student == null) return false
        return when (user.role) {
            UserRole.SuperAdmin, UserRole.Administrator -> true
            UserRole.Teacher -> student.assignedTeacherIds.contains(user.id) || user.assignedStudentIds.contains(student.permanentStudentId)
            UserRole.Parent, UserRole.Student -> false
        }
    }

    fun canUserAccessStorage(user: UserProfile?, studentPermanentId: String): Boolean {
        if (user == null) return false
        val student = allStudents.find { it.permanentStudentId == studentPermanentId } ?: return false
        return canUserViewStudent(user, student)
    }

    fun getStudentByPermanentId(permanentId: String): StudentRecord? =
        allStudents.find { it.permanentStudentId == permanentId }

    fun getStudentByPermanentId(permanentId: String, requestingUser: UserProfile?): StudentRecord? {
        val student = allStudents.find { it.permanentStudentId == permanentId } ?: return null
        if (requestingUser != null && !canUserViewStudent(requestingUser, student)) return null
        return student
    }

    fun canUserEditSkills(user: UserProfile?, student: StudentRecord): Boolean {
        if (user == null) return false
        return when (user.role) {
            UserRole.SuperAdmin, UserRole.Administrator -> true
            UserRole.Teacher -> student.assignedTeacherIds.contains(user.id) || user.assignedStudentIds.contains(student.permanentStudentId)
            UserRole.Parent, UserRole.Student -> false
        }
    }

    fun canUserEditStudent(user: UserProfile?, student: StudentRecord? = null): Boolean {
        if (user == null) return false
        if (user.role == UserRole.Administrator || user.role == UserRole.SuperAdmin) return true
        if (student == null) return user.role == UserRole.Teacher || user.role == UserRole.Parent
        return when (user.role) {
            UserRole.SuperAdmin, UserRole.Administrator -> true
            UserRole.Teacher -> student.assignedTeacherIds.contains(user.id) || user.assignedStudentIds.contains(student.permanentStudentId)
            UserRole.Parent -> student.parentId == user.id || user.linkedChildIds.contains(student.id)
            UserRole.Student -> false
        }
    }

    fun canUserUpdateStudentPhoto(user: UserProfile?, student: StudentRecord): Boolean {
        if (user == null) return false
        return when (user.role) {
            UserRole.SuperAdmin, UserRole.Administrator -> true
            UserRole.Teacher -> student.assignedTeacherIds.contains(user.id) || user.assignedStudentIds.contains(student.permanentStudentId)
            UserRole.Parent -> student.parentId == user.id || user.linkedChildIds.contains(student.id)
            UserRole.Student -> false
        }
    }

    fun canUserDeleteStudent(user: UserProfile?): Boolean =
        user != null && (user.role == UserRole.Administrator || user.role == UserRole.SuperAdmin)

    fun getMaskedBForm(bForm: String, viewerRole: UserRole): String? {
        if (viewerRole != UserRole.Administrator && viewerRole != UserRole.SuperAdmin) return null
        val digitsOnly = bForm.filter { it.isDigit() }
        if (digitsOnly.isEmpty()) return "Not provided"
        val last4 = digitsOnly.takeLast(4)
        return "••••-•••••••-$last4"
    }

    // ============================================================
    // INSTITUTION & UNIVERSAL SYNC
    // ============================================================

    fun addInstitution(record: InstitutionRecord) {
        _institutions.value = _institutions.value + record
        syncUpsert("institutions", JSONObject().apply {
            put("id", record.id)
            put("name", record.name)
            put("urdu_name", record.urduName)
            put("institution_type", record.institutionType.name)
            put("city", record.city)
            put("admin_contact_name", record.adminContactName)
            put("admin_email", record.adminEmail)
            put("student_count", record.studentCount)
            put("teacher_count", record.teacherCount)
            put("plan", record.plan.name)
            put("status", record.status.englishName)
            put("registered_date", record.registeredDate.ifBlank { todayDate() })
            put("is_integration_connected", record.isIntegrationConnected)
            put("active_sync_method", record.activeSyncMethod.name)
        }, "id")
    }

    fun updateInstitutionPlan(institutionId: String, plan: SubscriptionPlan) {
        _institutions.value = _institutions.value.map {
            if (it.id == institutionId) it.copy(plan = plan) else it
        }
        syncPatch("institutions", "id=eq.${SupabaseClient.encodeQueryParam(institutionId)}", JSONObject().apply {
            put("plan", plan.name)
        })
    }

    fun approveInstitution(institutionId: String) {
        _institutions.value = _institutions.value.map {
            if (it.id == institutionId) it.copy(status = ApprovalStatus.Approved) else it
        }
        syncPatch("institutions", "id=eq.${SupabaseClient.encodeQueryParam(institutionId)}", JSONObject().apply {
            put("status", "Approved")
        })
    }

    fun connectSchoolIntegration(method: SyncMethod, endpointOrDetails: String): SyncHistoryLog {
        val newLog = SyncHistoryLog(
            id = "sync_" + UUID.randomUUID().toString().take(8),
            timestamp = currentTimestamp(),
            method = method,
            recordsSynced = allStudents.size,
            duplicatesFound = 0,
            status = "Connected: ${endpointOrDetails.take(40)}",
            initiatedBy = _currentUser.value?.name ?: "Administrator"
        )
        _syncHistoryLogs.value = listOf(newLog) + _syncHistoryLogs.value
        syncUpsert("sync_history_logs", JSONObject().apply {
            put("id", newLog.id)
            put("timestamp", newLog.timestamp)
            put("method", newLog.method.name)
            put("records_synced", newLog.recordsSynced)
            put("duplicates_found", newLog.duplicatesFound)
            put("status", newLog.status)
            put("initiated_by", newLog.initiatedBy)
        }, "id")
        return newLog
    }

    fun importExcelStudentRecords(csvText: String): Pair<Int, Int> {
        val lines = csvText.lines().filter { it.isNotBlank() }
        var imported = 0
        var duplicates = 0
        val actingName = _currentUser.value?.name ?: "Administrator"
        val today = todayDate()

        lines.forEachIndexed { index, line ->
            if (index == 0 && (line.contains("Name", ignoreCase = true) || line.contains("ID", ignoreCase = true))) {
                return@forEachIndexed
            }
            val parts = line.split(",").map { it.trim() }
            if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                val name = parts[0]
                val bFormOrId = if (parts.size > 1) parts[1] else ""
                val father = if (parts.size > 2 && parts[2].isNotBlank()) parts[2] else ""
                val grade = if (parts.size > 3 && parts[3].isNotBlank()) parts[3] else "Grade 9"
                val section = if (parts.size > 4 && parts[4].isNotBlank()) parts[4] else "Section A"

                val isDuplicate = allStudents.any {
                    it.fullName.equals(name, ignoreCase = true) &&
                        (bFormOrId.isBlank() || it.bFormNumber == bFormOrId)
                }

                if (isDuplicate) {
                    duplicates++
                } else {
                    registerNewStudent(
                        name = name,
                        fatherName = father,
                        bForm = bFormOrId,
                        grade = grade,
                        section = section
                    )
                    imported++
                }
            }
        }
        refreshDataForCurrentUser()

        val log = SyncHistoryLog(
            id = "sync_" + UUID.randomUUID().toString().take(8),
            timestamp = currentTimestamp(),
            method = SyncMethod.EXCEL_IMPORT,
            recordsSynced = imported,
            duplicatesFound = duplicates,
            status = if (imported > 0) "Import completed ($imported added, $duplicates duplicates skipped)" else "No new records imported",
            initiatedBy = actingName
        )
        _syncHistoryLogs.value = listOf(log) + _syncHistoryLogs.value
        syncUpsert("sync_history_logs", JSONObject().apply {
            put("id", log.id)
            put("timestamp", log.timestamp)
            put("method", log.method.name)
            put("records_synced", log.recordsSynced)
            put("duplicates_found", log.duplicatesFound)
            put("status", log.status)
            put("initiated_by", log.initiatedBy)
        }, "id")
        return Pair(imported, duplicates)
    }

    // ============================================================
    // EXAMINATIONS, CALENDAR, SUBJECTS, ANNOUNCEMENTS, EVIDENCE
    // ============================================================

    fun addExamSchedule(schedule: ExamScheduleItem, user: UserProfile? = _currentUser.value): Result<ExamScheduleItem> {
        if (user?.role !in listOf(UserRole.Administrator, UserRole.SuperAdmin, UserRole.Teacher)) {
            return Result.failure(IllegalAccessException("Only teachers or administrators can add exam schedules."))
        }
        _examSchedules.value = _examSchedules.value + schedule
        syncUpsert("exam_schedules", JSONObject().apply {
            put("id", schedule.id)
            put("exam_name", schedule.examName)
            put("subject", schedule.subject)
            put("date", schedule.date)
            put("time", schedule.time)
            put("room", schedule.room)
            put("total_marks", schedule.totalMarks)
            put("passing_marks", schedule.passingMarks)
        }, "id")
        return Result.success(schedule)
    }

    fun updateExamSchedule(schedule: ExamScheduleItem, user: UserProfile? = _currentUser.value): Result<ExamScheduleItem> {
        if (user?.role !in listOf(UserRole.Administrator, UserRole.SuperAdmin, UserRole.Teacher)) {
            return Result.failure(IllegalAccessException("Only teachers or administrators can edit exam schedules."))
        }
        val list = _examSchedules.value.toMutableList()
        val idx = list.indexOfFirst { it.id == schedule.id }
        if (idx == -1) return Result.failure(IllegalArgumentException("Exam schedule not found"))
        list[idx] = schedule
        _examSchedules.value = list
        syncUpsert("exam_schedules", JSONObject().apply {
            put("id", schedule.id)
            put("exam_name", schedule.examName)
            put("subject", schedule.subject)
            put("date", schedule.date)
            put("time", schedule.time)
            put("room", schedule.room)
            put("total_marks", schedule.totalMarks)
            put("passing_marks", schedule.passingMarks)
        }, "id")
        return Result.success(schedule)
    }

    fun deleteExamSchedule(scheduleId: String, user: UserProfile? = _currentUser.value): Result<Boolean> {
        if (user?.role != UserRole.Administrator && user?.role != UserRole.SuperAdmin) {
            return Result.failure(IllegalAccessException("Only administrators can delete exam schedules."))
        }
        val removed = _examSchedules.value.any { it.id == scheduleId }
        if (!removed) return Result.failure(IllegalArgumentException("Exam schedule not found"))
        _examSchedules.value = _examSchedules.value.filterNot { it.id == scheduleId }
        syncDelete("exam_schedules", "id=eq.${SupabaseClient.encodeQueryParam(scheduleId)}")
        return Result.success(true)
    }

    fun recordExamResult(result: StudentExamResult) {
        val currentMap = _examResults.value.toMutableMap()
        val list = currentMap[result.studentPermanentId]?.toMutableList() ?: mutableListOf()
        val existingIndex = list.indexOfFirst {
            it.id == result.id || (it.subject == result.subject && it.term == result.term)
        }
        if (existingIndex != -1) list[existingIndex] = result else list.add(result)
        currentMap[result.studentPermanentId] = list
        _examResults.value = currentMap

        syncUpsert("student_exam_results", JSONObject().apply {
            put("id", result.id)
            put("student_permanent_id", result.studentPermanentId)
            put("exam_name", result.examName)
            put("term", result.term)
            put("subject", result.subject)
            put("marks_obtained", result.marksObtained)
            put("total_marks", result.totalMarks)
            put("grade", result.grade)
            put("remarks", result.remarks)
            put("date", result.date.ifBlank { todayDate() })
        }, "id")
    }

    fun deleteExamResult(studentPermanentId: String, resultId: String, user: UserProfile? = _currentUser.value): Result<Boolean> {
        if (user?.role !in listOf(UserRole.Administrator, UserRole.SuperAdmin, UserRole.Teacher)) {
            return Result.failure(IllegalAccessException("Only teachers or administrators can delete exam results."))
        }
        val currentMap = _examResults.value.toMutableMap()
        val list = currentMap[studentPermanentId]?.toMutableList()
            ?: return Result.failure(IllegalArgumentException("Exam result not found"))
        val removed = list.removeAll { it.id == resultId }
        if (!removed) return Result.failure(IllegalArgumentException("Exam result not found"))
        currentMap[studentPermanentId] = list
        _examResults.value = currentMap
        syncDelete("student_exam_results", "id=eq.${SupabaseClient.encodeQueryParam(resultId)}")
        return Result.success(true)
    }

    fun getExamResultsForStudent(permanentId: String): List<StudentExamResult> =
        _examResults.value[permanentId] ?: emptyList()

    fun addCalendarEvent(event: SchoolCalendarEvent, user: UserProfile? = _currentUser.value): Result<SchoolCalendarEvent> {
        if (user != null && user.role !in listOf(UserRole.Administrator, UserRole.SuperAdmin, UserRole.Teacher)) {
            return Result.failure(IllegalAccessException("Only teachers or administrators can add calendar events."))
        }
        _schoolCalendarEvents.value = (_schoolCalendarEvents.value + event).sortedBy { it.date }
        syncUpsert("school_calendar_events", JSONObject().apply {
            put("id", event.id)
            put("title", event.title)
            put("urdu_title", event.urduTitle)
            put("date", event.date)
            put("event_type", event.type.name)
            put("description", event.description)
            _currentUser.value?.id?.takeIf { it.isValidUuid() }?.let { put("created_by", it) }
        }, "id")
        return Result.success(event)
    }

    fun updateCalendarEvent(event: SchoolCalendarEvent, user: UserProfile? = _currentUser.value): Result<SchoolCalendarEvent> {
        if (user != null && user.role !in listOf(UserRole.Administrator, UserRole.SuperAdmin, UserRole.Teacher)) {
            return Result.failure(IllegalAccessException("Only teachers or administrators can edit calendar events."))
        }
        val list = _schoolCalendarEvents.value.toMutableList()
        val idx = list.indexOfFirst { it.id == event.id }
        if (idx == -1) return Result.failure(IllegalArgumentException("Calendar event not found"))
        list[idx] = event
        _schoolCalendarEvents.value = list.sortedBy { it.date }
        syncUpsert("school_calendar_events", JSONObject().apply {
            put("id", event.id)
            put("title", event.title)
            put("urdu_title", event.urduTitle)
            put("date", event.date)
            put("event_type", event.type.name)
            put("description", event.description)
        }, "id")
        return Result.success(event)
    }

    fun deleteCalendarEvent(eventId: String, user: UserProfile? = _currentUser.value): Result<Boolean> {
        if (user != null && user.role !in listOf(UserRole.Administrator, UserRole.SuperAdmin, UserRole.Teacher)) {
            return Result.failure(IllegalAccessException("Only teachers or administrators can delete calendar events."))
        }
        val exists = _schoolCalendarEvents.value.any { it.id == eventId }
        if (!exists) return Result.failure(IllegalArgumentException("Calendar event not found"))
        _schoolCalendarEvents.value = _schoolCalendarEvents.value.filterNot { it.id == eventId }
        syncDelete("school_calendar_events", "id=eq.${SupabaseClient.encodeQueryParam(eventId)}")
        return Result.success(true)
    }

    fun addAnnouncement(announcement: ParentTeacherAnnouncement) {
        _parentAnnouncements.value = listOf(announcement) + _parentAnnouncements.value
        syncUpsert("parent_announcements", JSONObject().apply {
            put("id", announcement.id)
            put("title", announcement.title)
            put("urdu_title", announcement.urduTitle)
            put("message", announcement.message)
            put("urdu_message", announcement.urduMessage)
            put("author_name", announcement.authorName)
            put("author_role", announcement.authorRole.name)
            put("target_grade", announcement.targetGrade)
            put("date", announcement.date.ifBlank { todayDate() })
        }, "id")
    }

    fun addAnnouncementComment(announcementId: String, comment: AnnouncementComment) {
        _parentAnnouncements.value = _parentAnnouncements.value.map { ann ->
            if (ann.id == announcementId) ann.copy(comments = ann.comments + comment) else ann
        }
        syncUpsert("announcement_comments", JSONObject().apply {
            put("id", comment.id)
            put("announcement_id", announcementId)
            _currentUser.value?.id?.takeIf { it.isValidUuid() }?.let { put("author_id", it) }
            put("author_name", comment.authorName)
            put("author_role", comment.authorRole.name)
            put("text", comment.text)
            put("timestamp", comment.timestamp)
        }, "id")
    }

    fun addSkillEvidence(evidence: SkillEvidenceItem) {
        val currentMap = _skillEvidence.value.toMutableMap()
        val list = currentMap[evidence.studentPermanentId]?.toMutableList() ?: mutableListOf()
        list.add(0, evidence)
        currentMap[evidence.studentPermanentId] = list
        _skillEvidence.value = currentMap

        syncUpsert("skill_evidence", JSONObject().apply {
            put("id", evidence.id)
            put("skill", evidence.skill.name)
            put("student_permanent_id", evidence.studentPermanentId)
            put("title", evidence.title)
            put("description", evidence.description)
            put("media_type", evidence.mediaType.name)
            put("file_path", evidence.fileUri)
            put("score_out_of_ten", evidence.scoreOutOfTen)
            put("score_level", evidence.scoreLevel)
            put("recorded_date", evidence.recordedDate.ifBlank { todayDate() })
            put("evaluated_by", evidence.evaluatedBy)
        }, "id")
    }

    fun getEvidenceForSkill(studentPermanentId: String, skill: CoreSkill): List<SkillEvidenceItem> =
        (_skillEvidence.value[studentPermanentId] ?: emptyList()).filter { it.skill == skill }

    // ============================================================
    // 10 CORE SKILLS & PRIVATE / SHARED NOTES
    // ============================================================

    fun saveSkillAssessment(
        studentPermanentId: String,
        teacherName: String,
        scores: Map<CoreSkill, Int>,
        sharedNotes: Map<CoreSkill, String> = emptyMap(),
        privateNotes: Map<CoreSkill, String> = emptyMap(),
        date: String = todayDate()
    ) {
        val newId = "eval_" + UUID.randomUUID().toString().take(8)
        val newAssessment = SkillAssessment(
            id = newId,
            studentPermanentId = studentPermanentId,
            assessedByTeacherName = teacherName,
            date = date,
            scores = scores,
            sharedNotes = sharedNotes,
            privateNotes = privateNotes
        )

        val currentMap = _skillAssessments.value.toMutableMap()
        val existingHistory = currentMap[studentPermanentId]?.toMutableList() ?: mutableListOf()
        existingHistory.add(0, newAssessment)
        currentMap[studentPermanentId] = existingHistory
        _skillAssessments.value = currentMap

        persistAssessmentToSupabase(newAssessment)
    }

    fun saveSkillAssessment(
        studentPermanentId: String,
        teacherName: String,
        scores: Map<CoreSkill, Int>,
        notes: Map<CoreSkill, String>,
        date: String = todayDate()
    ) {
        saveSkillAssessment(studentPermanentId, teacherName, scores, notes, emptyMap(), date)
    }

    private fun persistAssessmentToSupabase(assessment: SkillAssessment) {
        val token = currentAccessToken
        val actingId = _currentUser.value?.id
        if (!supabaseClient.isConfigured() || token.isBlank() || !_isOnline.value) return

        repoScope.launch {
            val scoresObj = JSONObject()
            assessment.scores.forEach { (k, v) -> scoresObj.put(k.name, v) }
            val sharedObj = JSONObject()
            assessment.sharedNotes.forEach { (k, v) -> if (v.isNotBlank()) sharedObj.put(k.name, v) }

            supabaseClient.restUpsert(
                "skill_assessments",
                JSONObject().apply {
                    put("id", assessment.id)
                    put("student_permanent_id", assessment.studentPermanentId)
                    if (actingId != null && actingId.isValidUuid()) put("assessed_by_teacher_id", actingId)
                    put("assessed_by_teacher_name", assessment.assessedByTeacherName)
                    put("assessment_date", assessment.date)
                    put("scores", scoresObj)
                    put("shared_notes", sharedObj)
                },
                "id",
                token
            )

            // Persist private notes into the strictly isolated faculty table
            if (actingId != null && actingId.isValidUuid()) {
                assessment.privateNotes.forEach { (skill, note) ->
                    if (note.isNotBlank()) {
                        supabaseClient.restUpsert(
                            "skill_assessment_private_notes",
                            JSONObject().apply {
                                put("assessment_id", assessment.id)
                                put("student_permanent_id", assessment.studentPermanentId)
                                put("skill_name", skill.name)
                                put("private_note", note)
                                put("created_by", actingId)
                            },
                            "assessment_id,skill_name",
                            token
                        )
                    } else {
                        supabaseClient.restDelete(
                            "skill_assessment_private_notes",
                            "assessment_id=eq.${SupabaseClient.encodeQueryParam(assessment.id)}&skill_name=eq.${skill.name}",
                            token
                        )
                    }
                }
            }
        }
    }

    fun editSkillAssessment(
        assessmentId: String,
        studentPermanentId: String,
        scores: Map<CoreSkill, Int>,
        sharedNotes: Map<CoreSkill, String>,
        privateNotes: Map<CoreSkill, String>,
        editor: UserProfile
    ): Result<SkillAssessment> {
        val student = getStudentByPermanentId(studentPermanentId)
            ?: return Result.failure(IllegalArgumentException("Student record not found"))
        if (!canUserEditSkills(editor, student)) {
            return Result.failure(IllegalAccessException("Only assigned teachers or administrators can edit skill assessments."))
        }
        val currentMap = _skillAssessments.value.toMutableMap()
        val history = currentMap[studentPermanentId]?.toMutableList() ?: mutableListOf()
        val index = history.indexOfFirst { it.id == assessmentId }
        if (index == -1) return Result.failure(IllegalArgumentException("Assessment record not found"))

        val updated = history[index].copy(scores = scores, sharedNotes = sharedNotes, privateNotes = privateNotes)
        history[index] = updated
        currentMap[studentPermanentId] = history
        _skillAssessments.value = currentMap

        persistAssessmentToSupabase(updated)
        return Result.success(updated)
    }

    fun deleteSkillAssessment(
        assessmentId: String,
        studentPermanentId: String,
        user: UserProfile
    ): Result<Boolean> {
        val student = getStudentByPermanentId(studentPermanentId)
            ?: return Result.failure(IllegalArgumentException("Student record not found"))
        if (user.role != UserRole.Administrator && user.role != UserRole.SuperAdmin &&
            (user.role != UserRole.Teacher || !canUserEditSkills(user, student))
        ) {
            return Result.failure(IllegalAccessException("Only assigned teachers or administrators can delete skill evaluations."))
        }
        val currentMap = _skillAssessments.value.toMutableMap()
        val history = currentMap[studentPermanentId]?.toMutableList() ?: mutableListOf()
        val removed = history.removeAll { it.id == assessmentId }
        if (!removed) return Result.failure(IllegalArgumentException("Assessment not found"))
        currentMap[studentPermanentId] = history
        _skillAssessments.value = currentMap
        syncDelete("skill_assessments", "id=eq.${SupabaseClient.encodeQueryParam(assessmentId)}")
        return Result.success(true)
    }

    fun getSkillHistory(studentPermanentId: String): List<SkillAssessment> =
        _skillAssessments.value[studentPermanentId] ?: emptyList()

    fun getSkillHistory(studentPermanentId: String, requestingUser: UserProfile?): List<SkillAssessment> {
        val rawList = _skillAssessments.value[studentPermanentId] ?: emptyList()
        if (requestingUser == null) return emptyList()
        val student = allStudents.find { it.permanentStudentId == studentPermanentId } ?: return emptyList()
        if (!canUserViewStudent(requestingUser, student)) return emptyList()
        return if (canUserViewPrivateNotes(requestingUser, student)) rawList
        else rawList.map { it.copy(privateNotes = emptyMap()) }
    }

    fun getLatestAssessment(studentPermanentId: String): SkillAssessment? =
        _skillAssessments.value[studentPermanentId]?.firstOrNull()

    fun getLatestAssessment(studentPermanentId: String, requestingUser: UserProfile?): SkillAssessment? =
        getSkillHistory(studentPermanentId, requestingUser).firstOrNull()

    // ============================================================
    // STUDENT RECORD CRUD
    // ============================================================

    fun updateStudent(
        permanentId: String,
        name: String,
        fatherName: String,
        grade: String,
        section: String,
        avatarColorHex: Long,
        photoUri: String? = null,
        parentMediaConsentGiven: Boolean? = null
    ): Result<StudentRecord> {
        val index = allStudents.indexOfFirst { it.permanentStudentId == permanentId }
        if (index == -1) return Result.failure(IllegalArgumentException("Student not found"))
        val current = allStudents[index]
        val updated = current.copy(
            fullName = name.ifBlank { current.fullName },
            urduName = name.ifBlank { current.urduName },
            fatherName = fatherName.ifBlank { current.fatherName },
            parentName = fatherName.ifBlank { current.parentName },
            grade = grade.ifBlank { current.grade },
            section = section.ifBlank { current.section },
            avatarColorHex = avatarColorHex,
            photoUri = photoUri ?: current.photoUri,
            parentMediaConsentGiven = parentMediaConsentGiven ?: current.parentMediaConsentGiven
        )
        allStudents[index] = updated
        refreshDataForCurrentUser()

        syncPatch(
            "students",
            "permanent_student_id=eq.${SupabaseClient.encodeQueryParam(permanentId)}",
            JSONObject().apply {
                put("full_name", updated.fullName)
                put("urdu_name", updated.urduName)
                put("father_name", updated.fatherName)
                put("grade", updated.grade)
                put("section", updated.section)
                put("avatar_color_hex", updated.avatarColorHex)
                if (updated.photoUri != null) put("photo_uri", updated.photoUri)
                put("parent_media_consent_given", updated.parentMediaConsentGiven)
            }
        )
        return Result.success(updated)
    }

    fun updateStudentPhoto(
        permanentId: String,
        photoUri: String?,
        updatedByUser: UserProfile
    ): Result<StudentRecord> {
        val index = allStudents.indexOfFirst { it.permanentStudentId == permanentId }
        if (index == -1) return Result.failure(IllegalArgumentException("Student not found"))
        val current = allStudents[index]
        if (!canUserUpdateStudentPhoto(updatedByUser, current)) {
            return Result.failure(IllegalAccessException("Only Administrator, assigned Teacher, or Parent (own child) can update student photo."))
        }
        if (!photoUri.isNullOrBlank() && !current.parentMediaConsentGiven) {
            return Result.failure(IllegalStateException("Parental consent is required before saving or storing student photo."))
        }
        val updated = current.copy(photoUri = photoUri)
        allStudents[index] = updated
        refreshDataForCurrentUser()
        syncPatch(
            "students",
            "permanent_student_id=eq.${SupabaseClient.encodeQueryParam(permanentId)}",
            JSONObject().apply { put("photo_uri", photoUri ?: JSONObject.NULL) }
        )
        return Result.success(updated)
    }

    fun updateParentMediaConsent(
        permanentId: String,
        consentGiven: Boolean,
        updatedByUser: UserProfile
    ): Result<StudentRecord> {
        val index = allStudents.indexOfFirst { it.permanentStudentId == permanentId }
        if (index == -1) return Result.failure(IllegalArgumentException("Student not found"))
        val current = allStudents[index]
        val canModify = updatedByUser.role == UserRole.Administrator || updatedByUser.role == UserRole.SuperAdmin ||
            (updatedByUser.role == UserRole.Parent && (current.parentId == updatedByUser.id || updatedByUser.linkedChildIds.contains(current.id)))
        if (!canModify) {
            return Result.failure(IllegalAccessException("Only the child's Parent or Administrator can update media consent."))
        }
        val updated = current.copy(parentMediaConsentGiven = consentGiven)
        allStudents[index] = updated
        refreshDataForCurrentUser()
        syncPatch(
            "students",
            "permanent_student_id=eq.${SupabaseClient.encodeQueryParam(permanentId)}",
            JSONObject().apply { put("parent_media_consent_given", consentGiven) }
        )
        return Result.success(updated)
    }

    fun archiveStudent(permanentId: String): Result<Unit> {
        val user = _currentUser.value
        if (user?.role != UserRole.Administrator && user?.role != UserRole.SuperAdmin) {
            return Result.failure(IllegalAccessException("Only Administrators are authorized to archive student records."))
        }
        val index = allStudents.indexOfFirst { it.permanentStudentId == permanentId }
        if (index == -1) return Result.failure(IllegalArgumentException("Student record not found"))
        val current = allStudents[index]
        allStudents[index] = current.copy(isArchived = true, approvalStatus = ApprovalStatus.Archived)
        refreshDataForCurrentUser()
        syncPatch(
            "students",
            "permanent_student_id=eq.${SupabaseClient.encodeQueryParam(permanentId)}",
            JSONObject().apply { put("is_archived", true) }
        )
        return Result.success(Unit)
    }

    fun registerNewStudent(
        name: String,
        fatherName: String,
        bForm: String,
        grade: String,
        section: String,
        photoColorHex: Long = 0xFF1B3B6F,
        photoUri: String? = null,
        parentMediaConsentGiven: Boolean = true,
        parentId: String = ""
    ): StudentRecord {
        val permanentId = PermanentIdRegistry.issuePermanentId(name, bForm)
        val today = todayDate()
        val actingUser = _currentUser.value
        val resolvedParentId = parentId.takeIf { it.isNotBlank() } ?: ""
        val assignedTeachers = if (actingUser?.role == UserRole.Teacher) listOf(actingUser.id) else emptyList()

        val student = StudentRecord(
            id = "student_" + UUID.randomUUID().toString().take(8),
            permanentStudentId = permanentId,
            fullName = name.trim(),
            urduName = name.trim(),
            bFormNumber = bForm.trim(),
            grade = grade.ifBlank { "Grade 9" },
            section = section.ifBlank { "Section A" },
            parentId = resolvedParentId,
            parentName = fatherName.trim(),
            fatherName = fatherName.trim(),
            assignedTeacherIds = assignedTeachers,
            attendanceRate = 0,
            approvalStatus = ApprovalStatus.Approved,
            issuedDate = today,
            avatarColorHex = photoColorHex,
            isArchived = false,
            photoUri = if (parentMediaConsentGiven) photoUri else null,
            parentMediaConsentGiven = parentMediaConsentGiven
        )
        allStudents.add(student)
        refreshDataForCurrentUser()

        syncUpsert("students", JSONObject().apply {
            put("permanent_student_id", student.permanentStudentId)
            put("full_name", student.fullName)
            put("urdu_name", student.urduName)
            put("father_name", student.fatherName)
            put("b_form_number", student.bFormNumber)
            put("grade", student.grade)
            put("section", student.section)
            if (resolvedParentId.isValidUuid()) put("parent_id", resolvedParentId)
            put("parent_media_consent_given", student.parentMediaConsentGiven)
            put("is_archived", false)
            put("avatar_color_hex", student.avatarColorHex)
            if (student.photoUri != null) put("photo_uri", student.photoUri)
            put("issued_date", today)
        }, "permanent_student_id")

        return student
    }

    // ============================================================
    // CLASS ACTIVITY MEDIA & ASSIGNMENTS
    // ============================================================

    fun addClassActivityMedia(media: ClassActivityMedia, uploader: UserProfile): Result<ClassActivityMedia> {
        val student = getStudentByPermanentId(media.studentPermanentId)
            ?: return Result.failure(IllegalArgumentException("Student not found"))
        if (uploader.role != UserRole.Administrator && uploader.role != UserRole.SuperAdmin &&
            (uploader.role != UserRole.Teacher || !canUserEditSkills(uploader, student))
        ) {
            return Result.failure(IllegalAccessException("Only assigned teachers or administrators can record class activity media."))
        }
        if (!student.parentMediaConsentGiven) {
            return Result.failure(IllegalStateException("Parental consent is required before saving or storing photos or videos for this student."))
        }
        val (isValid, errorMsg) = MediaUtils.validateMediaUpload(media.mediaType, media.fileSizeBytes, media.durationSeconds)
        if (!isValid) return Result.failure(IllegalArgumentException(errorMsg ?: "Media file exceeds limits"))

        _classActivityMedia.value = listOf(media) + _classActivityMedia.value
        syncUpsert("class_activity_media", JSONObject().apply {
            put("id", media.id)
            put("student_permanent_id", media.studentPermanentId)
            put("media_type", media.mediaType.name)
            put("file_path", media.fileUri)
            put("caption", media.caption)
            put("activity_name", media.activityName)
            put("teacher_name", media.teacherName)
            if (media.teacherId.isValidUuid()) put("teacher_id", media.teacherId)
            put("date", media.date.ifBlank { todayDate() })
            put("file_size_bytes", media.fileSizeBytes)
            put("duration_seconds", media.durationSeconds)
        }, "id")
        return Result.success(media)
    }

    fun deleteClassActivityMedia(mediaId: String, requestingUser: UserProfile): Result<Boolean> {
        val media = _classActivityMedia.value.find { it.id == mediaId }
            ?: return Result.failure(IllegalArgumentException("Media record not found"))
        val canDelete = requestingUser.role == UserRole.Administrator || requestingUser.role == UserRole.SuperAdmin ||
            (requestingUser.role == UserRole.Teacher && media.teacherId == requestingUser.id)
        if (!canDelete) {
            return Result.failure(IllegalAccessException("Only the teacher who added this file or an Administrator can delete it."))
        }
        _classActivityMedia.value = _classActivityMedia.value.filterNot { it.id == mediaId }
        syncDelete("class_activity_media", "id=eq.${SupabaseClient.encodeQueryParam(mediaId)}")
        return Result.success(true)
    }

    fun updateClassActivityMedia(mediaId: String, caption: String, user: UserProfile): Result<ClassActivityMedia> {
        val list = _classActivityMedia.value.toMutableList()
        val idx = list.indexOfFirst { it.id == mediaId }
        if (idx == -1) return Result.failure(IllegalArgumentException("Media record not found"))
        val current = list[idx]
        val canEdit = user.role == UserRole.Administrator || user.role == UserRole.SuperAdmin ||
            (user.role == UserRole.Teacher && current.teacherId == user.id)
        if (!canEdit) return Result.failure(IllegalAccessException("Only the teacher who added this media or an administrator can edit it."))
        val updated = current.copy(caption = caption)
        list[idx] = updated
        _classActivityMedia.value = list
        syncPatch("class_activity_media", "id=eq.${SupabaseClient.encodeQueryParam(mediaId)}", JSONObject().apply {
            put("caption", caption)
        })
        return Result.success(updated)
    }

    fun getMediaForStudent(studentPermanentId: String, requestingUser: UserProfile?): List<ClassActivityMedia> {
        if (requestingUser == null) return emptyList()
        val student = getStudentByPermanentId(studentPermanentId) ?: return emptyList()
        if (!canUserViewStudent(requestingUser, student)) return emptyList()
        return _classActivityMedia.value.filter { it.studentPermanentId == studentPermanentId }
    }

    fun getAssignmentsForStudent(studentPermanentId: String, requestingUser: UserProfile?): List<StudentAssignment> {
        if (requestingUser == null) return emptyList()
        val student = getStudentByPermanentId(studentPermanentId) ?: return emptyList()
        if (!canUserViewStudent(requestingUser, student)) return emptyList()
        return _studentAssignments.value.filter { it.studentPermanentId == studentPermanentId }
    }

    fun submitAssignment(
        assignmentId: String,
        mediaType: MediaType,
        fileUri: String,
        fileName: String,
        fileSizeBytes: Long,
        submittingUser: UserProfile
    ): Result<StudentAssignment> {
        val assignment = _studentAssignments.value.find { it.id == assignmentId }
            ?: return Result.failure(IllegalArgumentException("Assignment not found"))
        val student = getStudentByPermanentId(assignment.studentPermanentId)
            ?: return Result.failure(IllegalArgumentException("Student not found"))
        val canSubmit = (submittingUser.role == UserRole.Student && submittingUser.permanentStudentId == student.permanentStudentId) ||
            (submittingUser.role == UserRole.Parent && (student.parentId == submittingUser.id || submittingUser.linkedChildIds.contains(student.id))) ||
            submittingUser.role == UserRole.Administrator || submittingUser.role == UserRole.SuperAdmin
        if (!canSubmit) return Result.failure(IllegalAccessException("Not authorized to submit assignments for this student."))

        val (isValid, errorMsg) = MediaUtils.validateMediaUpload(mediaType, fileSizeBytes)
        if (!isValid) return Result.failure(IllegalArgumentException(errorMsg ?: "File exceeds allowed size limit"))

        val subDate = todayDate()
        val updated = assignment.copy(
            isSubmitted = true,
            submissionDate = subDate,
            mediaType = mediaType,
            fileUri = fileUri,
            fileName = fileName,
            fileSizeBytes = fileSizeBytes
        )
        _studentAssignments.value = _studentAssignments.value.map { if (it.id == assignmentId) updated else it }
        syncPatch("student_assignments", "id=eq.${SupabaseClient.encodeQueryParam(assignmentId)}", JSONObject().apply {
            put("is_submitted", true)
            put("submission_date", subDate)
            put("media_type", mediaType.name)
            put("file_path", fileUri)
            put("file_name", fileName)
            put("file_size_bytes", fileSizeBytes)
        })
        return Result.success(updated)
    }

    fun addAssignment(assignment: StudentAssignment, user: UserProfile): Result<StudentAssignment> {
        if (user.role !in listOf(UserRole.Administrator, UserRole.SuperAdmin, UserRole.Teacher)) {
            return Result.failure(IllegalAccessException("Only teachers and administrators can add assignments."))
        }
        _studentAssignments.value = listOf(assignment) + _studentAssignments.value
        syncUpsert("student_assignments", JSONObject().apply {
            put("id", assignment.id)
            put("student_permanent_id", assignment.studentPermanentId)
            put("title", assignment.title)
            put("subject", assignment.subject)
            put("due_date", assignment.dueDate.ifBlank { todayDate() })
            put("is_submitted", assignment.isSubmitted)
            put("file_size_bytes", assignment.fileSizeBytes)
        }, "id")
        return Result.success(assignment)
    }

    fun updateAssignment(assignment: StudentAssignment, user: UserProfile): Result<StudentAssignment> {
        if (user.role !in listOf(UserRole.Administrator, UserRole.SuperAdmin, UserRole.Teacher)) {
            return Result.failure(IllegalAccessException("Only teachers and administrators can edit assignments."))
        }
        val list = _studentAssignments.value.toMutableList()
        val idx = list.indexOfFirst { it.id == assignment.id }
        if (idx == -1) return Result.failure(IllegalArgumentException("Assignment not found"))
        list[idx] = assignment
        _studentAssignments.value = list
        syncPatch("student_assignments", "id=eq.${SupabaseClient.encodeQueryParam(assignment.id)}", JSONObject().apply {
            put("title", assignment.title)
            put("subject", assignment.subject)
            put("due_date", assignment.dueDate)
            if (assignment.grade != null) put("grade", assignment.grade)
        })
        return Result.success(assignment)
    }

    fun deleteAssignment(assignmentId: String, user: UserProfile): Result<Boolean> {
        if (user.role !in listOf(UserRole.Administrator, UserRole.SuperAdmin, UserRole.Teacher)) {
            return Result.failure(IllegalAccessException("Only teachers and administrators can delete assignments."))
        }
        val removed = _studentAssignments.value.any { it.id == assignmentId }
        if (!removed) return Result.failure(IllegalArgumentException("Assignment not found"))
        _studentAssignments.value = _studentAssignments.value.filterNot { it.id == assignmentId }
        syncDelete("student_assignments", "id=eq.${SupabaseClient.encodeQueryParam(assignmentId)}")
        return Result.success(true)
    }

    // ============================================================
    // SUBJECTS CRUD
    // ============================================================

    fun addSubject(subject: SubjectItem, user: UserProfile): Result<SubjectItem> {
        if (user.role != UserRole.Administrator && user.role != UserRole.SuperAdmin) {
            return Result.failure(IllegalAccessException("Only administrators can add new subjects."))
        }
        _subjectsList.value = _subjectsList.value + subject
        syncUpsert("subjects", JSONObject().apply {
            put("id", subject.id)
            put("name", subject.name)
            put("urdu_name", subject.urduName)
            put("code", subject.code)
            put("grade", subject.grade)
            put("teacher_name", subject.teacherName)
            put("weekly_hours", subject.weeklyHours)
            put("description", subject.description)
        }, "id")
        return Result.success(subject)
    }

    fun updateSubject(subject: SubjectItem, user: UserProfile): Result<SubjectItem> {
        if (user.role != UserRole.Administrator && user.role != UserRole.SuperAdmin) {
            return Result.failure(IllegalAccessException("Only administrators can edit subjects."))
        }
        val list = _subjectsList.value.toMutableList()
        val idx = list.indexOfFirst { it.id == subject.id }
        if (idx == -1) return Result.failure(IllegalArgumentException("Subject not found"))
        list[idx] = subject
        _subjectsList.value = list
        syncUpsert("subjects", JSONObject().apply {
            put("id", subject.id)
            put("name", subject.name)
            put("urdu_name", subject.urduName)
            put("code", subject.code)
            put("grade", subject.grade)
            put("teacher_name", subject.teacherName)
            put("weekly_hours", subject.weeklyHours)
            put("description", subject.description)
        }, "id")
        return Result.success(subject)
    }

    fun deleteSubject(subjectId: String, user: UserProfile): Result<Boolean> {
        if (user.role != UserRole.Administrator && user.role != UserRole.SuperAdmin) {
            return Result.failure(IllegalAccessException("Only administrators can delete subjects."))
        }
        val removed = _subjectsList.value.any { it.id == subjectId }
        if (!removed) return Result.failure(IllegalArgumentException("Subject not found"))
        _subjectsList.value = _subjectsList.value.filterNot { it.id == subjectId }
        syncDelete("subjects", "id=eq.${SupabaseClient.encodeQueryParam(subjectId)}")
        return Result.success(true)
    }

    fun approveStudent(studentPermanentId: String) {
        val index = allStudents.indexOfFirst { it.permanentStudentId == studentPermanentId }
        if (index != -1) {
            allStudents[index] = allStudents[index].copy(approvalStatus = ApprovalStatus.Approved)
            refreshDataForCurrentUser()
        }
    }

    // ============================================================
    // ADMIN-ONLY ACCOUNT & ASSIGNMENT CRUD (SUPABASE PERSISTED)
    // ============================================================

    private fun generateNextLoginId(role: UserRole): String {
        val prefix = when (role) {
            UserRole.SuperAdmin -> "SA"
            UserRole.Administrator -> "A"
            UserRole.Teacher -> "T"
            UserRole.Student -> "S"
            UserRole.Parent -> "P"
        }
        val existingNums = _schoolAccounts.value
            .filter { it.loginId.startsWith("$prefix-") }
            .mapNotNull { it.loginId.substringAfter("$prefix-").toIntOrNull() }
        val nextNum = (existingNums.maxOrNull() ?: 0) + 1
        return String.format(Locale.US, "%s-%03d", prefix, nextNum)
    }

    fun getAccountsByRole(role: UserRole? = null): List<SchoolAccount> =
        _schoolAccounts.value.filter { !it.isArchived && (role == null || it.role == role) }

    suspend fun createAccountSuspend(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole,
        grade: String = "Grade 9",
        section: String = "Section A",
        fatherName: String = "",
        bForm: String = "",
        actingUser: UserProfile? = _currentUser.value
    ): Result<SchoolAccount> {
        if (actingUser?.role != UserRole.Administrator && actingUser?.role != UserRole.SuperAdmin) {
            return Result.failure(IllegalAccessException("Only Administrators can create school accounts."))
        }
        val trimmedName = name.trim()
        val trimmedEmail = email.trim()
        if (trimmedName.isEmpty()) return Result.failure(IllegalArgumentException("Name is required"))
        if (trimmedEmail.isEmpty() || !trimmedEmail.contains("@")) {
            return Result.failure(IllegalArgumentException("Valid email is required"))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }
        if (_schoolAccounts.value.any { !it.isArchived && it.email.equals(trimmedEmail, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Email is already registered"))
        }
        if (!supabaseClient.isConfigured() || currentAccessToken.isBlank()) {
            return Result.failure(IllegalStateException(supabaseClient.configErrorMessage()))
        }

        val loginId = generateNextLoginId(role)
        val permanentId = if (role == UserRole.Student) {
            PermanentIdRegistry.issuePermanentId(trimmedName, bForm)
        } else null

        val createdUid = supabaseClient.signUpUserByAdmin(
            email = trimmedEmail,
            password = password,
            name = trimmedName,
            roleName = role.englishName,
            loginId = loginId
        ).getOrElse { err -> return Result.failure(err) }

        val avatarColor = when (role) {
            UserRole.Administrator, UserRole.SuperAdmin -> 0xFF7F1D1D
            UserRole.Teacher -> 0xFF006D77
            UserRole.Parent -> 0xFF1B3B6F
            UserRole.Student -> 0xFF01411C
        }

        val profileBody = JSONObject().apply {
            put("id", createdUid)
            put("login_id", loginId)
            put("name", trimmedName)
            put("email", trimmedEmail)
            put("phone", phone.trim())
            put("role", role.name)
            if (permanentId != null) put("permanent_student_id", permanentId)
            put("approval_status", "Approved")
            put("avatar_color_hex", avatarColor)
        }
        supabaseClient.restUpsert("profiles", profileBody, "id", currentAccessToken)
            .onFailure { err -> return Result.failure(IllegalStateException("Auth user created, but saving profile failed: ${err.message}")) }

        if (role == UserRole.Student && permanentId != null) {
            val today = todayDate()
            val resolvedFather = fatherName.trim()
            val studentBody = JSONObject().apply {
                put("permanent_student_id", permanentId)
                put("auth_user_id", createdUid)
                put("full_name", trimmedName)
                put("urdu_name", trimmedName)
                put("father_name", resolvedFather)
                put("b_form_number", bForm.trim())
                put("grade", grade.ifBlank { "Grade 9" })
                put("section", section.ifBlank { "Section A" })
                put("parent_media_consent_given", true)
                put("is_archived", false)
                put("avatar_color_hex", avatarColor)
                put("issued_date", today)
            }
            supabaseClient.restUpsert("students", studentBody, "permanent_student_id", currentAccessToken)
                .onFailure { err -> return Result.failure(err) }

            val record = StudentRecord(
                id = createdUid,
                permanentStudentId = permanentId,
                fullName = trimmedName,
                urduName = trimmedName,
                bFormNumber = bForm.trim(),
                grade = grade.ifBlank { "Grade 9" },
                section = section.ifBlank { "Section A" },
                parentId = "",
                parentName = resolvedFather,
                fatherName = resolvedFather,
                assignedTeacherIds = emptyList(),
                attendanceRate = 0,
                approvalStatus = ApprovalStatus.Approved,
                issuedDate = today,
                avatarColorHex = avatarColor,
                isArchived = false
            )
            allStudents.add(record)
        }

        val newAccount = SchoolAccount(
            id = createdUid,
            loginId = loginId,
            name = trimmedName,
            email = trimmedEmail,
            phone = phone.trim(),
            password = "",
            role = role,
            permanentStudentId = permanentId,
            approvalStatus = ApprovalStatus.Approved,
            avatarColorHex = avatarColor
        )
        _schoolAccounts.value = _schoolAccounts.value + newAccount
        refreshDataForCurrentUser()
        return Result.success(newAccount)
    }

    fun updateAccount(
        accountId: String,
        name: String,
        email: String,
        phone: String,
        password: String,
        role: UserRole,
        approvalStatus: ApprovalStatus,
        actingUser: UserProfile? = _currentUser.value
    ): Result<SchoolAccount> {
        if (actingUser?.role != UserRole.Administrator && actingUser?.role != UserRole.SuperAdmin) {
            return Result.failure(IllegalAccessException("Only Administrators can edit accounts."))
        }
        val list = _schoolAccounts.value.toMutableList()
        val idx = list.indexOfFirst { it.id == accountId }
        if (idx == -1) return Result.failure(IllegalArgumentException("Account not found"))
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty"))

        val updated = list[idx].copy(
            name = name.trim(),
            email = email.trim().ifEmpty { list[idx].email },
            phone = phone.trim(),
            password = "",
            role = role,
            approvalStatus = approvalStatus
        )
        list[idx] = updated
        _schoolAccounts.value = list

        if (updated.permanentStudentId != null) {
            val sIdx = allStudents.indexOfFirst { it.permanentStudentId == updated.permanentStudentId }
            if (sIdx != -1) {
                allStudents[sIdx] = allStudents[sIdx].copy(
                    fullName = updated.name,
                    urduName = updated.name,
                    approvalStatus = approvalStatus
                )
            }
        }
        if (_currentUser.value?.id == accountId) {
            _currentUser.value = updated.toUserProfile()
        }
        refreshDataForCurrentUser()

        syncPatch(
            "profiles",
            "id=eq.${SupabaseClient.encodeQueryParam(accountId)}",
            JSONObject().apply {
                put("name", updated.name)
                put("email", updated.email)
                put("phone", updated.phone)
                put("role", updated.role.name)
                put("approval_status", updated.approvalStatus.englishName)
            }
        )
        return Result.success(updated)
    }

    fun deleteAccount(accountId: String, actingUser: UserProfile? = _currentUser.value): Result<Boolean> {
        if (actingUser?.role != UserRole.Administrator && actingUser?.role != UserRole.SuperAdmin) {
            return Result.failure(IllegalAccessException("Only Administrators can delete accounts."))
        }
        if (actingUser.id == accountId) {
            return Result.failure(IllegalArgumentException("Cannot delete the currently signed-in Administrator account."))
        }
        val target = _schoolAccounts.value.find { it.id == accountId }
            ?: return Result.failure(IllegalArgumentException("Account not found"))

        if (target.role == UserRole.Student && target.permanentStudentId != null) {
            val sIdx = allStudents.indexOfFirst { it.permanentStudentId == target.permanentStudentId }
            if (sIdx != -1) {
                allStudents[sIdx] = allStudents[sIdx].copy(isArchived = true, approvalStatus = ApprovalStatus.Archived)
            }
            syncPatch(
                "students",
                "permanent_student_id=eq.${SupabaseClient.encodeQueryParam(target.permanentStudentId)}",
                JSONObject().apply { put("is_archived", true) }
            )
        }
        if (target.role == UserRole.Teacher) {
            allStudents.forEachIndexed { idx, s ->
                if (s.assignedTeacherIds.contains(target.id)) {
                    allStudents[idx] = s.copy(assignedTeacherIds = s.assignedTeacherIds.filterNot { it == target.id })
                }
            }
            syncDelete("student_teachers", "teacher_id=eq.${SupabaseClient.encodeQueryParam(target.id)}")
        }
        if (target.role == UserRole.Parent) {
            allStudents.forEachIndexed { idx, s ->
                if (s.parentId == target.id) allStudents[idx] = s.copy(parentId = "")
            }
            syncPatch("students", "parent_id=eq.${SupabaseClient.encodeQueryParam(target.id)}", JSONObject().apply {
                put("parent_id", JSONObject.NULL)
            })
        }

        _schoolAccounts.value = _schoolAccounts.value.filterNot { it.id == accountId }
        refreshDataForCurrentUser()
        syncDelete("profiles", "id=eq.${SupabaseClient.encodeQueryParam(accountId)}")
        return Result.success(true)
    }

    fun approveAccount(accountId: String, actingUser: UserProfile? = _currentUser.value): Result<SchoolAccount> {
        val current = _schoolAccounts.value.find { it.id == accountId }
            ?: return Result.failure(IllegalArgumentException("Account not found"))
        return updateAccount(
            accountId = current.id,
            name = current.name,
            email = current.email,
            phone = current.phone,
            password = "",
            role = current.role,
            approvalStatus = ApprovalStatus.Approved,
            actingUser = actingUser
        )
    }

    fun toggleAssignStudentToTeacher(
        studentId: String,
        teacherId: String,
        actingUser: UserProfile? = _currentUser.value
    ): Result<Unit> {
        if (actingUser?.role != UserRole.Administrator && actingUser?.role != UserRole.SuperAdmin) {
            return Result.failure(IllegalAccessException("Only Administrators can assign students to teachers."))
        }
        val sIdx = allStudents.indexOfFirst { it.id == studentId || it.permanentStudentId == studentId }
        if (sIdx == -1) return Result.failure(IllegalArgumentException("Student not found"))
        val student = allStudents[sIdx]
        val isAssigned = student.assignedTeacherIds.contains(teacherId)
        val newTeacherIds = if (isAssigned) student.assignedTeacherIds.filterNot { it == teacherId }
        else (student.assignedTeacherIds + teacherId).distinct()
        allStudents[sIdx] = student.copy(assignedTeacherIds = newTeacherIds)

        _schoolAccounts.value = _schoolAccounts.value.map { acc ->
            if (acc.id == teacherId) {
                val updatedIds = if (isAssigned) acc.assignedStudentIds.filterNot { it == student.id || it == student.permanentStudentId }
                else (acc.assignedStudentIds + student.id).distinct()
                acc.copy(assignedStudentIds = updatedIds)
            } else acc
        }
        if (_currentUser.value?.id == teacherId) {
            _schoolAccounts.value.find { it.id == teacherId }?.let { _currentUser.value = it.toUserProfile() }
        }
        refreshDataForCurrentUser()

        if (isAssigned) {
            syncDelete(
                "student_teachers",
                "student_permanent_id=eq.${SupabaseClient.encodeQueryParam(student.permanentStudentId)}&teacher_id=eq.${SupabaseClient.encodeQueryParam(teacherId)}"
            )
        } else if (teacherId.isValidUuid()) {
            syncUpsert("student_teachers", JSONObject().apply {
                put("student_permanent_id", student.permanentStudentId)
                put("teacher_id", teacherId)
            }, "student_permanent_id,teacher_id")
        }
        return Result.success(Unit)
    }

    fun linkParentToChild(
        parentId: String,
        studentId: String,
        unlink: Boolean = false,
        actingUser: UserProfile? = _currentUser.value
    ): Result<Unit> {
        if (actingUser?.role != UserRole.Administrator && actingUser?.role != UserRole.SuperAdmin) {
            return Result.failure(IllegalAccessException("Only Administrators can link parents to children."))
        }
        val sIdx = allStudents.indexOfFirst { it.id == studentId || it.permanentStudentId == studentId }
        if (sIdx == -1) return Result.failure(IllegalArgumentException("Student not found"))
        val student = allStudents[sIdx]
        val parentAcc = _schoolAccounts.value.find { it.id == parentId && it.role == UserRole.Parent }

        allStudents[sIdx] = student.copy(
            parentId = if (unlink) "" else parentId,
            parentName = if (unlink) student.fatherName else (parentAcc?.name ?: student.parentName)
        )
        _schoolAccounts.value = _schoolAccounts.value.map { acc ->
            when {
                acc.id == parentId -> {
                    val newChildren = if (unlink) acc.linkedChildIds.filterNot { it == student.id }
                    else (acc.linkedChildIds + student.id).distinct()
                    acc.copy(linkedChildIds = newChildren)
                }
                !unlink && acc.role == UserRole.Parent && acc.linkedChildIds.contains(student.id) ->
                    acc.copy(linkedChildIds = acc.linkedChildIds.filterNot { it == student.id })
                acc.id == student.id -> acc.copy(linkedParentId = if (unlink) null else parentId)
                else -> acc
            }
        }
        if (_currentUser.value?.id == parentId) {
            _schoolAccounts.value.find { it.id == parentId }?.let { _currentUser.value = it.toUserProfile() }
        }
        refreshDataForCurrentUser()

        syncPatch(
            "students",
            "permanent_student_id=eq.${SupabaseClient.encodeQueryParam(student.permanentStudentId)}",
            JSONObject().apply {
                put("parent_id", if (unlink || !parentId.isValidUuid()) JSONObject.NULL else parentId)
            }
        )
        return Result.success(Unit)
    }

    fun updateProfile(name: String, phone: String, avatarColorHex: Long? = null) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            name = name.ifBlank { current.name },
            phone = phone.trim(),
            avatarColorHex = avatarColorHex ?: current.avatarColorHex,
            avatarInitials = (name.ifBlank { current.name }).take(2).uppercase()
        )
        _currentUser.value = updated
        _schoolAccounts.value = _schoolAccounts.value.map {
            if (it.id == updated.id) it.copy(
                name = updated.name,
                phone = updated.phone,
                avatarColorHex = updated.avatarColorHex
            ) else it
        }
        refreshDataForCurrentUser()
        syncPatch("profiles", "id=eq.${SupabaseClient.encodeQueryParam(updated.id)}", JSONObject().apply {
            put("name", updated.name)
            put("phone", updated.phone)
            put("avatar_color_hex", updated.avatarColorHex)
        })
    }

    fun revokeOtherDevices() {
        _activeDevices.value = _activeDevices.value.filter { it.isCurrent }
    }

    fun setNotifyAssignments(enabled: Boolean) { _notifyAssignments.value = enabled; persistPrefs() }
    fun setNotifyQuizzes(enabled: Boolean) { _notifyQuizzes.value = enabled; persistPrefs() }
    fun setNotifyReports(enabled: Boolean) { _notifyReports.value = enabled; persistPrefs() }
    fun setNotifyMessages(enabled: Boolean) { _notifyMessages.value = enabled; persistPrefs() }
    fun setTwoStepLogin(enabled: Boolean) { _twoStepLoginEnabled.value = enabled; persistPrefs() }
    fun setThemeMode(mode: ThemeMode) { _themeMode.value = mode; persistPrefs() }
    fun setThemePalette(palette: ThemePalette) { _themePalette.value = palette; persistPrefs() }
    fun setTextScale(scale: TextScale) { _textScale.value = scale; persistPrefs() }
    fun setAppLanguage(language: AppLanguage) { _appLanguage.value = language; persistPrefs() }

    // ============================================================
    // MODULE 3: ATTENDANCE BUSINESS LOGIC & CLOUD PERSISTENCE
    // ============================================================

    fun markDailyAttendance(
        studentPermanentId: String,
        date: String,
        status: AttendanceStatus,
        note: String = "",
        teacherName: String
    ): Result<AttendanceRecord> {
        val currentMap = _attendanceRecords.value.toMutableMap()
        val list = currentMap[studentPermanentId]?.toMutableList() ?: mutableListOf()
        val existingIndex = list.indexOfFirst { it.date == date }
        val student = getStudentByPermanentId(studentPermanentId)
        val studentName = student?.fullName ?: studentPermanentId
        val recordId = "att_${studentPermanentId}_$date"

        val updatedRecord: AttendanceRecord
        var newLogEntry: AttendanceChangeLog? = null

        if (existingIndex != -1) {
            val existing = list[existingIndex]
            val timeStamp = "$date " + SimpleDateFormat("HH:mm", Locale.US).format(Calendar.getInstance().time)
            val log = AttendanceChangeLog(
                timestamp = timeStamp,
                modifiedBy = teacherName,
                previousStatus = existing.status,
                newStatus = status,
                note = note.ifBlank { "Status adjusted by $teacherName" }
            )
            newLogEntry = log
            updatedRecord = existing.copy(
                status = status,
                note = note,
                markedBy = teacherName,
                lastModified = date,
                changeLog = existing.changeLog + log
            )
            list[existingIndex] = updatedRecord
        } else {
            updatedRecord = AttendanceRecord(
                id = recordId,
                studentPermanentId = studentPermanentId,
                date = date,
                status = status,
                note = note,
                markedBy = teacherName,
                lastModified = date
            )
            list.add(0, updatedRecord)
        }

        currentMap[studentPermanentId] = list
        _attendanceRecords.value = currentMap

        // Update computed student attendance rate in real time
        val sIdx = allStudents.indexOfFirst { it.permanentStudentId == studentPermanentId }
        if (sIdx != -1) {
            val presentOrLate = list.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.LATE }
            val newRate = ((presentOrLate.toFloat() / list.size.toFloat()) * 100f).toInt().coerceIn(0, 100)
            allStudents[sIdx] = allStudents[sIdx].copy(attendanceRate = newRate)
            refreshDataForCurrentUser()
        }

        syncUpsert("attendance_records", JSONObject().apply {
            put("id", updatedRecord.id)
            put("student_permanent_id", studentPermanentId)
            put("date", date)
            put("status", status.englishName)
            put("note", note)
            put("marked_by", teacherName)
            put("last_modified", date)
        }, "student_permanent_id,date")

        newLogEntry?.let { entry ->
            syncUpsert("attendance_change_logs", JSONObject().apply {
                put("attendance_record_id", updatedRecord.id)
                put("modified_by", entry.modifiedBy)
                put("previous_status", entry.previousStatus.englishName)
                put("new_status", entry.newStatus.englishName)
                put("note", entry.note)
            }, null)
        }

        if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.LATE) {
            val alert = AttendanceAlert(
                id = "alert_" + UUID.randomUUID().toString().take(8),
                studentPermanentId = studentPermanentId,
                studentName = studentName,
                type = AttendanceAlertType.ABSENT_OR_LATE,
                message = "$studentName was marked ${status.englishName} on $date. Note: ${note.ifBlank { "None" }}",
                urduMessage = "$studentName کو تاریخ $date پر ${status.urduName} درج کیا گیا۔ نوٹ: ${note.ifBlank { "کوئی نہیں" }}",
                date = date
            )
            _attendanceAlerts.value = listOf(alert) + _attendanceAlerts.value
            persistAlert(alert)
        }

        val cal = Calendar.getInstance()
        val yearPart = date.substringBefore("-").toIntOrNull() ?: cal.get(Calendar.YEAR)
        val monthPart = date.split("-").getOrNull(1)?.toIntOrNull()
        val summary = getAttendanceSummary(studentPermanentId, yearPart, monthPart)
        if (summary.totalWorkingDays > 0 && summary.attendancePercentage < _lowAttendanceThreshold.value) {
            val pctStr = String.format(Locale.US, "%.1f", summary.attendancePercentage)
            val adminAlert = AttendanceAlert(
                id = "alert_low_" + UUID.randomUUID().toString().take(8),
                studentPermanentId = studentPermanentId,
                studentName = studentName,
                type = AttendanceAlertType.LOW_ATTENDANCE,
                message = "Attention: $studentName monthly attendance is $pctStr% (below ${_lowAttendanceThreshold.value}% threshold)",
                urduMessage = "توجہ طلب: $studentName کی ماہانہ حاضری $pctStr% ہے جو کہ مقررہ حد ${_lowAttendanceThreshold.value}% سے کم ہے",
                date = date
            )
            if (_attendanceAlerts.value.none { it.studentPermanentId == studentPermanentId && it.type == AttendanceAlertType.LOW_ATTENDANCE }) {
                _attendanceAlerts.value = listOf(adminAlert) + _attendanceAlerts.value
                persistAlert(adminAlert)
            }
        }

        return Result.success(updatedRecord)
    }

    private fun persistAlert(alert: AttendanceAlert) {
        syncUpsert("attendance_alerts", JSONObject().apply {
            put("id", alert.id)
            put("student_permanent_id", alert.studentPermanentId)
            put("student_name", alert.studentName)
            put("alert_type", alert.type.name)
            put("message", alert.message)
            put("urdu_message", alert.urduMessage)
            put("date", alert.date)
            put("is_read", alert.isRead)
        }, "id")
    }

    fun getAttendanceSummary(
        studentPermanentId: String,
        year: Int = Calendar.getInstance().get(Calendar.YEAR),
        month: Int? = null
    ): AttendanceSummary {
        val records = _attendanceRecords.value[studentPermanentId] ?: emptyList()
        val filtered = records.filter { rec ->
            val parts = rec.date.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            y == year && (month == null || m == month)
        }

        val present = filtered.count { it.status == AttendanceStatus.PRESENT }
        val absent = filtered.count { it.status == AttendanceStatus.ABSENT }
        val late = filtered.count { it.status == AttendanceStatus.LATE }
        val leave = filtered.count { it.status == AttendanceStatus.LEAVE }

        val totalWorkingDays = present + absent + late + leave
        val percentage = if (totalWorkingDays == 0) 0f
        else ((present + late).toFloat() / totalWorkingDays.toFloat() * 100f).coerceIn(0f, 100f)

        return AttendanceSummary(
            presentDays = present,
            absentDays = absent,
            lateDays = late,
            leaveDays = leave,
            totalWorkingDays = totalWorkingDays,
            attendancePercentage = percentage
        )
    }

    fun getDailyRecord(studentPermanentId: String, date: String): AttendanceRecord? =
        _attendanceRecords.value[studentPermanentId]?.find { it.date == date }

    fun getAttendanceStudentsList(user: UserProfile?): List<StudentRecord> {
        if (user == null) return emptyList()
        val liveAccount = _schoolAccounts.value.find { it.id == user.id }
        return when (user.role) {
            UserRole.Teacher -> {
                val assigned = ((liveAccount?.assignedStudentIds ?: emptyList()) + user.assignedStudentIds).toSet()
                allStudents.filter { !it.isArchived && (it.assignedTeacherIds.contains(user.id) || assigned.contains(it.id) || assigned.contains(it.permanentStudentId)) }
            }
            UserRole.Parent -> {
                val linkedChildren = ((liveAccount?.linkedChildIds ?: emptyList()) + user.linkedChildIds).toSet()
                allStudents.filter { !it.isArchived && (it.parentId == user.id || linkedChildren.contains(it.id)) }
            }
            UserRole.Student -> {
                val permId = liveAccount?.permanentStudentId ?: user.permanentStudentId
                allStudents.filter { !it.isArchived && (it.id == user.id || (permId != null && it.permanentStudentId == permId)) }
            }
            UserRole.Administrator, UserRole.SuperAdmin -> allStudents.filter { !it.isArchived }
        }
    }

    fun canUserMarkAttendance(user: UserProfile?, student: StudentRecord): Boolean {
        if (user == null) return false
        return when (user.role) {
            UserRole.SuperAdmin, UserRole.Administrator -> true
            UserRole.Teacher -> student.assignedTeacherIds.contains(user.id) || user.assignedStudentIds.contains(student.permanentStudentId)
            UserRole.Parent, UserRole.Student -> false
        }
    }

    fun exportAttendanceCsv(students: List<StudentRecord>): String {
        val sb = StringBuilder()
        sb.append("Permanent Student ID,Student Name,Father Name,Grade,Section,Date,Status,Note,Marked By\n")
        for (student in students) {
            val list = _attendanceRecords.value[student.permanentStudentId] ?: emptyList()
            for (rec in list) {
                sb.append("${student.permanentStudentId},\"${student.fullName}\",\"${student.fatherName}\",\"${student.grade}\",\"${student.section}\",${rec.date},${rec.status.englishName},\"${rec.note}\",\"${rec.markedBy}\"\n")
            }
        }
        return sb.toString()
    }

    fun setWeeklyOffDays(days: Set<Int>) {
        _weeklyOffDays.value = days
        persistPrefs()
        syncSchoolSettings()
    }

    fun addSchoolHoliday(name: String, urduName: String, date: String) {
        val holiday = SchoolHoliday("hol_" + UUID.randomUUID().toString().take(8), name, urduName.ifBlank { name }, date)
        _schoolHolidays.value = _schoolHolidays.value + holiday
        syncUpsert("school_holidays", JSONObject().apply {
            put("id", holiday.id)
            put("name", holiday.name)
            put("urdu_name", holiday.urduName)
            put("date", holiday.date)
        }, "id")
    }

    fun removeSchoolHoliday(id: String) {
        _schoolHolidays.value = _schoolHolidays.value.filter { it.id != id }
        syncDelete("school_holidays", "id=eq.${SupabaseClient.encodeQueryParam(id)}")
    }

    fun setLowAttendanceThreshold(threshold: Int) {
        _lowAttendanceThreshold.value = threshold.coerceIn(50, 95)
        persistPrefs()
        syncSchoolSettings()
    }

    private fun syncSchoolSettings() {
        val daysArr = JSONArray()
        _weeklyOffDays.value.forEach { daysArr.put(it) }
        syncUpsert("school_settings", JSONObject().apply {
            put("id", "default")
            put("low_attendance_threshold", _lowAttendanceThreshold.value)
            put("weekly_off_days", daysArr)
        }, "id")
    }

    fun dismissAlert(id: String) {
        _attendanceAlerts.value = _attendanceAlerts.value.filter { it.id != id }
        syncPatch("attendance_alerts", "id=eq.${SupabaseClient.encodeQueryParam(id)}", JSONObject().apply {
            put("is_read", true)
        })
    }

    // ============================================================
    // INTERNAL SYNC HELPERS & UI PREFERENCES PERSISTENCE
    // ============================================================

    private fun syncUpsert(table: String, body: JSONObject, onConflict: String?) {
        val token = currentAccessToken
        if (!supabaseClient.isConfigured() || token.isBlank() || !_isOnline.value) return
        repoScope.launch {
            supabaseClient.restUpsert(table, body, onConflict, token).onFailure { err ->
                _syncError.value = "Failed saving to $table: ${err.message}"
            }
        }
    }

    private fun syncPatch(table: String, filterQuery: String, body: JSONObject) {
        val token = currentAccessToken
        if (!supabaseClient.isConfigured() || token.isBlank() || !_isOnline.value) return
        repoScope.launch {
            supabaseClient.restPatch(table, filterQuery, body, token).onFailure { err ->
                _syncError.value = "Failed updating $table: ${err.message}"
            }
        }
    }

    private fun syncDelete(table: String, filterQuery: String) {
        val token = currentAccessToken
        if (!supabaseClient.isConfigured() || token.isBlank() || !_isOnline.value) return
        repoScope.launch {
            supabaseClient.restDelete(table, filterQuery, token).onFailure { err ->
                _syncError.value = "Failed deleting from $table: ${err.message}"
            }
        }
    }

    fun initPersistence(context: Context) {
        if (prefs != null) return
        try {
            val sp = context.applicationContext.getSharedPreferences("dbus_app_prefs", Context.MODE_PRIVATE)
            prefs = sp

            sp.getString("theme_mode", null)?.let { runCatching { _themeMode.value = ThemeMode.valueOf(it) } }
            sp.getString("theme_palette", null)?.let { runCatching { _themePalette.value = ThemePalette.valueOf(it) } }
            sp.getString("text_scale", null)?.let { runCatching { _textScale.value = TextScale.valueOf(it) } }
            sp.getString("app_language", null)?.let { runCatching { _appLanguage.value = AppLanguage.valueOf(it) } }
            if (sp.contains("notify_assignments")) _notifyAssignments.value = sp.getBoolean("notify_assignments", true)
            if (sp.contains("notify_quizzes")) _notifyQuizzes.value = sp.getBoolean("notify_quizzes", true)
            if (sp.contains("notify_reports")) _notifyReports.value = sp.getBoolean("notify_reports", true)
            if (sp.contains("notify_messages")) _notifyMessages.value = sp.getBoolean("notify_messages", true)
            if (sp.contains("two_step_login")) _twoStepLoginEnabled.value = sp.getBoolean("two_step_login", false)
            if (sp.contains("low_att_threshold")) _lowAttendanceThreshold.value = sp.getInt("low_att_threshold", 75)
            sp.getString("weekly_off_days", null)?.let { raw ->
                val parsed = raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
                if (parsed.isNotEmpty()) _weeklyOffDays.value = parsed
            }
        } catch (_: Exception) {
            // Safe fallback in unit test environments
        }
    }

    private fun persistPrefs() {
        val sp = prefs ?: return
        try {
            sp.edit()
                .putString("theme_mode", _themeMode.value.name)
                .putString("theme_palette", _themePalette.value.name)
                .putString("text_scale", _textScale.value.name)
                .putString("app_language", _appLanguage.value.name)
                .putBoolean("notify_assignments", _notifyAssignments.value)
                .putBoolean("notify_quizzes", _notifyQuizzes.value)
                .putBoolean("notify_reports", _notifyReports.value)
                .putBoolean("notify_messages", _notifyMessages.value)
                .putBoolean("two_step_login", _twoStepLoginEnabled.value)
                .putInt("low_att_threshold", _lowAttendanceThreshold.value)
                .putString("weekly_off_days", _weeklyOffDays.value.joinToString(","))
                .apply()
        } catch (_: Exception) {
        }
    }

    private fun parseUserRole(raw: String): UserRole? = when (raw.trim()) {
        "SuperAdmin" -> UserRole.SuperAdmin
        "Administrator", "Admin" -> UserRole.Administrator
        "Teacher" -> UserRole.Teacher
        "Parent" -> UserRole.Parent
        "Student" -> UserRole.Student
        else -> null
    }

    private fun parseAttendanceStatus(raw: String): AttendanceStatus = when (raw.trim().lowercase()) {
        "present" -> AttendanceStatus.PRESENT
        "absent" -> AttendanceStatus.ABSENT
        "late" -> AttendanceStatus.LATE
        "leave" -> AttendanceStatus.LEAVE
        else -> AttendanceStatus.PRESENT
    }

    private fun String.isValidUuid(): Boolean =
        matches(Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))

    companion object {
        @Volatile
        private var instance: SchoolRepository? = null

        fun getInstance(): SchoolRepository {
            return instance ?: synchronized(this) {
                instance ?: SchoolRepository().also { instance = it }
            }
        }
    }
}
