package com.example.bpexch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bpexch.databinding.ActivityMainBinding

/**
 * Single-screen host. All behaviour lives in the feature controllers wired up here; they share
 * state and reach each other through [AppContext].
 */
class MainActivity : AppCompatActivity() {

    private lateinit var ctx: AppContext

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val api = ApiClient(this)
        ctx = AppContext(this, binding, api).apply {
            auth = AuthController(this)
            verification = VerificationController(this)
            navigation = NavigationController(this)
            home = HomeController(this)
            deposit = DepositController(this)
            withdraw = WithdrawController(this)
            history = HistoryController(this)
            support = SupportChatController(this)
            sheets = AppSheets(this)
        }

        ctx.auth.setup()
        ctx.verification.setup()
        ctx.navigation.setup()
        ctx.home.setup()
        ctx.deposit.setup()
        ctx.withdraw.setup()
        ctx.history.setup()
        ctx.support.setup()
        ctx.home.setupAnnouncementBar()

        if (api.isLoggedIn && api.cachedStatus != "approved") {
            ctx.verification.show(api.username)
        } else if (api.isLoggedIn) {
            ctx.auth.enterMain(api.username)
            // A language change recreates the activity: put the user back where they were.
            savedInstanceState?.let {
                ctx.navigation.switchTab(it.getInt(STATE_TAB, 0))
                if (it.getBoolean(STATE_CHAT_OPEN, false)) ctx.support.open()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_TAB, ctx.navigation.currentTab)
        outState.putBoolean(STATE_CHAT_OPEN, ctx.support.isOpen)
    }

    override fun onStart() {
        super.onStart()
        if (::ctx.isInitialized && ctx.navigation.currentTab == 3) ctx.history.startPolling()
    }

    override fun onStop() {
        if (::ctx.isInitialized) ctx.history.stopPolling()
        super.onStop()
    }

    override fun onDestroy() {
        ctx.support.onDestroy()
        ctx.verification.onDestroy()
        super.onDestroy()
    }

    private companion object {
        const val STATE_TAB = "tab"
        const val STATE_CHAT_OPEN = "chat_open"
    }
}
