package com.example.bpexch

import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Creates the WhatsApp-style message bubbles in the support chat and owns voice-note playback. */
class ChatBubbles(private val ctx: AppContext) {
    private val b = ctx.binding
    private val context = ctx.activity

    private var activeVoicePlayer: MediaPlayer? = null
    private var activeVoiceButton: TextView? = null

    fun release() {
        activeVoicePlayer?.release()
    }

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

    fun addImageMessage(uri: Uri, caption: String) {
        val density = ctx.density
        val bubble = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_whatsapp_bubble_user)
            val pad = (8 * density).toInt()
            setPadding(pad, pad, pad, pad)
            elevation = 1f * density
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.END
                marginStart = (44 * density).toInt()
                bottomMargin = (8 * density).toInt()
            }

            addView(ImageView(context).apply {
                setImageURI(uri)
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams((200 * density).toInt(), (180 * density).toInt()).apply {
                    bottomMargin = (4 * density).toInt()
                }
            })
            addView(TextView(context).apply {
                this.text = caption
                setTextColor(ctx.color(R.color.text_primary))
                textSize = 12.5f
            })
            addView(sentMetaRow(topMarginDp = 2))
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

    fun addVoiceMessage(file: File, durationSeconds: Int) {
        val density = ctx.density
        val durationLabel = String.format(Locale.getDefault(), "%02d:%02d", durationSeconds / 60, durationSeconds % 60)
        val bubble = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_whatsapp_bubble_user)
            val pad = (10 * density).toInt()
            setPadding(pad, pad, pad, pad)
            elevation = density
            layoutParams = LinearLayout.LayoutParams(
                (230 * density).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ).apply {
                gravity = Gravity.END
                marginStart = (44 * density).toInt()
                bottomMargin = (8 * density).toInt()
            }
        }

        val playButton = TextView(context).apply {
            text = "▶"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(ctx.color(R.color.deposit_green_dark))
            contentDescription = ctx.str(R.string.play_voice_message)
            background = ctx.drawable(R.drawable.bg_circle_icon_button)
            layoutParams = LinearLayout.LayoutParams((40 * density).toInt(), (40 * density).toInt())
        }
        val messageLabel = TextView(context).apply {
            text = ctx.str(R.string.voice_message_label, durationLabel)
            textSize = 13f
            setTextColor(ctx.color(R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = (10 * density).toInt()
            }
        }
        bubble.addView(LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(playButton)
            addView(messageLabel)
        })

        bubble.addView(TextView(context).apply {
            text = "${currentTime()}  ✓✓"
            textSize = 10f
            gravity = Gravity.END
            setTextColor(Color.parseColor("#667781"))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (3 * density).toInt()
            }
        })

        playButton.setOnClickListener { togglePlayback(file, playButton) }
        append(bubble)
    }

    // ---- helpers ----

    private fun togglePlayback(file: File, playButton: TextView) {
        if (activeVoiceButton === playButton) {
            activeVoicePlayer?.let { player ->
                if (player.isPlaying) {
                    player.pause()
                    playButton.text = "▶"
                } else {
                    player.start()
                    playButton.text = "Ⅱ"
                }
            }
            return
        }

        activeVoicePlayer?.release()
        activeVoiceButton?.text = "▶"
        val player = MediaPlayer()
        try {
            player.setDataSource(file.absolutePath)
            player.prepare()
            player.setOnCompletionListener { completedPlayer ->
                if (activeVoicePlayer === completedPlayer) {
                    activeVoicePlayer = null
                    activeVoiceButton?.text = "▶"
                    activeVoiceButton = null
                }
                completedPlayer.release()
            }
            activeVoicePlayer = player
            activeVoiceButton = playButton
            playButton.text = "Ⅱ"
            player.start()
        } catch (exception: Exception) {
            player.release()
            activeVoicePlayer = null
            activeVoiceButton = null
            playButton.text = "▶"
            ctx.toast(ctx.str(R.string.could_not_play_voice))
        }
    }

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
