package com.example.bpexch

import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog

/** The Wallet / Bank dropdown cards on the withdraw tab and the bottom sheets that fill them. */
class WithdrawMethodPicker(private val ctx: AppContext) {
    private val b = ctx.binding

    var selectedMethod = ""
        private set

    fun setup() {
        b.btnDropdownWallet.setOnClickListener { showWalletPicker() }
        b.btnDropdownBank.setOnClickListener { showBankPicker() }
        reset()
    }

    fun reset() {
        selectedMethod = ""

        b.tvSelectedWalletName.text = ""
        b.tvSelectedWalletName.visibility = View.GONE
        b.ivSelectedWalletIcon.setImageResource(R.drawable.ic_cards)
        b.ivSelectedWalletIcon.visibility = View.VISIBLE
        b.tvWalletDropdownTag.text = ctx.str(R.string.select_wallet)
        b.tvWalletDropdownTag.setTextColor(ctx.color(R.color.text_secondary))
        b.pillWalletDropdown.setBackgroundResource(R.drawable.bg_pill_lang)
        b.btnDropdownWallet.setBackgroundResource(R.drawable.bg_dropdown_card_inactive)

        b.tvSelectedBankName.text = ""
        b.tvSelectedBankName.visibility = View.GONE
        b.ivSelectedBankIcon.setImageResource(R.drawable.ic_bank)
        b.ivSelectedBankIcon.visibility = View.VISIBLE
        b.tvBankDropdownTag.text = ctx.str(R.string.select_bank_2)
        b.tvBankDropdownTag.setTextColor(ctx.color(R.color.text_secondary))
        b.pillBankDropdown.setBackgroundResource(R.drawable.bg_pill_lang)
        b.btnDropdownBank.setBackgroundResource(R.drawable.bg_dropdown_card_inactive)
    }

    private fun showWalletPicker() {
        val dialog = BottomSheetDialog(ctx.activity)
        val sheetView = ctx.activity.layoutInflater.inflate(R.layout.layout_wallet_picker_sheet, null)
        dialog.setContentView(sheetView)

        sheetView.findViewById<View>(R.id.btnDismissWalletPicker)?.setOnClickListener { dialog.dismiss() }

        val walletItems = mapOf(
            R.id.itemWalletJazzCash to "JazzCash",
            R.id.itemWalletEasypaisa to "Easypaisa",
            R.id.itemWalletSadaPay to "SadaPay",
            R.id.itemWalletNayaPay to "NayaPay",
            R.id.itemWalletUPaisa to "UPaisa",
            R.id.itemWalletZindigi to "Zindigi",
        )

        walletItems.forEach { (resId, method) ->
            sheetView.findViewById<View>(resId)?.setOnClickListener {
                selectWallet(method)
                dialog.dismiss()
                ctx.toast(ctx.str(R.string.selected_item, method))
            }
        }
        dialog.show()
    }

    private fun selectWallet(method: String) {
        selectedMethod = method
        b.tvSelectedWalletName.text = method
        b.tvSelectedWalletName.visibility = View.VISIBLE
        b.ivSelectedWalletIcon.visibility = View.VISIBLE
        b.tvWalletDropdownTag.text = ctx.str(R.string.selected)
        b.tvWalletDropdownTag.setTextColor(ctx.color(R.color.deposit_green_dark))
        b.pillWalletDropdown.setBackgroundResource(R.drawable.bg_badge_verified)
        b.pillWalletDropdown.backgroundTintList = ctx.colorStateList(R.color.deposit_green_light)
        b.btnDropdownWallet.setBackgroundResource(R.drawable.bg_dropdown_card_active)

        // Deactivate bank card
        b.tvSelectedBankName.text = ctx.str(R.string.select_bank_2)
        b.tvSelectedBankName.visibility = View.VISIBLE
        b.ivSelectedBankIcon.setImageResource(R.drawable.ic_bank)
        b.ivSelectedBankIcon.imageTintList = ctx.colorStateList(R.color.text_secondary)
        b.ivSelectedBankIcon.visibility = View.VISIBLE
        b.tvBankDropdownTag.text = ctx.str(R.string.select)
        b.tvBankDropdownTag.setTextColor(ctx.color(R.color.text_secondary))
        b.pillBankDropdown.setBackgroundResource(R.drawable.bg_pill_lang)
        b.pillBankDropdown.backgroundTintList = null
        b.btnDropdownBank.setBackgroundResource(R.drawable.bg_dropdown_card_inactive)

        val logo = when (method) {
            "JazzCash" -> R.drawable.logo_jazzcash
            "Easypaisa" -> R.drawable.logo_easypaisa
            "SadaPay" -> R.drawable.logo_sadapay
            else -> null
        }
        if (logo != null) {
            b.ivSelectedWalletIcon.setImageResource(logo)
            b.ivSelectedWalletIcon.imageTintList = null
        } else {
            b.ivSelectedWalletIcon.setImageResource(R.drawable.ic_cards)
            b.ivSelectedWalletIcon.imageTintList = ctx.colorStateList(R.color.deposit_green_dark)
        }
    }

