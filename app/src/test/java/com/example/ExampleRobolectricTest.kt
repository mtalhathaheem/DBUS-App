package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.PermanentIdRegistry
import com.example.data.SchoolRepository
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.InetSocketAddress

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private var server: HttpServer? = null

    private val adminId = "11111111-1111-1111-1111-111111111111"
    private val teacherId = "22222222-2222-2222-2222-222222222222"
    private val parentId = "33333333-3333-3333-3333-333333333333"
    private val studentId = "44444444-4444-4444-4444-444444444444"

    private fun startEmptyDbMockSupabase(): String {
        val srv = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        srv.createContext("/") { ex -> handleRequest(ex) }
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

    private fun handleRequest(ex: HttpExchange) {
        val path = ex.requestURI.path
        val query = ex.requestURI.rawQuery.orEmpty()
        val method = ex.requestMethod
        val body = ex.requestBody.bufferedReader().readText()

        when {
            path == "/rest/v1/rpc/resolve_login_email" && method == "POST" -> {
                val id = JSONObject(body).optString("p_identifier").trim().uppercase()
                val email = when (id) {
                    "A-001" -> "\"admin@dbus.test\""
                    "T-001" -> "\"teacher@dbus.test\""
                    "S-001" -> "\"student@dbus.test\""
                    "P-001" -> "\"parent@dbus.test\""
                    else -> "null"
                }
                respondJson(ex, 200, email)
            }

            path == "/auth/v1/token" && method == "POST" -> {
                val obj = JSONObject(body)
                val email = obj.optString("email")
                val password = obj.optString("password")
                if (password != "SecretPass1") {
                    respondJson(ex, 400, """{"error_description":"Invalid login credentials"}""")
                    return
                }
                val uid = when (email) {
                    "admin@dbus.test" -> adminId
                    "teacher@dbus.test" -> teacherId
                    "student@dbus.test" -> studentId
                    "parent@dbus.test" -> parentId
                    else -> {
                        respondJson(ex, 400, """{"error_description":"Invalid login credentials"}""")
                        return
                    }
                }
                respondJson(
                    ex,
                    200,
                    """{"access_token":"jwt-$uid","refresh_token":"ref-$uid","user":{"id":"$uid","email":"$email"}}"""
                )
            }

            path == "/auth/v1/logout" -> respondJson(ex, 204, "")

            path == "/rest/v1/profiles" && method == "GET" -> {
                val all = JSONArray().apply {
                    put(profileObj(adminId, "A-001", "DBUS Admin", "admin@dbus.test", "Administrator"))
                    put(profileObj(teacherId, "T-001", "DBUS Teacher", "teacher@dbus.test", "Teacher"))
                    put(profileObj(studentId, "S-001", "DBUS Student", "student@dbus.test", "Student"))
                    put(profileObj(parentId, "P-001", "DBUS Parent", "parent@dbus.test", "Parent"))
                }
                if (query.contains("id=eq.")) {
                    val targetId = query.substringAfter("id=eq.").substringBefore("&")
                    val filtered = JSONArray()
                    for (i in 0 until all.length()) {
                        if (all.getJSONObject(i).getString("id") == targetId) filtered.put(all.getJSONObject(i))
                    }
                    respondJson(ex, 200, filtered.toString())
                } else {
                    respondJson(ex, 200, all.toString())
                }
            }

            path.startsWith("/rest/v1/") -> respondJson(ex, 200, "[]")
            else -> respondJson(ex, 200, "{}")
        }
    }

    private fun profileObj(id: String, loginId: String, name: String, email: String, role: String): JSONObject =
        JSONObject().apply {
            put("id", id)
            put("login_id", loginId)
            put("name", name)
            put("email", email)
            put("phone", "")
            put("role", role)
            put("approval_status", "Approved")
            put("avatar_color_hex", 0xFF1B3B6F)
        }

    @Before
    fun resetSession() {
        val repo = SchoolRepository.getInstance()
        repo.logout()
        PermanentIdRegistry.reset()
        repo.setOnline(true)
        composeTestRule.waitForIdle()
    }

    @After
    fun tearDown() {
        SchoolRepository.getInstance().logout()
        server?.stop(0)
        server = null
    }

    private fun loginThroughUi(id: String, password: String) {
        composeTestRule.onNodeWithTag("input_email").performScrollTo().performTextInput(id)
        composeTestRule.onNodeWithTag("input_password").performScrollTo().performTextInput(password)
        composeTestRule.onNodeWithTag("btn_auth_submit").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("nav_dashboard").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    private fun logoutViaAccountTab() {
        composeTestRule.onNodeWithTag("nav_account").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_settings_logout").performScrollTo().performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("btn_auth_submit").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun firstScreen_isLoginOnly_noSplashNoSkipNoSocialButtons_strictDbusBranding() {
        // Immediately on AuthScreen without clicking any splash screen
        assertTrue(composeTestRule.onAllNodesWithTag("opening_splash_screen").fetchSemanticsNodes().isEmpty())
        composeTestRule.onNodeWithTag("input_email").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_password").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_auth_submit").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_phone_otp").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_google_credential_signin").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_forgot_password").performScrollTo().assertIsDisplayed()

        // No skip button, no Apple/social buttons, no quick demo, no "DBus" mixed-case text
        assertTrue(composeTestRule.onAllNodesWithText("Skip", substring = true, ignoreCase = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodesWithText("Who are you?", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodesWithText("Quick Demo", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodesWithTag("btn_social_google").fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodesWithTag("btn_social_facebook").fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodesWithTag("btn_social_microsoft").fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodesWithText("Apple", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodesWithText("DBus", substring = true, ignoreCase = false).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun loginFailure_displaysErrorWithoutCrashing() {
        val baseUrl = startEmptyDbMockSupabase()
        SchoolRepository.getInstance().configureSupabaseForTest(baseUrl, "anon-key-test")

        composeTestRule.onNodeWithTag("input_email").performScrollTo().performTextInput("A-001")
        composeTestRule.onNodeWithTag("input_password").performScrollTo().performTextInput("WrongPass9")
        composeTestRule.onNodeWithTag("btn_auth_submit").performScrollTo().performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("auth_error_banner").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("auth_error_banner").assertIsDisplayed()
    }

    @Test
    fun adminPortal_withEmptyDatabase_allTabsCalendarCalculatorRenderCleanly() {
        val baseUrl = startEmptyDbMockSupabase()
        SchoolRepository.getInstance().configureSupabaseForTest(baseUrl, "anon-key-test")

        loginThroughUi("A-001", "SecretPass1")

        // TopBar DBUS + single role icon + Calendar button; NO Cloud/RLS chips or global calculator button
        composeTestRule.onNodeWithText("DBUS").assertIsDisplayed()
        composeTestRule.onNodeWithTag("role_icon_badge_administrator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_tools_calendar").assertIsDisplayed()
        assertTrue(composeTestRule.onAllNodesWithText("Cloud Connected", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodesWithText("RLS Policy Enforced", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeTestRule.onAllNodesWithTag("btn_activity_calculator").fetchSemanticsNodes().isEmpty())

        // Open Full Calendar -> empty state + calendar grid render cleanly
        composeTestRule.onNodeWithTag("btn_tools_calendar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calendar_month_grid_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_calendar_tools_back").performClick()
        composeTestRule.waitForIdle()

        // Attendance tab -> empty database + weekly chart does not crash (weight > 0f safe)
        composeTestRule.onNodeWithTag("nav_attendance").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("No students", substring = true, ignoreCase = true).performScrollTo().assertIsDisplayed()

        // Activity tab -> calculator icon exists inside Activity only -> opens calculator safely
        composeTestRule.onNodeWithTag("nav_activity").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_activity_calculator").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DBUS School Calculator").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_activity_calc_back").performClick()
        composeTestRule.waitForIdle()

        // Logout returns cleanly to AuthScreen
        logoutViaAccountTab()
        composeTestRule.onNodeWithTag("btn_auth_submit").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun allOtherPortals_openByDatabaseRole_withEmptyStatesWithoutCrash() {
        val baseUrl = startEmptyDbMockSupabase()
        SchoolRepository.getInstance().configureSupabaseForTest(baseUrl, "anon-key-test")

        loginThroughUi("T-001", "SecretPass1")
        composeTestRule.onNodeWithTag("role_icon_badge_teacher").assertIsDisplayed()
        logoutViaAccountTab()

        loginThroughUi("S-001", "SecretPass1")
        composeTestRule.onNodeWithTag("role_icon_badge_student").assertIsDisplayed()
        composeTestRule.onNodeWithTag("empty_student_profile").performScrollTo().assertIsDisplayed()
        logoutViaAccountTab()

        loginThroughUi("P-001", "SecretPass1")
        composeTestRule.onNodeWithTag("role_icon_badge_parent").assertIsDisplayed()
        logoutViaAccountTab()
    }

    @Test
    fun activityTabs_andSettingsSubpages_navigateAndReturnCleanly() {
        val baseUrl = startEmptyDbMockSupabase()
        SchoolRepository.getInstance().configureSupabaseForTest(baseUrl, "anon-key-test")

        loginThroughUi("A-001", "SecretPass1")

        // Activity tabs
        composeTestRule.onNodeWithTag("nav_activity").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tab_activity_subjects").performClick()
        composeTestRule.onNodeWithTag("tab_activity_media").performClick()
        composeTestRule.onNodeWithTag("tab_activity_assignments").performClick()
        composeTestRule.onNodeWithTag("tab_activity_examinations").performClick()
        composeTestRule.onNodeWithTag("tab_activity_communication").performClick()
        composeTestRule.waitForIdle()

        // Account -> Settings Language & Region RTL switch and back
        composeTestRule.onNodeWithTag("nav_account").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_language_region").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lang_urdu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("lang_english").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_back_lang_region").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_appearance").performScrollTo().assertIsDisplayed()
    }
}
