package com.example.bpexch

import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout

/**
 * Floating support chat: backend message sync (polled while open) and canned quick replies.
 * Bubble rendering lives in [ChatBubbles].
 */
class SupportChatController(private val ctx: AppContext) {
    private val b = ctx.binding
    private val api = ctx.api
    private val bubbles = ChatBubbles(ctx)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val poller = Handler(Looper.getMainLooper())
    private val pollTask = object : Runnable {
        override fun run() {
            if (b.layoutSupportChatModal.visibility == View.VISIBLE && api.isLoggedIn) {
                refreshMessages()
                poller.postDelayed(this, POLL_INTERVAL_MS)
            }
        }
    }

    private val renderedIds = mutableSetOf<String>()
    private var pendingSends = 0
    private var isFullscreen = false

    fun setup() {
        b.fabSupportChat.setOnClickListener { open() }
        b.btnChatBack.setOnClickListener { close() }
        b.btnChatClose.setOnClickListener { close() }
        b.viewChatScrim.setOnClickListener { close() }
        b.btnChatFullscreen.setOnClickListener { toggleFullscreen() }
        b.btnChatEmoji.setOnClickListener { EmojiPicker.show(ctx) }

        bindQuickQuestion(b.chipQuickDeposit, 0)
        bindQuickQuestion(b.chipQuickWithdraw, 1)
        bindQuickQuestion(b.chipQuickAccount, 2)
        bindQuickQuestion(b.chipQuickPassword, 3)

        b.chipQuickWhatsApp.setOnClickListener {
            ctx.openWhatsApp("923001234567", "Hello Bpexch Official Support, I am contacting you from the app regarding account: ${api.username}")
        }

        b.btnChatSend.setOnClickListener { sendTypedMessage() }
        b.etChatMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendTypedMessage()
                true
            } else false
        }
    }

    val isOpen: Boolean get() = b.layoutSupportChatModal.visibility == View.VISIBLE

    fun open() {
        b.layoutSupportChatModal.visibility = View.VISIBLE
        if (api.isLoggedIn) {
            refreshMessages()
            poller.removeCallbacks(pollTask)
            poller.postDelayed(pollTask, POLL_INTERVAL_MS)
        }
    }

    fun close() {
        b.layoutSupportChatModal.visibility = View.GONE
        poller.removeCallbacks(pollTask)
    }

    fun stopPolling() = poller.removeCallbacks(pollTask)

    fun clearRendered() = renderedIds.clear()

    fun onDestroy() {
        stopPolling()
    }

    private fun bindQuickQuestion(chip: View, index: Int) {
        chip.setOnClickListener {
            val reply = SupportContent.quickReplies[index]
            bubbles.addUserMessage(ctx.str(reply.questionRes))
            mainHandler.postDelayed({ bubbles.addBotMessage(ctx.str(reply.answerRes)) }, 500)
        }
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        if (isFullscreen) {
            b.cardChatContainer.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ).apply { setMargins(0, 0, 0, 0) }
            b.btnChatFullscreen.setImageResource(R.drawable.ic_fullscreen_exit)
        } else {
            val margin = ctx.dp(8)
            b.cardChatContainer.layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ctx.dp(540),
            ).apply {
                gravity = Gravity.BOTTOM
                setMargins(margin, 0, margin, margin)
            }
            b.btnChatFullscreen.setImageResource(R.drawable.ic_fullscreen)
        }
    }

    private fun sendTypedMessage() {
        val text = b.etChatMessage.text.toString().trim()
        if (text.isEmpty()) return
        b.etChatMessage.setText("")
        bubbles.addUserMessage(text)

        pendingSends++
        api.sendSupportMessage(text) { result ->
            pendingSends--
            result.onSuccess { renderedIds.add(it.id) }
                .onFailure { ctx.showError(it, ctx.str(R.string.message_send_failed)) }
        }
    }

    private fun refreshMessages() {
        if (pendingSends > 0) return
        api.supportMessages { result ->
            result.onSuccess { messages ->
                if (pendingSends > 0) return@onSuccess
                messages.filter { renderedIds.add(it.id) }.forEach { message ->
                    if (message.text.isEmpty()) return@forEach
                    if (message.sender == "bot") bubbles.addBotMessage(message.text)
                    else bubbles.addUserMessage(message.text)
                }
            }
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 8_000L
    }
}
