package com.m7md7sn.labra.data.models

import com.google.gson.annotations.SerializedName

// Request models for n8n webhooks
data class VoiceRecognitionRequest(
    @SerializedName("audio_data")
    val audioData: String, // Base64 encoded audio
    @SerializedName("experiment_id")
    val experimentId: String,
    @SerializedName("session_id")
    val sessionId: String
)

data class AIAssistantRequest(
    @SerializedName("message")
    val message: String,
    @SerializedName("experiment_id")
    val experimentId: String,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("context")
    val context: ExperimentContext?
)

data class TextToSpeechRequest(
    @SerializedName("text")
    val text: String,
    @SerializedName("voice_settings")
    val voiceSettings: VoiceSettings = VoiceSettings()
)

// Response models from n8n
data class VoiceRecognitionResponse(
    @SerializedName("transcribed_text")
    val transcribedText: String,
    @SerializedName("confidence")
    val confidence: Float,
    @SerializedName("session_id")
    val sessionId: String
)

data class AIAssistantResponse(
    @SerializedName("response_text")
    val responseText: String,
    @SerializedName("audio_url")
    val audioUrl: String?,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("suggestions")
    val suggestions: List<String>?,
    @SerializedName("context_update")
    val contextUpdate: ExperimentContext?
)

data class TextToSpeechResponse(
    @SerializedName("audio_url")
    val audioUrl: String,
    @SerializedName("duration")
    val duration: Float
)

// Supporting data models
data class ExperimentContext(
    @SerializedName("experiment_type")
    val experimentType: String,
    @SerializedName("current_step")
    val currentStep: Int,
    @SerializedName("completed_steps")
    val completedSteps: List<Int>,
    @SerializedName("variables")
    val variables: Map<String, String>,
    @SerializedName("safety_notes")
    val safetyNotes: List<String>
)

data class VoiceSettings(
    @SerializedName("voice_id")
    val voiceId: String = "default",
    @SerializedName("speed")
    val speed: Float = 1.0f,
    @SerializedName("pitch")
    val pitch: Float = 1.0f
)

// Error response model
data class ApiErrorResponse(
    @SerializedName("error")
    val error: String,
    @SerializedName("message")
    val message: String,
    @SerializedName("code")
    val code: Int
)
