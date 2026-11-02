package com.example.bpexch

import android.os.Handler
import android.os.Looper
import android.view.View

/**
 * Shown after Create Account (or a login while still pending). It stays up and checks the account
 * status every few seconds until an admin approves it, then opens the app.
 */
class VerificationController(private val ctx: AppContext) {
    private val b = ctx.binding
    private val api = ctx.api
    private val handler = Handler(Looper.getMainLooper())
    private val pollTask = Runnable { check() }
    private var active = false

    fun setup() {
        b.btnVerificationBack.setOnClickListener {
            stop()
            api.logout()
            b.containerVerification.visibility = View.GONE
            b.containerAuth.visibility = View.VISIBLE
        }
    }

    fun show(username: String) {
        b.containerAuth.visibility = View.GONE
        b.layoutMain.visibility = View.GONE
        b.containerVerification.visibility = View.VISIBLE
        b.tvVerificationUser.text = username
        showWaiting()
        active = true
        handler.removeCallbacks(pollTask)
        check()
    }

    fun onDestroy() = stop()

    private fun stop() {
        active = false
        handler.removeCallbacks(pollTask)
    }

    private fun check() {
        if (!active) return
        api.profile { result ->
            if (!active) return@profile
            result.onSuccess { status ->
                when (status) {
                    "approved" -> {
                        stop()
                        b.containerVerification.visibility = View.GONE
                        ctx.toast(ctx.str(R.string.verify_approved))
                        ctx.auth.enterMain(api.username)
                    }
                    "rejected" -> {
                        stop()
                        showRejected()
                    }
                    else -> schedule()
                }
            }.onFailure {
                if (api.isLoggedIn) {
                    schedule() // temporary network problem: keep waiting
                } else {
                    stop() // session ended
                    b.containerVerification.visibility = View.GONE
                    b.containerAuth.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun schedule() {
        handler.postDelayed(pollTask, POLL_MS)
    }

    private fun showWaiting() {
        b.progressVerification.visibility = View.VISIBLE
        b.tvVerificationTitle.setText(R.string.verify_title)
        b.tvVerificationMessage.setText(R.string.verify_message)
    }

    private fun showRejected() {
        b.progressVerification.visibility = View.GONE
        b.tvVerificationTitle.setText(R.string.verify_rejected_title)
        b.tvVerificationMessage.setText(R.string.verify_rejected_message)
    }

    private companion object {
        const val POLL_MS = 5_000L
    }
}
