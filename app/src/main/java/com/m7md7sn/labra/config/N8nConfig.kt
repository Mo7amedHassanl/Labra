package com.m7md7sn.labra.config

object N8nConfig {
    // Configure your n8n instance URL here
    // Example: "https://your-n8n-instance.com/" or "http://localhost:5678/"
    const val BASE_URL = "https://your-n8n-instance.com/"

    // Webhook endpoints - update these to match your n8n webhook URLs
    object Endpoints {
        const val VOICE_RECOGNITION = "webhook/voice-recognition"
        const val VOICE_RECOGNITION_FILE = "webhook/voice-recognition-file"
        const val AI_ASSISTANT = "webhook/ai-assistant"
        const val TEXT_TO_SPEECH = "webhook/text-to-speech"
        const val EXPERIMENT_INIT = "webhook/experiment/initialize"
        const val SESSION_START = "webhook/session/start"
        const val SESSION_END = "webhook/session/end"
        const val EXPERIMENT_INSTRUCTIONS = "webhook/experiment/{experimentId}/instructions"
    }

    // Authentication settings (if your n8n instance requires authentication)
    object Auth {
        const val USE_AUTH = false
        const val API_KEY = "" // Set your API key here if needed
        const val AUTH_HEADER = "Authorization"
        const val AUTH_PREFIX = "Bearer " // or "Basic " for basic auth
    }

    // Audio settings
    object Audio {
        const val AUDIO_FORMAT = "audio/wav"
        const val SAMPLE_RATE = 44100
        const val CHANNELS = 1 // Mono
        const val BIT_RATE = 128000
    }

    // Voice settings
    object Voice {
        const val DEFAULT_VOICE_ID = "default"
        const val DEFAULT_SPEED = 1.0f
        const val DEFAULT_PITCH = 1.0f
        const val DEFAULT_LANGUAGE = "en-US"
    }
}
