package com.trustline.ai
import android.util.Log
import android.content.Context
import android.media.MediaRecorder
import java.io.File

class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording() {

        outputFile = File(
            context.cacheDir,
            "TrustLine_${System.currentTimeMillis()}.m4a"
        )

        recorder = MediaRecorder().apply {

            setAudioSource(MediaRecorder.AudioSource.MIC)

            setOutputFormat(
                MediaRecorder.OutputFormat.MPEG_4
            )

            setAudioEncoder(
                MediaRecorder.AudioEncoder.AAC
            )

            setOutputFile(
                outputFile!!.absolutePath
            )

            prepare()
            start()
        }
    }

    fun stopRecording(): File? {

        val currentRecorder = recorder
            ?: return null

        val currentFile = outputFile

        recorder = null
        outputFile = null

        try {

            currentRecorder.stop()

        } catch (e: RuntimeException) {

            Log.e(
                "TrustLine",
                "MediaRecorder stop failed",
                e
            )

            return null

        } finally {

            currentRecorder.release()
        }

        return currentFile?.takeIf {
            it.exists() && it.length() > 0
        }
    }
}