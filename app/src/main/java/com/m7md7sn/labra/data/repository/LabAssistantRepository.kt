package com.m7md7sn.labra.data.repository

import android.util.Base64
import com.m7md7sn.labra.data.api.*
import com.m7md7sn.labra.data.models.*
import com.m7md7sn.labra.data.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.util.*

class LabAssistantRepository(
    private val apiService: N8nApiService = NetworkClient.n8nApiService
) {

    private var currentSessionId: String? = null
    private var currentExperimentContext: ExperimentContext? = null

    // Initialize experiment session
    suspend fun initializeExperiment(experimentId: String, deviceId: String): Result<ExperimentInitResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // For demo purposes, create a fake successful response
                val fakeResponse = ExperimentInitResponse(
                    sessionId = generateSessionId(),
                    experimentContext = ExperimentContext(
                        experimentType = getExperimentType(experimentId),
                        currentStep = 1,
                        completedSteps = emptyList(),
                        variables = mapOf("experiment_id" to experimentId),
                        safetyNotes = getSafetyNotes(experimentId)
                    ),
                    initialMessage = getInitialMessage(experimentId)
                )

                currentSessionId = fakeResponse.sessionId
                currentExperimentContext = fakeResponse.experimentContext
                Result.success(fakeResponse)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Voice recognition using base64 encoded audio
    suspend fun recognizeVoice(audioData: ByteArray, experimentId: String): Result<VoiceRecognitionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val base64Audio = Base64.encodeToString(audioData, Base64.DEFAULT)
                val request = VoiceRecognitionRequest(
                    audioData = base64Audio,
                    experimentId = experimentId,
                    sessionId = currentSessionId ?: generateSessionId()
                )

                val response = apiService.recognizeVoice(request)
                handleResponse(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Voice recognition using audio file - fake implementation
    suspend fun recognizeVoiceFromFile(audioFile: File, experimentId: String): Result<VoiceRecognitionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Simulate processing delay
                kotlinx.coroutines.delay(1500)

                // Generate fake transcription based on common lab questions
                val fakeTranscriptions = listOf(
                    "What should I do first?",
                    "How do I measure this correctly?",
                    "What's the expected result?",
                    "Is this procedure safe?",
                    "What materials do I need?",
                    "How long should this take?",
                    "What if something goes wrong?",
                    "Can you explain this step?"
                )

                val fakeResponse = VoiceRecognitionResponse(
                    transcribedText = fakeTranscriptions.random(),
                    confidence = 0.95f,
                    sessionId = currentSessionId ?: generateSessionId()
                )

                Result.success(fakeResponse)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Get AI assistant response - fake implementation
    suspend fun getAIResponse(message: String, experimentId: String): Result<AIAssistantResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Simulate processing delay
                kotlinx.coroutines.delay(2000)

                val fakeResponse = AIAssistantResponse(
                    responseText = generateAIResponse(message, experimentId),
                    audioUrl = null, // No audio URL for fake implementation
                    sessionId = currentSessionId ?: generateSessionId(),
                    suggestions = generateSuggestions(experimentId),
                    contextUpdate = null
                )

                Result.success(fakeResponse)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Convert text to speech - fake implementation
    suspend fun textToSpeech(text: String, voiceSettings: VoiceSettings): Result<TextToSpeechResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Simulate processing delay
                kotlinx.coroutines.delay(1000)

                val fakeResponse = TextToSpeechResponse(
                    audioUrl = "fake://audio/response.mp3",
                    duration = (text.length * 0.05f) // Approximate speaking time
                )

                Result.success(fakeResponse)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Session management - fake implementations
    suspend fun startSession(experimentId: String): Result<SessionStartResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val sessionId = currentSessionId ?: generateSessionId()
                currentSessionId = sessionId

                val fakeResponse = SessionStartResponse(
                    success = true,
                    sessionId = sessionId,
                    message = "Session started successfully for experiment: ${getExperimentName(experimentId)}"
                )

                Result.success(fakeResponse)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun endSession(): Result<SessionEndResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val fakeResponse = SessionEndResponse(
                    success = true,
                    summary = "Experiment session completed successfully. Great work!"
                )

                currentSessionId = null
                currentExperimentContext = null

                Result.success(fakeResponse)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    // Helper methods
    private fun <T> handleResponse(response: Response<T>): Result<T> {
        return if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("API call failed: ${response.code()} - ${response.message()}"))
        }
    }

    private fun generateSessionId(): String {
        return UUID.randomUUID().toString()
    }

    fun getCurrentSessionId(): String? = currentSessionId
    fun getCurrentExperimentContext(): ExperimentContext? = currentExperimentContext

    // Helper methods for fake data generation
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
        return "Hello! I'm your AI lab assistant for $experimentName. I'm here to guide you through the experiment safely and answer any questions you might have. What would you like to know first?"
    }

    private fun getSafetyNotes(experimentId: String): List<String> {
        return when (getExperimentType(experimentId)) {
            "chemistry" -> listOf(
                "Always wear safety goggles",
                "Keep chemicals away from heat sources",
                "Work in well-ventilated area",
                "Have safety shower location in mind"
            )
            "physics" -> listOf(
                "Handle equipment carefully",
                "Be aware of electrical hazards",
                "Secure all moving parts",
                "Follow measurement protocols"
            )
            "biology" -> listOf(
                "Maintain sterile conditions",
                "Dispose of biological waste properly",
                "Wash hands thoroughly",
                "Use proper protective equipment"
            )
            else -> listOf(
                "Follow all safety protocols",
                "Ask for help if unsure",
                "Keep workspace organized",
                "Report any incidents immediately"
            )
        }
    }

    private fun generateAIResponse(message: String, experimentId: String): String {
        val experimentType = getExperimentType(experimentId)

        return when {
            message.contains("first", ignoreCase = true) || message.contains("start", ignoreCase = true) -> {
                when (experimentType) {
                    "chemistry" -> "First, put on your safety goggles and lab coat. Then gather all your materials according to the lab manual. Make sure your workspace is clean and well-ventilated."
                    "physics" -> "Start by familiarizing yourself with the equipment. Check all connections and ensure everything is properly calibrated before beginning measurements."
                    "biology" -> "Begin by sterilizing your workspace and putting on gloves. Prepare your microscope and any specimens you'll be observing."
                    else -> "First, review the experimental procedure and gather all necessary materials. Ensure you understand each step before proceeding."
                }
            }
            message.contains("measure", ignoreCase = true) -> {
                "For accurate measurements, use the appropriate measuring tools. Read measurements at eye level and record them immediately. Double-check your readings and take multiple measurements if needed for accuracy."
            }
            message.contains("result", ignoreCase = true) || message.contains("expect", ignoreCase = true) -> {
                "The expected results depend on your specific experiment, but you should observe clear changes or patterns. Document everything you see, even if it's different from what you expected - that's valuable scientific data!"
            }
            message.contains("safe", ignoreCase = true) || message.contains("danger", ignoreCase = true) -> {
                "Safety is our top priority! Always follow the safety guidelines, wear appropriate protective equipment, and don't hesitate to ask for help if you're unsure about any procedure."
            }
            message.contains("material", ignoreCase = true) || message.contains("equipment", ignoreCase = true) -> {
                "Make sure you have all the materials listed in your lab manual. If anything is missing or damaged, inform your instructor before proceeding."
            }
            message.contains("time", ignoreCase = true) || message.contains("long", ignoreCase = true) -> {
                "The experiment timing depends on the specific procedures, but most lab experiments take 1-2 hours. Some steps may require waiting periods - use this time to record observations and prepare for the next steps."
            }
            message.contains("wrong", ignoreCase = true) || message.contains("problem", ignoreCase = true) -> {
                "If something goes wrong, stop immediately and assess the situation. Don't panic - most issues can be resolved safely. Check your procedure, measurements, and equipment. If needed, ask for assistance."
            }
            message.contains("explain", ignoreCase = true) || message.contains("help", ignoreCase = true) -> {
                "I'd be happy to help explain any part of the experiment! Can you be more specific about what step or concept you'd like me to clarify?"
            }
            else -> {
                "That's a great question! Based on your experiment, I recommend following the step-by-step procedure carefully. Remember to observe and record everything, and don't hesitate to ask if you need clarification on any specific step."
            }
        }
    }

    private fun generateSuggestions(experimentId: String): List<String> {
        val experimentType = getExperimentType(experimentId)

        return when (experimentType) {
            "chemistry" -> listOf(
                "Check chemical compatibility",
                "Measure reagents carefully",
                "Monitor reaction progress",
                "Record observations"
            )
            "physics" -> listOf(
                "Calibrate instruments",
                "Take multiple measurements",
                "Check for systematic errors",
                "Graph your data"
            )
            "biology" -> listOf(
                "Maintain sterile conditions",
                "Use proper magnification",
                "Document cellular structures",
                "Compare with reference images"
            )
            else -> listOf(
                "Follow the procedure step by step",
                "Record all observations",
                "Ask questions if unsure",
                "Review safety protocols"
            )
        }
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
}