    private fun showBankPicker() {
        val dialog = BottomSheetDialog(ctx.activity)
        val sheetView = ctx.activity.layoutInflater.inflate(R.layout.layout_bank_picker_sheet, null)
        dialog.setContentView(sheetView)

        sheetView.findViewById<View>(R.id.btnDismissBankPicker)?.setOnClickListener { dialog.dismiss() }

        val bankItems = mapOf(
            R.id.itemBankAlfalah to "Bank Alfalah",
            R.id.itemBankMeezan to "Meezan Bank",
            R.id.itemBankHBL to "Habib Bank Limited (HBL)",
            R.id.itemBankUBL to "United Bank Limited (UBL)",
            R.id.itemBankMCB to "MCB Bank",
            R.id.itemBankABL to "Allied Bank Limited (ABL)",
            R.id.itemBankFaysal to "Faysal Bank",
            R.id.itemBankSCB to "Standard Chartered Bank",
            R.id.itemBankBOP to "The Bank of Punjab (BOP)",
            R.id.itemBankAskari to "Askari Bank",
            R.id.itemBankDIB to "Dubai Islamic Bank",
        )

        bankItems.forEach { (resId, bankName) ->
            sheetView.findViewById<View>(resId)?.setOnClickListener {
                selectBank(bankName)
                dialog.dismiss()
                ctx.toast(ctx.str(R.string.selected_item, bankName))
            }
        }
        dialog.show()
    }

    private fun selectBank(bankName: String) {
        selectedMethod = bankName
        b.tvSelectedBankName.text = bankName
        b.tvSelectedBankName.visibility = View.VISIBLE
        b.ivSelectedBankIcon.visibility = View.VISIBLE
        b.tvBankDropdownTag.text = ctx.str(R.string.selected)
        b.tvBankDropdownTag.setTextColor(ctx.color(R.color.deposit_green_dark))
        b.pillBankDropdown.setBackgroundResource(R.drawable.bg_badge_verified)
        b.pillBankDropdown.backgroundTintList = ctx.colorStateList(R.color.deposit_green_light)
        b.btnDropdownBank.setBackgroundResource(R.drawable.bg_dropdown_card_active)

        // Deactivate wallet card
        b.tvSelectedWalletName.text = ctx.str(R.string.select_wallet)
        b.tvSelectedWalletName.visibility = View.VISIBLE
        b.ivSelectedWalletIcon.setImageResource(R.drawable.ic_cards)
        b.ivSelectedWalletIcon.imageTintList = ctx.colorStateList(R.color.text_secondary)
        b.ivSelectedWalletIcon.visibility = View.VISIBLE
        b.tvWalletDropdownTag.text = ctx.str(R.string.select)
        b.tvWalletDropdownTag.setTextColor(ctx.color(R.color.text_secondary))
        b.pillWalletDropdown.setBackgroundResource(R.drawable.bg_pill_lang)
        b.pillWalletDropdown.backgroundTintList = null
        b.btnDropdownWallet.setBackgroundResource(R.drawable.bg_dropdown_card_inactive)

        if (bankName == "Bank Alfalah") {
            b.ivSelectedBankIcon.setImageResource(R.drawable.logo_bank_alfalah)
            b.ivSelectedBankIcon.imageTintList = null
        } else {
            b.ivSelectedBankIcon.setImageResource(R.drawable.ic_bank)
            b.ivSelectedBankIcon.imageTintList = ctx.colorStateList(R.color.deposit_green_dark)
        }
    }
}
