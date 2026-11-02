package com.example.simpleapp

data class SupportQuickReply(
    val question: String,
    val answer: String,
)

object SupportContent {
    val quickReplies: List<SupportQuickReply> = listOf(
        SupportQuickReply(
            question = "How to deposit funds?",
            answer = "To make a deposit on Bpexch:\n\n1. Tap '+ Make a Deposit' from Home or Deposit tab\n2. Select your payment channel (Bank Alfalah, JazzCash, Easypaisa)\n3. Transfer to the official account number\n4. Attach your payment slip screenshot\n\nDeposits are verified automatically within 2 to 5 minutes."
        ),
        SupportQuickReply(
            question = "What is the withdrawal processing speed?",
            answer = "Bpexch cashouts are active 24/7/365:\n\n• JazzCash & Easypaisa: Under 5 minutes\n• Bank Transfer: 10 - 20 minutes\n• Payout fee: 0% Free\n\nSubmit your payout request directly from the 'Withdrawal' tab anytime."
        ),
        SupportQuickReply(
            question = "How can I get account help?",
            answer = "For account or payment questions, use the Contact us on WhatsApp option or visit the official website."
        ),
        SupportQuickReply(
            question = "How do I change my account password?",
            answer = "For the security of account demo@3322, password updates are verified by our security desk.\n\nPlease confirm your registered phone number or message us on WhatsApp for an instant reset code."
        )
    )
}
