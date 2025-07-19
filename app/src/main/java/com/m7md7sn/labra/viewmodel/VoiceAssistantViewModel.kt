package com.m7md7sn.labra.viewmodel

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m7md7sn.labra.data.models.*
import com.m7md7sn.labra.data.repository.GeminiLabAssistantRepository
import com.m7md7sn.labra.screens.ChatMessage
import com.m7md7sn.labra.utils.AudioRecorderWithSpeechRecognition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VoiceAssistantState(
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isProcessing: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val currentExperiment: String = "",
    val experimentContext: ExperimentContext? = null,
    val errorMessage: String? = null,
    val isSessionActive: Boolean = false
)

class VoiceAssistantViewModel(
    private val context: Context,
    private val repository: GeminiLabAssistantRepository = GeminiLabAssistantRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(VoiceAssistantState())
    val state: StateFlow<VoiceAssistantState> = _state.asStateFlow()

    private val audioRecorder = AudioRecorderWithSpeechRecognition(context)
    private var currentExperimentId = ""

    fun initializeExperiment(experimentId: String) {
        currentExperimentId = experimentId
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isProcessing = true)

                // Get device ID
                val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

                // Initialize experiment with n8n
                val initResult = repository.initializeExperiment(experimentId, deviceId)

                if (initResult.isSuccess) {
                    val initResponse = initResult.getOrNull()!!

                    // Start session
                    val sessionResult = repository.startSession(experimentId)

                    if (sessionResult.isSuccess) {
                        val experimentName = getExperimentName(experimentId)
                        val initialMessage = initResponse.initialMessage
                        val initialMessages = listOf(
                            ChatMessage(initialMessage, isFromUser = false)
                        )

                        _state.value = _state.value.copy(
                            currentExperiment = experimentName,
                            experimentContext = initResponse.experimentContext,
                            chatMessages = initialMessages,
                            isSessionActive = true,
                            isProcessing = false,
                            errorMessage = null
                        )

                        // Convert initial message to speech with special handling
                        convertTextToSpeech(initialMessage, isInitialMessage = true)
                    } else {
                        _state.value = _state.value.copy(
                            errorMessage = "Failed to start session: ${sessionResult.exceptionOrNull()?.message}",
                            isProcessing = false
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        errorMessage = "Failed to initialize experiment: ${initResult.exceptionOrNull()?.message}",
                        isProcessing = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = "Error initializing experiment: ${e.message}",
                    isProcessing = false
                )
            }
        }
    }

    fun startListening() {
        if (_state.value.isListening || _state.value.isSpeaking || _state.value.isProcessing) return

        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isListening = true, errorMessage = null)

                // Start recording
                val startResult = audioRecorder.startRecording()
                if (startResult.isFailure) {
                    _state.value = _state.value.copy(
                        isListening = false,
                        errorMessage = "Failed to start recording: ${startResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                // Record for 5 seconds
                kotlinx.coroutines.delay(5000)

                // Stop recording
                val stopResult = audioRecorder.stopRecording()
                if (stopResult.isFailure) {
                    _state.value = _state.value.copy(
                        isListening = false,
                        errorMessage = "Failed to stop recording: ${stopResult.exceptionOrNull()?.message}"
                    )
                    return@launch
                }

                // Recognize speech using ElevenLabs
                val recognitionResult = audioRecorder.recognizeSpeech()

                if (recognitionResult.isSuccess) {
                    val transcribedText = recognitionResult.getOrNull()!!

                    // Add user message to chat
                    val userMessage = ChatMessage(transcribedText, isFromUser = true)
                    _state.value = _state.value.copy(
                        chatMessages = _state.value.chatMessages + userMessage,
                        isListening = false,
                        isProcessing = true
                    )

                    // Get AI response from Gemini
                    getAIResponse(transcribedText)

                } else {
                    _state.value = _state.value.copy(
                        isListening = false,
                        errorMessage = "Speech recognition failed: ${recognitionResult.exceptionOrNull()?.message}"
                    )
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isListening = false,
                    errorMessage = "Error during speech recognition: ${e.message}"
                )
            }
        }
    }

    fun stopListening() {
        if (!_state.value.isListening) return

        viewModelScope.launch {
            try {
                // Stop recording if active
                if (audioRecorder.isCurrentlyRecording()) {
                    audioRecorder.stopRecording()
                }
                _state.value = _state.value.copy(isListening = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isListening = false,
                    errorMessage = "Error stopping speech recognition: ${e.message}"
                )
            }
        }
    }

    private fun getAIResponse(userMessage: String) {
        viewModelScope.launch {
            try {
                val aiResult = repository.getAIResponse(userMessage, currentExperimentId)

                if (aiResult.isSuccess) {
                    val aiResponse = aiResult.getOrNull()!!

                    // Add AI response to chat
                    val assistantMessage = ChatMessage(aiResponse.responseText, isFromUser = false)
                    _state.value = _state.value.copy(
                        chatMessages = _state.value.chatMessages + assistantMessage,
                        experimentContext = aiResponse.contextUpdate ?: _state.value.experimentContext,
                        isProcessing = false
                    )

                    // Convert response to speech
                    convertTextToSpeech(aiResponse.responseText)

                } else {
                    _state.value = _state.value.copy(
                        errorMessage = "AI response failed: ${aiResult.exceptionOrNull()?.message}",
                        isProcessing = false
                    )
                }

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    errorMessage = "Error getting AI response: ${e.message}",
                    isProcessing = false
                )
            }
        }
    }

    private fun convertTextToSpeech(text: String, isInitialMessage: Boolean = false) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(isSpeaking = true)

                // For initial message, wait a bit longer for TTS to initialize
                if (isInitialMessage) {
                    kotlinx.coroutines.delay(1500) // Give TTS more time to initialize
                }

                // Use ElevenLabs TTS
                val speakResult = audioRecorder.speak(text)

                if (speakResult.isSuccess) {
                    // ElevenLabs API handles the speech timing internally
                    // No need to check isSpeaking() as it doesn't exist in our implementation
                    kotlinx.coroutines.delay((text.length * 80).toLong()) // Estimate speaking time
                } else {
                    // If TTS fails on initial message, try again after a delay
                    if (isInitialMessage) {
                        kotlinx.coroutines.delay(2000)
                        val retryResult = audioRecorder.speak(text)
                        if (retryResult.isSuccess) {
                            kotlinx.coroutines.delay((text.length * 80).toLong())
                        }
                    } else {
                        // Fallback: simulate speaking time based on text length
                        kotlinx.coroutines.delay((text.length * 80).toLong())
                    }
                }

                _state.value = _state.value.copy(isSpeaking = false)

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isSpeaking = false,
                    errorMessage = "Text-to-speech failed: ${e.message}"
                )
            }
        }
    }

    fun sendTextMessage(message: String) {
        viewModelScope.launch {
            // Add user message to chat
            val userMessage = ChatMessage(message, isFromUser = true)
            _state.value = _state.value.copy(
                chatMessages = _state.value.chatMessages + userMessage,
                isProcessing = true
            )

            // Get AI response
            getAIResponse(message)
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun endExperiment() {
        viewModelScope.launch {
            try {
                if (_state.value.isSessionActive) {
                    repository.endSession()
                }

                // Cleanup
                if (audioRecorder.isCurrentlyRecording()) {
                    audioRecorder.stopRecording()
                }
                // Remove deleteRecording() call as it doesn't exist in our new implementation
                audioRecorder.release()

                _state.value = VoiceAssistantState() // Reset to initial state

            } catch (e: Exception) {
                // Log error but don't show to user since they're exiting
            }
        }
    }

    private fun getExperimentName(experimentId: String): String {
        return when (experimentId) {
            "chem001" -> "Basic Chemical Reactions"
            "phys001" -> "Pendulum Motion Analysis"
            "bio001" -> "Cell Structure Observation"
            else -> "Lab Experiment #$experimentId"
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup resources
        if (audioRecorder.isCurrentlyRecording()) {
            viewModelScope.launch {
                audioRecorder.stopRecording()
            }
        }
        audioRecorder.release()
    }
}
