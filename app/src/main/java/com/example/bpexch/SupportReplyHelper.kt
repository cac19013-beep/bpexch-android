package com.example.bpexch

object SupportReplyHelper {
    fun getReply(text: String): String {
        return when {
            text.contains("deposit", ignoreCase = true) ->
                "To deposit, tap Deposit on the Home screen or open the Deposit tab. The account details shown there are always up to date."

            text.contains("withdraw", ignoreCase = true) || text.contains("cashout", ignoreCase = true) ->
                "Withdrawals work every day, all day, with no fee. JazzCash and Easypaisa usually take 2 to 5 minutes."

            text.contains("bonus", ignoreCase = true) || text.contains("promo", ignoreCase = true) ->
                "There is no bonus offer right now. Please check our official website for updates."

            text.contains("hello", ignoreCase = true) || text.contains("hi", ignoreCase = true) || text.contains("hey", ignoreCase = true) ->
                "Hello! How can I help you today?"

            else ->
                "Thank you for contacting Bpexch Support. We received your message \"$text\" and our team will reply shortly."
        }
    }
}
