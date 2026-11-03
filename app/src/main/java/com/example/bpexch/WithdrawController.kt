package com.example.bpexch

import android.view.View
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

/** Withdraw tab: amount + method + account details flow, and the confirmation sheet. */
class WithdrawController(private val ctx: AppContext) {
    private val b = ctx.binding
    private val steps = StepIndicator(
        ctx, b.layoutWithdrawStep1, b.layoutWithdrawStep2,
        b.tvWithdrawStep1Icon, b.lineWithdrawStep, b.tvWithdrawStep2Icon, b.tvWithdrawStep2Label,
    )
    private val picker = WithdrawMethodPicker(ctx)
    private var selectedAmount = 1000

    private class MethodStyle(val badge: String, val colorRes: Int, val numberLabelRes: Int)

    private val knownMethods = mapOf(
        "JazzCash" to MethodStyle("JC", R.color.jazzcash_pink, R.string.label_mobile_number),
        "Easypaisa" to MethodStyle("EP", R.color.easypaisa_green, R.string.label_mobile_number),
        "SadaPay" to MethodStyle("SP", R.color.deposit_green_dark, R.string.label_mobile_iban),
        "NayaPay" to MethodStyle("NP", R.color.power_green_primary, R.string.label_mobile_iban),
        "Bank Alfalah" to MethodStyle("BA", R.color.bank_alfalah_red, R.string.label_account_iban),
        "Meezan Bank" to MethodStyle("MB", R.color.power_green_deep, R.string.label_account_iban),
    )

    fun openFlow() {
        ctx.navigation.switchTab(2)
        steps.show(1)
    }

    fun resetMethodSelection() = picker.reset()

    fun setup() {
        steps.show(1)

        b.btnBackWithdrawFlow.setOnClickListener {
            if (b.layoutWithdrawStep2.visibility == View.VISIBLE) steps.show(1) else ctx.navigation.switchTab(0)
        }

        // Presets: 1,000 / 5,000 / 10,000
        listOf(b.chipWithdraw1000 to 1000, b.chipWithdraw5000 to 5000, b.chipWithdraw10000 to 10000)
            .forEach { (chip, amount) ->
                chip.setOnClickListener {
                    b.etWithdrawAmountFlow.setText(amount.toString())
                    selectedAmount = amount
                }
            }

        picker.setup()

        b.btnWithdrawNext.setOnClickListener { goToDetails() }
        b.btnWithdrawSubmit.setOnClickListener { requestConfirmation() }
    }

    private fun goToDetails() {
        val amount = b.etWithdrawAmountFlow.text.toString().trim().toIntOrNull()
        val method = picker.selectedMethod

        if (method.isBlank()) {
            ctx.toast(ctx.str(R.string.choose_method_first))
            return
        }
        if (amount == null) {
            ctx.toast(ctx.str(R.string.enter_amount))
            return
        }
        if (amount < 500) {
            ctx.toast(ctx.str(R.string.min_withdrawal))
            return
        }

        selectedAmount = amount

        b.tvWithdrawStep2Amount.text = ctx.str(R.string.amount_rs, selectedAmount.toString())
        b.tvWithdrawStep2Method.text = method
        b.tvWithdrawWalletNameBox.text = method
        applyMethodStyle(method)

        steps.show(2)
    }

    private fun applyMethodStyle(method: String) {
        val style = knownMethods[method]
        val badge: String
        val colorRes: Int
        val numberLabel: String
        if (style != null) {
            badge = style.badge
            colorRes = style.colorRes
            numberLabel = ctx.str(style.numberLabelRes, method)
        } else {
            badge = if (method.length >= 2) method.substring(0, 2).uppercase() else "BK"
            colorRes = R.color.deposit_green_dark
            numberLabel = if (method.contains("Bank", ignoreCase = true)) ctx.str(R.string.label_account_iban) else ctx.str(R.string.label_generic_number, method)
        }
        b.tvWithdrawStep2MethodBadge.text = badge
        b.tvWithdrawStep2MethodBadge.backgroundTintList = ctx.colorStateList(colorRes)
        b.tvWithdrawStep2MethodName.text = method
        b.tvWithdrawNumberLabel.text = numberLabel
    }

    /** Step 2 -> Submit: validates, then opens the verification sheet. */
    private fun requestConfirmation() {
        val title = b.etWithdrawAccountTitle.text.toString().trim()
        val number = b.etWithdrawNumber.text.toString().trim()

        if (title.isEmpty()) {
            ctx.toast(ctx.str(R.string.enter_account_title))
            return
        }
        if (number.isEmpty()) {
            ctx.toast(ctx.str(R.string.enter_method_number, picker.selectedMethod))
            return
        }

        showVerificationDialog(picker.selectedMethod, title, number, selectedAmount)
    }

    private fun showVerificationDialog(method: String, title: String, number: String, amount: Int) {
        val dialog = BottomSheetDialog(ctx.activity)
        val sheetView = ctx.activity.layoutInflater.inflate(R.layout.dialog_withdraw_verification, null)
        dialog.setContentView(sheetView)

        sheetView.findViewById<TextView>(R.id.tvVerifyMethod)?.text = method
        sheetView.findViewById<TextView>(R.id.tvVerifyTitle)?.text = title
        sheetView.findViewById<TextView>(R.id.tvVerifyNumber)?.text = number
        sheetView.findViewById<TextView>(R.id.tvVerifyAmount)?.text = "Rs " + String.format(Locale.getDefault(), "%,d", amount)

        sheetView.findViewById<View>(R.id.btnDismissVerification)?.setOnClickListener { dialog.dismiss() }
        sheetView.findViewById<View>(R.id.btnCancelVerification)?.setOnClickListener { dialog.dismiss() }

        sheetView.findViewById<View>(R.id.btnConfirmWithdrawal)?.setOnClickListener {
            dialog.dismiss()
            ctx.setLoading(true)
            ctx.api.withdraw(amount.toLong(), method, title, number) { result ->
                ctx.setLoading(false)
                result.onSuccess {
                    ctx.toast(ctx.str(R.string.withdraw_submitted, amount, title, number), long = true)
                    steps.show(1)
                    ctx.history.showAfterSubmit(section = 1)
                }.onFailure { ctx.showError(it, ctx.str(R.string.withdraw_failed)) }
            }
        }

        dialog.show()
    }
}
