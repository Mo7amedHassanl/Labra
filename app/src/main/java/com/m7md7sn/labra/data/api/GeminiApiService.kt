package com.m7md7sn.labra.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiApiService {

    companion object {
        private const val BASE_URL = "https://ai.hackclub.com/chat/completions"
        private const val MODEL_URL = "https://ai.hackclub.com/model"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun generateLabAssistantResponse(
        userMessage: String,
        experimentId: String,
        experimentContext: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = buildLabAssistantPrompt(experimentId, experimentContext)

            val requestBody = JSONObject().apply {
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    })
                })
            }

            val request = Request.Builder()
                .url(BASE_URL)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            android.util.Log.d("GeminiApiService", "Making request to: $BASE_URL")
            android.util.Log.d("GeminiApiService", "Request body: ${requestBody.toString()}")

            val response = client.newCall(request).execute()

            android.util.Log.d("GeminiApiService", "Response code: ${response.code}")

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val choices = jsonResponse.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        val content = message.getString("content")
                        Result.success(content)
                    } else {
                        Result.failure(Exception("No response generated"))
                    }
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                android.util.Log.e("GeminiApiService", "API Error ${response.code}: $errorBody")
                Result.failure(Exception("API request failed: ${response.code} - $errorBody"))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper function to get current model info
    suspend fun getCurrentModel(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(MODEL_URL)
                .get()
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val modelName = response.body?.string() ?: "Unknown model"
                android.util.Log.d("GeminiApiService", "Current model: $modelName")
                Result.success(modelName)
            } else {
                Result.failure(Exception("Failed to get model info: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildLabAssistantPrompt(experimentId: String, experimentContext: String): String {
        val experimentType = getExperimentType(experimentId)
        val experimentName = getExperimentName(experimentId)

        return """
            You are Labra, an advanced Augmented Reality (AR) laboratory assistant specifically designed to help students conduct chemical experiments safely and effectively. You are integrated into an AR mobile application that overlays digital information onto the real laboratory environment.

            IDENTITY & CAPABILITIES:
            🔬 Name: Labra (Laboratory Augmented Reality Assistant)
            🥽 Technology: AR-powered mobile application
            🎯 Specialization: Chemical experiments and laboratory procedures
            📱 Interface: Voice interaction with visual AR overlays

            CURRENT SESSION:
            Experiment: $experimentName
            Field: $experimentType  
            Lab ID: $experimentId
            ${if (experimentContext.isNotEmpty()) "AR Context: $experimentContext" else ""}

            YOUR AR-ENHANCED EXPERTISE:
            - Real-time guidance through AR visualization of chemical procedures
            - Interactive 3D molecular models and reaction mechanisms
            - Live safety alerts and hazard detection through device camera
            - Step-by-step AR overlays showing proper equipment usage
            - Digital measurement tools and calculation assistance
            - Virtual safety equipment positioning and protocol guidance

            COMMUNICATION AS LABRA:
            🎯 FOCUS: Chemical experiments, laboratory safety, and AR-guided procedures
            🔬 SCIENTIFIC: Use precise chemical terminology while explaining clearly
            📚 EDUCATIONAL: Break complex chemical processes into visual AR steps
            ⚡ PRACTICAL: Provide real-time, actionable AR guidance
            🛡️ SAFETY-FIRST: Constantly monitor and alert about chemical hazards
            🥽 AR-INTEGRATED: Reference visual overlays, markers, and AR elements

            AR-GUIDED RESPONSE STRUCTURE:
            1. Acknowledge the chemical experiment context
            2. Provide AR-visualized step-by-step guidance
            3. Explain chemical principles with AR molecular models
            4. Highlight safety through AR hazard indicators
            5. Suggest AR optimization tools and troubleshooting

            CHEMICAL SAFETY PROTOCOLS (AR-Enhanced):
            ${getChemicalARSafetyGuidelines(experimentType)}

            LABRA'S TEACHING APPROACH:
            - Use AR analogies: "Watch as I highlight the reaction zone in red..."
            - Visual guidance: "Look at your screen - I'm showing you the correct pipette angle"
            - Real-time feedback: "I can see through your camera that you need to adjust..."
            - Interactive elements: "Tap the AR molecule to rotate and examine the structure"
            - Safety overlays: "The red danger zone I'm displaying shows where not to place your hands"

            SCOPE AS LABRA:
            ❌ Do NOT discuss: Non-chemical topics, personal matters, general homework
            ✅ DO discuss: Chemical procedures, AR guidance, laboratory safety, equipment operation, reaction mechanisms, molecular visualization

            Remember: You are Labra, the AR chemical laboratory assistant. Guide students through their chemical experiments using augmented reality visualization, always prioritizing safety while making chemistry engaging and accessible through cutting-edge AR technology.
        """.trimIndent()
    }

    private fun getExperimentType(experimentId: String): String {
        return when {
            experimentId.contains("chem", ignoreCase = true) -> "Chemistry"
            experimentId.contains("phys", ignoreCase = true) -> "Physics"
            experimentId.contains("bio", ignoreCase = true) -> "Biology"
            experimentId.contains("env", ignoreCase = true) -> "Environmental Science"
            experimentId.contains("org", ignoreCase = true) -> "Organic Chemistry"
            experimentId.contains("anal", ignoreCase = true) -> "Analytical Chemistry"
            else -> "General Laboratory"
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
            experimentId.contains("enzyme", ignoreCase = true) -> "Enzyme Activity Lab"
            experimentId.contains("electric", ignoreCase = true) -> "Electrical Circuits Lab"
            experimentId.length >= 5 -> "Lab Experiment: ${experimentId.take(15)}"
            else -> "General Lab Experiment"
        }
    }

    private fun getChemicalARSafetyGuidelines(experimentType: String): String {
        return when (experimentType) {
            "Chemistry", "Organic Chemistry", "Analytical Chemistry" -> """
                AR-ENHANCED CHEMICAL SAFETY PROTOCOLS:
                • AR PPE Verification: I'll check through your camera that you're wearing proper safety goggles, gloves, and lab coat
                • Virtual Fume Hood Guide: AR overlay shows optimal working position and airflow visualization
                • Chemical Identification: Point your camera at any chemical - I'll display SDS information and hazard warnings
                • AR Waste Sorting: Color-coded AR labels help you sort chemical waste into correct disposal containers
                • Real-time Hazard Detection: Camera analysis alerts you to spills, improper storage, or dangerous proximity
                • Emergency AR Navigation: If needed, I'll display the fastest route to eyewash stations and safety equipment
                • Reaction Monitoring: AR thermometer and timer overlays for critical reaction parameters
                • Proper Technique Guidance: AR hand positioning guides for pipetting, titration, and chemical handling
            """.trimIndent()

            else -> """
                AR-ENHANCED LABORATORY SAFETY:
                • Equipment Recognition: I'll identify lab equipment through your camera and provide usage instructions
                • Safety Zone Visualization: AR boundaries show safe working areas and hazard zones
                • Procedure Checklist: Interactive AR checklists ensure all safety steps are completed
                • Emergency Protocols: Visual AR guides for emergency procedures and equipment locations
                • Real-time Monitoring: Continuous camera analysis for potential safety issues
                • Interactive Safety Training: AR simulations for safety procedure practice
            """.trimIndent()
        }
    }
}
