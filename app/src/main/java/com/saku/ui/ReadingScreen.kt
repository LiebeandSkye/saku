package com.saku.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saku.anki.ReadingVocabularyExtractor
import com.saku.data.AnkiVocabularyItem
import com.saku.data.GeneratedStory
import com.saku.data.PreferencesManager
import com.saku.data.ReadingHistoryManager
import com.saku.data.ReadingVocabularySummary
import com.saku.reading.FishAudioService
import com.saku.reading.GeminiStoryService
import com.saku.reading.GenerationStatus
import com.saku.reading.NekosService
import com.saku.reading.StoryGenerationManager
import com.saku.data.StoryThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReadingScreen(
    padding: PaddingValues,
    prefs: PreferencesManager,
    hasAnkiPermission: Boolean,
    readingTheme: ReadingTheme = ReadingTheme.fromId(prefs.readingScreenTheme),
    openHistoryTrigger: Int = 0,
    onHistoryTriggerConsumed: () -> Unit = {},
    onNavigateToJisho: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val historyManager = remember { ReadingHistoryManager(context) }
    val storyService = remember { GeminiStoryService() }
    val vocabExtractor = remember { ReadingVocabularyExtractor(context) }
    val audioService = remember { FishAudioService(context) }

    // State
    var apiKey by remember { mutableStateOf(prefs.geminiApiKey ?: "") }
    var selectedJlpt by remember { mutableStateOf(prefs.readingJlptLevel) }
    var selectedModel by remember { mutableStateOf(prefs.geminiModel) }
    var highlightWords by remember { mutableStateOf(prefs.highlightVocabularyWords) }
    var connectStudiedWords by remember { mutableStateOf(prefs.connectStudiedWords) }
    var showJlptMenu by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }

    // Fish Audio State
    var showFishAudioDialog by remember { mutableStateOf(false) }
    var fishAudioApiKey by remember { mutableStateOf(prefs.fishAudioApiKey ?: "") }
    var fishAudioVoiceId by remember { mutableStateOf(prefs.fishAudioVoiceId) }
    var fishAudioVoiceName by remember { mutableStateOf(prefs.fishAudioVoiceName) }
    var fishAudioModel by remember { mutableStateOf(prefs.fishAudioModel) }
    var isNarrating by remember { mutableStateOf(false) }
    var isSynthesizingAudio by remember { mutableStateOf(false) }
    var currentlyPlayingStoryId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            audioService.stopAudio()
        }
    }

    val selectedDeckIdsLongs = remember(prefs.selectedDeckIds) { prefs.getSelectedDeckIdsAsLongs() }
    var vocabSummary by remember { mutableStateOf<ReadingVocabularySummary?>(ReadingVocabularyExtractor.getCachedSummary(selectedDeckIdsLongs)) }
    var isLoadingVocab by remember { mutableStateOf(false) }
    var isVocabExpanded by remember { mutableStateOf(false) }
    var vocabFilterMode by remember { mutableStateOf("all") } // "all", "studied", "suspended"

    var currentStory by remember { mutableStateOf<GeneratedStory?>(null) }
    val isGeneratingStory = StoryGenerationManager.isGenerating
    var generationError by remember { mutableStateOf<String?>(null) }
    var showInternetConsentDialog by remember { mutableStateOf(false) }
    var savedStories by remember { mutableStateOf(historyManager.getStories()) }

    LaunchedEffect(StoryGenerationManager.latestStory) {
        StoryGenerationManager.latestStory?.let { newStory ->
            currentStory = newStory
            savedStories = historyManager.getStories()
        }
    }

    val generationStatus = StoryGenerationManager.status
    LaunchedEffect(generationStatus) {
        if (generationStatus is GenerationStatus.Error) {
            generationError = generationStatus.message
        }
    }

    val userAnswers = remember { mutableStateMapOf<Int, Int>() }

    LaunchedEffect(currentStory?.id) {
        userAnswers.clear()
        if (currentlyPlayingStoryId != null && currentlyPlayingStoryId != currentStory?.id) {
            audioService.stopAudio()
            isNarrating = false
            currentlyPlayingStoryId = null
        }
        currentStory?.id?.let { id ->
            prefs.lastReadStoryId = id
        }
    }

    // History Sheet
    val historySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showHistorySheet by remember { mutableStateOf(false) }
    val expandedHistoryCards = remember { mutableStateMapOf<String, Boolean>() }

    // Word Detail Sheet
    val wordDetailSheetState = rememberModalBottomSheetState()
    var selectedWordDetail by remember { mutableStateOf<AnkiVocabularyItem?>(null) }

    // Translation Sheet
    var showTranslationSheet by remember { mutableStateOf(false) }
    var translateTargetText by remember { mutableStateOf("") }

    // External trigger from top-bar paper icon
    LaunchedEffect(openHistoryTrigger) {
        if (openHistoryTrigger > 0) {
            savedStories = historyManager.getStories()
            showHistorySheet = true
            onHistoryTriggerConsumed()
        }
    }

    // Auto-fetch scenery background image for any stories missing an imageUrl or with old character images
    LaunchedEffect(showHistorySheet) {
        if (showHistorySheet) {
            val unassigned = savedStories.filter {
                it.imageUrl.isNullOrBlank() || it.imageUrl.contains("nekosapi.com")
            }
            if (unassigned.isNotEmpty()) {
                withContext(Dispatchers.IO) {
                    for (story in unassigned) {
                        val url = NekosService.fetchImageUrl(story.title, story.theme, story.topic)
                        if (!url.isNullOrBlank()) {
                            historyManager.updateStoryImageUrl(story.id, url)
                        }
                    }
                }
                savedStories = historyManager.getStories()
            }
        }
    }

    // Load initial story from history if available - restore where left off
    LaunchedEffect(Unit) {
        val past = historyManager.getStories()
        savedStories = past
        if (currentStory == null) {
            val latest = StoryGenerationManager.latestStory
            if (latest != null) {
                currentStory = latest
            } else if (past.isNotEmpty()) {
                val lastId = prefs.lastReadStoryId
                val found = if (lastId != null) past.find { it.id == lastId } else null
                currentStory = found ?: past.first()
            }
        }
    }

    // Load vocabulary stats when screen opens, permission is granted, or selected decks change
    fun loadVocabulary(forceRefresh: Boolean = false) {
        if (!hasAnkiPermission) return
        val deckIds = prefs.getSelectedDeckIdsAsLongs()
        val cached = if (!forceRefresh) ReadingVocabularyExtractor.getCachedSummary(deckIds) else null
        if (cached != null) {
            vocabSummary = cached
            isLoadingVocab = false
            return
        }
        coroutineScope.launch {
            isLoadingVocab = true
            vocabSummary = vocabExtractor.extractVocabulary(deckIds, forceRefresh = forceRefresh)
            isLoadingVocab = false
        }
    }

    LaunchedEffect(hasAnkiPermission, prefs.selectedDeckIds) {
        loadVocabulary()
    }

    val jlptLevels = listOf("N5", "N4", "N3", "N2", "N1")
    val hapticFeedback = LocalHapticFeedback.current
    var showThemeConfigDialog by remember { mutableStateOf(false) }
    var isCustomThemeModeActive by remember { mutableStateOf(prefs.isCustomThemeModeActive) }
    var customStoryTheme by remember { mutableStateOf(prefs.customStoryTheme) }
    var customStoryTopic by remember { mutableStateOf(prefs.customStoryTopic) }
    var holdProgress by remember { mutableFloatStateOf(0f) }

    fun executeGeneration() {
        generationError = null

        val (targetTheme, targetTopic) = if (isCustomThemeModeActive && !customStoryTheme.isNullOrBlank()) {
            val chosenTheme = customStoryTheme!!
            val chosenTopic = customStoryTopic ?: run {
                val topics = StoryThemes.CATEGORIES[chosenTheme] ?: emptyList()
                val eligible = topics.filter { it !in prefs.disabledStoryTopics }.ifEmpty { topics }
                if (eligible.isNotEmpty()) eligible.random() else "A memorable event"
            }
            Pair(chosenTheme, chosenTopic)
        } else {
            StoryThemes.getRandomThemeAndTopic(
                disabledThemes = prefs.disabledStoryThemes,
                disabledTopics = prefs.disabledStoryTopics
            )
        }

        val words = if (connectStudiedWords) (vocabSummary?.words ?: emptyList()) else emptyList()
        StoryGenerationManager.startGeneration(
            context = context,
            apiKey = apiKey,
            jlptLevel = selectedJlpt,
            vocabularyList = words,
            preferredModel = selectedModel,
            theme = targetTheme,
            topic = targetTopic,
            onSuccess = { story ->
                currentStory = story
                savedStories = historyManager.getStories()
            },
            onError = { err ->
                generationError = err
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Liquid Glass Container: Frosted see-through white glass showcasing backdrop artwork
        LiquidGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            cornerRadius = 26.dp,
            tintColor = Color.White.copy(alpha = 0.12f),
            darkBaseAlpha = 0f,
            backgroundColor = Color.White.copy(alpha = 0.10f),
            specularAlpha = 0.40f,
            borderAlpha = 0.30f,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val isLight = SakuColors.currentTheme == AppTheme.LIGHT
                    // JLPT Level Selector Dropdown Pill (Light mode optimized chip)
                    Box {
                        Surface(
                            onClick = { showJlptMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isLight) SakuColors.Surface.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, if (isLight) SakuColors.BorderHighlight else Color.White.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.School,
                                    contentDescription = null,
                                    tint = SakuColors.SagePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Level: $selectedJlpt",
                                    fontWeight = FontWeight.SemiBold,
                                    color = SakuColors.TextPrimary,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = SakuColors.TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showJlptMenu,
                            onDismissRequest = { showJlptMenu = false },
                            modifier = Modifier.background(SakuColors.SurfaceElevated)
                        ) {
                            jlptLevels.forEach { level ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "$level ${getJlptLabel(level)}",
                                            fontWeight = if (level == selectedJlpt) FontWeight.Bold else FontWeight.Normal,
                                            color = if (level == selectedJlpt) SakuColors.SagePrimary else SakuColors.TextPrimary
                                        )
                                    },
                                    onClick = {
                                        selectedJlpt = level
                                        prefs.readingJlptLevel = level
                                        showJlptMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Secondary History Icon & Key Status Pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Clock History Button
                        IconButton(
                            onClick = {
                                savedStories = historyManager.getStories()
                                showHistorySheet = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = "Reading History",
                                tint = if (savedStories.isNotEmpty()) SakuColors.SagePrimary else SakuColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Key Status / Setup Pill
                        Surface(
                            onClick = { showApiKeyDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (apiKey.isNotBlank()) SakuColors.SageContainer else SakuColors.AccentRoseContainer,
                            border = BorderStroke(
                                1.dp,
                                if (apiKey.isNotBlank()) SakuColors.SagePrimary.copy(alpha = 0.45f)
                                else SakuColors.AccentRose.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Key,
                                    contentDescription = null,
                                    tint = if (apiKey.isNotBlank()) SakuColors.SagePrimary else SakuColors.AccentRose,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (apiKey.isNotBlank()) "Key Active" else "Setup Key",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (apiKey.isNotBlank()) SakuColors.SagePrimary else SakuColors.AccentRose,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Row 2: Active Model Pill (Light mode optimized chip)
                val isLight = SakuColors.currentTheme == AppTheme.LIGHT
                Surface(
                    onClick = { showModelDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLight) SakuColors.Surface.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, if (isLight) SakuColors.BorderHighlight else Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
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
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Model: ${PreferencesManager.getModelDisplayName(selectedModel)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = SakuColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Switch",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SakuColors.TextSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = SakuColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Row 2b: Fish Audio Voice Narration Pill
                Surface(
                    onClick = { showFishAudioDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLight) SakuColors.Surface.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, if (isLight) SakuColors.BorderHighlight else Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
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
                                tint = SakuColors.VibrantMatcha,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val voiceLabel = fishAudioVoiceName?.takeIf { it.isNotBlank() }
                                ?: if (fishAudioVoiceId.isNotBlank()) "Voice: ${fishAudioVoiceId.take(12)}..." else "Fish Audio Voice"
                            Text(
                                text = "Fish Audio: $voiceLabel",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = SakuColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (fishAudioApiKey.isNotBlank()) "Key Active" else "Setup Key",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (fishAudioApiKey.isNotBlank()) SakuColors.SagePrimary else SakuColors.AccentRose,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = SakuColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Row 3: Highlight Words Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, start = 4.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Show highlight words?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SakuColors.TextSecondary
                    )
                    Switch(
                        checked = highlightWords,
                        onCheckedChange = {
                            highlightWords = it
                            prefs.highlightVocabularyWords = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SakuColors.SagePrimary,
                            uncheckedThumbColor = SakuColors.TextSecondary,
                            uncheckedTrackColor = Color(0xFF262A34)
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }

                // Row 4: Connect Studied Words Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, start = 4.dp, end = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Connect studied words?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = SakuColors.TextSecondary
                    )
                    Switch(
                        checked = connectStudiedWords,
                        onCheckedChange = {
                            connectStudiedWords = it
                            prefs.connectStudiedWords = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SakuColors.SagePrimary,
                            uncheckedThumbColor = SakuColors.TextSecondary,
                            uncheckedTrackColor = Color(0xFF262A34)
                        ),
                        modifier = Modifier.scale(0.85f)
                    )
                }
            }
        }

        // 2. Gemini API Key Onboarding Banner (if missing)
        if (apiKey.isBlank()) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SakuColors.SurfaceElevated),
                border = BorderStroke(1.dp, SakuColors.AccentLavender.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = SakuColors.AccentLavender)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GEMINI API KEY REQUIRED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SakuColors.AccentLavender,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To generate personalized Japanese reading stories based on your Anki flashcards, connect your free Google Gemini API key.",
                        fontSize = 13.sp,
                        color = SakuColors.TextSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showApiKeyDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SakuColors.SagePrimary,
                                contentColor = SakuColors.OnSage
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Enter API Key", fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = {
                                val url = "https://aistudio.google.com/app/apikey"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SakuColors.Border),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp), tint = SakuColors.TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Get Free Key", color = SakuColors.TextPrimary, fontSize = 12.5.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        // 3. Flashcard Vocabulary Source Card (Clean Bento Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
            border = BorderStroke(1.dp, SakuColors.Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isVocabExpanded = !isVocabExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "FLASHCARD VOCABULARY SOURCE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SakuColors.TextTertiary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (isLoadingVocab) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = SakuColors.SagePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing AnkiDroid database...", fontSize = 13.sp, color = SakuColors.TextSecondary)
                            }
                        } else {
                            val studied = if (connectStudiedWords) (vocabSummary?.studiedCount ?: 0) else 0
                            val suspended = if (connectStudiedWords) (vocabSummary?.suspendedCount ?: 0) else 0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$studied",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SakuColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Studied",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SakuColors.TextSecondary
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "|",
                                    fontSize = 18.sp,
                                    color = SakuColors.BorderFocus
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = "$suspended",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SakuColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Suspended",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = SakuColors.TextSecondary
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { loadVocabulary() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh Vocab", tint = SakuColors.TextSecondary, modifier = Modifier.size(20.dp))
                        }
                        Icon(
                            if (isVocabExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = SakuColors.TextSecondary
                        )
                    }
                }

                // Expandable Vocabulary Chips
                if (isVocabExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (!connectStudiedWords) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SakuColors.SurfaceElevated,
                            border = BorderStroke(1.dp, SakuColors.Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Word connection is disabled. Stories will be generated freely by Gemini without Anki vocabulary restrictions.",
                                fontSize = 12.sp,
                                color = SakuColors.TextSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    } else {
                        val words = vocabSummary?.words ?: emptyList()
                    if (words.isEmpty()) {
                        Text(
                            text = if (!hasAnkiPermission) "AnkiDroid permission required to read flashcards."
                            else "No studied or suspended cards found in selected deck(s).\n\nCards you study or suspend on your lock screen will appear here and be used when generating stories.",
                            fontSize = 12.sp,
                            color = SakuColors.TextSecondary,
                            lineHeight = 18.sp
                        )
                    } else {
                        // Filter tabs: All, Studied, Suspended
                        val studiedCount = vocabSummary?.studiedCount ?: 0
                        val suspendedCount = vocabSummary?.suspendedCount ?: 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                Triple("all", "All (${words.size})", null),
                                Triple("studied", "Studied ($studiedCount)", SakuColors.SagePrimary),
                                Triple("suspended", "Suspended ($suspendedCount)", SakuColors.AccentAmber)
                            ).forEach { (mode, label, tintColor) ->
                                val isSelected = vocabFilterMode == mode
                                Surface(
                                    onClick = { vocabFilterMode = mode },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) {
                                        tintColor?.let { it.copy(alpha = 0.18f) } ?: SakuColors.SageContainer
                                    } else SakuColors.SurfaceElevated,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) {
                                            tintColor?.copy(alpha = 0.6f) ?: SakuColors.SagePrimary
                                        } else SakuColors.Border
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            tintColor ?: SakuColors.SagePrimary
                                        } else SakuColors.TextSecondary,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        val filteredWords = when (vocabFilterMode) {
                            "studied" -> words.filter { !it.isSuspended }
                            "suspended" -> words.filter { it.isSuspended }
                            else -> words
                        }

                        if (filteredWords.isEmpty()) {
                            Text(
                                text = "No cards found under the \"$vocabFilterMode\" filter.",
                                fontSize = 12.sp,
                                color = SakuColors.TextTertiary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Text(
                                text = "These cards will be prioritized for AI story generation:",
                                fontSize = 12.sp,
                                color = SakuColors.TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                filteredWords.take(60).forEach { item ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (item.isSuspended) SakuColors.AccentAmberContainer else SakuColors.SurfaceElevated,
                                        border = BorderStroke(
                                            1.dp,
                                            if (item.isSuspended) SakuColors.AccentAmber.copy(alpha = 0.35f)
                                            else SakuColors.Border
                                        ),
                                        onClick = {
                                            selectedWordDetail = item
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.displayWord,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (item.isSuspended) SakuColors.AccentAmber else SakuColors.TextPrimary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            if (item.isSuspended) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "sus",
                                                    fontSize = 9.sp,
                                                    color = SakuColors.AccentAmber,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (filteredWords.size > 60) {
                                Text(
                                    text = "+ ${filteredWords.size - 60} more words available in pool",
                                    fontSize = 11.sp,
                                    color = SakuColors.TextTertiary,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                    }
                }
            }
        }

        // Custom Mode Active Indicator (Pill above Generate button)
        if (isCustomThemeModeActive && !customStoryTheme.isNullOrBlank()) {
            val themeBadge = StoryThemes.getThemeBadgeColors(customStoryTheme!!)
            val formattedTheme = StoryThemes.formatThemeName(customStoryTheme!!)
            val topicText = customStoryTopic ?: "Any topic"

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = themeBadge.backgroundColor,
                border = BorderStroke(1.dp, themeBadge.borderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
                    .clickable { showThemeConfigDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = themeBadge.contentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Theme: $formattedTheme • $topicText",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = themeBadge.contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = CircleShape,
                        color = themeBadge.contentColor.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable {
                                prefs.isCustomThemeModeActive = false
                                prefs.customStoryTheme = null
                                prefs.customStoryTopic = null
                                isCustomThemeModeActive = false
                                customStoryTheme = null
                                customStoryTopic = null
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Reset to random",
                                tint = themeBadge.contentColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Generate Story Action Button with 2.5s hold-to-configure
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isGeneratingStory) SakuColors.SagePrimary.copy(alpha = 0.40f)
                    else SakuColors.SagePrimary.copy(alpha = 0.85f)
                )
                .border(
                    BorderStroke(
                        width = if (holdProgress > 0f) 2.dp else 1.dp,
                        color = if (holdProgress > 0f) {
                            Color(0xFFFFD54F).copy(alpha = (0.5f + holdProgress * 0.5f).coerceIn(0f, 1f))
                        } else {
                            Color.White.copy(alpha = 0.25f)
                        }
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .pointerInput(isGeneratingStory, apiKey, prefs.hasAcceptedInternetDisclosure) {
                    if (isGeneratingStory) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        val startTime = System.currentTimeMillis()
                        var holdCompleted = false

                        val holdJob = coroutineScope.launch {
                            val duration = 2500L
                            val stepMs = 16L
                            while (isActive) {
                                val elapsed = System.currentTimeMillis() - startTime
                                val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                                holdProgress = progress
                                if (progress >= 1f) {
                                    holdCompleted = true
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showThemeConfigDialog = true
                                    holdProgress = 0f
                                    break
                                }
                                delay(stepMs)
                            }
                        }

                        val up = waitForUpOrCancellation()
                        holdJob.cancel()
                        holdProgress = 0f

                        if (up != null && !holdCompleted) {
                            if (apiKey.isBlank()) {
                                showApiKeyDialog = true
                            } else if (!prefs.hasAcceptedInternetDisclosure) {
                                showInternetConsentDialog = true
                            } else {
                                executeGeneration()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Charging progress fill when holding
            if (holdProgress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxWidth(holdProgress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFFFD54F).copy(alpha = 0.45f),
                                    Color(0xFFFFB74D).copy(alpha = 0.35f)
                                )
                            )
                        )
                        .align(Alignment.CenterStart)
                )
            }

            // Button Content
            AnimatedContent(
                targetState = when {
                    isGeneratingStory -> 0
                    holdProgress > 0f -> 1
                    else -> 2
                },
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(150))
                },
                label = "StoryButtonStateTransition"
            ) { state ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    when (state) {
                        0 -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = SakuColors.OnSage
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            RotatingStatusText(
                                phrases = listOf(
                                    "Crafting $selectedJlpt story...",
                                    "Weaving the plot...",
                                    "Polishing details...",
                                    "Adding some flair...",
                                    "Fine-tuning emotions...",
                                    "Almost there..."
                                ),
                                isGenerating = isGeneratingStory,
                                color = SakuColors.OnSage
                            )
                        }
                        1 -> {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFFFFD54F)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Configuring... ${(holdProgress * 100).toInt()}%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                        else -> {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = SakuColors.OnSage
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentStory == null) "Generate $selectedJlpt Story" else "Generate Another Story",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = SakuColors.OnSage,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Error message if generation failed
        if (generationError != null) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SakuColors.AccentRoseContainer),
                border = BorderStroke(1.dp, SakuColors.AccentRose.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = SakuColors.AccentRose)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = generationError ?: "Error generating story",
                        fontSize = 13.sp,
                        color = SakuColors.AccentRose
                    )
                }
            }
        }

        // 5. Story Viewer Card (Warm Cream Parchment Immersion)
        val story = currentStory
        if (story != null) {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = readingTheme.containerColor),
                border = BorderStroke(1.dp, readingTheme.borderColor)
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    // Header: JLPT Tag Badge (Left) & Copy Button (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = readingTheme.jlptBadgeBackground,
                                border = BorderStroke(1.dp, readingTheme.jlptBadgeBorder)
                            ) {
                                Text(
                                    text = "JLPT ${story.jlptLevel}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = readingTheme.jlptBadgeText,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isCurrentStoryPlaying = isNarrating && currentlyPlayingStoryId == story.id
                            val isCurrentStorySynthesizing = isSynthesizingAudio && currentlyPlayingStoryId == story.id

                            // Speaker Narration Button
                            IconButton(
                                onClick = {
                                    if (isCurrentStoryPlaying) {
                                        audioService.stopAudio()
                                        isNarrating = false
                                        currentlyPlayingStoryId = null
                                    } else {
                                        if (fishAudioApiKey.isBlank()) {
                                            showFishAudioDialog = true
                                            Toast.makeText(context, "Please configure your Fish Audio API key", Toast.LENGTH_SHORT).show()
                                            return@IconButton
                                        }

                                        audioService.stopAudio()
                                        isNarrating = false
                                        isSynthesizingAudio = true
                                        currentlyPlayingStoryId = story.id

                                        coroutineScope.launch {
                                            val narrationText = "${story.title}。\n\n${story.content}"
                                            val result = audioService.synthesizeStoryAudio(
                                                apiKey = fishAudioApiKey,
                                                voiceId = fishAudioVoiceId,
                                                model = fishAudioModel,
                                                storyId = story.id,
                                                text = narrationText
                                            )
                                            isSynthesizingAudio = false
                                            result.onSuccess { audioFile ->
                                                isNarrating = true
                                                audioService.playAudio(
                                                    file = audioFile,
                                                    onPlaybackStateChanged = { playing ->
                                                        isNarrating = playing
                                                        if (!playing && currentlyPlayingStoryId == story.id) {
                                                            currentlyPlayingStoryId = null
                                                        }
                                                    },
                                                    onCompletion = {
                                                        isNarrating = false
                                                        currentlyPlayingStoryId = null
                                                    }
                                                )
                                            }.onFailure { error ->
                                                isNarrating = false
                                                currentlyPlayingStoryId = null
                                                Toast.makeText(context, "Narration error: ${error.message ?: "Failed to generate audio"}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            ) {
                                if (isCurrentStorySynthesizing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = readingTheme.iconTintColor
                                    )
                                } else if (isCurrentStoryPlaying) {
                                    Icon(
                                        Icons.Filled.Stop,
                                        contentDescription = "Stop Narration",
                                        tint = SakuColors.AccentRose,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Narrate Story",
                                        tint = readingTheme.iconTintColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Japanese Story", "${story.title}\n\n${story.content}")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Story copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Story",
                                    tint = readingTheme.iconTintColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    val isCurrentStoryPlaying = isNarrating && currentlyPlayingStoryId == story.id
                    val storyPhrases = remember(story.title, story.content) {
                        segmentStoryIntoPhrases(content = story.content, title = story.title)
                    }
                    val titleEndFraction = remember(storyPhrases) {
                        storyPhrases.firstOrNull()?.startFraction ?: 0f
                    }
                    val phraseWeights = remember(story.id, storyPhrases.size) {
                        FloatArray(storyPhrases.size)
                    }
                    var titleHighlightWeight by remember(story.id) { mutableFloatStateOf(0f) }
                    var narrationBlend by remember(story.id) { mutableFloatStateOf(0f) }
                    var narrationAnimTick by remember(story.id) { mutableIntStateOf(0) }

                    LaunchedEffect(isCurrentStoryPlaying, story.id, storyPhrases) {
                        if (isCurrentStoryPlaying && storyPhrases.isNotEmpty()) {
                            phraseWeights.fill(0f)
                            titleHighlightWeight = if (titleEndFraction > 0f) 1f else 0f
                            var lastNanos = 0L
                            while (isActive && isCurrentStoryPlaying) {
                                withFrameNanos { now ->
                                    val dt = if (lastNanos == 0L) 0.016f else ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                                    lastNanos = now

                                    narrationBlend = (narrationBlend + dt * 5.0f).coerceAtMost(1f)

                                    val posMs = audioService.getCurrentPosition()
                                    val durMs = audioService.getDuration()
                                    val frac = if (durMs > 0) {
                                        val effectivePos = (posMs - 40).coerceAtLeast(0).toFloat()
                                        val effectiveDur = (durMs - 140).coerceAtLeast(1).toFloat()
                                        (effectivePos / effectiveDur).coerceIn(0f, 0.9999f)
                                    } else {
                                        0f
                                    }

                                    val isReadingTitle = frac < titleEndFraction
                                    val titleTarget = if (isReadingTitle) 1f else 0f
                                    titleHighlightWeight = if (titleHighlightWeight < titleTarget) {
                                        (titleHighlightWeight + dt * 5.0f).coerceAtMost(titleTarget)
                                    } else if (titleHighlightWeight > titleTarget) {
                                        (titleHighlightWeight - dt * 5.0f).coerceAtLeast(titleTarget)
                                    } else {
                                        titleHighlightWeight
                                    }

                                    val activeIdx = if (isReadingTitle) {
                                        -1
                                    } else {
                                        val found = storyPhrases.indexOfFirst { frac >= it.startFraction && frac < it.endFraction }
                                        if (found != -1) found else storyPhrases.lastIndex
                                    }

                                    for (i in phraseWeights.indices) {
                                        val target = if (i == activeIdx) 1f else 0f
                                        val cur = phraseWeights[i]
                                        phraseWeights[i] = if (cur < target) {
                                            (cur + dt * 5.0f).coerceAtMost(target)
                                        } else if (cur > target) {
                                            (cur - dt * 5.0f).coerceAtLeast(target)
                                        } else {
                                            cur
                                        }
                                    }
                                    narrationAnimTick++
                                }
                            }
                        } else if (narrationBlend > 0f) {
                            var lastNanos = 0L
                            while (isActive && narrationBlend > 0.001f) {
                                withFrameNanos { now ->
                                    val dt = if (lastNanos == 0L) 0.016f else ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                                    lastNanos = now

                                    narrationBlend = (narrationBlend - dt * 6.0f).coerceAtLeast(0f)
                                    titleHighlightWeight = (titleHighlightWeight - dt * 6.0f).coerceAtLeast(0f)
                                    for (i in phraseWeights.indices) {
                                        phraseWeights[i] = (phraseWeights[i] - dt * 6.0f).coerceAtLeast(0f)
                                    }
                                    narrationAnimTick++
                                }
                            }
                            narrationBlend = 0f
                            titleHighlightWeight = 0f
                            phraseWeights.fill(0f)
                            narrationAnimTick++
                        }
                    }

                    CustomSelectionContainer(
                        onTranslate = { selectedText ->
                            translateTargetText = selectedText
                            showTranslationSheet = true
                        }
                    ) {
                        SelectionContainer {
                            Column {
                                Spacer(modifier = Modifier.height(16.dp))

                                val syncBgColor = getNarrationSyncBgColor(readingTheme)
                                val activeTitleAlpha = (titleHighlightWeight * narrationBlend).coerceIn(0f, 1f)

                                // Story Title in Japanese (highlights with Saku theme bg-color when read)
                                val titleAnnotated = remember(story.title, syncBgColor, activeTitleAlpha, narrationAnimTick) {
                                    if (activeTitleAlpha > 0.01f) {
                                        buildAnnotatedString {
                                            append(story.title)
                                            addStyle(
                                                style = SpanStyle(
                                                    background = syncBgColor.copy(alpha = (syncBgColor.alpha * activeTitleAlpha).coerceIn(0f, 1f))
                                                ),
                                                start = 0,
                                                end = story.title.length
                                            )
                                        }
                                    } else {
                                        AnnotatedString(story.title)
                                    }
                                }
                                Text(
                                    text = titleAnnotated,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = readingTheme.textPrimaryColor,
                                    lineHeight = 30.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Base Story Content (with normal vocabulary highlights if enabled)
                                val baseAnnotatedContent = remember(story.content, vocabSummary?.words, highlightWords, readingTheme) {
                                    if (highlightWords && vocabSummary != null && vocabSummary!!.words.isNotEmpty()) {
                                        buildHighlightedStoryText(
                                            content = story.content,
                                            vocabWords = vocabSummary!!.words,
                                            highlightBg = readingTheme.wordHighlightBackground,
                                            highlightText = readingTheme.wordHighlightTextColor,
                                            onWordTapped = { item ->
                                                selectedWordDetail = item
                                            }
                                        )
                                    } else {
                                        AnnotatedString(story.content)
                                    }
                                }

                                // Overlay Saku theme bg-color sync highlight when narrating
                                val displayedStoryContent = remember(
                                    baseAnnotatedContent,
                                    storyPhrases,
                                    syncBgColor,
                                    narrationBlend,
                                    narrationAnimTick
                                ) {
                                    if (narrationBlend > 0.001f && storyPhrases.isNotEmpty()) {
                                        buildBgSyncedStoryText(
                                            baseText = baseAnnotatedContent,
                                            phrases = storyPhrases,
                                            phraseWeights = phraseWeights,
                                            narrationBlend = narrationBlend,
                                            syncBgColor = syncBgColor
                                        )
                                    } else {
                                        baseAnnotatedContent
                                    }
                                }

                                Text(
                                    text = displayedStoryContent,
                                    fontSize = 17.sp,
                                    color = readingTheme.textPrimaryColor,
                                    lineHeight = 34.sp,
                                    letterSpacing = 0.5.sp
                                )

                    // Target Vocabulary Chips
                    if (story.targetWords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = readingTheme.surfaceColor,
                            border = BorderStroke(1.dp, readingTheme.surfaceBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.School,
                                        contentDescription = null,
                                        tint = readingTheme.iconTintColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "TARGET WORDS FROM YOUR CARDS (${story.targetWords.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = readingTheme.textSecondaryColor,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    story.targetWords.forEach { word ->
                                        val isPresent = story.content.contains(word)
                                        val matchedItem = vocabSummary?.words?.find { it.displayWord == word || it.kanji == word }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isPresent) readingTheme.targetChipPresentBackground else readingTheme.targetChipBackground,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isPresent) readingTheme.targetChipPresentBorder else readingTheme.targetChipBorder
                                            ),
                                            onClick = {
                                                matchedItem?.let { selectedWordDetail = it }
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = word,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Normal,
                                                    color = readingTheme.targetChipText,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                if (isPresent) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = "Used in story",
                                                        tint = if (readingTheme.isDark) Color(0xFF6EE7A0) else Color(0xFF4A6B4F),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Reading Comprehension Quiz
                    if (story.questions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = readingTheme.surfaceColor,
                            border = BorderStroke(1.dp, readingTheme.surfaceBorderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.AutoAwesome,
                                            contentDescription = null,
                                            tint = readingTheme.iconTintColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Reading Comprehension Quiz",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = readingTheme.quizQuestionText
                                        )
                                    }

                                    if (userAnswers.isNotEmpty()) {
                                        TextButton(
                                            onClick = { userAnswers.clear() },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Refresh,
                                                contentDescription = "Reset Quiz",
                                                modifier = Modifier.size(14.dp),
                                                tint = readingTheme.iconTintColor
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset", color = readingTheme.iconTintColor, fontSize = 12.sp, maxLines = 1, softWrap = false)
                                        }
                                    }
                                }

                                Text(
                                    text = "Test your understanding of the story and vocabulary.",
                                    fontSize = 12.sp,
                                    color = readingTheme.textSecondaryColor,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                )

                                val answeredCount = story.questions.count { userAnswers.containsKey(it.id) }
                                val correctCount = story.questions.count { userAnswers[it.id] == it.correctOptionIndex }

                                if (answeredCount == story.questions.size && story.questions.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (correctCount == story.questions.size) {
                                            if (readingTheme.isDark) Color(0xFF1E3827) else Color(0xFFD8E8D5)
                                        } else {
                                            readingTheme.targetChipBackground
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (correctCount == story.questions.size) {
                                                if (readingTheme.isDark) Color(0xFF4A8F5C) else Color(0xFF7E9F85)
                                            } else {
                                                readingTheme.surfaceBorderColor
                                            }
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = if (correctCount == story.questions.size) "🎉 Perfect! Score: $correctCount / ${story.questions.size}"
                                                else "Score: $correctCount / ${story.questions.size}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = readingTheme.quizQuestionText
                                            )
                                        }
                                    }
                                }

                                story.questions.forEachIndexed { qIdx, q ->
                                    if (qIdx > 0) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                    Text(
                                        text = "${qIdx + 1}. ${q.questionText}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = readingTheme.quizQuestionText
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    val selectedOpt = userAnswers[q.id]
                                    val isAnswered = selectedOpt != null

                                    q.options.forEachIndexed { optIdx, optText ->
                                        val isSelected = selectedOpt == optIdx
                                        val isCorrectOption = q.correctOptionIndex == optIdx

                                        val bgColor = when {
                                            !isAnswered -> if (isSelected) readingTheme.quizOptionSelectedBackground else readingTheme.quizOptionBackground
                                            isCorrectOption -> readingTheme.quizOptionCorrectBackground
                                            isSelected && !isCorrectOption -> readingTheme.quizOptionWrongBackground
                                            else -> readingTheme.quizOptionBackground
                                        }

                                        val borderColor = when {
                                            !isAnswered -> if (isSelected) readingTheme.quizOptionSelectedBorder else readingTheme.quizOptionBorder
                                            isCorrectOption -> readingTheme.quizOptionCorrectBorder
                                            isSelected && !isCorrectOption -> readingTheme.quizOptionWrongBorder
                                            else -> readingTheme.quizOptionBorder
                                        }

                                        Surface(
                                            onClick = {
                                                if (!isAnswered) {
                                                    userAnswers[q.id] = optIdx
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = bgColor,
                                            border = BorderStroke(1.dp, borderColor),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = optText,
                                                    fontSize = 13.sp,
                                                    color = readingTheme.quizQuestionText,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isAnswered && isCorrectOption) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = "Correct",
                                                        tint = if (readingTheme.isDark) Color(0xFF6EE7A0) else Color(0xFF4A6B4F),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                } else if (isAnswered && isSelected && !isCorrectOption) {
                                                    Icon(
                                                        Icons.Filled.Close,
                                                        contentDescription = "Incorrect",
                                                        tint = if (readingTheme.isDark) Color(0xFFE06C75) else Color(0xFFA84E5B),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (isAnswered && q.explanation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = readingTheme.quizExplanationBackground,
                                            border = BorderStroke(1.dp, readingTheme.quizExplanationBorder),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "💡 ${q.explanation}",
                                                fontSize = 12.sp,
                                                color = readingTheme.quizExplanationText,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                            }
                        }
                    }
                }
            }
        } else if (!isGeneratingStory) {
            // Empty state placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.School,
                        contentDescription = null,
                        tint = SakuColors.BorderFocus,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No story generated yet",
                        color = SakuColors.TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Pick your JLPT level and tap Generate Story above",
                        color = SakuColors.TextTertiary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }

    // Modal Bottom Sheet: Past Stories History
    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = historySheetState,
            containerColor = SakuColors.Surface,
            contentColor = SakuColors.TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Reading History (${savedStories.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SakuColors.TextPrimary
                    )
                    if (savedStories.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                historyManager.clearAll()
                                savedStories = emptyList()
                            }
                        ) {
                            Text("Clear All", color = SakuColors.AccentRose, fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (savedStories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No saved stories yet", color = SakuColors.TextTertiary, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(savedStories, key = { it.id }) { item ->
                            val isExpanded = expandedHistoryCards[item.id] == true
                            val rotationAngle by animateFloatAsState(
                                targetValue = if (isExpanded) 180f else 0f,
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                                label = "card_chevron_rotation"
                            )
                            val cardMinHeight = if (isExpanded) 280.dp else 125.dp

                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SakuColors.SurfaceElevated),
                                border = BorderStroke(1.dp, if (item.isPinned) SakuColors.SagePrimary.copy(alpha = 0.7f) else SakuColors.Border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateContentSize(
                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                    )
                                    .defaultMinSize(minHeight = cardMinHeight)
                                    .clickable {
                                        currentStory = item
                                        coroutineScope.launch {
                                            historySheetState.hide()
                                            showHistorySheet = false
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = cardMinHeight)
                                ) {
                                    // Background Scenery Image
                                    if (!item.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.matchParentSize()
                                        )
                                        // Dual top-and-bottom vertical gradient scrim:
                                        // Top scrim for badges & action buttons; middle is clear for scenery art; bottom scrim for title & date
                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        0.0f to Color(0xCC121214),
                                                        0.22f to Color(0x55121214),
                                                        0.50f to Color(0x11121214),
                                                        0.72f to Color(0xAA121214),
                                                        1.0f to Color(0xF2121214)
                                                    )
                                                )
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = cardMinHeight)
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // --- TOP HEADER ROW ---
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Top-Left: Badges (Pinned, JLPT, Theme)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f, fill = false)
                                            ) {
                                                if (item.isPinned) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = SakuColors.SagePrimary.copy(alpha = 0.25f),
                                                        border = BorderStroke(1.dp, SakuColors.SagePrimary.copy(alpha = 0.8f))
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.PushPin,
                                                                contentDescription = null,
                                                                tint = SakuColors.SagePrimary,
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = "PINNED",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = SakuColors.SagePrimary,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = SakuColors.SageContainer
                                                ) {
                                                    Text(
                                                        text = item.jlptLevel,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = SakuColors.SagePrimary,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }

                                                if (!item.theme.isNullOrBlank()) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    val themeBadge = StoryThemes.getThemeBadgeColors(item.theme)
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = themeBadge.backgroundColor,
                                                        border = BorderStroke(1.dp, themeBadge.borderColor)
                                                    ) {
                                                        val historyBadgeText = if (!item.topic.isNullOrBlank()) {
                                                            "${StoryThemes.formatThemeName(item.theme)} • ${item.topic}"
                                                        } else {
                                                            StoryThemes.formatThemeName(item.theme)
                                                        }
                                                        Text(
                                                            text = historyBadgeText,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = themeBadge.contentColor,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Top-Right: Action Buttons pill: [Pin] [Expand Dropdown] [Delete]
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = Color.Black.copy(alpha = 0.45f),
                                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f))
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    // Pin Button
                                                    IconButton(
                                                        onClick = {
                                                            historyManager.togglePin(item.id)
                                                            savedStories = historyManager.getStories()
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (item.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                                            contentDescription = if (item.isPinned) "Unpin Story" else "Pin Story",
                                                            tint = if (item.isPinned) SakuColors.SagePrimary else Color.White.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }

                                                    // Dropdown / Expand Chevron Button
                                                    IconButton(
                                                        onClick = {
                                                            expandedHistoryCards[item.id] = !isExpanded
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.KeyboardArrowDown,
                                                            contentDescription = if (isExpanded) "Collapse Card" else "Expand Card",
                                                            tint = if (isExpanded) SakuColors.SagePrimary else Color.White.copy(alpha = 0.8f),
                                                            modifier = Modifier
                                                                .size(18.dp)
                                                                .rotate(rotationAngle)
                                                        )
                                                    }

                                                    // Delete Button
                                                    IconButton(
                                                        onClick = {
                                                            historyManager.deleteStory(item.id)
                                                            savedStories = historyManager.getStories()
                                                            if (currentStory?.id == item.id) {
                                                                currentStory = savedStories.firstOrNull()
                                                            }
                                                        },
                                                        modifier = Modifier.size(28.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.DeleteOutline,
                                                            contentDescription = "Delete Story",
                                                            tint = SakuColors.AccentRose,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Center breathing space to showcase scenery artwork clearly (expands when card is expanded)
                                        Spacer(modifier = Modifier.height(if (isExpanded) 120.dp else 28.dp))

                                        // --- BOTTOM SECTION ---
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            // Optional story excerpt when expanded so user can read a snippet
                                            if (isExpanded && item.content.isNotBlank()) {
                                                Text(
                                                    text = item.content.replace("\n", " ").trim(),
                                                    fontSize = 13.sp,
                                                    lineHeight = 18.sp,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(bottom = 8.dp)
                                                )
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.Bottom,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                // Bottom-Left: Story Title
                                                Text(
                                                    text = item.title,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 16.sp,
                                                    color = if (!item.imageUrl.isNullOrBlank()) Color.White else SakuColors.TextPrimary,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .padding(end = 12.dp)
                                                )

                                                // Bottom-Right: Date (no longer trimmed by header)
                                                Text(
                                                    text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.createdAt)),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (!item.imageUrl.isNullOrBlank()) Color.White.copy(alpha = 0.75f) else SakuColors.TextSecondary,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Highlighted Word Meaning & Details
    if (selectedWordDetail != null) {
        WordDetailBottomSheet(
            item = selectedWordDetail!!,
            sheetState = wordDetailSheetState,
            onDismiss = {
                coroutineScope.launch {
                    wordDetailSheetState.hide()
                    selectedWordDetail = null
                }
            },
            onNavigateToJisho = onNavigateToJisho
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

    // API Key Entry / Edit Dialog
    if (showApiKeyDialog) {
        ApiKeySetupDialog(
            currentKey = apiKey,
            onSave = { newKey ->
                apiKey = newKey
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
            currentModel = selectedModel,
            onSave = { newModel ->
                selectedModel = newModel
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

    // Internet Confirmation & Privacy Disclosure Dialog
    if (showInternetConsentDialog) {
        InternetAccessDisclosureDialog(
            onConfirm = {
                prefs.hasAcceptedInternetDisclosure = true
                showInternetConsentDialog = false
                executeGeneration()
            },
            onDismiss = { showInternetConsentDialog = false }
        )
    }

    // Story Theme & Topic Configuration Dialog
    if (showThemeConfigDialog) {
        StoryThemeConfigDialog(
            prefs = prefs,
            onDismiss = { showThemeConfigDialog = false },
            onConfigurationChanged = {
                isCustomThemeModeActive = prefs.isCustomThemeModeActive
                customStoryTheme = prefs.customStoryTheme
                customStoryTopic = prefs.customStoryTopic
            }
        )
    }
}

data class StoryPhrase(
    val startIndex: Int,
    val endIndex: Int,
    val text: String,
    val startFraction: Float,
    val endFraction: Float
)

/**
 * Splits a Japanese story body into natural spoken phrases/clauses and computes normalized
 * acoustic time intervals [startFraction, endFraction] for each phrase, accounting for
 * the spoken title offset when [title] is provided.
 */
internal fun segmentStoryIntoPhrases(content: String, title: String = ""): List<StoryPhrase> {
    if (content.isEmpty()) return emptyList()

    val hardDelimiters = setOf('。', '！', '？', '!', '?', '\n')
    val softDelimiters = setOf('、', '，', ',', '…', '・')
    val closingBrackets = setOf('」', '』', '）', ')', '】', '”', '"')
    val splitParticles = setOf('は', 'が', 'を', 'に', 'で', 'と', 'も', 'へ', 'て')
    val smallKana = setOf('ゃ', 'ゅ', 'ょ', 'っ', 'ャ', 'ュ', 'ョ', 'ッ', 'ぁ', 'ぃ', 'ぅ', 'ぇ', 'ぉ', 'ァ', 'ィ', 'ゥ', 'ェ', 'ォ')

    fun isKanjiOrKatakanaOrQuote(c: Char): Boolean {
        return (c in '\u4E00'..'\u9FFF') || (c in '\u30A0'..'\u30FF') || c == '「' || c == '『'
    }

    fun countSpokenChars(s: String): Int {
        return s.count { c ->
            !c.isWhitespace() && c !in hardDelimiters && c !in softDelimiters && c !in closingBrackets && c != '「' && c != '『'
        }
    }

    fun computeAcousticWeight(text: String): Float {
        var w = 0f
        for (ch in text) {
            w += when {
                ch in '\u4E00'..'\u9FFF' -> 1.8f
                ch in smallKana -> 0.6f
                ch in '\u3040'..'\u30FF' -> 1.0f
                ch.isLetterOrDigit() -> 1.4f
                ch in softDelimiters -> 2.6f
                ch == '\n' -> 1.8f
                ch in hardDelimiters -> 4.8f
                else -> 0.1f
            }
        }
        return w.coerceAtLeast(1.0f)
    }

    val rawRanges = mutableListOf<IntRange>()
    var chunkStart = 0
    var i = 0
    val len = content.length

    while (i < len) {
        val c = content[i]
        val isHard = c in hardDelimiters
        val isSoft = c in softDelimiters

        var shouldSplit = false
        var splitEnd = i + 1

        if (isHard || isSoft) {
            while (splitEnd < len && (content[splitEnd] in closingBrackets || content[splitEnd] in hardDelimiters || content[splitEnd] in softDelimiters)) {
                splitEnd++
            }
            val currentSpoken = countSpokenChars(content.substring(chunkStart, splitEnd))
            if (isHard) {
                if (currentSpoken > 0) {
                    shouldSplit = true
                }
            } else {
                // Soft delimiter: avoid splitting tiny 1-3 char prefixes unless near end
                if (currentSpoken >= 4) {
                    shouldSplit = true
                }
            }
        } else if (c in splitParticles && i + 1 < len && isKanjiOrKatakanaOrQuote(content[i + 1])) {
            val currentSpoken = countSpokenChars(content.substring(chunkStart, i + 1))
            if (currentSpoken >= 11) {
                // Check distance to next punctuation delimiter
                var nextDelimIdx = i + 1
                while (nextDelimIdx < len && content[nextDelimIdx] !in hardDelimiters && content[nextDelimIdx] !in softDelimiters) {
                    nextDelimIdx++
                }
                val remainingToPunct = countSpokenChars(content.substring(i + 1, nextDelimIdx))
                if (remainingToPunct >= 6) {
                    shouldSplit = true
                    splitEnd = i + 1
                }
            }
        }

        if (shouldSplit) {
            rawRanges.add(chunkStart until splitEnd)
            chunkStart = splitEnd
            i = splitEnd
        } else {
            i++
        }
    }

    if (chunkStart < len) {
        val trailing = content.substring(chunkStart, len)
        if (countSpokenChars(trailing) > 0 || rawRanges.isEmpty()) {
            rawRanges.add(chunkStart until len)
        } else if (rawRanges.isNotEmpty()) {
            val last = rawRanges.removeAt(rawRanges.lastIndex)
            rawRanges.add(last.first until len)
        }
    }

    val titleWeight = if (title.isNotBlank()) computeAcousticWeight("${title}。\n\n") else 0f
    val weights = FloatArray(rawRanges.size)
    var totalWeight = titleWeight

    for (idx in rawRanges.indices) {
        val range = rawRanges[idx]
        val text = content.substring(range.first, range.last + 1)
        val safeWeight = computeAcousticWeight(text)
        weights[idx] = safeWeight
        totalWeight += safeWeight
    }

    if (totalWeight <= 0f) totalWeight = 1f

    var cumulative = titleWeight
    return rawRanges.mapIndexed { idx, range ->
        val startFrac = cumulative / totalWeight
        cumulative += weights[idx]
        val endFrac = if (idx == rawRanges.lastIndex) 1.0f else (cumulative / totalWeight)
        StoryPhrase(
            startIndex = range.first,
            endIndex = range.last + 1,
            text = content.substring(range.first, range.last + 1),
            startFraction = startFrac,
            endFraction = endFrac
        )
    }
}

/**
 * Returns the theme-harmonized Saku background highlight color for audio narration sync.
 */
private fun getNarrationSyncBgColor(readingTheme: ReadingTheme): Color {
    return if (readingTheme.isDark) {
        lerp(
            readingTheme.wordHighlightBackground,
            SakuColors.SagePrimary,
            0.42f
        ).copy(alpha = 0.50f)
    } else {
        lerp(
            readingTheme.quizOptionCorrectBackground,
            SakuColors.SagePrimary,
            0.38f
        ).copy(alpha = 0.48f)
    }
}

/**
 * Overlays a smooth Saku-theme background-color highlight on the currently narrated phrase
 * without changing font size or greying out other text.
 */
private fun buildBgSyncedStoryText(
    baseText: AnnotatedString,
    phrases: List<StoryPhrase>,
    phraseWeights: FloatArray,
    narrationBlend: Float,
    syncBgColor: Color
): AnnotatedString {
    val clampedBlend = narrationBlend.coerceIn(0f, 1f)
    val easedBlend = clampedBlend * clampedBlend * (3f - 2f * clampedBlend)

    return buildAnnotatedString {
        append(baseText)
        for (i in phrases.indices) {
            val rawW = if (i < phraseWeights.size) phraseWeights[i].coerceIn(0f, 1f) else 0f
            if (rawW > 0.01f) {
                val easedW = rawW * rawW * (3f - 2f * rawW)
                val alpha = (syncBgColor.alpha * easedW * easedBlend).coerceIn(0f, 1f)
                if (alpha > 0.01f) {
                    val phrase = phrases[i]
                    addStyle(
                        style = SpanStyle(
                            background = syncBgColor.copy(alpha = alpha)
                        ),
                        start = phrase.startIndex.coerceIn(0, baseText.length),
                        end = phrase.endIndex.coerceIn(0, baseText.length)
                    )
                }
            }
        }
    }
}

/**
 * Builds an AnnotatedString with theme-matched background highlights and clickable links
 * for vocabulary words appearing in the Japanese story.
 */
private fun buildHighlightedStoryText(
    content: String,
    vocabWords: List<AnkiVocabularyItem>,
    highlightBg: Color = Color(0xFFF7D5B5),
    highlightText: Color = Color(0xFF1E1E1E),
    onWordTapped: (AnkiVocabularyItem) -> Unit
): AnnotatedString {
    if (vocabWords.isEmpty() || content.isEmpty()) {
        return AnnotatedString(content)
    }

    // Filter valid words: at least 2 characters, or 1 character if it's kanji
    val validWords = vocabWords
        .filter { it.displayWord.isNotBlank() && (it.displayWord.length >= 2 || it.kanji.isNotBlank()) }
        .distinctBy { it.displayWord }
        .sortedByDescending { it.displayWord.length }

    data class RangeMatch(val start: Int, val end: Int, val item: AnkiVocabularyItem)
    val matches = mutableListOf<RangeMatch>()
    val occupied = BooleanArray(content.length)

    for (item in validWords) {
        val word = item.displayWord
        var searchFrom = 0
        while (searchFrom < content.length) {
            val idx = content.indexOf(word, searchFrom)
            if (idx == -1) break
            val endIdx = idx + word.length
            var isFree = true
            for (i in idx until endIdx) {
                if (occupied[i]) {
                    isFree = false
                    break
                }
            }
            if (isFree) {
                for (i in idx until endIdx) {
                    occupied[i] = true
                }
                matches.add(RangeMatch(idx, endIdx, item))
            }
            searchFrom = idx + 1
        }
    }

    matches.sortBy { it.start }

    return buildAnnotatedString {
        append(content)
        for (match in matches) {
            addStyle(
                style = SpanStyle(
                    background = highlightBg,
                    textDecoration = TextDecoration.Underline,
                    color = highlightText,
                    fontWeight = FontWeight.Medium
                ),
                start = match.start,
                end = match.end
            )
            addLink(
                clickable = LinkAnnotation.Clickable(
                    tag = match.item.displayWord,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            background = highlightBg,
                            textDecoration = TextDecoration.Underline,
                            color = highlightText,
                            fontWeight = FontWeight.Medium
                        )
                    ),
                    linkInteractionListener = {
                        onWordTapped(match.item)
                    }
                ),
                start = match.start,
                end = match.end
            )
        }
    }
}

/**
 * Modern modal bottom sheet displaying the tapped vocabulary word's reading, English meaning, and card status.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordDetailBottomSheet(
    item: AnkiVocabularyItem,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onNavigateToJisho: (String) -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SakuColors.Surface,
        contentColor = SakuColors.TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.isSuspended) SakuColors.AccentAmberContainer else SakuColors.SageContainer,
                    border = BorderStroke(
                        1.dp,
                        if (item.isSuspended) SakuColors.AccentAmber.copy(alpha = 0.5f)
                        else SakuColors.SagePrimary.copy(alpha = 0.5f)
                    )
                ) {
                    Text(
                        text = if (item.isSuspended) "Suspended Card" else "Studied Card",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isSuspended) SakuColors.AccentAmber else SakuColors.SagePrimary,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = SakuColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Japanese Word
            Text(
                text = item.displayWord,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = SakuColors.TextPrimary
            )

            // Reading (Furigana / Kana)
            if (item.reading.isNotBlank() && item.reading != item.kanji) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.reading,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SakuColors.SagePrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Meaning / English definition
            if (item.meaning.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SakuColors.SurfaceElevated,
                    border = BorderStroke(1.dp, SakuColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ENGLISH MEANING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SakuColors.TextTertiary,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.meaning,
                            fontSize = 15.sp,
                            color = SakuColors.TextPrimary,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    onDismiss()
                    onNavigateToJisho(item.displayWord)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SakuColors.SagePrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Look up in Jisho",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
fun ApiKeySetupDialog(
    currentKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf(currentKey) }
    val context = LocalContext.current

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
            border = BorderStroke(1.dp, SakuColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = SakuColors.SagePrimary)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Gemini API Key",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = SakuColors.TextPrimary
                    )
                }

                Text(
                    text = "Your key is stored strictly on your device. It is used solely to generate reading immersion stories.",
                    fontSize = 13.sp,
                    color = SakuColors.TextSecondary,
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    placeholder = { Text("AIzaSy...", color = SakuColors.TextTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SakuColors.TextPrimary,
                        unfocusedTextColor = SakuColors.TextPrimary,
                        focusedBorderColor = SakuColors.SagePrimary,
                        unfocusedBorderColor = SakuColors.Border,
                        focusedContainerColor = SakuColors.SurfaceElevated,
                        unfocusedContainerColor = SakuColors.SurfaceElevated
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Get Free Key", fontSize = 12.sp, color = SakuColors.SagePrimary, maxLines = 1, softWrap = false)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = SakuColors.SagePrimary, modifier = Modifier.size(14.dp))
                        }
                    }

                    if (keyInput.isNotBlank()) {
                        TextButton(
                            onClick = { keyInput = "" },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Clear", fontSize = 12.sp, color = SakuColors.AccentRose, maxLines = 1, softWrap = false)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SakuColors.Border),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text("Cancel", color = SakuColors.TextSecondary, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = { onSave(keyInput.trim()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SakuColors.SagePrimary,
                            contentColor = SakuColors.OnSage
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

@Composable
fun InternetAccessDisclosureDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
            border = BorderStroke(1.dp, SakuColors.Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Public,
                        contentDescription = null,
                        tint = SakuColors.SagePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Enable Internet for AI?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = SakuColors.TextPrimary
                    )
                }

                Text(
                    text = "Saku is 100% offline for flashcards and widgets.\n\nAI Reading connects directly to Google's Gemini API with your private API key to compose custom Japanese stories.\n\nOnly vocabulary from your cards is sent for prompt generation. No personal data, passwords, or tracking telemetry are ever sent.",
                    fontSize = 13.sp,
                    color = SakuColors.TextSecondary,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SakuColors.Border),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text("Cancel", color = SakuColors.TextSecondary, maxLines = 1, softWrap = false)
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SakuColors.SagePrimary,
                            contentColor = SakuColors.OnSage
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text("Allow & Continue", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

private fun getJlptLabel(level: String): String {
    return when (level) {
        "N5" -> "(Beginner)"
        "N4" -> "(Upper Beginner)"
        "N3" -> "(Intermediate)"
        "N2" -> "(Pre-Advanced)"
        "N1" -> "(Advanced)"
        else -> ""
    }
}
