package com.example.bpexch

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** Bottom navigation (Home, Deposit, Withdrawal, History). */
class NavigationController(private val ctx: AppContext) {
    private val b = ctx.binding

    private class Tab(val button: LinearLayout, val icon: ImageView, val label: TextView, val container: View)

    private val tabs = listOf(
        Tab(b.tabHome, b.ivTabHome, b.tvTabHome, b.containerHome),
        Tab(b.tabDeposit, b.ivTabDeposit, b.tvTabDeposit, b.containerDepositTab),
        Tab(b.tabWithdrawal, b.ivTabWithdrawal, b.tvTabWithdrawal, b.containerWithdrawTab),
        Tab(b.tabHistory, b.ivTabHistory, b.tvTabHistory, b.containerHistoryTab),
    )

    /** Index of the visible tab; saved so the screen survives a language change. */
    var currentTab = 0
        private set

    fun setup() {
        tabs.forEachIndexed { index, tab -> tab.button.setOnClickListener { switchTab(index) } }
    }

    fun switchTab(tabIndex: Int) {
        currentTab = tabIndex
        if (tabIndex == 2) ctx.withdraw.resetMethodSelection()

        b.mainScrollView.scrollTo(0, 0)

        val activeBg = ctx.drawable(R.drawable.bg_tab_active_blue)
        val activeColor = ctx.color(R.color.primary_blue)
        val inactiveColor = ctx.color(R.color.text_secondary)

        tabs.forEachIndexed { index, tab ->
            val active = index == tabIndex
            tab.container.visibility = if (active) View.VISIBLE else View.GONE
            tab.button.background = if (active) activeBg else null
            tab.icon.setColorFilter(if (active) activeColor else inactiveColor)
            tab.label.setTextColor(if (active) activeColor else inactiveColor)
        }
    }
}
