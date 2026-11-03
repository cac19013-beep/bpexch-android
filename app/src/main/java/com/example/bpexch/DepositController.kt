package com.example.bpexch

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.ShapeAppearanceModel

/** Deposit tab: amount + payment-slip flow, and the payment account details it shows. */
class DepositController(private val ctx: AppContext) {
    private val b = ctx.binding
    private val steps = StepIndicator(
        ctx, b.layoutDepositStep1, b.layoutDepositStep2, b.tvDepositStep1Icon, b.lineDepositStep, b.tvDepositStep2Icon, b.tvDepositStep2Label,
    )
    private var depositMethodName = ""
    private var methods: List<PaymentMethod> = emptyList()

    fun openFlow() {
        ctx.navigation.switchTab(1)
        steps.show(1)
        loadPaymentMethods()
    }

    fun setup() {
        steps.show(1)

        b.btnBackDepositFlow.setOnClickListener {
            if (b.layoutDepositStep2.visibility == View.VISIBLE) steps.show(1) else ctx.navigation.switchTab(0)
        }

        b.btnDepositNext.setOnClickListener {
            if (depositMethodName.isEmpty()) ctx.toast(ctx.str(R.string.no_payment_methods)) else steps.show(2)
        }

        b.btnCopyDepositAcc.setOnClickListener {
            ctx.copyToClipboard(ctx.str(R.string.account_number), b.tvDepositAccNumber.text.toString())
        }
        b.btnCopyDepositIban.setOnClickListener {
            ctx.copyToClipboard(ctx.str(R.string.iban), b.tvDepositIban.text.toString())
        }

        setupAmountChips()

        b.btnDepositSubmit.setOnClickListener { submit() }
    }

    private fun setupAmountChips() {
        val chips = listOf(b.chipDeposit500, b.chipDeposit800, b.chipDeposit1000)
        fun setActive(amount: String, activeChip: TextView) {
            b.etDepositAmountFlow.setText(amount)
            chips.forEach { chip ->
                val isActive = chip == activeChip
                chip.setBackgroundResource(if (isActive) R.drawable.bg_badge_verified else R.drawable.bg_card_white)
                if (isActive) {
                    chip.backgroundTintList = ctx.colorStateList(R.color.deposit_green_light)
                    chip.setTextColor(ctx.color(R.color.deposit_green_dark))
                } else {
                    chip.backgroundTintList = null
                    chip.setTextColor(ctx.color(R.color.text_secondary))
                }
            }
        }
        b.chipDeposit500.setOnClickListener { setActive("500", b.chipDeposit500) }
        b.chipDeposit800.setOnClickListener { setActive("800", b.chipDeposit800) }
        b.chipDeposit1000.setOnClickListener { setActive("1000", b.chipDeposit1000) }
    }

    private fun submit() {
        val amount = b.etDepositAmountFlow.text.toString().trim().toIntOrNull() ?: 500

        if (amount < 500) {
            ctx.toast(ctx.str(R.string.minimum_deposit_is_rs_500))
            return
        }

        b.btnDepositSubmit.isEnabled = false
        ctx.setLoading(true)
        ctx.api.deposit(amount.toLong(), depositMethodName) { result ->
            b.btnDepositSubmit.isEnabled = true
            ctx.setLoading(false)
            result.onSuccess {
                ctx.toast(ctx.str(R.string.deposit_submitted, amount), long = true)
                steps.show(1)
                ctx.history.showAfterSubmit(section = 0)
            }.onFailure { ctx.showError(it, ctx.str(R.string.deposit_failed)) }
        }
    }

    /** Fetches the methods staff have activated; called each time the deposit tab opens. */
    fun loadPaymentMethods() {
        ctx.api.paymentMethods { result ->
            result.onSuccess { list ->
                methods = list
                if (list.none { it.channelName == depositMethodName }) {
                    depositMethodName = list.firstOrNull()?.channelName ?: ""
                }
                renderMethods()
            }
        }
    }

    private fun renderMethods() {
        val container = b.layoutDepositMethods
        container.removeAllViews()
        b.tvDepositMethodsEmpty.visibility = if (methods.isEmpty()) View.VISIBLE else View.GONE
        b.btnDepositNext.isEnabled = methods.isNotEmpty()
        methods.forEach { container.addView(methodCard(it)) }
        methods.firstOrNull { it.channelName == depositMethodName }?.let { showAccount(it) }
    }

    private fun methodCard(method: PaymentMethod): View {
        val card = LinearLayout(ctx.activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_card_white)
            elevation = ctx.dp(2).toFloat()
            setPadding(ctx.dp(16), ctx.dp(16), ctx.dp(16), ctx.dp(16))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .apply { bottomMargin = ctx.dp(12) }
            isClickable = true
            isFocusable = true
        }
        card.addView(logoFor(method.channelName))
        card.addView(LinearLayout(ctx.activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = ctx.dp(14) }
            addView(TextView(ctx.activity).apply {
                text = method.channelName
                textSize = 15f
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setTextColor(ctx.color(R.color.text_primary))
            })
            addView(TextView(ctx.activity).apply {
                text = method.accountTitle
                textSize = 12f
                setTextColor(ctx.color(R.color.text_secondary))
            })
        })
        card.addView(RadioButton(ctx.activity).apply {
            isChecked = method.channelName == depositMethodName
            isClickable = false
        })
        card.setOnClickListener {
            depositMethodName = method.channelName
            renderMethods()
        }
        return card
    }

    private fun logoFor(name: String): View {
        val logo = when {
            name.contains("alfalah", ignoreCase = true) -> R.drawable.logo_bank_alfalah
            name.contains("jazz", ignoreCase = true) -> R.drawable.logo_jazzcash
            name.contains("easypaisa", ignoreCase = true) -> R.drawable.logo_easypaisa
            name.contains("sadapay", ignoreCase = true) -> R.drawable.logo_sadapay
            else -> null
        }
        val size = ctx.dp(44)
        val params = LinearLayout.LayoutParams(size, size)
        if (logo != null) {
            return ShapeableImageView(ctx.activity).apply {
                layoutParams = params
                setBackgroundResource(R.drawable.bg_circle_icon_button)
                setPadding(ctx.dp(3), ctx.dp(3), ctx.dp(3), ctx.dp(3))
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageResource(logo)
                shapeAppearanceModel = ShapeAppearanceModel.builder().setAllCornerSizes(ShapeAppearanceModel.PILL).build()
            }
        }
        return TextView(ctx.activity).apply {
            layoutParams = params
            setBackgroundResource(R.drawable.bg_circle_icon_button)
            gravity = Gravity.CENTER
            text = name.take(2).uppercase()
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ctx.color(R.color.deposit_green_dark))
        }
    }

    private fun showAccount(method: PaymentMethod) {
        b.tvDepositMethodName.text = method.channelName
        b.tvDepositAccTitle.text = method.accountTitle
        b.tvDepositAccNumber.text = method.accountNumber
        b.tvDepositIban.text = method.iban ?: ""
        b.rowDepositIban.visibility = if (method.iban.isNullOrEmpty()) View.GONE else View.VISIBLE
    }
}
