package com.example.bpexch

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Creates the WhatsApp-style message bubbles in the support chat. */
class ChatBubbles(private val ctx: AppContext) {
    private val b = ctx.binding
    private val context = ctx.activity

    // ---- public API ----

    fun addUserMessage(text: String) {
        val density = ctx.density
        val bubble = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_whatsapp_bubble_user)
            val pad = (10 * density).toInt()
            setPadding(pad, pad, pad, pad)
            elevation = 1f * density
            val sideMargin = (44 * density).toInt()
            val tv = TextView(context).apply {
                this.text = text
                setTextColor(ctx.color(R.color.text_primary))
                textSize = 13.5f
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                bubbleWidth(tv, text, pad, sideMargin),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.END
                marginStart = sideMargin
                bottomMargin = (8 * density).toInt()
            }
            addView(tv)
            addView(sentMetaRow(topMarginDp = 3))
        }
        append(bubble)
    }

    fun addBotMessage(text: String) {
        val density = ctx.density
        val bubble = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_whatsapp_bubble_bot)
            val pad = (10 * density).toInt()
            setPadding(pad, pad, pad, pad)
            elevation = 1f * density
            val sideMargin = (44 * density).toInt()
            val tv = TextView(context).apply {
                this.text = text
                setTextColor(Color.parseColor("#111B21"))
                textSize = 13.5f
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }
            layoutParams = LinearLayout.LayoutParams(
                bubbleWidth(tv, text, pad, sideMargin),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.START
                marginEnd = sideMargin
                bottomMargin = (8 * density).toInt()
            }
            addView(tv)
            addView(timeLabel().apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply {
                    gravity = Gravity.END
                    topMargin = (3 * density).toInt()
                }
            })
        }
        append(bubble)
    }

    // ---- helpers ----

    private fun currentTime(): String = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

    private fun timeLabel() = TextView(context).apply {
        text = currentTime()
        setTextColor(Color.parseColor("#667781"))
        textSize = 10f
    }

    /** Timestamp + double tick shown under an outgoing message. */
    private fun sentMetaRow(topMarginDp: Int): View {
        val density = ctx.density
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.END
                topMargin = (topMarginDp * density).toInt()
            }
            addView(timeLabel())
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_double_check)
                layoutParams = LinearLayout.LayoutParams((14 * density).toInt(), (14 * density).toInt()).apply {
                    marginStart = (4 * density).toInt()
                }
            })
        }
    }

    private fun bubbleWidth(textView: TextView, text: String, padding: Int, sideMargin: Int): Int {
        val scrollView = b.chatScrollView
        val availableWidth = scrollView.width - scrollView.paddingLeft - scrollView.paddingRight - sideMargin
        val minimumWidth = (72 * ctx.density).toInt()
        val maximumWidth = (availableWidth * 0.9f).toInt().coerceAtLeast(minimumWidth)
        val textWidth = text.lineSequence().maxOfOrNull { textView.paint.measureText(it).toInt() } ?: 0
        return (textWidth + padding * 2).coerceIn(minimumWidth, maximumWidth)
    }

    private fun append(bubble: View) {
        b.layoutChatMessages.addView(bubble)
        b.chatScrollView.post { b.chatScrollView.fullScroll(View.FOCUS_DOWN) }
    }
}
