package com.example.bpexch

import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** History tab: deposit and withdrawal lists with status filters, backed by the user's real transactions. */
class HistoryController(private val ctx: AppContext) {
    private val b = ctx.binding
    private var items: List<TransactionItem> = emptyList()
    private var depositFilter = 0
    private var withdrawFilter = 0
    private var depositRows: LinearLayout? = null
    private var withdrawRows: LinearLayout? = null

    fun setup() {
        b.btnHistoryDeposit.setOnClickListener { switchSection(0) }
        b.btnHistoryWithdraw.setOnClickListener { switchSection(1) }

        depositRows = LinearLayout(ctx.activity).apply { orientation = LinearLayout.VERTICAL }
        b.layoutHistoryDeposit.addView(depositRows, b.layoutHistoryDeposit.indexOfChild(b.cardDepositEmpty))
        withdrawRows = LinearLayout(ctx.activity).apply { orientation = LinearLayout.VERTICAL }
        b.layoutHistoryWithdraw.addView(withdrawRows, b.layoutHistoryWithdraw.indexOfChild(b.cardWithdrawEmpty))

        setupFilterChips(
            listOf(b.chipDepositAll, b.chipDepositApproved, b.chipDepositPending, b.chipDepositRejected),
            current = { depositFilter },
            onSelect = { depositFilter = it },
        )
        setupFilterChips(
            listOf(b.chipWithdrawAll, b.chipWithdrawApproved, b.chipWithdrawPending, b.chipWithdrawRejected),
            current = { withdrawFilter },
            onSelect = { withdrawFilter = it },
        )

        render()
        switchSection(0) // default to Deposit History
    }

    private fun setupFilterChips(chips: List<TextView>, current: () -> Int, onSelect: (Int) -> Unit) {
        chips.forEachIndexed { index, chip ->
            chip.setOnClickListener {
                onSelect(index)
                styleChips(chips, index)
                render()
            }
        }
        styleChips(chips, current())
    }

    private fun styleChips(chips: List<TextView>, activeIndex: Int) {
        chips.forEachIndexed { i, chip ->
            val isActive = i == activeIndex
            chip.background = ctx.drawable(
                if (isActive) R.drawable.bg_chip_filter_active_blue else R.drawable.bg_chip_filter_white
            )
            chip.setTextColor(ctx.color(if (isActive) R.color.text_white else R.color.text_secondary))
        }
    }

    fun switchSection(sectionIndex: Int) {
        val activePillBg = ctx.drawable(R.drawable.bg_segmented_item_active)
        val activeTextColor = ctx.color(R.color.text_white)
        val inactiveTextColor = ctx.color(R.color.text_secondary)

        b.layoutHistoryDeposit.visibility = if (sectionIndex == 0) View.VISIBLE else View.GONE
        b.layoutHistoryWithdraw.visibility = if (sectionIndex == 1) View.VISIBLE else View.GONE

        b.btnHistoryDeposit.background = if (sectionIndex == 0) activePillBg else null
        b.btnHistoryDeposit.setTextColor(if (sectionIndex == 0) activeTextColor else inactiveTextColor)

        b.btnHistoryWithdraw.background = if (sectionIndex == 1) activePillBg else null
        b.btnHistoryWithdraw.setTextColor(if (sectionIndex == 1) activeTextColor else inactiveTextColor)
    }

    fun load(silent: Boolean = false) {
        ctx.api.history { result ->
            result.onSuccess {
                if (it == items) return@onSuccess // nothing changed: keep the list (and scroll position) as is
                items = it
                render()
            }.onFailure {
                if (silent) ctx.returnToLoginIfSignedOut() else ctx.showError(it, ctx.str(R.string.history_load_failed))
            }
        }
    }

    private val poller = Handler(Looper.getMainLooper())
    private val pollTask = object : Runnable {
        override fun run() {
            if (ctx.api.isLoggedIn) {
                load(silent = true)
                poller.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
    }

    /** Keeps the list in step with staff decisions while the History tab is on screen. */
    fun startPolling() {
        poller.removeCallbacks(pollTask)
        if (!ctx.api.isLoggedIn) return
        load(silent = true)
        poller.postDelayed(pollTask, POLL_INTERVAL_MS)
    }

    fun stopPolling() = poller.removeCallbacks(pollTask)

    private companion object {
        const val POLL_INTERVAL_MS = 8_000L
    }

    /** After a successful deposit/withdrawal: refresh and jump to the matching history list. */
    fun showAfterSubmit(section: Int) {
        load()
        ctx.navigation.switchTab(3)
        switchSection(section)
    }

    fun clear() {
        stopPolling()
        items = emptyList()
        render()
    }

    private fun render() {
        fillSection(depositRows, b.cardDepositEmpty, "deposit", depositFilter)
        fillSection(withdrawRows, b.cardWithdrawEmpty, "withdraw", withdrawFilter)
    }

    private fun fillSection(container: LinearLayout?, empty: View, type: String, filter: Int) {
        if (container == null) return
        container.removeAllViews()
        val wanted = when (filter) {
            1 -> "approved"
            2 -> "pending"
            3 -> "rejected"
            else -> null
        }
        val rows = items.filter { it.type == type && (wanted == null || HistoryRows.displayStatus(it.status) == wanted) }
        rows.forEach { container.addView(HistoryRows.build(ctx, it)) }
        empty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
    }
}
