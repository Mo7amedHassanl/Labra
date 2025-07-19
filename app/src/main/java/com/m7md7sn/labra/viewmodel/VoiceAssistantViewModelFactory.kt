package com.m7md7sn.labra.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class VoiceAssistantViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceAssistantViewModel::class.java)) {
            return VoiceAssistantViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
