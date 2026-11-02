package com.example.bpexch

import androidx.annotation.StringRes

data class SupportQuickReply(
    @StringRes val questionRes: Int,
    @StringRes val answerRes: Int,
)

object SupportContent {
    val quickReplies: List<SupportQuickReply> = listOf(
        SupportQuickReply(R.string.qr_deposit_q, R.string.qr_deposit_a),
        SupportQuickReply(R.string.qr_withdraw_q, R.string.qr_withdraw_a),
        SupportQuickReply(R.string.qr_account_q, R.string.qr_account_a),
        SupportQuickReply(R.string.qr_password_q, R.string.qr_password_a),
    )
}
