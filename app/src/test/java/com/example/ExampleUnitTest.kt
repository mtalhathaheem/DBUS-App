package com.example

import com.example.data.PermanentIdRegistry
import com.example.data.SchoolRepository
import com.example.model.AnnouncementComment
import com.example.model.ApprovalStatus
import com.example.model.AttendanceAlertType
import com.example.model.AttendanceStatus
import com.example.model.CalendarEventType
import com.example.model.ClassActivityMedia
import com.example.model.CoreSkill
import com.example.model.ExamScheduleItem
import com.example.model.InstitutionRecord
import com.example.model.InstitutionType
import com.example.model.MediaType
import com.example.model.ParentTeacherAnnouncement
import com.example.model.SchoolCalendarEvent
import com.example.model.SkillEvidenceItem
import com.example.model.SkillLevel
import com.example.model.StudentAssignment
import com.example.model.StudentExamResult
import com.example.model.SubjectItem
import com.example.model.SubscriptionPlan
import com.example.model.SyncMethod
import com.example.model.UserRole
import com.example.util.MediaUtils
import com.example.util.SchoolCalculatorUtil
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress
import java.util.Calendar
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleUnitTest {

    private lateinit var repository: SchoolRepository
    private var server: HttpServer? = null

    private val adminId = "11111111-1111-1111-1111-111111111111"
    private val teacher1Id = "22222222-2222-2222-2222-222222222222"
    private val teacher2Id = "22222222-2222-2222-2222-222222222223"
    private val parent1Id = "33333333-3333-3333-3333-333333333333"
    private val student1AuthId = "44444444-4444-4444-4444-444444444444"
    private val student2AuthId = "44444444-4444-4444-4444-444444444445"
    private val pendingUserId = "55555555-5555-5555-5555-555555555555"

    private val stu1PermId = "PK-DBUS-2026-0001"
    private val stu2PermId = "PK-DBUS-2026-0002"

    private fun startMockSupabaseServer(): String {
        val srv = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        srv.createContext("/") { ex ->
            handleSupabaseRequest(ex)
        }
        srv.start()
        server = srv
        return "http://127.0.0.1:${srv.address.port}"
    }

    private fun respondJson(ex: HttpExchange, code: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        ex.responseHeaders.add("Content-Type", "application/json")
        ex.sendResponseHeaders(code, bytes.size.toLong())
        ex.responseBody.use { it.write(bytes) }
    }

    private fun handleSupabaseRequest(ex: HttpExchange) {
        val path = ex.requestURI.path
        val query = ex.requestURI.rawQuery.orEmpty()
        val method = ex.requestMethod
        val reqBody = ex.requestBody.bufferedReader().readText()

        when {
            path == "/rest/v1/rpc/resolve_login_email" && method == "POST" -> {
                val obj = JSONObject(reqBody)
                val id = obj.optString("p_identifier").trim().uppercase()
                val email = when (id) {
                    "A-001" -> "\"admin@school.test\""
                    "T-001" -> "\"teacher@school.test\""
                    "S-001", stu1PermId -> "\"student1@school.test\""
                    "P-001" -> "\"parent1@school.test\""
                    "P-999" -> "\"pending@school.test\""
                    else -> "null"
                }
                respondJson(ex, 200, email)
            }

            path == "/auth/v1/token" && method == "POST" -> {
                val obj = JSONObject(reqBody)
                if (query.contains("grant_type=id_token")) {
                    val token = obj.optString("id_token")
                    if (token == "valid-google-id-token") {
                        val resp = JSONObject().apply {
                            put("access_token", "token-$teacher1Id")
                            put("refresh_token", "refresh-$teacher1Id")
                            put("user", JSONObject().apply {
                                put("id", teacher1Id)
                                put("email", "teacher@school.test")
                            })
                        }
                        respondJson(ex, 200, resp.toString())
                    } else {
                        respondJson(ex, 400, """{"error":"invalid_grant","error_description":"Invalid login credentials"}""")
                    }
                    return
                }
                val email = obj.optString("email")
                val password = obj.optString("password")
                val valid = when (email) {
                    "admin@school.test" -> (password == "SecretPass1") to adminId
                    "teacher@school.test" -> (password == "SecretPass1") to teacher1Id
                    "student1@school.test" -> (password == "SecretPass1") to student1AuthId
                    "parent1@school.test" -> (password == "SecretPass1") to parent1Id
                    "pending@school.test" -> (password == "SecretPass1") to pendingUserId
                    else -> false to ""
                }
                if (!valid.first) {
                    respondJson(ex, 400, """{"error":"invalid_grant","error_description":"Invalid login credentials"}""")
                } else {
                    val uid = valid.second
                    val resp = JSONObject().apply {
                        put("access_token", "token-$uid")
                        put("refresh_token", "refresh-$uid")
                        put("user", JSONObject().apply {
                            put("id", uid)
                            put("email", email)
                        })
                    }
                    respondJson(ex, 200, resp.toString())
                }
            }

            path == "/auth/v1/signup" && method == "POST" -> {
                val newUid = UUID.randomUUID().toString()
                respondJson(ex, 200, """{"id":"$newUid","user":{"id":"$newUid"}}""")
            }

            path == "/auth/v1/logout" -> {
                respondJson(ex, 204, "")
            }

            path == "/rest/v1/profiles" && method == "GET" -> {
                val profiles = JSONArray().apply {
                    put(profileJson(adminId, "A-001", "School Principal", "admin@school.test", "Administrator", null, "Approved"))
                    put(profileJson(teacher1Id, "T-001", "Class Teacher", "teacher@school.test", "Teacher", null, "Approved"))
                    put(profileJson(student1AuthId, "S-001", "Student One", "student1@school.test", "Student", stu1PermId, "Approved"))
                    put(profileJson(parent1Id, "P-001", "Parent One", "parent1@school.test", "Parent", null, "Approved"))
                    put(profileJson(pendingUserId, "P-999", "Pending User", "pending@school.test", "Parent", null, "Pending Approval"))
                }
                if (query.contains("id=eq.")) {
                    val targetId = query.substringAfter("id=eq.").substringBefore("&")
                    val filtered = JSONArray()
                    for (i in 0 until profiles.length()) {
                        if (profiles.getJSONObject(i).getString("id") == targetId) {
                            filtered.put(profiles.getJSONObject(i))
                        }
                    }
                    respondJson(ex, 200, filtered.toString())
                } else {
                    respondJson(ex, 200, profiles.toString())
                }
            }

            path == "/rest/v1/students" && method == "GET" -> {
                val arr = JSONArray().apply {
                    put(JSONObject().apply {
                        put("permanent_student_id", stu1PermId)
                        put("auth_user_id", student1AuthId)
                        put("full_name", "Student One")
                        put("urdu_name", "طالب علم اول")
                        put("father_name", "Parent One")
                        put("b_form_number", "42101-1112223-1")
                        put("grade", "Grade 9")
                        put("section", "Section A")
                        put("parent_id", parent1Id)
                        put("parent_media_consent_given", true)
                        put("is_archived", false)
                    })
                    put(JSONObject().apply {
                        put("permanent_student_id", stu2PermId)
                        put("auth_user_id", student2AuthId)
                        put("full_name", "Student Two")
                        put("urdu_name", "طالب علم دوم")
                        put("father_name", "Other Father")
                        put("b_form_number", "42101-8889990-2")
                        put("grade", "Grade 10")
                        put("section", "Section B")
                        put("parent_id", "99999999-9999-9999-9999-999999999999")
                        put("parent_media_consent_given", true)
                        put("is_archived", false)
                    })
                }
                respondJson(ex, 200, arr.toString())
            }

            path == "/rest/v1/student_teachers" && method == "GET" -> {
                val arr = JSONArray().apply {
                    put(JSONObject().apply {
                        put("student_permanent_id", stu1PermId)
                        put("teacher_id", teacher1Id)
                    })
                    put(JSONObject().apply {
                        put("student_permanent_id", stu2PermId)
                        put("teacher_id", teacher2Id)
                    })
                }
                respondJson(ex, 200, arr.toString())
            }

            path.startsWith("/rest/v1/") -> {
                respondJson(ex, 200, "[]")
            }

            else -> {
                respondJson(ex, 200, "{}")
            }
        }
    }

    private fun profileJson(
        id: String,
        loginId: String,
        name: String,
        email: String,
        role: String,
        permId: String?,
        status: String
    ): JSONObject = JSONObject().apply {
        put("id", id)
        put("login_id", loginId)
        put("name", name)
        put("email", email)
        put("phone", "")
        put("role", role)
        if (permId != null) put("permanent_student_id", permId)
        put("approval_status", status)
        put("avatar_color_hex", 0xFF1B3B6F)
    }

    @Before
    fun setUp() {
        repository = SchoolRepository.getInstance()
        repository.logout()
        PermanentIdRegistry.reset()
        repository.setOnline(true)
    }

    @After
    fun tearDown() {
        repository.logout()
        server?.stop(0)
        server = null
    }

    @Test
    fun initialState_hasZeroDummyData_andNoSignedInUser() {
        assertNull(repository.currentUser.value)
        assertTrue(repository.studentsList.value.isEmpty())
        assertTrue(repository.schoolAccounts.value.isEmpty())
        assertTrue(repository.pendingApprovals.value.isEmpty())
        assertTrue(repository.attendanceRecords.value.isEmpty())
        assertTrue(repository.skillAssessments.value.isEmpty())
        assertTrue(repository.classActivityMedia.value.isEmpty())
        assertTrue(repository.studentAssignments.value.isEmpty())
        assertTrue(repository.subjectsList.value.isEmpty())
        assertTrue(repository.schoolCalendarEvents.value.isEmpty())
        assertTrue(repository.schoolHolidays.value.isEmpty())
        assertTrue(repository.attendanceAlerts.value.isEmpty())
        assertTrue(repository.examSchedules.value.isEmpty())
        assertTrue(repository.examResults.value.isEmpty())
        assertTrue(repository.parentAnnouncements.value.isEmpty())
        assertTrue(repository.skillEvidence.value.isEmpty())
        assertTrue(repository.institutions.value.isEmpty())
        assertTrue(repository.syncHistoryLogs.value.isEmpty())
        assertTrue(repository.activeDevices.value.isEmpty())
    }

    @Test
    fun permanentStudentId_isUniqueAndNeverReused() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
        val id1 = PermanentIdRegistry.issuePermanentId("Student Alpha", "42101-1111111-1")
        val id2 = PermanentIdRegistry.issuePermanentId("Student Beta", "42101-2222222-2")

        assertTrue(id1.startsWith("PK-DBUS-$currentYear-"))
        assertTrue(id2.startsWith("PK-DBUS-$currentYear-"))
        assertNotEquals(id1, id2)
        assertTrue(PermanentIdRegistry.isIdValidAndRegistered(id1))
        assertTrue(PermanentIdRegistry.isIdValidAndRegistered(id2))
    }

    @Test
    fun loginWithSupabase_handlesErrors_pendingUsers_andAllFourRoles() = runBlocking {
        // 1. Fails cleanly when Supabase URL/key is missing/placeholder
        repository.configureSupabaseForTest("https://xyzcompany.supabase.co", "public-anon-key-placeholder")
        val unconfiguredRes = repository.loginWithSchoolIdOrEmail("A-001", "SecretPass1")
        assertTrue(unconfiguredRes.isFailure)

        // 2. Wire test Supabase server
        val baseUrl = startMockSupabaseServer()
        repository.configureSupabaseForTest(baseUrl, "anon-test-key-123456")

        // Wrong password returns clear error
        val wrongPass = repository.loginWithSchoolIdOrEmail("A-001", "WrongPass123")
        assertTrue(wrongPass.isFailure)
        assertNull(repository.currentUser.value)

        // PendingApproval account is blocked cleanly
        val pendingRes = repository.loginWithSchoolIdOrEmail("P-999", "SecretPass1")
        assertTrue(pendingRes.isFailure)
        assertTrue(pendingRes.exceptionOrNull()?.message?.contains("pending", ignoreCase = true) == true)

        // All 4 roles open ONLY their database role
        val admin = repository.loginWithSchoolIdOrEmail("A-001", "SecretPass1").getOrThrow()
        assertEquals(UserRole.Administrator, admin.role)
        assertEquals(2, repository.studentsList.value.size)
        repository.logout()
        assertTrue(repository.studentsList.value.isEmpty())

        val teacher = repository.loginWithSchoolIdOrEmail("T-001", "SecretPass1").getOrThrow()
        assertEquals(UserRole.Teacher, teacher.role)
        assertEquals(1, repository.studentsList.value.size)
        assertEquals(stu1PermId, repository.studentsList.value.first().permanentStudentId)
        repository.logout()

        val student = repository.loginWithSchoolIdOrEmail("S-001", "SecretPass1").getOrThrow()
        assertEquals(UserRole.Student, student.role)
        assertEquals(stu1PermId, student.permanentStudentId)
        assertEquals(1, repository.studentsList.value.size)
        repository.logout()

        val parent = repository.loginWithSchoolIdOrEmail("P-001", "SecretPass1").getOrThrow()
        assertEquals(UserRole.Parent, parent.role)
        assertEquals(1, repository.studentsList.value.size)
        repository.logout()

        // Google ID token exchange routes strictly by database profile role
        val googleTeacher = repository.loginWithGoogleIdToken("valid-google-id-token").getOrThrow()
        assertEquals(UserRole.Teacher, googleTeacher.role)
        repository.logout()
        assertNull(repository.currentUser.value)
    }

    @Test
    fun coreSkills_dualNotesPrivacy_andRoleIsolationEnforced() = runBlocking {
        val baseUrl = startMockSupabaseServer()
        repository.configureSupabaseForTest(baseUrl, "anon-test-key-123456")
        val adminUser = repository.loginWithSchoolIdOrEmail("A-001", "SecretPass1").getOrThrow()

        val skills = CoreSkill.values()
        assertEquals(10, skills.size)
        assertEquals(1, SkillLevel.NEEDS_SUPPORT.score)
        assertEquals(4, SkillLevel.EXCELLENT.score)

        val scores = skills.associateWith { 4 }
        val sharedNotes = mapOf(CoreSkill.READING to "Strong comprehension progress.")
        val privateNotes = mapOf(CoreSkill.PRESSURE_HANDLING to "Faculty only observation note.")

        repository.saveSkillAssessment(
            studentPermanentId = stu1PermId,
            teacherName = adminUser.name,
            scores = scores,
            sharedNotes = sharedNotes,
            privateNotes = privateNotes,
            date = repository.todayDate()
        )

        val accounts = repository.schoolAccounts.value
        val teacherProfile = accounts.first { it.loginId == "T-001" }.toUserProfile()
        val studentProfile = accounts.first { it.loginId == "S-001" }.toUserProfile()
        val parentProfile = accounts.first { it.loginId == "P-001" }.toUserProfile()

        val student1 = repository.getStudentByPermanentId(stu1PermId)!!
        val student2 = repository.getStudentByPermanentId(stu2PermId)!!

        // Student sees own assessment WITHOUT private notes, and CANNOT access student2
        val stuHistory = repository.getSkillHistory(stu1PermId, studentProfile)
        assertEquals(1, stuHistory.size)
        assertTrue(stuHistory.first().privateNotes.isEmpty())
        assertEquals("Strong comprehension progress.", stuHistory.first().sharedNotes[CoreSkill.READING])
        assertNull(repository.getStudentByPermanentId(stu2PermId, studentProfile))
        assertTrue(repository.getSkillHistory(stu2PermId, studentProfile).isEmpty())
        assertTrue(repository.canUserAccessStorage(studentProfile, stu1PermId))
        assertFalse(repository.canUserAccessStorage(studentProfile, stu2PermId))

        // Parent sees own child WITHOUT private notes, CANNOT access student2
        val parentHistory = repository.getSkillHistory(stu1PermId, parentProfile)
        assertEquals(1, parentHistory.size)
        assertTrue(parentHistory.first().privateNotes.isEmpty())
        assertFalse(repository.canUserViewStudent(parentProfile, student2))
        assertFalse(repository.canUserAccessStorage(parentProfile, stu2PermId))

        // Teacher sees private notes ONLY for assigned student1, NOT unassigned student2
        assertTrue(repository.canUserViewStudent(teacherProfile, student1))
        assertFalse(repository.canUserViewStudent(teacherProfile, student2))
        val teacherHistory = repository.getSkillHistory(stu1PermId, teacherProfile)
        assertEquals("Faculty only observation note.", teacherHistory.first().privateNotes[CoreSkill.PRESSURE_HANDLING])
        assertTrue(repository.getSkillHistory(stu2PermId, teacherProfile).isEmpty())

        // Edit & delete assessment
        val evalId = teacherHistory.first().id
        val editRes = repository.editSkillAssessment(
            assessmentId = evalId,
            studentPermanentId = stu1PermId,
            scores = skills.associateWith { 3 },
            sharedNotes = mapOf(CoreSkill.READING to "Updated shared remark"),
            privateNotes = mapOf(CoreSkill.PRESSURE_HANDLING to "Updated private note"),
            editor = teacherProfile
        )
        assertTrue(editRes.isSuccess)
        assertEquals("Updated shared remark", repository.getLatestAssessment(stu1PermId)?.sharedNotes?.get(CoreSkill.READING))

        assertTrue(repository.deleteSkillAssessment(evalId, stu1PermId, adminUser).isSuccess)
        assertTrue(repository.getSkillHistory(stu1PermId).isEmpty())
    }

    @Test
    fun attendance_changeLog_alerts_andZeroWorkingDaysSafe() = runBlocking {
        val baseUrl = startMockSupabaseServer()
        repository.configureSupabaseForTest(baseUrl, "anon-test-key-123456")
        val adminUser = repository.loginWithSchoolIdOrEmail("A-001", "SecretPass1").getOrThrow()

        // Empty attendance summary never crashes or divides by zero
        val emptySummary = repository.getAttendanceSummary(stu1PermId)
        assertEquals(0, emptySummary.totalWorkingDays)
        assertEquals(0f, emptySummary.attendancePercentage, 0.001f)

        val date = repository.todayDate()
        repository.markDailyAttendance(stu1PermId, date, AttendanceStatus.PRESENT, "On time", adminUser.name)
        var rec = repository.getDailyRecord(stu1PermId, date)!!
        assertEquals(AttendanceStatus.PRESENT, rec.status)
        assertEquals(0, rec.changeLog.size)

        // Same day edit appends changeLog and fires parent alert when ABSENT
        repository.markDailyAttendance(stu1PermId, date, AttendanceStatus.ABSENT, "Family emergency", adminUser.name)
        rec = repository.getDailyRecord(stu1PermId, date)!!
        assertEquals(AttendanceStatus.ABSENT, rec.status)
        assertEquals(1, rec.changeLog.size)
        assertEquals(AttendanceStatus.PRESENT, rec.changeLog.first().previousStatus)
        assertTrue(repository.attendanceAlerts.value.any { it.studentPermanentId == stu1PermId && it.type == AttendanceAlertType.ABSENT_OR_LATE })

        repository.setLowAttendanceThreshold(80)
        assertEquals(80, repository.lowAttendanceThreshold.value)
    }

    @Test
    fun admin_createAccount_archiveStudent_andBFormMasking() = runBlocking {
        val baseUrl = startMockSupabaseServer()
        repository.configureSupabaseForTest(baseUrl, "anon-test-key-123456")
        val adminUser = repository.loginWithSchoolIdOrEmail("A-001", "SecretPass1").getOrThrow()

        // B-Form masking: Admin only
        val masked = repository.getMaskedBForm("42101-1112223-1", UserRole.Administrator)
        assertNotNull(masked)
        assertTrue(masked!!.endsWith("2231"))
        assertNull(repository.getMaskedBForm("42101-1112223-1", UserRole.Teacher))
        assertNull(repository.getMaskedBForm("42101-1112223-1", UserRole.Parent))
        assertNull(repository.getMaskedBForm("42101-1112223-1", UserRole.Student))

        // Admin creates a new student via Supabase signup & profiles/students upsert
        val createRes = repository.createAccountSuspend(
            name = "New Student Three",
            email = "student3@school.test",
            phone = "+923001112233",
            password = "Password99",
            role = UserRole.Student,
            grade = "Grade 8",
            section = "Section A",
            fatherName = "Father Three",
            bForm = "42101-3334445-1",
            actingUser = adminUser
        )
        assertTrue(createRes.isSuccess)
        val createdPermId = createRes.getOrThrow().permanentStudentId!!
        assertTrue(PermanentIdRegistry.isIdValidAndRegistered(createdPermId))

        // Archive keeps ID registered and blocks non-admins
        assertTrue(repository.archiveStudent(createdPermId).isSuccess)
        assertTrue(repository.getStudentByPermanentId(createdPermId)?.isArchived == true)
        assertTrue(PermanentIdRegistry.isIdValidAndRegistered(createdPermId))
    }

    @Test
    fun activityCrud_calendar_andCalculator_workCorrectly() = runBlocking {
        val baseUrl = startMockSupabaseServer()
        repository.configureSupabaseForTest(baseUrl, "anon-test-key-123456")
        val adminUser = repository.loginWithSchoolIdOrEmail("A-001", "SecretPass1").getOrThrow()

        // Subjects CRUD
        val sub = SubjectItem("sub_1", "Mathematics", "ریاضی", "MTH-101", "Grade 9", adminUser.name, 5, "Algebra")
        assertTrue(repository.addSubject(sub, adminUser).isSuccess)
        assertTrue(repository.updateSubject(sub.copy(weeklyHours = 6), adminUser).isSuccess)
        assertEquals(6, repository.subjectsList.value.first().weeklyHours)
        assertTrue(repository.deleteSubject("sub_1", adminUser).isSuccess)
        assertTrue(repository.subjectsList.value.isEmpty())

        // Assignments CRUD
        val assign = StudentAssignment("asgn_1", stu1PermId, "Essay", "English", repository.todayDate())
        assertTrue(repository.addAssignment(assign, adminUser).isSuccess)
        assertTrue(repository.deleteAssignment("asgn_1", adminUser).isSuccess)

        // Calendar CRUD
        val ev = SchoolCalendarEvent("cal_1", "Midterm", "وسط مدتی", repository.todayDate(), CalendarEventType.EXAM, "Hall A")
        assertTrue(repository.addCalendarEvent(ev, adminUser).isSuccess)
        assertTrue(repository.updateCalendarEvent(ev.copy(description = "Hall B"), adminUser).isSuccess)
        assertEquals("Hall B", repository.schoolCalendarEvents.value.first().description)
        assertTrue(repository.deleteCalendarEvent("cal_1", adminUser).isSuccess)

        // Calculator math & formulas
        assertEquals(40.0, SchoolCalculatorUtil.evaluateExpression("(2 + 3) * 8").getOrNull()!!, 0.001)
        assertEquals("A+", SchoolCalculatorUtil.calculateGrade(85.0, 100.0).gradeLetter)
        assertTrue(SchoolCalculatorUtil.calculateAttendancePercentage(14, 20).second)
        assertEquals(100f, SchoolCalculatorUtil.calculateAttendancePercentage(0, 0).first, 0.001f)
    }

    @Test
    fun mediaUtils_validatesSizeAndDurationLimits() {
        assertTrue(MediaUtils.validateMediaUpload(MediaType.PHOTO, 2 * 1024 * 1024L).first)
        assertFalse(MediaUtils.validateMediaUpload(MediaType.PHOTO, 12 * 1024 * 1024L).first)

        assertTrue(MediaUtils.validateMediaUpload(MediaType.VIDEO, 40 * 1024 * 1024L, durationSeconds = 90).first)
        val overDuration = MediaUtils.validateMediaUpload(MediaType.VIDEO, 10 * 1024 * 1024L, durationSeconds = 121)
        assertFalse(overDuration.first)
        assertTrue(overDuration.second!!.contains("2 minutes"))

        val overSize = MediaUtils.validateMediaUpload(MediaType.VIDEO, 51 * 1024 * 1024L, durationSeconds = 60)
        assertFalse(overSize.first)
        assertTrue(overSize.second!!.contains("50 MB"))

        assertFalse(MediaUtils.validateMediaUpload(MediaType.AUDIO, 26 * 1024 * 1024L).first)
        assertFalse(MediaUtils.validateMediaUpload(MediaType.DOCUMENT, 26 * 1024 * 1024L).first)
        assertEquals("01:35", MediaUtils.formatDuration(95))
    }

    @Test
    fun exams_announcements_andClassMediaPermissionsWorkCorrectly() = runBlocking {
        val baseUrl = startMockSupabaseServer()
        repository.configureSupabaseForTest(baseUrl, "anon-test-key-123456")
        val adminUser = repository.loginWithSchoolIdOrEmail("A-001", "SecretPass1").getOrThrow()
        val accounts = repository.schoolAccounts.value
        val teacherProfile = accounts.first { it.loginId == "T-001" }.toUserProfile()
        val parentProfile = accounts.first { it.loginId == "P-001" }.toUserProfile()
        val studentProfile = accounts.first { it.loginId == "S-001" }.toUserProfile()

        // Exam schedules & results CRUD
        val sched = ExamScheduleItem("ex_1", "Final", "Mathematics", repository.todayDate(), "09:00 AM", "Room 1", 100, 40)
        assertTrue(repository.addExamSchedule(sched, adminUser).isSuccess)
        assertEquals(1, repository.examSchedules.value.size)
        repository.recordExamResult(StudentExamResult("res_1", stu1PermId, "Final", "Term 1", "Mathematics", 92, 100, "A+", "Great", repository.todayDate()))
        assertEquals(1, repository.getExamResultsForStudent(stu1PermId).size)
        assertTrue(repository.deleteExamSchedule("ex_1", adminUser).isSuccess)

        // Announcements & comments
        val ann = ParentTeacherAnnouncement("ann_1", "PTM", "میٹنگ", "Saturday at 10 AM", "ہفتہ صبح 10 بجے", adminUser.name, UserRole.Administrator, "All Grades", repository.todayDate())
        repository.addAnnouncement(ann)
        repository.addAnnouncementComment("ann_1", AnnouncementComment("c1", parentProfile.name, UserRole.Parent, "We will attend", "10:05"))
        assertEquals(1, repository.parentAnnouncements.value.first().comments.size)

        // Class activity media: only creator teacher or Admin can delete
        val media = ClassActivityMedia(
            id = "med_1",
            studentPermanentId = stu1PermId,
            teacherId = teacherProfile.id,
            teacherName = teacherProfile.name,
            activityName = "Presentation",
            date = repository.todayDate(),
            mediaType = MediaType.VIDEO,
            caption = "Recitation",
            fileUri = "student-media/$stu1PermId/clip.mp4",
            fileSizeBytes = 1024L,
            durationSeconds = 30
        )
        assertTrue(repository.addClassActivityMedia(media, teacherProfile).isSuccess)
        assertTrue(repository.deleteClassActivityMedia("med_1", parentProfile).isFailure)
        assertTrue(repository.deleteClassActivityMedia("med_1", studentProfile).isFailure)
        assertTrue(repository.deleteClassActivityMedia("med_1", teacherProfile).isSuccess)
    }

    @Test
    fun clientKey_neverUsesServiceRole() {
        assertFalse(BuildConfig.SUPABASE_ANON_KEY.contains("service_role", ignoreCase = true))
        assertFalse(BuildConfig.SUPABASE_ANON_KEY.startsWith("sb_secret_", ignoreCase = true))
        assertTrue(com.example.data.SupabaseClient("https://example.supabase.co", "sb_publishable_demo_key").isConfigured())
        assertFalse(com.example.data.SupabaseClient("https://example.supabase.co", "sb_secret_demo_key").isConfigured())
    }
}
