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

    var currentStory by remember { mutableStateOf<GeneratedStory?>(null) }
    var isGeneratingStory by remember { mutableStateOf(false) }
    var generationError by remember { mutableStateOf<String?>(null) }
    var showInternetConsentDialog by remember { mutableStateOf(false) }

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

    // Load vocabulary stats when screen opens or permission is granted
    fun loadVocabulary() {
        if (!hasAnkiPermission) return
        coroutineScope.launch {
            isLoadingVocab = true
            val deckIds = prefs.getSelectedDeckIdsAsLongs()
            vocabSummary = vocabExtractor.extractVocabulary(deckIds)
            isLoadingVocab = false
        }
    }

    LaunchedEffect(hasAnkiPermission) {
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
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            border = BorderStroke(1.dp, Color(0xFF262626))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
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
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.School,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Level: $selectedJlpt",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showJlptMenu,
                            onDismissRequest = { showJlptMenu = false }
                        ) {
                            jlptLevels.forEach { level ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "$level ${getJlptLabel(level)}",
                                            fontWeight = if (level == selectedJlpt) FontWeight.Bold else FontWeight.Normal,
                                            color = if (level == selectedJlpt) Color(0xFF38BDF8) else Color.White
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
                                tint = if (savedStories.isNotEmpty()) Color(0xFF38BDF8) else Color(0xFF888888)
                            )
                        }

                        // Key Status / Edit Button
                        Surface(
                            onClick = { showApiKeyDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (apiKey.isNotBlank()) Color(0xFF162520) else Color(0xFF2D1B1B),
                            border = BorderStroke(
                                1.dp,
                                if (apiKey.isNotBlank()) Color(0xFF10B981).copy(alpha = 0.5f)
                                else Color(0xFFEF4444).copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Key,
                                    contentDescription = null,
                                    tint = if (apiKey.isNotBlank()) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (apiKey.isNotBlank()) "Key Active" else "Setup Key",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (apiKey.isNotBlank()) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Model Bar
                Surface(
                    onClick = { showModelDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1E1829),
                    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Model: ${PreferencesManager.getModelDisplayName(selectedModel)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Switch",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD8B4FE)
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                tint = Color(0xFFD8B4FE),
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
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1424)),
                border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFA855F7))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GEMINI API KEY REQUIRED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA855F7),
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To generate personalized Japanese reading stories based on your Anki flashcards, connect your free Google Gemini API key.",
                        fontSize = 13.sp,
                        color = Color(0xFFCBD5E1),
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
                                containerColor = Color(0xFFA855F7),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
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
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.6f))
                        ) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Get Free Key", color = Color.White)
                        }
                    }
                }
            }
        }

        // 3. Vocabulary Summary Card (Studied & Suspended Cards)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
            border = BorderStroke(1.dp, Color(0xFF262626))
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
                            color = Color(0xFF888888),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (isLoadingVocab) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = Color(0xFF38BDF8))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing AnkiDroid database...", fontSize = 13.sp, color = Color.LightGray)
                            }
                        } else {
                            val studied = vocabSummary?.studiedCount ?: 0
                            val suspended = vocabSummary?.suspendedCount ?: 0
                            Text(
                                text = "$studied studied words • $suspended suspended cards",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { loadVocabulary() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh Vocab", tint = Color(0xFF888888), modifier = Modifier.size(20.dp))
                        }
                        Icon(
                            if (isVocabExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = Color.White
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
                            else "No studied cards found in selected decks yet.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = "These words will be injected into AI story generation:",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            words.take(60).forEach { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (item.isSuspended) Color(0xFF2E201B) else Color(0xFF1E293B),
                                    border = BorderStroke(
                                        1.dp,
                                        if (item.isSuspended) Color(0xFFF97316).copy(alpha = 0.4f)
                                        else Color(0xFF38BDF8).copy(alpha = 0.3f)
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
                                            color = if (item.isSuspended) Color(0xFFFDBA74) else Color(0xFFBAE6FD)
                                        )
                                        if (item.isSuspended) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "sus",
                                                fontSize = 9.sp,
                                                color = Color(0xFFF97316)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (words.size > 60) {
                            Text(
                                text = "+ ${words.size - 60} more words available in pool",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 6.dp)
                            )
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
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF38BDF8),
                contentColor = Color.Black
            )
        ) {
            if (isGeneratingStory) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = Color.Black
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
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF331515)),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = generationError ?: "Error generating story",
                        fontSize = 13.sp,
                        color = Color(0xFFFCA5A5)
                    )
                }
            }
        }

        // 5. Story Viewer Card (Pure Japanese immersion)
        val story = currentStory
        if (story != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
                border = BorderStroke(1.dp, Color(0xFF262626))
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
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "JLPT ${story.jlptLevel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
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
                                    tint = Color.LightGray,
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
                        color = Color.White,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Pure Japanese Story Content
                    Text(
                        text = story.content,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 32.sp,
                        letterSpacing = 0.5.sp,
                        fontFamily = FontFamily.Default
                    )
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
                        tint = Color(0xFF333333),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No story generated yet",
                        color = Color(0xFF666666),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Pick your JLPT level and tap Generate Story above",
                        color = Color(0xFF444444),
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
            containerColor = Color(0xFF141414),
            contentColor = Color.White
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
                        color = Color.White
                    )
                    if (savedStories.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                historyManager.clearAll()
                                savedStories = emptyList()
                            }
                        ) {
                            Text("Clear All", color = Color(0xFFEF4444), fontSize = 13.sp)
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
                        Text("No saved stories yet", color = Color.Gray, fontSize = 14.sp)
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
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
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
                                                color = Color(0xFF0F172A)
                                            ) {
                                                Text(
                                                    text = item.jlptLevel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF38BDF8),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.createdAt)),
                                                fontSize = 11.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = Color.White,
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
                                            tint = Color(0xFFEF4444)
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
            border = BorderStroke(1.dp, Color(0xFF333333)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Key, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Gemini API Key",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                Text(
                    text = "Your key is stored strictly on your device. It is used solely to generate reading immersion stories.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    placeholder = { Text("AIzaSy...", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF444444),
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
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
                            Text("Get Free Key", fontSize = 12.sp, color = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                        }
                    }

                    if (keyInput.isNotBlank()) {
                        TextButton(
                            onClick = { keyInput = "" }
                        ) {
                            Text("Clear", fontSize = 12.sp, color = Color(0xFFEF4444))
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
                        border = BorderStroke(1.dp, Color(0xFF444444)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = { onSave(keyInput.trim()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color.Black
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
            border = BorderStroke(1.dp, Color(0xFF333333)),
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
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Enable Internet for AI?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }

                Text(
                    text = "Saku is 100% offline for flashcards and widgets.\n\nAI Reading connects directly to Google's Gemini API with your private API key to compose custom Japanese stories.\n\nOnly vocabulary from your cards is sent for prompt generation. No personal data, passwords, or tracking telemetry are ever sent.",
                    fontSize = 13.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF444444)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8),
                            contentColor = Color.Black
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
