package com.m7md7sn.labra.utils

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class AudioRecorderWithSpeechRecognition(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false
    private val elevenLabsClient = ElevenLabsClient(context)

    // Record audio for speech recognition
    suspend fun startRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (isRecording) {
                return@withContext Result.failure(Exception("Already recording"))
            }

            // Create output file
            outputFile = File(context.cacheDir, "recorded_audio_${System.currentTimeMillis()}.mp3")

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile?.absolutePath)

                try {
                    prepare()
                    start()
                    isRecording = true
                    Log.d("AudioRecorder", "Recording started")
                    Result.success(Unit)
                } catch (e: IOException) {
                    Log.e("AudioRecorder", "Failed to start recording", e)
                    Result.failure(e)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error starting recording", e)
            Result.failure(e)
        }
    }

    suspend fun stopRecording(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isRecording) {
                return@withContext Result.failure(Exception("Not currently recording"))
            }

            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            Log.d("AudioRecorder", "Recording stopped")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error stopping recording", e)
            isRecording = false
            mediaRecorder?.release()
            mediaRecorder = null
            Result.failure(e)
        }
    }

    // Enhanced Speech Recognition with ElevenLabs
    suspend fun recognizeSpeech(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val audioFile = outputFile
            if (audioFile == null || !audioFile.exists()) {
                return@withContext Result.failure(Exception("No audio file available for recognition"))
            }

            Log.d("AudioRecorder", "Using ElevenLabs Speech-to-Text API")
            return@withContext elevenLabsClient.speechToText(audioFile)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error during speech recognition", e)
            Result.failure(e)
        }
    }

    // Record and recognize speech in one operation
    suspend fun recordAndRecognize(durationMs: Long = 5000): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Start recording
            val startResult = startRecording()
            if (startResult.isFailure) {
                return@withContext startResult.map { "" }
            }

            // Wait for specified duration
            kotlinx.coroutines.delay(durationMs)

            // Stop recording
            val stopResult = stopRecording()
            if (stopResult.isFailure) {
                return@withContext stopResult.map { "" }
            }

            // Recognize speech
            return@withContext recognizeSpeech()
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error in record and recognize", e)
            Result.failure(e)
        }
    }

    // Text-to-Speech with ElevenLabs
    suspend fun speak(text: String): Result<Unit> {
        return try {
            Log.d("AudioRecorder", "Using ElevenLabs Text-to-Speech API")
            elevenLabsClient.textToSpeech(text)
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error during text-to-speech", e)
            Result.failure(e)
        }
    }

    fun release() {
        try {
            if (isRecording) {
                mediaRecorder?.stop()
            }
            mediaRecorder?.release()
            elevenLabsClient.release()

            // Clean up temporary files
            outputFile?.delete()

            Log.d("AudioRecorder", "Resources released")
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Error releasing resources", e)
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording
}
