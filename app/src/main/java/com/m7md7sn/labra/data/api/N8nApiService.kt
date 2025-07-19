package com.m7md7sn.labra.data.api

import com.m7md7sn.labra.data.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface N8nApiService {

    // Voice Recognition webhook
    @POST("webhook/voice-recognition")
    suspend fun recognizeVoice(
        @Body request: VoiceRecognitionRequest
    ): Response<VoiceRecognitionResponse>

    // Alternative endpoint for audio file upload
    @Multipart
    @POST("webhook/voice-recognition-file")
    suspend fun recognizeVoiceFromFile(
        @Part audio: MultipartBody.Part,
        @Part("experiment_id") experimentId: RequestBody,
        @Part("session_id") sessionId: RequestBody
    ): Response<VoiceRecognitionResponse>

    // AI Assistant webhook
    @POST("webhook/ai-assistant")
    suspend fun getAIAssistantResponse(
        @Body request: AIAssistantRequest
    ): Response<AIAssistantResponse>

    // Text to Speech webhook
    @POST("webhook/text-to-speech")
    suspend fun textToSpeech(
        @Body request: TextToSpeechRequest
    ): Response<TextToSpeechResponse>

    // Experiment initialization
    @POST("webhook/experiment/initialize")
    suspend fun initializeExperiment(
        @Body request: ExperimentInitRequest
    ): Response<ExperimentInitResponse>

    // Session management
    @POST("webhook/session/start")
    suspend fun startSession(
        @Body request: SessionStartRequest
    ): Response<SessionStartResponse>

    @POST("webhook/session/end")
    suspend fun endSession(
        @Body request: SessionEndRequest
    ): Response<SessionEndResponse>

    // Get experiment instructions
    @GET("webhook/experiment/{experimentId}/instructions")
    suspend fun getExperimentInstructions(
        @Path("experimentId") experimentId: String
    ): Response<ExperimentInstructionsResponse>
}

// Additional request/response models for new endpoints
data class ExperimentInitRequest(
    val experimentId: String,
    val userId: String?,
    val deviceId: String
)

data class ExperimentInitResponse(
    val sessionId: String,
    val experimentContext: ExperimentContext,
    val initialMessage: String
)

data class SessionStartRequest(
    val experimentId: String,
    val sessionId: String,
    val timestamp: Long
)

data class SessionStartResponse(
    val success: Boolean,
    val sessionId: String,
    val message: String
)

data class SessionEndRequest(
    val sessionId: String,
    val timestamp: Long,
    val duration: Long
)

data class SessionEndResponse(
    val success: Boolean,
    val summary: String
)

data class ExperimentInstructionsResponse(
    val title: String,
    val description: String,
    val steps: List<ExperimentStep>,
    val safetyNotes: List<String>,
    val materials: List<String>
)

data class ExperimentStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val estimatedTime: String,
    val warnings: List<String>?
)
