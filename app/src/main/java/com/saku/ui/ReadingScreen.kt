package com.saku.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.saku.anki.ReadingVocabularyExtractor
import com.saku.data.AnkiVocabularyItem
import com.saku.data.GeneratedStory
import com.saku.data.PreferencesManager
import com.saku.data.ReadingHistoryManager
import com.saku.data.ReadingVocabularySummary
import com.saku.reading.FishAudioService
import com.saku.reading.GeminiStoryService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReadingScreen(
    padding: PaddingValues,
    prefs: PreferencesManager,
    hasAnkiPermission: Boolean,
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

    var vocabSummary by remember { mutableStateOf<ReadingVocabularySummary?>(null) }
    var isLoadingVocab by remember { mutableStateOf(false) }
    var isVocabExpanded by remember { mutableStateOf(false) }
    var vocabFilterMode by remember { mutableStateOf("all") } // "all", "studied", "suspended"

    var currentStory by remember { mutableStateOf<GeneratedStory?>(null) }
    var isGeneratingStory by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var showInternetConsentDialog by remember { mutableStateOf(false) }

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
    var savedStories by remember { mutableStateOf(historyManager.getStories()) }

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

    // Load initial story from history if available - restore where left off
    LaunchedEffect(Unit) {
        val past = historyManager.getStories()
        savedStories = past
        if (past.isNotEmpty() && currentStory == null) {
            val lastId = prefs.lastReadStoryId
            val found = if (lastId != null) past.find { it.id == lastId } else null
            currentStory = found ?: past.first()
        }
    }

    // Load vocabulary stats when screen opens, permission is granted, or selected decks change
    fun loadVocabulary() {
        if (!hasAnkiPermission) return
        coroutineScope.launch {
            isLoadingVocab = true
            val deckIds = prefs.getSelectedDeckIdsAsLongs()
            vocabSummary = vocabExtractor.extractVocabulary(deckIds)
            isLoadingVocab = false
        }
    }

    LaunchedEffect(hasAnkiPermission, prefs.selectedDeckIds) {
        loadVocabulary()
    }

    val jlptLevels = listOf("N5", "N4", "N3", "N2", "N1")

    fun executeGeneration() {
        coroutineScope.launch {
            isGeneratingStory = true
            generationError = null
            val words = if (connectStudiedWords) (vocabSummary?.words ?: emptyList()) else emptyList()
            val result = storyService.generateStory(
                apiKey = apiKey,
                jlptLevel = selectedJlpt,
                vocabularyList = words,
                preferredModel = selectedModel
            )
            result.onSuccess { story ->
                currentStory = story
                historyManager.saveStory(story)
                savedStories = historyManager.getStories()
            }.onFailure { err ->
                generationError = err.message ?: "Failed to generate story"
            }
            isGeneratingStory = false
        }
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
                                tint = SakuColors.AccentLavender,
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

        // 4. Generate Story Action Button (Frosted Sage Pill)
        Button(
            onClick = {
                if (apiKey.isBlank()) {
                    showApiKeyDialog = true
                    return@Button
                }
                if (!prefs.hasAcceptedInternetDisclosure) {
                    showInternetConsentDialog = true
                    return@Button
                }
                executeGeneration()
            },
            enabled = !isGeneratingStory,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SakuColors.SagePrimary.copy(alpha = 0.85f),
                contentColor = SakuColors.OnSage,
                disabledContainerColor = SakuColors.SagePrimary.copy(alpha = 0.40f)
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
        ) {
            if (isGeneratingStory) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = SakuColors.OnSage
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Crafting $selectedJlpt Japanese Story...",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SakuColors.OnSage,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5EEDB)),
                border = BorderStroke(1.dp, Color(0xFFE5DDC7))
            ) {
                Column(modifier = Modifier.padding(22.dp)) {
                    // Header: JLPT Tag Badge (Left) & Copy Button (Right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8DECB),
                            border = BorderStroke(1.dp, Color(0xFFDDD2BC))
                        ) {
                            Text(
                                text = "JLPT ${story.jlptLevel}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5A5243),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
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
                                        color = Color(0xFF6C6453)
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
                                        tint = Color(0xFF6C6453),
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
                                    tint = Color(0xFF6C6453),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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

                                // Story Title in Japanese
                                Text(
                                    text = story.title,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E1E1E),
                                    lineHeight = 30.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Pure Japanese Story Content with In-Text Highlights
                                if (highlightWords && vocabSummary != null && vocabSummary!!.words.isNotEmpty()) {
                                    val annotatedContent = remember(story.content, vocabSummary?.words, highlightWords) {
                                        buildHighlightedStoryText(
                                            content = story.content,
                                            vocabWords = vocabSummary!!.words,
                                            onWordTapped = { item ->
                                                selectedWordDetail = item
                                            }
                                        )
                                    }
                                    Text(
                                        text = annotatedContent,
                                        fontSize = 17.sp,
                                        color = Color(0xFF1E1E1E),
                                        lineHeight = 34.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                } else {
                                    Text(
                                        text = story.content,
                                        fontSize = 17.sp,
                                        color = Color(0xFF1E1E1E),
                                        lineHeight = 34.sp,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                    // Target Vocabulary Chips
                    if (story.targetWords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFEBE2CF),
                            border = BorderStroke(1.dp, Color(0xFFDDD2BC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.School,
                                        contentDescription = null,
                                        tint = Color(0xFF5A5243),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "TARGET WORDS FROM YOUR CARDS (${story.targetWords.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6C6453),
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
                                            color = if (isPresent) Color(0xFFDFD4BE) else Color(0xFFF0E8D7),
                                            border = BorderStroke(
                                                1.dp,
                                                if (isPresent) Color(0xFFC9BC9F) else Color(0xFFE0D5C0)
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
                                                    color = Color(0xFF2C2820),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                if (isPresent) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = "Used in story",
                                                        tint = Color(0xFF4A6B4F),
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
                            color = Color(0xFFEDE5D3),
                            border = BorderStroke(1.dp, Color(0xFFDDD2BC)),
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
                                            tint = Color(0xFF5A5243),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Reading Comprehension Quiz",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2C2820)
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
                                                tint = Color(0xFF6C6453)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset", color = Color(0xFF6C6453), fontSize = 12.sp, maxLines = 1, softWrap = false)
                                        }
                                    }
                                }

                                Text(
                                    text = "Test your understanding of the story and vocabulary.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF6C6453),
                                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                )

                                val answeredCount = story.questions.count { userAnswers.containsKey(it.id) }
                                val correctCount = story.questions.count { userAnswers[it.id] == it.correctOptionIndex }

                                if (answeredCount == story.questions.size && story.questions.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (correctCount == story.questions.size) Color(0xFFD8E8D5) else Color(0xFFE2D6C0),
                                        border = BorderStroke(
                                            1.dp,
                                            if (correctCount == story.questions.size) Color(0xFF7E9F85) else Color(0xFFC5B89F)
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
                                                color = Color(0xFF2C2820)
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
                                        color = Color(0xFF2C2820)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    val selectedOpt = userAnswers[q.id]
                                    val isAnswered = selectedOpt != null

                                    q.options.forEachIndexed { optIdx, optText ->
                                        val isSelected = selectedOpt == optIdx
                                        val isCorrectOption = q.correctOptionIndex == optIdx

                                        val bgColor = when {
                                            !isAnswered -> if (isSelected) Color(0xFFDFD4BE) else Color(0xFFF7F2E6)
                                            isCorrectOption -> Color(0xFFD8E8D5)
                                            isSelected && !isCorrectOption -> Color(0xFFF5D6D9)
                                            else -> Color(0xFFF7F2E6)
                                        }

                                        val borderColor = when {
                                            !isAnswered -> if (isSelected) Color(0xFF7E9F85) else Color(0xFFDDD2BC)
                                            isCorrectOption -> Color(0xFF7E9F85)
                                            isSelected && !isCorrectOption -> Color(0xFFCF7B88)
                                            else -> Color(0xFFDDD2BC)
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
                                                    color = Color(0xFF2C2820),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (isAnswered && isCorrectOption) {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = "Correct",
                                                        tint = Color(0xFF4A6B4F),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                } else if (isAnswered && isSelected && !isCorrectOption) {
                                                    Icon(
                                                        Icons.Filled.Close,
                                                        contentDescription = "Incorrect",
                                                        tint = Color(0xFFA84E5B),
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
                                            color = Color(0xFFF2EADC),
                                            border = BorderStroke(1.dp, Color(0xFFDDD2BC)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "💡 ${q.explanation}",
                                                fontSize = 12.sp,
                                                color = Color(0xFF5A5243),
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
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = SakuColors.SurfaceElevated),
                                border = BorderStroke(1.dp, SakuColors.Border),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentStory = item
                                        coroutineScope.launch {
                                            historySheetState.hide()
                                            showHistorySheet = false
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.createdAt)),
                                                fontSize = 11.sp,
                                                color = SakuColors.TextSecondary,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = SakuColors.TextPrimary,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            historyManager.deleteStory(item.id)
                                            savedStories = historyManager.getStories()
                                            if (currentStory?.id == item.id) {
                                                currentStory = savedStories.firstOrNull()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = "Delete Story",
                                            tint = SakuColors.AccentRose
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
}

/**
 * Builds an AnnotatedString with warm amber/peach background highlights and clickable links
 * for vocabulary words appearing in the Japanese story.
 */
private fun buildHighlightedStoryText(
    content: String,
    vocabWords: List<AnkiVocabularyItem>,
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
                    background = Color(0xFFF7D5B5),
                    textDecoration = TextDecoration.Underline,
                    color = Color(0xFF1E1E1E),
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
                            background = Color(0xFFF7D5B5),
                            textDecoration = TextDecoration.Underline,
                            color = Color(0xFF1E1E1E),
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
