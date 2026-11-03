package com.example.bpexch

import android.view.View
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog

/** Bottom sheets opened from the home screen and login screen. */
class AppSheets(private val ctx: AppContext) {

    private fun inflate(layoutRes: Int): Pair<BottomSheetDialog, View> {
        val dialog = BottomSheetDialog(ctx.activity)
        val sheetView = ctx.activity.layoutInflater.inflate(layoutRes, null)
        dialog.setContentView(sheetView)
        return dialog to sheetView
    }

    private val prefs = ctx.activity.getSharedPreferences("ui", android.content.Context.MODE_PRIVATE)

    /** The bell's unread badge stays hidden once the notifications sheet has been opened. */
    val notificationsSeen: Boolean get() = prefs.getBoolean("notifications_seen", false)

    fun showNotifications() {
        prefs.edit().putBoolean("notifications_seen", true).apply()
        val (dialog, sheet) = inflate(R.layout.layout_notifications_sheet)

        val tvCount = sheet.findViewById<TextView>(R.id.tvNotificationCount)
        val btnMarkRead = sheet.findViewById<TextView>(R.id.btnMarkAllRead)

        btnMarkRead?.setOnClickListener {
            tvCount?.text = ctx.str(R.string.zero_new)
            tvCount?.visibility = View.GONE
            btnMarkRead.visibility = View.GONE
            ctx.toast(ctx.str(R.string.all_marked_read))
        }
        sheet.findViewById<View>(R.id.btnDismissNotifications)?.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    fun showProfile() {
        val (dialog, sheet) = inflate(R.layout.layout_profile_sheet)

        sheet.findViewById<TextView>(R.id.tvProfileSheetUsername)?.text = ctx.binding.tvUsernameValue.text.toString()

        val openChat = View.OnClickListener {
            dialog.dismiss()
            ctx.support.open()
        }
        sheet.findViewById<View>(R.id.btnProfileWhatsApp)?.setOnClickListener(openChat)
        sheet.findViewById<View>(R.id.btnProfileLiveChat)?.setOnClickListener(openChat)

        sheet.findViewById<View>(R.id.btnProfileLogout)?.setOnClickListener {
            dialog.dismiss()
            ctx.logout()
        }

        dialog.show()
    }

    fun showVideoGuide() {
        val (dialog, sheet) = inflate(R.layout.dialog_video_guide)

        sheet.findViewById<View>(R.id.btnVideoWhatsAppAgent)?.setOnClickListener {
            dialog.dismiss()
            ctx.support.open()
        }
        sheet.findViewById<View>(R.id.cardVideoPlayer)?.setOnClickListener {
            ctx.toast(ctx.str(R.string.playing_video_guide))
        }
        sheet.findViewById<View>(R.id.btnDismissVideoGuide)?.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }
}
