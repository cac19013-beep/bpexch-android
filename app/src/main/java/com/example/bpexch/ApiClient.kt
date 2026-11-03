package com.example.bpexch

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class ApiException(message: String, val status: Int = 0) : Exception(message)

data class TransactionItem(
    val id: String,
    val type: String,
    val amount: Long,
    val method: String,
    val status: String,
    val createdAt: String,
)

data class SupportMessage(
    val id: String,
    val sender: String,
    val text: String,
)

data class PaymentMethod(
    val channelName: String,
    val accountTitle: String,
    val accountNumber: String,
    val iban: String?,
)

/**
 * Talks to the Next.js backend (which uses Supabase). Requests run on one background thread and
 * results are delivered on the main thread. A 401 triggers a single token refresh and retry.
 */
class ApiClient(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("session", Context.MODE_PRIVATE)
    private val baseUrl = context.applicationContext.getString(R.string.api_base_url).trimEnd('/')
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    val isLoggedIn: Boolean get() = !prefs.getString(KEY_TOKEN, null).isNullOrEmpty()
    val username: String get() = prefs.getString(KEY_USERNAME, "") ?: ""

    /** The password typed at login, decrypted from the Keystore-backed copy. Null if unavailable. */
    val savedPassword: String? get() = PasswordVault.decrypt(prefs.getString(KEY_PASSWORD, null))

    /** Last known status, so a pending user lands on the verification screen at launch. */
    val cachedStatus: String get() = prefs.getString(KEY_STATUS, "approved") ?: "approved"

    fun logout() = prefs.edit().clear().apply()

    private fun saveStatus(status: String) = prefs.edit().putString(KEY_STATUS, status).apply()

    // ---- public API ----

    /** Returns the account status: "approved" or "pending". */
    fun login(username: String, password: String, done: (Result<String>) -> Unit) {
        enqueue(done, {
            val res = request("POST", "/api/auth/login", JSONObject().put("username", username).put("password", password), auth = false)
            val user = res.getJSONObject("user")
            saveSession(res, user.getString("username"))
            prefs.edit().putString(KEY_PASSWORD, PasswordVault.encrypt(password)).apply()
            user.optString("status", "approved").also { saveStatus(it) }
        })
    }

    fun register(username: String, password: String, done: (Result<String>) -> Unit) {
        enqueue(done, {
            val res = request("POST", "/api/auth/register", JSONObject().put("username", username).put("password", password), auth = false)
            if (res.has("token")) {
                // Signed in while pending, so the app can poll its approval status.
                saveSession(res, res.getJSONObject("user").getString("username"))
                prefs.edit().putString(KEY_PASSWORD, PasswordVault.encrypt(password)).apply()
                saveStatus("pending")
            }
            res.optString("message", "Your account has been created. Please wait for approval.")
        })
    }

    /** Current account status from the server: "pending", "approved" or "rejected". */
    fun profile(done: (Result<String>) -> Unit) {
        enqueue(done, {
            request("GET", "/api/user/profile").getJSONObject("user").getString("status").also { saveStatus(it) }
        })
    }

    fun paymentMethods(done: (Result<List<PaymentMethod>>) -> Unit) {
        enqueue(done, {
            val arr = request("GET", "/api/payment-methods").getJSONArray("methods")
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                PaymentMethod(
                    o.getString("channelName"),
                    o.getString("accountTitle"),
                    o.getString("accountNumber"),
                    o.optString("iban").takeIf { it.isNotEmpty() && it != "null" },
                )
            }
        })
    }

    /** [receipt] is the payment screenshot as base64 (JPEG). */
    fun deposit(amount: Long, method: String, receipt: String, done: (Result<TransactionItem>) -> Unit) {
        enqueue(done, {
            val body = JSONObject().put("amount", amount).put("method", method).put("receipt", receipt)
            parseTransaction(request("POST", "/api/deposit", body).getJSONObject("transaction"))
        })
    }

    fun withdraw(amount: Long, method: String, accountTitle: String, accountNumber: String, done: (Result<TransactionItem>) -> Unit) {
        enqueue(done, {
            val body = JSONObject()
                .put("amount", amount)
                .put("method", method)
                .put("accountTitle", accountTitle)
                .put("accountNumber", accountNumber)
            parseTransaction(request("POST", "/api/withdraw", body).getJSONObject("transaction"))
        })
    }

    fun history(done: (Result<List<TransactionItem>>) -> Unit) {
        enqueue(done, {
            val arr = request("GET", "/api/history").getJSONArray("transactions")
            List(arr.length()) { i -> parseTransaction(arr.getJSONObject(i)) }
        })
    }

    fun supportMessages(done: (Result<List<SupportMessage>>) -> Unit) {
        enqueue(done, {
            val arr = request("GET", "/api/support").getJSONArray("messages")
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                SupportMessage(o.getString("id"), o.getString("sender"), o.optString("text", ""))
            }
        })
    }

    fun sendSupportMessage(text: String, done: (Result<SupportMessage>) -> Unit) {
        enqueue(done, {
            val o = request("POST", "/api/support", JSONObject().put("text", text)).getJSONObject("message")
            SupportMessage(o.getString("id"), o.getString("sender"), o.optString("text", ""))
        })
    }

    // ---- internals ----

    private fun <T> enqueue(done: (Result<T>) -> Unit, block: () -> T) {
        io.execute {
            val result = try {
                Result.success(block())
            } catch (e: ApiException) {
                Result.failure(e)
            } catch (e: IOException) {
                Result.failure(ApiException("Can't reach the server. Check your connection."))
            } catch (e: Exception) {
                Result.failure(ApiException("Something went wrong. Please try again."))
            }
            main.post { done(result) }
        }
    }

    private fun parseTransaction(o: JSONObject) = TransactionItem(
        id = o.getString("id"),
        type = o.getString("type"),
        amount = o.getLong("amount"),
        method = o.getString("method"),
        status = o.getString("status"),
        createdAt = o.getString("createdAt"),
    )

    private fun saveSession(res: JSONObject, username: String) {
        prefs.edit()
            .putString(KEY_TOKEN, res.getString("token"))
            .putString(KEY_REFRESH, res.getString("refreshToken"))
            .putString(KEY_USERNAME, username)
            .apply()
    }

    /** Must run on the io thread. Returns the parsed JSON body, or throws ApiException. */
    private fun request(method: String, path: String, body: JSONObject? = null, auth: Boolean = true, retried: Boolean = false): JSONObject {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 10_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Accept", "application/json")
            if (auth) conn.setRequestProperty("Authorization", "Bearer " + (prefs.getString(KEY_TOKEN, "") ?: ""))
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }

            val status = conn.responseCode
            val text = (if (status in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
            val json = try { JSONObject(text) } catch (e: Exception) { JSONObject() }

            if (status == 401 && auth && !retried && refreshSession()) {
                return request(method, path, body, auth, retried = true)
            }
            if (status !in 200..299 || !json.optBoolean("success", false)) {
                // A suspended account can't use the app either: end the session instead of leaving it half-working.
                if (auth && (status == 401 || (status == 403 && json.optString("error") == "Account is suspended"))) logout()
                // Validation errors carry the specific reason (e.g. which username rule failed).
                val reason = json.optJSONArray("errors")?.optJSONObject(0)?.optString("message")?.takeIf { it.isNotEmpty() }
                throw ApiException(reason ?: json.optString("error", "Request failed ($status)"), status)
            }
            return json
        } finally {
            conn.disconnect()
        }
    }

    private fun refreshSession(): Boolean {
        val refresh = prefs.getString(KEY_REFRESH, null) ?: return false
        return try {
            val res = request("POST", "/api/auth/refresh", JSONObject().put("refreshToken", refresh), auth = false)
            saveSession(res, username)
            true
        } catch (e: ApiException) {
            false
        }
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password_enc"
        const val KEY_STATUS = "status"
    }
}
