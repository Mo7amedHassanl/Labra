package com.m7md7sn.labra.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import com.m7md7sn.labra.data.api.ElevenLabsApiService
import com.m7md7sn.labra.data.api.TextToSpeechRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent
import android.os.Bundle
import java.util.*

class ElevenLabsClient(private val context: Context) {

    private val apiKey = "sk_02a0d88b42edce1f4d673efce931eece36986a4110f9e521"
    private val voiceId = ElevenLabsApiService.DEFAULT_VOICE_ID

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService = Retrofit.Builder()
        .baseUrl(ElevenLabsApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ElevenLabsApiService::class.java)

    private var mediaPlayer: MediaPlayer? = null

    suspend fun textToSpeech(text: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d("ElevenLabsClient", "Converting text to speech: $text")

            val request = TextToSpeechRequest(text = text)
            val response = apiService.textToSpeech(voiceId, apiKey, request)

            if (response.isSuccessful) {
                val audioBytes = response.body()?.bytes()
                if (audioBytes != null) {
                    // Save audio to temporary file
                    val tempFile = File(context.cacheDir, "elevenlabs_tts_${System.currentTimeMillis()}.mp3")
                    FileOutputStream(tempFile).use { it.write(audioBytes) }

                    // Play audio
                    withContext(Dispatchers.Main) {
                        playAudio(tempFile)
                    }

                    Log.d("ElevenLabsClient", "Text-to-speech completed successfully")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Empty response from ElevenLabs TTS API"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ElevenLabsClient", "TTS API error: ${response.code()} - $errorBody")
                Result.failure(Exception("TTS API error: ${response.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("ElevenLabsClient", "Error in text-to-speech", e)
            Result.failure(e)
        }
    }

    suspend fun speechToText(audioFile: File): Result<String> = withContext(Dispatchers.Main) {
        // Use Android's built-in SpeechRecognizer instead of ElevenLabs API
        return@withContext startSpeechRecognition()
    }

    private suspend fun startSpeechRecognition(): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                continuation.resume(Result.failure(Exception("Speech recognition is not available on this device")))
                return@suspendCancellableCoroutine
            }

            val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d("ElevenLabsClient", "Ready for speech input")
                }

                override fun onBeginningOfSpeech() {
                    Log.d("ElevenLabsClient", "Speech input detected")
                }

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    Log.d("ElevenLabsClient", "End of speech")
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech input matched"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input detected"
                        else -> "Unknown error"
                    }
                    Log.e("ElevenLabsClient", "Speech recognition error: $errorMessage")
                    speechRecognizer.destroy()
                    if (continuation.isActive) {
                        continuation.resume(Result.failure(Exception("Speech recognition failed: $errorMessage")))
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val recognizedText = matches[0]
                        Log.d("ElevenLabsClient", "Speech recognition result: $recognizedText")
                        speechRecognizer.destroy()
                        if (continuation.isActive) {
                            continuation.resume(Result.success(recognizedText))
                        }
                    } else {
                        speechRecognizer.destroy()
                        if (continuation.isActive) {
                            continuation.resume(Result.failure(Exception("No speech recognized")))
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            continuation.invokeOnCancellation {
                speechRecognizer.destroy()
            }

            speechRecognizer.startListening(intent)

        } catch (e: Exception) {
            Log.e("ElevenLabsClient", "Error starting speech recognition", e)
            if (continuation.isActive) {
                continuation.resume(Result.failure(e))
            }
        }
    }

    private suspend fun playAudio(audioFile: File) = suspendCancellableCoroutine<Unit> { continuation ->
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnPreparedListener {
                    start()
                    Log.d("ElevenLabsClient", "Started playing audio")
                }
                setOnCompletionListener {
                    Log.d("ElevenLabsClient", "Audio playback completed")
                    audioFile.delete() // Clean up temporary file
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("ElevenLabsClient", "MediaPlayer error: what=$what, extra=$extra")
                    audioFile.delete() // Clean up temporary file
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                    true
                }
                prepareAsync()
            }

            continuation.invokeOnCancellation {
                mediaPlayer?.release()
                mediaPlayer = null
                audioFile.delete()
            }

        } catch (e: Exception) {
            Log.e("ElevenLabsClient", "Error playing audio", e)
            audioFile.delete()
            if (continuation.isActive) {
                continuation.resume(Unit)
            }
        }
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
