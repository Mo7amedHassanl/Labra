package com.m7md7sn.labra.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ElevenLabsApiService {

    @POST("v1/text-to-speech/{voice_id}")
    @Headers("Accept: audio/mpeg")
    suspend fun textToSpeech(
        @Path("voice_id") voiceId: String,
        @Header("xi-api-key") apiKey: String,
        @Body request: TextToSpeechRequest
    ): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://api.elevenlabs.io/"
        const val DEFAULT_VOICE_ID = "21m00Tcm4TlvDq8ikWAM" // Rachel voice
    }
}

data class TextToSpeechRequest(
    val text: String,
    val model_id: String = "eleven_monolingual_v1",
    val voice_settings: ElevenLabsVoiceSettings = ElevenLabsVoiceSettings()
)

data class ElevenLabsVoiceSettings(
    val stability: Float = 0.5f,
    val similarity_boost: Float = 0.5f,
    val style: Float = 0.0f,
    val use_speaker_boost: Boolean = true
)
