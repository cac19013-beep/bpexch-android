package com.example.bpexch

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.bpexch.databinding.ActivityMainBinding

/**
 * Shared state handed to every feature controller: the host activity, its view binding, the API
 * client and the other controllers (so one feature can trigger another, e.g. deposit -> history).
 */
class AppContext(
    val activity: AppCompatActivity,
    val binding: ActivityMainBinding,
    val api: ApiClient,
) {
    lateinit var auth: AuthController
    lateinit var verification: VerificationController
    lateinit var navigation: NavigationController
    lateinit var home: HomeController
    lateinit var deposit: DepositController
    lateinit var withdraw: WithdrawController
    lateinit var history: HistoryController
    lateinit var support: SupportChatController
    lateinit var sheets: AppSheets

    val density: Float get() = activity.resources.displayMetrics.density

    fun color(resId: Int): Int = ContextCompat.getColor(activity, resId)
    fun colorStateList(resId: Int): ColorStateList? = ContextCompat.getColorStateList(activity, resId)
    fun drawable(resId: Int): Drawable? = ContextCompat.getDrawable(activity, resId)
    fun str(resId: Int, vararg args: Any): String = activity.getString(resId, *args)
    fun dp(value: Int): Int = (value * density).toInt()

    /** Full-screen spinner that also blocks taps while a request is running. */
    fun setLoading(show: Boolean) {
        binding.layoutLoading.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun toast(message: String, long: Boolean = false) {
        Toast.makeText(activity, message, if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()
    }

    fun showError(error: Throwable, fallback: String) {
        toast(error.message ?: fallback, long = true)
        returnToLoginIfSignedOut()
    }

    /**
     * The session may have ended (api.logout() already ran on an unrecoverable 401, or on a
     * suspended account). Background refreshes call this too, so a dead session never leaves the
     * user on a screen that silently stopped updating.
     */
    fun returnToLoginIfSignedOut() {
        if (api.isLoggedIn || binding.layoutMain.visibility != View.VISIBLE) return
        support.stopPolling()
        history.clear()
        support.clearRendered()
        binding.layoutMain.visibility = View.GONE
        binding.containerAuth.visibility = View.VISIBLE
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        toast(str(R.string.copied_to_clipboard, label))
    }

    fun openWhatsApp(phoneNumber: String, message: String) {
        try {
            val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=" + Uri.encode(message)
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            toast(str(R.string.opening_whatsapp_for, phoneNumber))
        }
    }

    /** Signs the user out locally and returns to the login screen. */
    fun logout() {
        support.stopPolling()
        api.logout()
        history.clear()
        support.clearRendered()
        binding.layoutMain.visibility = View.GONE
        binding.containerAuth.visibility = View.VISIBLE
        toast(str(R.string.logged_out))
    }
}
