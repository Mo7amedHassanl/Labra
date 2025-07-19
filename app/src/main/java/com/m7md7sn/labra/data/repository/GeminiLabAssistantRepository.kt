package com.m7md7sn.labra.data.repository

import com.m7md7sn.labra.data.api.GeminiApiService
import com.m7md7sn.labra.data.api.ExperimentInitResponse
import com.m7md7sn.labra.data.api.SessionStartResponse
import com.m7md7sn.labra.data.api.SessionEndResponse
import com.m7md7sn.labra.data.models.*
import com.m7md7sn.labra.utils.AudioRecorderWithSpeechRecognition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class GeminiLabAssistantRepository {

    private val geminiApiService = GeminiApiService()
    private var currentSessionId: String? = null
    private var currentExperimentContext: ExperimentContext? = null

    // Initialize experiment session
    suspend fun initializeExperiment(experimentId: String, deviceId: String): Result<ExperimentInitResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = generateSessionId()
                val experimentContext = ExperimentContext(
                    experimentType = getExperimentType(experimentId),
                    currentStep = 1,
                    completedSteps = emptyList(),
                    variables = mapOf("experiment_id" to experimentId, "device_id" to deviceId),
                    safetyNotes = getSafetyNotes(experimentId)
                )

                val response = ExperimentInitResponse(
                    sessionId = sessionId,
                    experimentContext = experimentContext,
                    initialMessage = getInitialMessage(experimentId)
                )

                currentSessionId = sessionId
                currentExperimentContext = experimentContext
                Result.success(response)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Voice recognition using real speech-to-text
    suspend fun recognizeVoiceFromSpeech(audioRecorderWithSpeechRecognition: AudioRecorderWithSpeechRecognition): Result<VoiceRecognitionResponse> {
        return withContext(Dispatchers.Main) {
            try {
                // Use real speech recognition instead of fake transcription
                val speechResult = audioRecorderWithSpeechRecognition.recognizeSpeech()

                if (speechResult.isSuccess) {
                    val transcribedText = speechResult.getOrNull()!!

                    val response = VoiceRecognitionResponse(
                        transcribedText = transcribedText,
                        confidence = 0.95f, // High confidence for real speech recognition
                        sessionId = currentSessionId ?: generateSessionId()
                    )

                    Result.success(response)
                } else {
                    Result.failure(speechResult.exceptionOrNull() ?: Exception("Speech recognition failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Get AI assistant response using Gemini API
    suspend fun getAIResponse(message: String, experimentId: String): Result<AIAssistantResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val contextString = currentExperimentContext?.let { context ->
                    "Current step: ${context.currentStep}, Completed steps: ${context.completedSteps.joinToString()}"
                } ?: ""

                val geminiResult = geminiApiService.generateLabAssistantResponse(
                    userMessage = message,
                    experimentId = experimentId,
                    experimentContext = contextString
                )

                if (geminiResult.isSuccess) {
                    val geminiResponse = geminiResult.getOrNull()!!

                    val response = AIAssistantResponse(
                        responseText = geminiResponse,
                        audioUrl = null, // No audio URL for TTS
                        sessionId = currentSessionId ?: generateSessionId(),
                        suggestions = generateSuggestions(experimentId),
                        contextUpdate = updateExperimentContext(message)
                    )

                    Result.success(response)
                } else {
                    Result.failure(geminiResult.exceptionOrNull() ?: Exception("Failed to get Gemini response"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Text to speech - simulate
    suspend fun textToSpeech(text: String, voiceSettings: VoiceSettings): Result<TextToSpeechResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Simulate processing delay
                kotlinx.coroutines.delay(500)

                val response = TextToSpeechResponse(
                    audioUrl = "local://tts/response.mp3",
                    duration = (text.length * 0.08f) // Realistic speaking duration
                )

                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Session management
    suspend fun startSession(experimentId: String): Result<SessionStartResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = currentSessionId ?: generateSessionId()
                currentSessionId = sessionId

                val response = SessionStartResponse(
                    success = true,
                    sessionId = sessionId,
                    message = "Session started for ${getExperimentName(experimentId)} with Gemini AI assistant"
                )

                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun endSession(): Result<SessionEndResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = SessionEndResponse(
                    success = true,
                    summary = "Lab session completed successfully with Gemini AI assistance!"
                )

                currentSessionId = null
                currentExperimentContext = null

                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Helper methods
    private fun generateSessionId(): String = UUID.randomUUID().toString()

    private fun getExperimentType(experimentId: String): String {
        return when {
            experimentId.contains("chem", ignoreCase = true) -> "chemistry"
            experimentId.contains("phys", ignoreCase = true) -> "physics"
            experimentId.contains("bio", ignoreCase = true) -> "biology"
            else -> "general_lab"
        }
    }

    private fun getInitialMessage(experimentId: String): String {
        val experimentName = getExperimentName(experimentId)
        return "Hello! I'm your AI lab assistant powered by Gemini. I'm here to help you with $experimentName. I can guide you through procedures, answer safety questions, and help you understand your results. What would you like to know?"
    }

    private fun getExperimentName(experimentId: String): String {
        return when {
            experimentId.contains("chem", ignoreCase = true) -> "Chemical Reactions Lab"
            experimentId.contains("phys", ignoreCase = true) -> "Physics Measurement Lab"
            experimentId.contains("bio", ignoreCase = true) -> "Biology Observation Lab"
            experimentId.contains("dna", ignoreCase = true) -> "DNA Analysis Lab"
            experimentId.contains("acid", ignoreCase = true) -> "Acid-Base Titration"
            experimentId.contains("motion", ignoreCase = true) -> "Motion Analysis Lab"
            experimentId.contains("cell", ignoreCase = true) -> "Cell Structure Study"
            experimentId.contains("light", ignoreCase = true) -> "Optics and Light Lab"
            experimentId.length >= 5 -> "Lab Experiment: ${experimentId.take(10)}"
            else -> "General Lab Experiment"
        }
    }

    private fun getSafetyNotes(experimentId: String): List<String> {
        return when (getExperimentType(experimentId)) {
            "chemistry" -> listOf(
                "Always wear safety goggles and lab coat",
                "Work in well-ventilated area or fume hood",
                "Handle all chemicals with appropriate tools",
                "Know location of safety shower and eyewash station"
            )
            "physics" -> listOf(
                "Be aware of electrical hazards",
                "Handle precision instruments carefully",
                "Secure all equipment before measurements",
                "Never look directly into laser beams"
            )
            "biology" -> listOf(
                "Maintain sterile technique when required",
                "Wear gloves when handling biological materials",
                "Dispose of biological waste properly",
                "Wash hands thoroughly before and after"
            )
            else -> listOf(
                "Follow all safety protocols",
                "Wear appropriate protective equipment",
                "Ask for help if unsure about procedures",
                "Report any incidents immediately"
            )
        }
    }

    private fun generateSuggestions(experimentId: String): List<String> {
        return when (getExperimentType(experimentId)) {
            "chemistry" -> listOf(
                "Check chemical compatibility before mixing",
                "Measure reagents accurately",
                "Record all observations",
                "Monitor reaction temperature"
            )
            "physics" -> listOf(
                "Calibrate instruments before use",
                "Take multiple measurements",
                "Check for systematic errors",
                "Graph data for analysis"
            )
            "biology" -> listOf(
                "Use proper magnification settings",
                "Document all observations",
                "Compare with reference materials",
                "Maintain sample integrity"
            )
            else -> listOf(
                "Follow procedure step-by-step",
                "Record detailed observations",
                "Ask questions when unsure",
                "Double-check all measurements"
            )
        }
    }

    private fun updateExperimentContext(userMessage: String): ExperimentContext? {
        val currentContext = currentExperimentContext ?: return null

        // Simple context update based on message content
        val updatedStep = when {
            userMessage.contains("next", ignoreCase = true) -> currentContext.currentStep + 1
            userMessage.contains("done", ignoreCase = true) -> currentContext.currentStep + 1
            userMessage.contains("finished", ignoreCase = true) -> currentContext.currentStep + 1
            else -> currentContext.currentStep
        }

        val updatedCompletedSteps = if (updatedStep > currentContext.currentStep) {
            currentContext.completedSteps + currentContext.currentStep
        } else {
            currentContext.completedSteps
        }

        return currentContext.copy(
            currentStep = updatedStep,
            completedSteps = updatedCompletedSteps
        )
    }

    fun getCurrentSessionId(): String? = currentSessionId
    fun getCurrentExperimentContext(): ExperimentContext? = currentExperimentContext
}
