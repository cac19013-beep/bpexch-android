package com.example.bpexch

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Builds the transaction cards shown in the History tab. */
object HistoryRows {

    /** The backend's "held" status is presented to users as "pending". */
    fun displayStatus(status: String) = if (status == "held") "pending" else status

    private fun methodLogo(method: String): Int = when {
        method.contains("alfalah", ignoreCase = true) -> R.drawable.logo_bank_alfalah
        method.contains("jazz", ignoreCase = true) -> R.drawable.logo_jazzcash
        method.contains("easypaisa", ignoreCase = true) -> R.drawable.logo_easypaisa
        method.contains("sada", ignoreCase = true) -> R.drawable.logo_sadapay
        else -> R.drawable.logo
    }

    private fun formatDate(iso: String): String = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(iso.take(19))
        if (date == null) iso.take(10) else SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(date)
    } catch (e: Exception) {
        iso.take(10)
    }

    /** One history card, styled like the original static cards. */
    fun build(ctx: AppContext, item: TransactionItem): View {
        val context = ctx.activity
        val density = ctx.density
        fun dp(value: Int) = ctx.dp(value)
        val status = displayStatus(item.status)
        val isDeposit = item.type == "deposit"
        val accentRes = when (status) {
            "approved" -> R.color.power_green_primary
            "rejected" -> R.color.badge_red
            else -> R.color.withdraw_orange
        }

        val logo = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageResource(methodLogo(item.method))
            layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
        }

        val title = TextView(context).apply {
            text = ctx.str(if (isDeposit) R.string.history_deposit_title else R.string.history_withdraw_title, item.method)
            setTextColor(ctx.color(R.color.text_primary))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
        }
        val subtitle = TextView(context).apply {
            text = "TXN#" + item.id.take(6).uppercase() + " • " + formatDate(item.createdAt)
            setTextColor(ctx.color(R.color.text_secondary))
            textSize = 11f
            setPadding(0, dp(2), 0, 0)
        }
        val left = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) }
            addView(title)
            addView(subtitle)
        }

        val amount = TextView(context).apply {
            text = ctx.str(if (isDeposit) R.string.amount_plus else R.string.amount_minus, String.format(Locale.getDefault(), "%,d", item.amount))
            setTextColor(ctx.color(accentRes))
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
        }
        val badge = TextView(context).apply {
            text = when (status) {
                "approved" -> ctx.str(R.string.approved)
                "rejected" -> ctx.str(R.string.rejected)
                else -> ctx.str(R.string.pending_2)
            }
            textSize = 10f
            setTypeface(null, Typeface.BOLD)
            setPadding(dp(6), dp(2), dp(6), dp(2))
            when (status) {
                "approved" -> {
                    setBackgroundResource(R.drawable.bg_card_mint_notice)
                    setTextColor(ctx.color(R.color.power_green_primary))
                }
                "rejected" -> {
                    setBackgroundResource(R.drawable.bg_badge_announcement)
                    backgroundTintList = ctx.colorStateList(R.color.badge_red_light)
                    setTextColor(ctx.color(R.color.badge_red_text))
                }
                else -> {
                    setBackgroundResource(R.drawable.bg_card_peach)
                    setTextColor(ctx.color(R.color.withdraw_orange))
                }
            }
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(2)
            }
        }
        val right = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            addView(amount)
            addView(badge)
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_card_white)
            elevation = density
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(10)
            }
            addView(logo)
            addView(left)
            addView(right)
        }
    }
}
