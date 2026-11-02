package com.example.bpexch

import android.text.InputType
import android.view.View

/** Login / register screen. */
class AuthController(private val ctx: AppContext) {
    private val b = ctx.binding
    private val api = ctx.api
    private var isAuthPasswordVisible = false
    private var isCreateAccountMode = false

    fun setup() {
        b.btnAuthTogglePassword.setOnClickListener {
            isAuthPasswordVisible = !isAuthPasswordVisible
            if (isAuthPasswordVisible) {
                b.etAuthPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                b.btnAuthTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                b.etAuthPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                b.btnAuthTogglePassword.setImageResource(R.drawable.ic_eye)
            }
            b.etAuthPassword.setSelection(b.etAuthPassword.text.length)
        }

        b.btnAuthLogin.setOnClickListener { submit() }
        b.boxCreateAccount.setOnClickListener { toggleCreateAccountMode() }
        b.boxWhatsApp.setOnClickListener { ctx.support.open() }
        b.boxDummyVideo.setOnClickListener { ctx.sheets.showVideoGuide() }
    }

    private fun submit() {
        val username = b.etAuthUsername.text.toString().trim()
        val password = b.etAuthPassword.text.toString().trim()

        if (username.isEmpty()) {
            b.etAuthUsername.error = ctx.str(R.string.auth_enter_username)
            b.etAuthUsername.requestFocus()
            return
        }
        if (password.isEmpty()) {
            b.etAuthPassword.error = ctx.str(R.string.auth_enter_password)
            b.etAuthPassword.requestFocus()
            return
        }

        b.btnAuthLogin.isEnabled = false
        ctx.setLoading(true)
        if (isCreateAccountMode) {
            api.register(username, password) { result ->
                b.btnAuthLogin.isEnabled = true
                ctx.setLoading(false)
                result.onSuccess { message ->
                    if (api.isLoggedIn) {
                        ctx.verification.show(api.username)
                    } else {
                        ctx.toast(message, long = true)
                        b.boxCreateAccount.performClick() // back to login
                    }
                }.onFailure {
                    ctx.toast(it.message ?: ctx.str(R.string.auth_create_failed), long = true)
                }
            }
        } else {
            api.login(username, password) { result ->
                b.btnAuthLogin.isEnabled = true
                ctx.setLoading(false)
                result.onSuccess { status ->
                    if (status == "pending") {
                        ctx.verification.show(api.username)
                    } else {
                        enterMain(api.username)
                        ctx.toast(ctx.str(R.string.auth_welcome_back, api.username))
                    }
                }.onFailure {
                    ctx.toast(it.message ?: ctx.str(R.string.auth_login_failed), long = true)
                }
            }
        }
    }

    private fun toggleCreateAccountMode() {
        isCreateAccountMode = !isCreateAccountMode
        if (isCreateAccountMode) {
            b.tvAuthSubtitle.visibility = View.VISIBLE
            b.tvAuthSubtitle.text = ctx.str(R.string.auth_create_subtitle)
            b.tvCreateAccountSubtitle.visibility = View.VISIBLE
            b.btnAuthLogin.text = ctx.str(R.string.create_account)
            b.tvCreateAccountTitle.text = ctx.str(R.string.auth_already_have)
            b.tvCreateAccountSubtitle.text = ctx.str(R.string.auth_go_back_login)
        } else {
            b.tvAuthSubtitle.visibility = View.GONE
            b.tvCreateAccountSubtitle.visibility = View.GONE
            b.btnAuthLogin.text = ctx.str(R.string.login)
            b.tvCreateAccountTitle.text = ctx.str(R.string.create_account)
        }
    }

    /** Shows the signed-in UI and loads this user's live data. */
    fun enterMain(username: String) {
        b.tvUsernameValue.text = username
        ctx.home.resetPasswordDisplay()
        b.containerAuth.visibility = View.GONE
        b.layoutMain.visibility = View.VISIBLE
        ctx.navigation.switchTab(0)
        ctx.deposit.loadPaymentMethods()
        ctx.history.load()
    }
}
