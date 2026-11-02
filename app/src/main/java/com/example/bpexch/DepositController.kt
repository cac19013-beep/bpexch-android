package com.example.bpexch

import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts

/** Deposit tab: amount + payment-slip flow, and the payment account details it shows. */
class DepositController(private val ctx: AppContext) {
    private val b = ctx.binding
    private val steps = StepIndicator(
        ctx, b.layoutDepositStep1, b.layoutDepositStep2, b.tvDepositStep1Icon, b.lineDepositStep, b.tvDepositStep2Icon, b.tvDepositStep2Label,
    )
    private var depositMethodName = "Bank Alfalah"
    private var selectedReceiptUri: Uri? = null

    // Android SAF picker: no runtime permission needed.
    private val pickReceiptLauncher = ctx.activity.registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedReceiptUri = uri
            b.tvReceiptStatus.text = ctx.str(R.string.screenshot_attached)
            b.tvReceiptStatus.setTextColor(ctx.color(R.color.deposit_green_dark))
            b.ivReceiptPreview.setImageURI(uri)
            b.ivReceiptPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            b.ivReceiptPreview.setPadding(0, 0, 0, 0)
            ctx.toast(ctx.str(R.string.screenshot_added))
        }
    }

    fun openFlow() {
        ctx.navigation.switchTab(1)
        steps.show(1)
    }

    fun setup() {
        steps.show(1)

        b.btnBackDepositFlow.setOnClickListener {
            if (b.layoutDepositStep2.visibility == View.VISIBLE) steps.show(1) else ctx.navigation.switchTab(0)
        }

        b.cardBankAlfalah.setOnClickListener { b.rbBankAlfalah.isChecked = true }
        b.btnDepositNext.setOnClickListener { steps.show(2) }

        b.btnCopyDepositAcc.setOnClickListener {
            ctx.copyToClipboard(ctx.str(R.string.account_number), b.tvDepositAccNumber.text.toString())
        }
        b.btnCopyDepositIban.setOnClickListener {
            ctx.copyToClipboard(ctx.str(R.string.iban), b.tvDepositIban.text.toString())
        }

        setupAmountChips()

        b.boxAttachReceipt.setOnClickListener {
            try {
                pickReceiptLauncher.launch("image/*")
            } catch (e: Exception) {
                ctx.toast(ctx.str(R.string.opening_photos))
            }
        }

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

    fun loadPaymentMethods() {
        ctx.api.paymentMethods { result ->
            result.onSuccess { methods ->
                val method = methods.firstOrNull { it.channelName.equals("Bank Alfalah", ignoreCase = true) }
                    ?: methods.firstOrNull()
                    ?: return@onSuccess
                depositMethodName = method.channelName
                b.tvDepositAccTitle.text = method.accountTitle
                b.tvDepositAccNumber.text = method.accountNumber
                b.tvDepositIban.text = method.iban ?: ""
            }
        }
    }
}
