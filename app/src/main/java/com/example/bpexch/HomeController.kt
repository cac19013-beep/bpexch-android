package com.example.bpexch

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Home tab: quick actions, language pill and the announcement banner. */
class HomeController(private val ctx: AppContext) {
    private val b = ctx.binding
    private var passwordVisible = false

    fun setup() {
        showActiveLanguage()

        b.btnCopyUsername.setOnClickListener {
            ctx.copyToClipboard(ctx.str(R.string.username), b.tvUsernameValue.text.toString())
        }

        // Password: the copy typed at login, kept encrypted on this device only
        b.btnTogglePassword.setOnClickListener {
            val saved = ctx.api.savedPassword
            if (saved == null) {
                ctx.toast(ctx.str(R.string.password_login_again_view))
                return@setOnClickListener
            }
            passwordVisible = !passwordVisible
            b.tvPasswordValue.text = if (passwordVisible) saved else MASK
            b.btnTogglePassword.setImageResource(if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye)
        }

        b.btnCopyPassword.setOnClickListener {
            val saved = ctx.api.savedPassword
            if (saved == null) ctx.toast(ctx.str(R.string.password_login_again_copy))
            else ctx.copyToClipboard(ctx.str(R.string.password), saved)
        }

        b.btnOpenWebsite.setOnClickListener {
            try {
                ctx.activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bpexch.com")))
            } catch (e: Exception) {
                ctx.toast(ctx.str(R.string.opening_website))
            }
        }

        b.cardHomeDeposit.setOnClickListener { ctx.deposit.openFlow() }
        b.cardHomeWithdraw.setOnClickListener { ctx.withdraw.openFlow() }
        b.cardHomeWhatsApp.setOnClickListener { ctx.support.open() }
        languagePills.forEach { it.pill.setOnClickListener { toggleLanguage() } }
        b.tvNotificationBadge.visibility = if (ctx.sheets.notificationsSeen) View.GONE else View.VISIBLE
        b.btnNotifications.setOnClickListener {
            ctx.sheets.showNotifications()
            b.tvNotificationBadge.visibility = View.GONE
        }
        b.btnProfile.setOnClickListener { ctx.sheets.showProfile() }
    }

    /** Hides the password again, e.g. after signing in. */
    fun resetPasswordDisplay() {
        passwordVisible = false
        b.tvPasswordValue.text = MASK
        b.btnTogglePassword.setImageResource(R.drawable.ic_eye)
    }

    fun setupAnnouncementBar() {
        b.tvAnnouncementTag.text = ctx.str(R.string.important_update)
        b.tvAnnouncementHeadline.text = ctx.str(R.string.please_check_our_official_website)
        b.tvAnnouncementSub.text = ctx.str(R.string.please_confirm_details_from_our)
    }

    private class LanguagePill(val pill: View, val en: TextView, val ur: TextView)

    // The same EN / اردو pill appears in the Home, Deposit and Withdraw headers.
    private val languagePills = listOf(
        LanguagePill(b.pillLanguage, b.tvLangEn, b.tvLangUr),
        LanguagePill(b.pillLanguageDeposit, b.tvLangEnDeposit, b.tvLangUrDeposit),
        LanguagePill(b.pillLanguageWithdraw, b.tvLangEnWithdraw, b.tvLangUrWithdraw),
    )

    /** Highlights the active EN / اردو side for the language the app is currently showing. */
    private fun showActiveLanguage() {
        val selected = ctx.drawable(R.drawable.bg_pill_lang_selected)
        val urdu = isUrdu()
        languagePills.forEach { p ->
            p.en.background = if (urdu) null else selected
            p.en.setTextColor(ctx.color(if (urdu) R.color.text_secondary else R.color.text_primary))
            p.ur.background = if (urdu) selected else null
            p.ur.setTextColor(ctx.color(if (urdu) R.color.text_primary else R.color.text_secondary))
        }
    }

    private fun isUrdu(): Boolean = ctx.activity.resources.configuration.locales[0].language == "ur"

    /** Switches the whole app between English and Urdu; the choice is remembered by AppCompat. */
    private fun toggleLanguage() {
        val target = if (isUrdu()) "en" else "ur"
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(target))
    }

    private companion object {
        const val MASK = "••••••"
    }
}
