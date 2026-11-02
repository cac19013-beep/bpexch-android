package com.example.bpexch

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

/** Small emoji strip shown above the chat input. */
object EmojiPicker {
    private val emojis = listOf("😀", "😃", "😊", "🙂", "😉", "😍", "🙏", "👍", "❤️", "🎉")

    fun show(ctx: AppContext) {
        val b = ctx.binding
        val density = ctx.density
        val popupHeight = (52 * density).toInt()
        val emojiRow = LinearLayout(ctx.activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((4 * density).toInt(), 0, (4 * density).toInt(), 0)
            setBackgroundColor(ctx.color(R.color.bg_card))
        }
        val popupContent = HorizontalScrollView(ctx.activity).apply {
            isHorizontalScrollBarEnabled = false
            addView(emojiRow, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        val popup = PopupWindow(
            popupContent,
            (b.cardChatContainer.width - (24 * density).toInt()).coerceAtLeast((240 * density).toInt()),
            popupHeight,
            true,
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 8 * density
            isOutsideTouchable = true
        }

        emojis.forEach { emoji ->
            val emojiButton = TextView(ctx.activity).apply {
                text = emoji
                textSize = 22f
                gravity = Gravity.CENTER
                minWidth = (42 * density).toInt()
                contentDescription = "Insert $emoji"
                setOnClickListener {
                    val input = b.etChatMessage
                    val start = input.selectionStart.coerceAtLeast(0)
                    val end = input.selectionEnd.coerceAtLeast(0)
                    val insertionPoint = minOf(start, end)
                    input.text.replace(insertionPoint, maxOf(start, end), emoji)
                    input.setSelection(insertionPoint + emoji.length)
                    popup.dismiss()
                }
            }
            emojiRow.addView(emojiButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }

        popup.showAsDropDown(b.btnChatEmoji, 0, -popupHeight - b.btnChatEmoji.height)
    }
}
