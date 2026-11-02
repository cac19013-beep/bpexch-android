package com.example.simpleapp

object SupportReplyHelper {
    fun getReply(text: String): String {
        return when {
            text.contains("deposit", ignoreCase = true) ->
                "To make a deposit, tap the '+ Make a Deposit' button on the Deposit tab or the Deposit card on the Home screen. Our verified company accounts are updated live."

            text.contains("withdraw", ignoreCase = true) || text.contains("cashout", ignoreCase = true) ->
                "Withdrawals are processed 24/7 with zero fees. Average payout time is 2 to 5 minutes via JazzCash or Easypaisa."

            text.contains("bonus", ignoreCase = true) || text.contains("promo", ignoreCase = true) ->
                "No bonus offers are currently available. Check the official website for current information."

            text.contains("hello", ignoreCase = true) || text.contains("hi", ignoreCase = true) || text.contains("hey", ignoreCase = true) ->
                "Hello! Welcome to Bpexch Live Support. How can we help you today with your wallet or account?"

            else ->
                "Thank you for contacting Bpexch Live Support. A representative has received your message regarding \"$text\" and will assist you momentarily."
        }
    }
}
