package com.example.bpexch

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import java.io.File

/** Records a voice note from the chat mic button (handles the RECORD_AUDIO permission request). */
class VoiceRecorder(
    private val ctx: AppContext,
    private val onRecorded: (file: File, durationSeconds: Int) -> Unit,
) {
    private val b = ctx.binding
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L

    private val permissionLauncher = ctx.activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else ctx.toast(ctx.str(R.string.mic_permission_needed))
    }

    fun toggle() {
        if (recorder != null) {
            stop()
        } else if (ContextCompat.checkSelfPermission(ctx.activity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            start()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun release() {
        recorder?.let {
            runCatching { it.stop() }
            it.release()
        }
        outputFile?.delete()
    }

    private fun start() {
        val file = File(ctx.activity.cacheDir, "support-voice-${System.currentTimeMillis()}.m4a")
        val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(ctx.activity) else MediaRecorder()

        try {
            newRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            newRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            newRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            newRecorder.setAudioEncodingBitRate(96000)
            newRecorder.setAudioSamplingRate(44100)
            newRecorder.setOutputFile(file.absolutePath)
            newRecorder.prepare()
            newRecorder.start()

            recorder = newRecorder
            outputFile = file
            startedAt = SystemClock.elapsedRealtime()
            b.btnChatVoice.setImageResource(R.drawable.ic_stop)
            b.btnChatVoice.setColorFilter(Color.parseColor("#DC2626"))
            b.btnChatVoice.contentDescription = ctx.str(R.string.stop_send_voice)
            ctx.toast(ctx.str(R.string.recording_tap_send))
        } catch (exception: Exception) {
            newRecorder.release()
            file.delete()
            ctx.toast(ctx.str(R.string.recording_start_failed))
        }
    }

    private fun stop() {
        val activeRecorder = recorder ?: return
        val file = outputFile
        val durationSeconds = ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt().coerceAtLeast(1)
        recorder = null
        outputFile = null
        b.btnChatVoice.setImageResource(R.drawable.ic_mic)
        b.btnChatVoice.clearColorFilter()
        b.btnChatVoice.contentDescription = ctx.str(R.string.record_voice_message)

        try {
            activeRecorder.stop()
            if (file == null || !file.exists() || file.length() == 0L) {
                ctx.toast(ctx.str(R.string.no_voice_recorded))
                file?.delete()
                return
            }
            onRecorded(file, durationSeconds)
        } catch (exception: RuntimeException) {
            file?.delete()
            ctx.toast(ctx.str(R.string.recording_too_short))
        } finally {
            activeRecorder.release()
        }
    }
}
