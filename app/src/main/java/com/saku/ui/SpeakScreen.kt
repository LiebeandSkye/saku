package com.saku.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.saku.data.PreferencesManager
import com.saku.reading.FishAudioService
import com.saku.speak.ChatMessage
import com.saku.speak.GeminiConversationService
import com.saku.speak.SpeakConversationManager
import com.saku.speak.SpeechRecognizerHelper
import com.saku.util.JapaneseTtsHelper
import kotlinx.coroutines.launch

@Composable
fun SpeakScreen(
    padding: PaddingValues,
    prefs: PreferencesManager,
    isActive: Boolean = true,
    onNavigateToJisho: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Conversational state backed by singleton so messages persist across tab navigation
    val messages = SpeakConversationManager.messages
    var isListening by remember { mutableStateOf(false) }
    var liveTranscript by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var audioRmsDb by remember { mutableFloatStateOf(0f) }

    // Input mode: Voice (mic) vs Typing (keyboard)
    var isKeyboardMode by remember { mutableStateOf(false) }
    var typedText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Text selection translation state
    var showTranslationSheet by remember { mutableStateOf(false) }
    var translateTargetText by remember { mutableStateOf("") }

    // Configuration states matching ReadingScreen
    var showModelDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showFishAudioDialog by remember { mutableStateOf(false) }

    var currentModel by remember { mutableStateOf(prefs.geminiModel) }
    var geminiApiKey by remember { mutableStateOf(prefs.geminiApiKey ?: "") }
    var fishAudioApiKey by remember { mutableStateOf(prefs.fishAudioApiKey ?: "") }
    var fishAudioVoiceId by remember { mutableStateOf(prefs.fishAudioVoiceId) }
    var fishAudioVoiceName by remember { mutableStateOf(prefs.fishAudioVoiceName) }
    var fishAudioModel by remember { mutableStateOf(prefs.fishAudioModel) }

    // Keep state synchronized whenever preferences are updated externally
    LaunchedEffect(
        prefs.geminiModel,
        prefs.geminiApiKey,
        prefs.fishAudioApiKey,
        prefs.fishAudioVoiceId,
        prefs.fishAudioVoiceName,
        prefs.fishAudioModel
    ) {
        currentModel = prefs.geminiModel
        geminiApiKey = prefs.geminiApiKey ?: ""
        fishAudioApiKey = prefs.fishAudioApiKey ?: ""
        fishAudioVoiceId = prefs.fishAudioVoiceId
        fishAudioVoiceName = prefs.fishAudioVoiceName
        fishAudioModel = prefs.fishAudioModel
    }

    // Helpers & services
    val geminiService = remember { GeminiConversationService() }
    val fishAudioService = remember { FishAudioService(context) }
    val systemTtsHelper = remember { JapaneseTtsHelper(context) }

    // Scroll state for conversation list
    val listState = rememberLazyListState()

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size, liveTranscript, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Forward reference for speech handler
    var handleFinalSpeechRef by remember { mutableStateOf<((String) -> Unit)?>(null) }

    // SpeechRecognizer helper
    val speechHelper = remember {
        SpeechRecognizerHelper(
            context = context.applicationContext,
            onPartialResult = { partial ->
                liveTranscript = partial
            },
            onFinalResult = { final ->
                handleFinalSpeechRef?.invoke(final)
            },
            onRmsChanged = { rms ->
                audioRmsDb = rms
            },
            onStateChange = { listening ->
                isListening = listening
                if (!listening) {
                    audioRmsDb = 0f
                }
            },
            onError = { errorMsg ->
                isListening = false
                audioRmsDb = 0f
                liveTranscript = ""
                if (errorMsg.isNotBlank() && 
                    !errorMsg.contains("No speech detected", ignoreCase = true) && 
                    !errorMsg.contains("timeout", ignoreCase = true)
                ) {
                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Play voice audio for text (Fish Audio with system TTS fallback)
    val playAiVoice: (String) -> Unit = { text ->
        // Cancel any active speech recognition and live transcript before audio playback
        // to prevent acoustic feedback loops where the speaker output is transcribed
        speechHelper.cancel()
        liveTranscript = ""
        isListening = false

        coroutineScope.launch {
            val fishKey = fishAudioApiKey
            val voiceId = fishAudioVoiceId
            val model = fishAudioModel

            // Stop any ongoing playback before starting new voice
            fishAudioService.stopAudio()
            systemTtsHelper.stop()

            if (!fishKey.isNullOrBlank() && voiceId.isNotBlank()) {
                isSpeaking = true
                val result = fishAudioService.synthesizeSpeechAudio(
                    apiKey = fishKey,
                    voiceId = voiceId,
                    model = model,
                    text = text
                )
                result.fold(
                    onSuccess = { audioFile ->
                        fishAudioService.playAudio(
                            file = audioFile,
                            onPlaybackStateChanged = { playing ->
                                isSpeaking = playing
                            },
                            onCompletion = {
                                isSpeaking = false
                            }
                        )
                    },
                    onFailure = {
                        // Fallback to system TTS with proper state callbacks
                        systemTtsHelper.speak(
                            text = text,
                            onStart = { isSpeaking = true },
                            onDone = { isSpeaking = false }
                        )
                    }
                )
            } else {
                // Use system Japanese TTS directly with proper state callbacks
                systemTtsHelper.speak(
                    text = text,
                    onStart = { isSpeaking = true },
                    onDone = { isSpeaking = false }
                )
            }
        }
    }

    // Process user input and query Gemini Flash-Lite
    val handleFinalSpeech: (String) -> Unit = { spokenText ->
        val trimmed = spokenText.trim()
        if (trimmed.isNotBlank() && !isThinking) {
            val userMsg = ChatMessage(text = trimmed, isUser = true)
            messages.add(userMsg)
            liveTranscript = ""
            isThinking = true

            coroutineScope.launch {
                val apiKey = geminiApiKey
                val model = currentModel

                val result = geminiService.sendConversationTurn(
                    apiKey = apiKey,
                    messages = messages.toList(),
                    preferredModel = model
                )

                isThinking = false
                result.fold(
                    onSuccess = { reply ->
                        val aiMsg = ChatMessage(text = reply, isUser = false)
                        messages.add(aiMsg)
                        playAiVoice(reply)
                    },
                    onFailure = { error ->
                        val errorText = error.localizedMessage ?: "Failed to connect to Gemini"
                        val errorMsg = ChatMessage(
                            text = "エラー: $errorText",
                            isUser = false
                        )
                        messages.add(errorMsg)
                    }
                )
            }
        }
    }
    handleFinalSpeechRef = handleFinalSpeech

    // Pause/cancel recording and audio playback whenever the user navigates away from Speak tab
    LaunchedEffect(isActive) {
        if (!isActive) {
            speechHelper.cancel()
            liveTranscript = ""
            isListening = false
            fishAudioService.stopAudio()
            systemTtsHelper.stop()
            isSpeaking = false
            keyboardController?.hide()
        }
    }

    // Lifecycle & cleanup handling: stop microphone and audio playback on background/pause
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                speechHelper.cancel()
                fishAudioService.stopAudio()
                systemTtsHelper.stop()
                isSpeaking = false
                isListening = false
                liveTranscript = ""
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speechHelper.destroy()
            fishAudioService.stopAudio()
            systemTtsHelper.shutdown()
        }
    }

    // Runtime microphone permission launcher
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            if (!speechHelper.isAvailable()) {
                Toast.makeText(context, "Voice recognition service is not available on this device. Switched to typing mode.", Toast.LENGTH_LONG).show()
                isKeyboardMode = true
            } else {
                speechHelper.startListening()
            }
        } else {
            Toast.makeText(context, "Microphone permission is required to speak", Toast.LENGTH_SHORT).show()
        }
    }

    // Button action callers
    val startSpeakingAction: () -> Unit = {
        // Stop any ongoing playback before listening to prevent acoustic feedback loop
        fishAudioService.stopAudio()
        systemTtsHelper.stop()
        isSpeaking = false

        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else if (!speechHelper.isAvailable()) {
            Toast.makeText(context, "Voice recognition is not available on this device. Switched to typing mode.", Toast.LENGTH_LONG).show()
            isKeyboardMode = true
            coroutineScope.launch {
                kotlinx.coroutines.delay(120)
                try {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                } catch (_: Exception) {}
            }
        } else {
            liveTranscript = ""
            speechHelper.startListening()
        }
    }

    val stopSpeakingAction = {
        speechHelper.stopListening()
    }

    val toggleInputMode: () -> Unit = {
        isKeyboardMode = !isKeyboardMode
        if (isKeyboardMode) {
            speechHelper.cancel()
            isListening = false
            liveTranscript = ""
            coroutineScope.launch {
                kotlinx.coroutines.delay(120)
                if (isKeyboardMode) {
                    try {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    } catch (_: Exception) {}
                }
            }
        } else {
            keyboardController?.hide()
            typedText = ""
        }
    }

    val submitTypedText: () -> Unit = {
        if (typedText.isNotBlank()) {
            handleFinalSpeech(typedText)
            typedText = ""
            keyboardController?.hide()
        } else {
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header bar: minimal title and clear button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "話す • SPEAK",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SakuColors.TextSecondary,
                    letterSpacing = 1.2.sp
                )

                if (messages.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            SpeakConversationManager.clear()
                            liveTranscript = ""
                            fishAudioService.stopAudio()
                            systemTtsHelper.stop()
                            isSpeaking = false
                            FishAudioService.clearSpeakAudio(context)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear conversation",
                            tint = SakuColors.TextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Reading-style minimal configuration pills for Gemini Model and Fish Audio Voice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isLight = SakuColors.currentTheme == AppTheme.LIGHT

                // 1. Model / Key Pill
                Surface(
                    onClick = {
                        if (geminiApiKey.isBlank()) {
                            showApiKeyDialog = true
                        } else {
                            showModelDialog = true
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLight) SakuColors.Surface.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (isLight) SakuColors.BorderHighlight else Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = SakuColors.SagePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (geminiApiKey.isBlank()) "Setup Key" else PreferencesManager.getModelDisplayName(currentModel),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (geminiApiKey.isBlank()) SakuColors.AccentRose else SakuColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = SakuColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 2. Fish Audio Voice Pill
                Surface(
                    onClick = { showFishAudioDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLight) SakuColors.Surface.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, if (isLight) SakuColors.BorderHighlight else Color.White.copy(alpha = 0.20f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = if (fishAudioApiKey.isNotBlank()) SakuColors.VibrantMatcha else SakuColors.TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            val voiceLabel = fishAudioVoiceName?.takeIf { it.isNotBlank() }
                                ?: if (fishAudioVoiceId.isNotBlank()) "Voice: ${fishAudioVoiceId.take(8)}..." else "Voice Setup"
                            Text(
                                text = if (fishAudioApiKey.isNotBlank()) voiceLabel else "Voice Setup",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SakuColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = null,
                            tint = SakuColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Top Section: Pure conversation text (No bubbles)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty() && liveTranscript.isBlank() && !isThinking) {
                    // Minimalist empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "話してみましょう",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light,
                            color = SakuColors.TextPrimary.copy(alpha = 0.75f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Practice conversational Japanese naturally.\nTap once or hold the mic button below to talk.",
                            fontSize = 13.sp,
                            color = SakuColors.TextSecondary.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    CustomSelectionContainer(
                        onTranslate = { selectedText ->
                            translateTargetText = selectedText
                            showTranslationSheet = true
                        }
                    ) {
                        SelectionContainer {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    ConversationTextItem(
                                        message = msg,
                                        onReplayVoice = {
                                            playAiVoice(msg.text)
                                        }
                                    )
                                }

                                if (isThinking) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.Start,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(14.dp),
                                                strokeWidth = 2.dp,
                                                color = SakuColors.SagePrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "考え中...",
                                                fontSize = 13.sp,
                                                color = SakuColors.TextSecondary.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Section: Live transcription & Big Circle Mic Button
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(bottom = 84.dp), // Leaves room above the floating dock
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Input / Transcription display directly above the button (NO text bubble)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isKeyboardMode) {
                        BasicTextField(
                            value = typedText,
                            onValueChange = { typedText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = SakuColors.TextPrimary,
                                textAlign = TextAlign.Center
                            ),
                            cursorBrush = SolidColor(SakuColors.SagePrimary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Send
                            ),
                            keyboardActions = KeyboardActions(
                                onSend = { submitTypedText() }
                            ),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (typedText.isEmpty()) {
                                        Text(
                                            text = "メッセージを入力 (Type Japanese)...",
                                            fontSize = 15.sp,
                                            color = SakuColors.TextSecondary.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    } else {
                        val displayText = when {
                            liveTranscript.isNotBlank() -> liveTranscript
                            isListening -> "聞いています..."
                            isSpeaking -> "Saku is speaking..."
                            isThinking -> "考え中..."
                            else -> "タップまたは長押しで話す"
                        }

                        val textColor = when {
                            liveTranscript.isNotBlank() -> SakuColors.TextPrimary
                            isListening -> SakuColors.SagePrimary
                            isSpeaking -> SakuColors.VibrantMatcha
                            else -> SakuColors.TextSecondary.copy(alpha = 0.5f)
                        }

                        Text(
                            text = displayText,
                            fontSize = if (liveTranscript.isNotBlank()) 16.sp else 13.sp,
                            fontWeight = if (liveTranscript.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom row: Small switch icon on the left + Centered Big Circle Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Small switch icon on the left (just icon, not circle, no background)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                toggleInputMode()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SwapHoriz,
                            contentDescription = if (isKeyboardMode) "Switch to voice" else "Switch to typing",
                            tint = if (isKeyboardMode) SakuColors.SagePrimary else SakuColors.TextSecondary.copy(alpha = 0.65f),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Big Circle Button in Saku Sage color with press and pulse animations
                    BigCircleMicButton(
                        isKeyboardMode = isKeyboardMode,
                        isListening = isListening,
                        isThinking = isThinking,
                        isSpeaking = isSpeaking,
                        rmsDb = audioRmsDb,
                        onStartListening = startSpeakingAction,
                        onStopListening = stopSpeakingAction,
                        onKeyboardSubmit = submitTypedText
                    )

                    // Right balance spacer so the big button stays perfectly centered (44.dp + 16.dp = 60.dp)
                    Spacer(modifier = Modifier.width(60.dp))
                }
            }
        }

        // Gemini API Key Dialog
        if (showApiKeyDialog) {
            ApiKeySetupDialog(
                currentKey = geminiApiKey,
                onSave = { newKey ->
                    geminiApiKey = newKey
                    prefs.geminiApiKey = newKey
                    showApiKeyDialog = false
                    Toast.makeText(context, "Gemini API key saved!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showApiKeyDialog = false }
            )
        }

        // Gemini Model Selection Dialog
        if (showModelDialog) {
            GeminiModelDialog(
                currentModel = currentModel,
                onSave = { newModel ->
                    currentModel = newModel
                    prefs.geminiModel = newModel
                    showModelDialog = false
                    Toast.makeText(context, "Gemini model set to $newModel", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showModelDialog = false }
            )
        }

        // Fish Audio Voice & API Key Dialog
        if (showFishAudioDialog) {
            FishAudioDialog(
                currentApiKey = fishAudioApiKey,
                currentVoiceId = fishAudioVoiceId,
                currentVoiceName = fishAudioVoiceName,
                currentModel = fishAudioModel,
                onSave = { newKey, newVoiceId, newVoiceName, newModel ->
                    fishAudioApiKey = newKey
                    prefs.fishAudioApiKey = newKey
                    fishAudioVoiceId = newVoiceId
                    prefs.fishAudioVoiceId = newVoiceId
                    fishAudioVoiceName = newVoiceName
                    prefs.fishAudioVoiceName = newVoiceName
                    fishAudioModel = newModel
                    prefs.fishAudioModel = newModel
                    showFishAudioDialog = false
                    Toast.makeText(context, "Fish Audio configuration saved!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showFishAudioDialog = false }
            )
        }

        // Modal Bottom Sheet: Selection Translation
        if (showTranslationSheet && translateTargetText.isNotBlank()) {
            TranslationBottomSheet(
                sourceText = translateTargetText,
                onDismiss = {
                    showTranslationSheet = false
                    translateTargetText = ""
                },
                onNavigateToJisho = onNavigateToJisho
            )
        }
    }
}

/**
 * Minimalist conversation text item without bubble containers.
 * User text aligned to the right. AI text aligned to the left.
 */
@Composable
private fun ConversationTextItem(
    message: ChatMessage,
    onReplayVoice: () -> Unit
) {
    if (message.isUser) {
        // User text: aligned to the right, clean text
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = message.text,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
                color = SakuColors.TextPrimary,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 290.dp)
            )
        }
    } else {
        // AI text: aligned to the left, clean text with audio replay capability
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.widthIn(max = 300.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = SakuColors.TextPrimary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onReplayVoice,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Replay audio",
                        tint = SakuColors.TextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Large circular microphone button using Saku Sage theme color.
 * Supports dual interaction:
 * 1) Tap to start listening until tapped again.
 * 2) Press & hold to speak, releases to stop and send immediately.
 */
@Composable
private fun BigCircleMicButton(
    isKeyboardMode: Boolean,
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    rmsDb: Float,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onKeyboardSubmit: () -> Unit
) {
    var isPressedState by remember { mutableStateOf(false) }

    val currentIsKeyboardMode by rememberUpdatedState(isKeyboardMode)
    val currentOnKeyboardSubmit by rememberUpdatedState(onKeyboardSubmit)
    val currentIsListening by rememberUpdatedState(isListening)
    val currentIsThinking by rememberUpdatedState(isThinking)
    val currentOnStart by rememberUpdatedState(onStartListening)
    val currentOnStop by rememberUpdatedState(onStopListening)

    // Spring scale down on physical press
    val scaleAnim by animateFloatAsState(
        targetValue = when {
            isThinking -> 0.96f
            isPressedState -> 0.92f
            isListening -> 1.05f
            else -> 1.0f
        },
        animationSpec = spring(stiffness = 400f),
        label = "micScale"
    )

    // Breathing pulse ring when actively listening or speaking
    val infiniteTransition = rememberInfiniteTransition(label = "pulseRing")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Subtle sound reactivity from RMS dB
    val dynamicWaveExtra = if (isListening && rmsDb > 1f) (rmsDb / 15f).coerceIn(0f, 0.25f) else 0f

    Box(
        modifier = Modifier.size(110.dp),
        contentAlignment = Alignment.Center
    ) {
        // Subtle animated outer aura when recording or AI speaking
        if (isListening || isSpeaking) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .scale(pulseScale + dynamicWaveExtra)
                    .clip(CircleShape)
                    .background(SakuColors.SagePrimary.copy(alpha = pulseAlpha))
            )
        }

        // The core Sage circle button
        val buttonColor = when {
            isThinking -> SakuColors.SagePrimary.copy(alpha = 0.55f)
            isPressedState -> SakuColors.SagePrimary.copy(alpha = 0.82f)
            isListening -> SakuColors.SageLight
            else -> SakuColors.SagePrimary
        }

        Box(
            modifier = Modifier
                .size(76.dp)
                .scale(scaleAnim)
                .clip(CircleShape)
                .background(buttonColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            if (currentIsThinking) return@detectTapGestures
                            if (currentIsKeyboardMode) {
                                isPressedState = true
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    isPressedState = false
                                }
                                currentOnKeyboardSubmit()
                            } else {
                                val startTime = System.currentTimeMillis()
                                val wasListeningBefore = currentIsListening
                                isPressedState = true

                                if (!wasListeningBefore) {
                                    currentOnStart()
                                }

                                try {
                                    tryAwaitRelease()
                                } finally {
                                    isPressedState = false
                                }

                                val duration = System.currentTimeMillis() - startTime
                                if (duration >= 350L) {
                                    // Long press / Hold gesture: releasing stops listening and sends!
                                    currentOnStop()
                                } else if (wasListeningBefore) {
                                    // Second tap stops and sends!
                                    currentOnStop()
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = isKeyboardMode,
                transitionSpec = {
                    if (targetState) {
                        // Switching from Mic -> Keyboard:
                        // Mic icon slides down (+height) and Typing icon floats in from top (-height)
                        (slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetY = { -it }
                        ) + fadeIn(tween(220))).togetherWith(
                            slideOutVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                targetOffsetY = { it }
                            ) + fadeOut(tween(180))
                        )
                    } else {
                        // Switching from Keyboard -> Mic:
                        // Typing icon slides up (-height) and Mic icon flows back up from bottom (+height)
                        (slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetY = { it }
                        ) + fadeIn(tween(220))).togetherWith(
                            slideOutVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                targetOffsetY = { -it }
                            ) + fadeOut(tween(180))
                        )
                    }
                },
                label = "ButtonIconTransition"
            ) { keyboardMode ->
                if (keyboardMode) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = "Type sentence",
                        tint = SakuColors.OnSage,
                        modifier = Modifier.size(36.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = "Speak microphone",
                        tint = SakuColors.OnSage,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}
