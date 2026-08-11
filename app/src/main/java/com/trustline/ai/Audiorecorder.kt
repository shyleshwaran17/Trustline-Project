package com.trustline.ai

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

        recorder = MediaRecorder(context).apply {

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

        recorder?.apply {
            stop()
            release()
        }

        recorder = null

        return outputFile
    }
}