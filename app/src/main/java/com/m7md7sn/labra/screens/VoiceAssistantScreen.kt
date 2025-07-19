package com.m7md7sn.labra.screens

import android.Manifest
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.m7md7sn.labra.viewmodel.VoiceAssistantViewModel
import com.m7md7sn.labra.viewmodel.VoiceAssistantViewModelFactory

data class ChatMessage(
    val message: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    experimentId: String,
    onExitExperiment: () -> Unit
) {
    val context = LocalContext.current
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Create ViewModel with context
    val viewModel: VoiceAssistantViewModel = viewModel(
        factory = VoiceAssistantViewModelFactory(context)
    )

    // Collect state from ViewModel
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Initialize experiment when screen loads
    LaunchedEffect(experimentId) {
        viewModel.initializeExperiment(experimentId)
    }

    // Handle exit experiment
    LaunchedEffect(Unit) {
        // Request audio permission if not granted
        if (!audioPermissionState.status.isGranted) {
            audioPermissionState.launchPermissionRequest()
        }
    }

    // Show error messages
    state.errorMessage?.let { error ->
        LaunchedEffect(error) {
            // You could show a snackbar here
            // For now, we'll auto-clear after 5 seconds
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview background
        CameraPreviewBackground()

        // Top bar with experiment info
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = state.currentExperiment.ifEmpty { "Loading..." },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = when {
                            state.isProcessing -> "Processing..."
                            state.isListening -> "Listening..."
                            state.isSpeaking -> "AI Speaking..."
                            state.isSessionActive -> "AI Assistant Active"
                            else -> "Initializing..."
                        },
                        fontSize = 12.sp,
                        color = when {
                            state.errorMessage != null -> Color.Red
                            state.isSessionActive -> Color.Green
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        viewModel.endExperiment()
                        onExitExperiment()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Exit Experiment",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = { /* Toggle chat visibility */ }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = "Chat",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.7f),
                titleContentColor = Color.White
            )
        )

        // Bottom section with chat and controls stacked vertically
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Chat overlay - conversation box
            ChatOverlay(
                messages = state.chatMessages,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp) // Fixed height for consistency
                    .padding(bottom = 16.dp) // Space between chat and controls
            )

            // Voice interaction controls - record button area
            VoiceControlPanel(
                isListening = state.isListening,
                isSpeaking = state.isSpeaking,
                isProcessing = state.isProcessing,
                audioPermissionGranted = audioPermissionState.status.isGranted,
                errorMessage = state.errorMessage,
                onStartListening = {
                    if (audioPermissionState.status.isGranted) {
                        viewModel.startListening()
                    } else {
                        audioPermissionState.launchPermissionRequest()
                    }
                },
                onStopListening = {
                    viewModel.stopListening()
                },
                onRequestPermission = {
                    audioPermissionState.launchPermissionRequest()
                },
                onClearError = {
                    viewModel.clearError()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // AI Status indicator
        AIStatusIndicator(
            isListening = state.isListening,
            isSpeaking = state.isSpeaking,
            isProcessing = state.isProcessing,
            isConnected = state.isSessionActive,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun CameraPreviewBackground() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (_: Exception) {
                    // Handle camera binding error
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ChatOverlay(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Conversation",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(message = message)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser)
                    MaterialTheme.colorScheme.primary
                else
                    Color.Gray.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.widthIn(max = 250.dp)
        ) {
            Text(
                text = message.message,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun VoiceControlPanel(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    audioPermissionGranted: Boolean,
    errorMessage: String?,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onRequestPermission: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // Error message display
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Red.copy(alpha = 0.9f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = onClearError,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Error",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (!audioPermissionGranted) {
                Text(
                    text = "Microphone permission required for voice interaction",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onRequestPermission) {
                    Text("Grant Permission")
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = when {
                                isProcessing -> "Processing your request..."
                                isListening -> "Listening... Tap to stop"
                                isSpeaking -> "AI is speaking..."
                                else -> "Tap and hold to ask a question"
                            },
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        VoiceButton(
                            isListening = isListening,
                            isSpeaking = isSpeaking,
                            isProcessing = isProcessing,
                            onStartListening = onStartListening,
                            onStopListening = onStopListening
                        )
                    }

                }

            }
        }
    }
}

@Composable
private fun VoiceButton(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isListening) 1.2f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice_button_scale"
    )

    val enabled = !isProcessing && !isSpeaking

    FloatingActionButton(
        onClick = {
            if (isListening) {
                onStopListening()
            } else {
                onStartListening()
            }
        },
        modifier = Modifier
            .size(80.dp)
            .scale(scale),
        containerColor = when {
            !enabled -> Color.Gray
            isListening -> Color.Red
            isSpeaking -> MaterialTheme.colorScheme.secondary
            isProcessing -> Color.Magenta
            else -> MaterialTheme.colorScheme.primary
        }
    ) {
        when {
            isProcessing -> {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
            else -> {
                Icon(
                    imageVector = when {
                        isListening -> Icons.Default.Stop
                        isSpeaking -> Icons.AutoMirrored.Filled.VolumeUp
                        else -> Icons.Default.Mic
                    },
                    contentDescription = "Voice Control",
                    modifier = Modifier.size(36.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
private fun AIStatusIndicator(
    isListening: Boolean,
    isSpeaking: Boolean,
    isProcessing: Boolean,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ai_status_transition")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_status_alpha"
    )

    val displayAlpha = if (isListening || isSpeaking || isProcessing) alpha else 0.6f

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            !isConnected -> Color.Gray
                            isProcessing -> Color.Magenta.copy(alpha = displayAlpha)
                            isListening -> Color.Red.copy(alpha = displayAlpha)
                            isSpeaking -> Color.Blue.copy(alpha = displayAlpha)
                            else -> Color.Green
                        }
                    )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "AI",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}
