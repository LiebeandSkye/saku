package com.saku.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saku.anki.ReadingVocabularyExtractor
import com.saku.data.GeneratedStory
import com.saku.data.PreferencesManager
import com.saku.data.ReadingHistoryManager
import com.saku.data.ReadingVocabularySummary
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
    hasAnkiPermission: Boolean
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val historyManager = remember { ReadingHistoryManager(context) }
    val storyService = remember { GeminiStoryService() }
    val vocabExtractor = remember { ReadingVocabularyExtractor(context) }

    // State
    var apiKey by remember { mutableStateOf(prefs.geminiApiKey ?: "") }
    var selectedJlpt by remember { mutableStateOf(prefs.readingJlptLevel) }
    var selectedModel by remember { mutableStateOf(prefs.geminiModel) }
    var showJlptMenu by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }

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
    }

    // History Sheet
    val historySheetState = rememberModalBottomSheetState()
    var showHistorySheet by remember { mutableStateOf(false) }
    var savedStories by remember { mutableStateOf(historyManager.getStories()) }

    // Load initial story from history if available
    LaunchedEffect(Unit) {
        val past = historyManager.getStories()
        savedStories = past
        if (past.isNotEmpty() && currentStory == null) {
            currentStory = past.first()
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
            val words = vocabSummary?.words ?: emptyList()
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Control Bar: JLPT Dropdown, Model & Key Settings
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
            border = BorderStroke(1.dp, SakuColors.Border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // JLPT Level Selector Dropdown
                    Box {
                        Surface(
                            onClick = { showJlptMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            color = SakuColors.SurfaceElevated,
                            border = BorderStroke(1.dp, SakuColors.Border)
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
                                    fontSize = 14.sp
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

                    // API Key & History Action Buttons
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // History Button
                        IconButton(
                            onClick = {
                                savedStories = historyManager.getStories()
                                showHistorySheet = true
                            }
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = "Reading History",
                                tint = if (savedStories.isNotEmpty()) SakuColors.SagePrimary else SakuColors.TextTertiary
                            )
                        }

                        // Key Status / Edit Button
                        Surface(
                            onClick = { showApiKeyDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (apiKey.isNotBlank()) SakuColors.SageContainer else SakuColors.AccentRoseContainer,
                            border = BorderStroke(
                                1.dp,
                                if (apiKey.isNotBlank()) SakuColors.SagePrimary.copy(alpha = 0.4f)
                                else SakuColors.AccentRose.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Key,
                                    contentDescription = null,
                                    tint = if (apiKey.isNotBlank()) SakuColors.SagePrimary else SakuColors.AccentRose,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (apiKey.isNotBlank()) "Key Active" else "Setup Key",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (apiKey.isNotBlank()) SakuColors.SagePrimary else SakuColors.AccentRose
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Model Bar
                Surface(
                    onClick = { showModelDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = SakuColors.SurfaceElevated,
                    border = BorderStroke(1.dp, SakuColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = SakuColors.AccentLavender,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Model: ${PreferencesManager.getModelDisplayName(selectedModel)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = SakuColors.TextPrimary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Switch",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SakuColors.AccentLavender
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = SakuColors.AccentLavender,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
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
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Enter API Key", fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
                                val url = "https://aistudio.google.com/app/apikey"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SakuColors.Border)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = SakuColors.TextPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Get Free Key", color = SakuColors.TextPrimary)
                        }
                    }
                }
            }
        }

        // 3. Vocabulary Summary Card (Studied & Suspended Cards)
        Card(
            shape = RoundedCornerShape(20.dp),
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
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isLoadingVocab) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = SakuColors.SagePrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing AnkiDroid database...", fontSize = 13.sp, color = SakuColors.TextSecondary)
                            }
                        } else {
                            val studied = vocabSummary?.studiedCount ?: 0
                            val suspended = vocabSummary?.suspendedCount ?: 0
                            Text(
                                text = "$studied studied words • $suspended suspended cards",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = SakuColors.TextPrimary
                            )
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
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.displayWord,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (item.isSuspended) SakuColors.AccentAmber else SakuColors.TextPrimary
                                            )
                                            if (item.isSuspended) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "sus",
                                                    fontSize = 9.sp,
                                                    color = SakuColors.AccentAmber
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

        // 4. Generate Story Action Button
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
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SakuColors.SagePrimary,
                contentColor = SakuColors.OnSage
            )
        ) {
            if (isGeneratingStory) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = SakuColors.OnSage
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Crafting $selectedJlpt Japanese Story...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (currentStory == null) "Generate $selectedJlpt Story" else "Generate Another Story",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
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

        // 5. Story Viewer Card (Pure Japanese immersion)
        val story = currentStory
        if (story != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
                border = BorderStroke(1.dp, SakuColors.Border)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Header with JLPT Tag and Copy Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SakuColors.SageContainer,
                            border = BorderStroke(1.dp, SakuColors.SagePrimary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "JLPT ${story.jlptLevel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SakuColors.SagePrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                    tint = SakuColors.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Story Title in Japanese
                    Text(
                        text = story.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SakuColors.TextPrimary,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pure Japanese Story Content
                    Text(
                        text = story.content,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = SakuColors.TextPrimary,
                        lineHeight = 32.sp,
                        letterSpacing = 0.5.sp,
                        fontFamily = FontFamily.Default
                    )

                    // Target Vocabulary from Flashcards integrated into story
                    if (story.targetWords.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SakuColors.SurfaceElevated,
                            border = BorderStroke(1.dp, SakuColors.Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.School,
                                        contentDescription = null,
                                        tint = SakuColors.SagePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "TARGET WORDS FROM YOUR CARDS (${story.targetWords.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SakuColors.TextSecondary,
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
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isPresent) SakuColors.SageContainer else SakuColors.Surface,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isPresent) SakuColors.SagePrimary.copy(alpha = 0.5f)
                                                else SakuColors.Border
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = word,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isPresent) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isPresent) SakuColors.SagePrimary else SakuColors.TextSecondary
                                                )
                                                if (isPresent) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = "Used in story",
                                                        tint = SakuColors.SagePrimary,
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
                }
            }

            // 6. Interactive Reading Comprehension Quiz
            if (story.questions.isNotEmpty()) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
                    border = BorderStroke(1.dp, SakuColors.Border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = SakuColors.AccentLavender,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Reading Comprehension Quiz",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SakuColors.TextPrimary
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
                                        tint = SakuColors.TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reset", color = SakuColors.TextSecondary, fontSize = 12.sp)
                                }
                            }
                        }

                        Text(
                            text = "Test your understanding of the story and vocabulary.",
                            fontSize = 12.sp,
                            color = SakuColors.TextTertiary,
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        val answeredCount = story.questions.count { userAnswers.containsKey(it.id) }
                        val correctCount = story.questions.count { userAnswers[it.id] == it.correctOptionIndex }

                        if (answeredCount == story.questions.size && story.questions.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (correctCount == story.questions.size) SakuColors.SageContainer else SakuColors.SurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (correctCount == story.questions.size) SakuColors.SagePrimary.copy(alpha = 0.5f) else SakuColors.Border
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (correctCount == story.questions.size) "🎉 Perfect! Score: $correctCount / ${story.questions.size}"
                                        else "Score: $correctCount / ${story.questions.size}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = SakuColors.TextPrimary
                                    )
                                }
                            }
                        }

                        story.questions.forEachIndexed { qIdx, q ->
                            if (qIdx > 0) {
                                Spacer(modifier = Modifier.height(18.dp))
                            }
                            Text(
                                text = "${qIdx + 1}. ${q.questionText}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SakuColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val selectedOpt = userAnswers[q.id]
                            val isAnswered = selectedOpt != null

                            q.options.forEachIndexed { optIdx, optText ->
                                val isSelected = selectedOpt == optIdx
                                val isCorrectOption = q.correctOptionIndex == optIdx

                                val bgColor = when {
                                    !isAnswered -> if (isSelected) SakuColors.SageContainer else SakuColors.SurfaceElevated
                                    isCorrectOption -> SakuColors.SageContainer
                                    isSelected && !isCorrectOption -> SakuColors.AccentRoseContainer
                                    else -> SakuColors.SurfaceElevated
                                }

                                val borderColor = when {
                                    !isAnswered -> if (isSelected) SakuColors.SagePrimary else SakuColors.Border
                                    isCorrectOption -> SakuColors.SagePrimary
                                    isSelected && !isCorrectOption -> SakuColors.AccentRose
                                    else -> SakuColors.Border
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
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = optText,
                                            fontSize = 13.sp,
                                            color = SakuColors.TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (isAnswered && isCorrectOption) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = "Correct",
                                                tint = SakuColors.SagePrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else if (isAnswered && isSelected && !isCorrectOption) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Incorrect",
                                                tint = SakuColors.AccentRose,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (isAnswered && q.explanation.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SakuColors.SurfaceElevated,
                                    border = BorderStroke(1.dp, SakuColors.Border),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "💡 ${q.explanation}",
                                        fontSize = 12.sp,
                                        color = SakuColors.TextSecondary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
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
                            Text("Clear All", color = SakuColors.AccentRose, fontSize = 13.sp)
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
                            .height(400.dp),
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
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.createdAt)),
                                                fontSize = 11.sp,
                                                color = SakuColors.TextSecondary
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
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Get Free Key", fontSize = 12.sp, color = SakuColors.SagePrimary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = SakuColors.SagePrimary, modifier = Modifier.size(14.dp))
                        }
                    }

                    if (keyInput.isNotBlank()) {
                        TextButton(
                            onClick = { keyInput = "" }
                        ) {
                            Text("Clear", fontSize = 12.sp, color = SakuColors.AccentRose)
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = SakuColors.TextSecondary)
                    }

                    Button(
                        onClick = { onSave(keyInput.trim()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SakuColors.SagePrimary,
                            contentColor = SakuColors.OnSage
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
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
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = SakuColors.TextSecondary)
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SakuColors.SagePrimary,
                            contentColor = SakuColors.OnSage
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Allow & Continue", fontWeight = FontWeight.Bold)
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
