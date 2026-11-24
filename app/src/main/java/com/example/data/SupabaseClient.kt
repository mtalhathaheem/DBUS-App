package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class SupabaseAuthSession(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val email: String
)

/**
 * HTTP client for Supabase Auth, PostgREST (with Row Level Security), and Private Storage.
 * Strictly uses only the project's ANON key + per-user JWT. Never uses a service_role key.
 */
class SupabaseClient(
    var baseUrl: String = BuildConfig.SUPABASE_URL.trim().trimEnd('/'),
    var anonKey: String = BuildConfig.SUPABASE_ANON_KEY.trim(),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun isConfigured(): Boolean {
        val url = baseUrl.trim()
        val key = anonKey.trim()
        if (url.isBlank() || key.isBlank()) return false
        if (url.contains("xyzcompany.supabase.co", ignoreCase = true)) return false
        if (key == "public-anon-key-placeholder" || key.contains("placeholder", ignoreCase = true)) return false
        if (key.contains("service_role", ignoreCase = true) || key.startsWith("sb_secret_", ignoreCase = true)) return false
        val parsed = url.toHttpUrlOrNull() ?: return false
        return parsed.scheme == "https" || parsed.scheme == "http"
    }

    fun configErrorMessage(isUrdu: Boolean = false): String {
        return if (isUrdu) {
            "سپا بیس کنفیگر نہیں ہے۔ AI Studio Secrets میں SUPABASE_URL اور SUPABASE_ANON_KEY درج کریں۔"
        } else {
            "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets."
        }
    }

    private fun requireConfigured(): Result<Unit> {
        return if (isConfigured()) Result.success(Unit)
        else Result.failure(IllegalStateException(configErrorMessage()))
    }

    private fun buildUrl(path: String): String {
        val cleanBase = baseUrl.trim().trimEnd('/')
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$cleanBase$cleanPath"
    }

    private fun extractErrorMessage(responseCode: Int, responseBody: String): String {
        return try {
            val trimmed = responseBody.trim()
            if (trimmed.startsWith("{")) {
                val obj = JSONObject(trimmed)
                val msg = obj.optString("error_description")
                    .ifBlank { obj.optString("msg") }
                    .ifBlank { obj.optString("message") }
                    .ifBlank { obj.optString("error") }
                when {
                    msg.contains("Invalid login credentials", ignoreCase = true) ->
                        "Invalid ID/Email or password. Please check your credentials and try again."
                    msg.contains("Email not confirmed", ignoreCase = true) ->
                        "Email address is not confirmed in Supabase Auth."
                    msg.isNotBlank() -> msg
                    else -> "Server error ($responseCode)"
                }
            } else if (trimmed.isNotBlank()) {
                trimmed.take(200)
            } else {
                "Server error ($responseCode)"
            }
        } catch (_: Exception) {
            "Server error ($responseCode)"
        }
    }

    private suspend fun executeRequest(request: Request): Result<Pair<Int, String>> = withContext(Dispatchers.IO) {
        try {
            httpClient.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Result.success(response.code to bodyStr)
                } else {
                    Result.failure(IllegalStateException(extractErrorMessage(response.code, bodyStr)))
                }
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException("Network error: ${e.message ?: "Unable to connect to Supabase"}"))
        }
    }

    suspend fun resolveLoginEmail(identifier: String): Result<String> {
        requireConfigured().onFailure { return Result.failure(it) }
        val trimmed = identifier.trim()
        val payload = JSONObject().apply { put("p_identifier", trimmed) }
        val reqBuilder = Request.Builder()
            .url(buildUrl("/rest/v1/rpc/resolve_login_email"))
            .post(payload.toString().toRequestBody(jsonMediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
        if (!anonKey.startsWith("sb_publishable_", ignoreCase = true)) {
            reqBuilder.addHeader("Authorization", "Bearer $anonKey")
        }
        val req = reqBuilder.build()

        return executeRequest(req).mapCatching { (_, body) ->
            val clean = body.trim().removeSurrounding("\"")
            if (clean.isBlank() || clean == "null") {
                throw IllegalArgumentException("Account not found for \"$trimmed\". Contact your DBUS Administrator.")
            }
            clean
        }
    }

    suspend fun signInWithPassword(email: String, password: String): Result<SupabaseAuthSession> {
        requireConfigured().onFailure { return Result.failure(it) }
        val payload = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
        }
        val req = Request.Builder()
            .url(buildUrl("/auth/v1/token?grant_type=password"))
            .post(payload.toString().toRequestBody(jsonMediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .build()

        return executeRequest(req).mapCatching { (_, body) ->
            val obj = JSONObject(body)
            val accessToken = obj.optString("access_token")
            val refreshToken = obj.optString("refresh_token")
            val userObj = obj.optJSONObject("user")
            val userId = userObj?.optString("id").orEmpty()
            val resolvedEmail = userObj?.optString("email").orEmpty().ifBlank { email.trim() }
            if (accessToken.isBlank() || userId.isBlank()) {
                throw IllegalStateException("Supabase returned an incomplete session.")
            }
            SupabaseAuthSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = userId,
                email = resolvedEmail
            )
        }
    }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<SupabaseAuthSession> {
        requireConfigured().onFailure { return Result.failure(it) }
        val payload = JSONObject().apply {
            put("provider", "google")
            put("id_token", idToken.trim())
        }
        val req = Request.Builder()
            .url(buildUrl("/auth/v1/token?grant_type=id_token"))
            .post(payload.toString().toRequestBody(jsonMediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .build()

        return executeRequest(req).mapCatching { (_, body) ->
            val obj = JSONObject(body)
            val accessToken = obj.optString("access_token")
            val refreshToken = obj.optString("refresh_token")
            val userObj = obj.optJSONObject("user")
            val userId = userObj?.optString("id").orEmpty()
            val resolvedEmail = userObj?.optString("email").orEmpty()
            if (accessToken.isBlank() || userId.isBlank()) {
                throw IllegalStateException("Supabase returned an incomplete Google session.")
            }
            SupabaseAuthSession(
                accessToken = accessToken,
                refreshToken = refreshToken,
                userId = userId,
                email = resolvedEmail
            )
        }
    }

    /**
     * Creates a new Supabase Auth user via /auth/v1/signup without replacing the currently
     * logged-in Administrator's session tokens.
     */
    suspend fun signUpUserByAdmin(
        email: String,
        password: String,
        name: String,
        roleName: String,
        loginId: String
    ): Result<String> {
        requireConfigured().onFailure { return Result.failure(it) }
        val dataObj = JSONObject().apply {
            put("name", name)
            put("role", roleName)
            put("login_id", loginId)
        }
        val payload = JSONObject().apply {
            put("email", email.trim())
            put("password", password)
            put("data", dataObj)
        }
        val req = Request.Builder()
            .url(buildUrl("/auth/v1/signup"))
            .post(payload.toString().toRequestBody(jsonMediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .build()

        return executeRequest(req).mapCatching { (_, body) ->
            val obj = JSONObject(body)
            val userId = obj.optJSONObject("user")?.optString("id")
                ?.ifBlank { obj.optString("id") }
                ?: obj.optString("id")
            if (userId.isBlank()) {
                throw IllegalStateException("Supabase did not return a user ID for the new account.")
            }
            userId
        }
    }

    suspend fun requestPhoneOtp(phone: String): Result<Unit> {
        requireConfigured().onFailure { return Result.failure(it) }
        val normalized = normalizePhone(phone)
        val payload = JSONObject().apply { put("phone", normalized) }
        val req = Request.Builder()
            .url(buildUrl("/auth/v1/otp"))
            .post(payload.toString().toRequestBody(jsonMediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .build()
        return executeRequest(req).map { Unit }
    }

    suspend fun verifyPhoneOtp(phone: String, token: String): Result<SupabaseAuthSession> {
        requireConfigured().onFailure { return Result.failure(it) }
        val normalized = normalizePhone(phone)
        val payload = JSONObject().apply {
            put("phone", normalized)
            put("token", token.trim())
            put("type", "sms")
        }
        val req = Request.Builder()
            .url(buildUrl("/auth/v1/verify"))
            .post(payload.toString().toRequestBody(jsonMediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .build()
        return executeRequest(req).mapCatching { (_, body) ->
            val obj = JSONObject(body)
            val accessToken = obj.optString("access_token")
            val refreshToken = obj.optString("refresh_token")
            val userObj = obj.optJSONObject("user")
            val userId = userObj?.optString("id").orEmpty()
            val resolvedEmail = userObj?.optString("email").orEmpty()
            if (accessToken.isBlank() || userId.isBlank()) {
                throw IllegalStateException("OTP verification failed to return a valid session.")
            }
            SupabaseAuthSession(accessToken, refreshToken, userId, resolvedEmail)
        }
    }

    suspend fun recoverPassword(email: String): Result<Unit> {
        requireConfigured().onFailure { return Result.failure(it) }
        val payload = JSONObject().apply { put("email", email.trim()) }
        val req = Request.Builder()
            .url(buildUrl("/auth/v1/recover"))
            .post(payload.toString().toRequestBody(jsonMediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Content-Type", "application/json")
            .build()
        return executeRequest(req).map { Unit }
    }

    suspend fun updatePassword(accessToken: String, newPassword: String): Result<Unit> {
        requireConfigured().onFailure { return Result.failure(it) }
        val payload = JSONObject().apply { put("password", newPassword) }
        val req = Request.Builder()
            .url(buildUrl("/auth/v1/user"))
            .put(payload.toString().toRequestBody(jsonMediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()
        return executeRequest(req).map { Unit }
    }

    suspend fun signOut(accessToken: String): Result<Unit> {
        if (!isConfigured() || accessToken.isBlank()) return Result.success(Unit)
        val req = Request.Builder()
            .url(buildUrl("/auth/v1/logout"))
            .post("{}".toRequestBody(jsonMediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        return executeRequest(req).map { Unit }
    }

    suspend fun restSelect(
        table: String,
        query: String = "select=*",
        accessToken: String
    ): Result<JSONArray> {
        requireConfigured().onFailure { return Result.failure(it) }
        val req = Request.Builder()
            .url(buildUrl("/rest/v1/$table?$query"))
            .get()
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Accept", "application/json")
            .build()
        return executeRequest(req).mapCatching { (_, body) ->
            if (body.isBlank()) JSONArray() else JSONArray(body)
        }
    }

    suspend fun restUpsert(
        table: String,
        body: JSONObject,
        onConflict: String? = null,
        accessToken: String
    ): Result<JSONArray> {
        requireConfigured().onFailure { return Result.failure(it) }
        val conflictParam = if (!onConflict.isNullOrBlank()) "?on_conflict=$onConflict" else ""
        val req = Request.Builder()
            .url(buildUrl("/rest/v1/$table$conflictParam"))
            .post(body.toString().toRequestBody(jsonMediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
            .build()
        return executeRequest(req).mapCatching { (_, resp) ->
            if (resp.isBlank()) JSONArray() else JSONArray(resp)
        }
    }

    suspend fun restPatch(
        table: String,
        filterQuery: String,
        body: JSONObject,
        accessToken: String
    ): Result<JSONArray> {
        requireConfigured().onFailure { return Result.failure(it) }
        val req = Request.Builder()
            .url(buildUrl("/rest/v1/$table?$filterQuery"))
            .patch(body.toString().toRequestBody(jsonMediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=representation")
            .build()
        return executeRequest(req).mapCatching { (_, resp) ->
            if (resp.isBlank()) JSONArray() else JSONArray(resp)
        }
    }

    suspend fun restDelete(
        table: String,
        filterQuery: String,
        accessToken: String
    ): Result<Unit> {
        requireConfigured().onFailure { return Result.failure(it) }
        val req = Request.Builder()
            .url(buildUrl("/rest/v1/$table?$filterQuery"))
            .delete()
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        return executeRequest(req).map { Unit }
    }

    suspend fun uploadStorageObject(
        bucket: String,
        objectPath: String,
        bytes: ByteArray,
        contentType: String,
        accessToken: String
    ): Result<String> {
        requireConfigured().onFailure { return Result.failure(it) }
        val cleanPath = objectPath.trimStart('/')
        val req = Request.Builder()
            .url(buildUrl("/storage/v1/object/$bucket/$cleanPath"))
            .post(bytes.toRequestBody(contentType.toMediaType()))
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("x-upsert", "true")
            .build()
        return executeRequest(req).map { cleanPath }
    }

    suspend fun deleteStorageObject(
        bucket: String,
        objectPath: String,
        accessToken: String
    ): Result<Unit> {
        requireConfigured().onFailure { return Result.failure(it) }
        val cleanPath = objectPath.trimStart('/')
        val req = Request.Builder()
            .url(buildUrl("/storage/v1/object/$bucket/$cleanPath"))
            .delete()
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        return executeRequest(req).map { Unit }
    }

    suspend fun createSignedStorageUrl(
        bucket: String,
        objectPath: String,
        expiresInSeconds: Int = 3600,
        accessToken: String
    ): Result<String> {
        requireConfigured().onFailure { return Result.failure(it) }
        val cleanPath = objectPath.trimStart('/')
        val payload = JSONObject().apply { put("expiresIn", expiresInSeconds) }
        val req = Request.Builder()
            .url(buildUrl("/storage/v1/object/sign/$bucket/$cleanPath"))
            .post(payload.toString().toRequestBody(jsonMediaType))
            .addHeader("apikey", anonKey)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()
        return executeRequest(req).mapCatching { (_, body) ->
            val obj = JSONObject(body)
            val signed = obj.optString("signedURL")
            if (signed.isBlank()) throw IllegalStateException("Could not generate signed URL")
            if (signed.startsWith("http")) signed else buildUrl("/storage/v1$signed")
        }
    }

    companion object {
        fun normalizePhone(phone: String): String {
            val digitsAndPlus = phone.trim().replace(" ", "").replace("-", "")
            return when {
                digitsAndPlus.startsWith("+") -> digitsAndPlus
                digitsAndPlus.startsWith("03") -> "+92" + digitsAndPlus.drop(1)
                digitsAndPlus.startsWith("92") -> "+$digitsAndPlus"
                else -> digitsAndPlus
            }
        }

        fun encodeQueryParam(value: String): String {
            return URLEncoder.encode(value, "UTF-8")
        }
    }
}
