package com.saku.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saku.data.JishoWord
import com.saku.data.PreferencesManager
import com.saku.jisho.JishoService
import com.saku.util.JapaneseTtsHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JishoScreen(
    padding: PaddingValues,
    prefs: PreferencesManager,
    initialQuery: String = ""
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val jishoService = remember { JishoService() }
    val ttsHelper = remember { JapaneseTtsHelper(context) }

    DisposableEffect(Unit) {
        onDispose {
            ttsHelper.shutdown()
        }
    }

    var query by remember { mutableStateOf(initialQuery) }
    var results by remember { mutableStateOf<List<JishoWord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var recentSearches by remember { mutableStateOf(prefs.recentJishoSearches) }
    val expandedSlugs = remember { mutableStateListOf<String>() }

    fun executeSearch(searchTerm: String) {
        val trimmed = searchTerm.trim()
        if (trimmed.isBlank()) {
            results = emptyList()
            isLoading = false
            errorMessage = null
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            val res = jishoService.searchWords(trimmed)
            res.onSuccess { list ->
                results = list
                isLoading = false
                if (list.isNotEmpty()) {
                    prefs.addRecentJishoSearch(trimmed)
                    recentSearches = prefs.recentJishoSearches
                }
            }.onFailure { err ->
                errorMessage = err.localizedMessage ?: "Failed to connect to Jisho"
                isLoading = false
            }
        }
    }

    // Debounced search when query changes
    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            isLoading = false
            errorMessage = null
            return@LaunchedEffect
        }
        delay(500)
        executeSearch(query)
    }

    // React to external initialQuery changes (e.g. from Reading tab lookup)
    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && initialQuery != query) {
            query = initialQuery
            executeSearch(initialQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Search Bar Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search Romaji, English, Kanji, Kana...",
                        fontSize = 14.sp,
                        color = SakuColors.TextTertiary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SakuColors.SagePrimary
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            query = ""
                            results = emptyList()
                            errorMessage = null
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = SakuColors.TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SakuColors.SurfaceElevated,
                    unfocusedContainerColor = SakuColors.SurfaceElevated,
                    focusedBorderColor = SakuColors.SagePrimary,
                    unfocusedBorderColor = SakuColors.Border,
                    cursorColor = SakuColors.SagePrimary,
                    focusedTextColor = SakuColors.TextPrimary,
                    unfocusedTextColor = SakuColors.TextPrimary
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        executeSearch(query)
                    }
                )
            )
        }

        // Main Body: Recent/Suggested Searches vs Loading vs Results
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (query.isBlank()) {
                // Empty state: Show Recents + Suggestions
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "RECENT SEARCHES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SakuColors.TextTertiary,
                                    letterSpacing = 0.8.sp
                                )
                                TextButton(
                                    onClick = {
                                        prefs.clearRecentJishoSearches()
                                        recentSearches = emptyList()
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "Clear",
                                        fontSize = 12.sp,
                                        color = SakuColors.SagePrimary
                                    )
                                }
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                recentSearches.forEach { term ->
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = SakuColors.SurfaceElevated,
                                        border = BorderStroke(1.dp, SakuColors.Border),
                                        modifier = Modifier.clickable {
                                            query = term
                                            keyboardController?.hide()
                                            executeSearch(term)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.History,
                                                contentDescription = null,
                                                tint = SakuColors.TextTertiary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = term,
                                                fontSize = 13.sp,
                                                color = SakuColors.TextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    // Popular / Suggested Searches
                    item {
                        Text(
                            text = "SUGGESTED EXPLORATIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SakuColors.TextTertiary,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        val suggested = listOf(
                            "JLPT N5", "JLPT N4", "食べる", "猫", "ありがとう",
                            "桜", "美しい", "勉強", "友だち", "taberu", "house"
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            suggested.forEach { term ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SakuColors.SageContainer.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, SakuColors.SagePrimary.copy(alpha = 0.3f)),
                                    modifier = Modifier.clickable {
                                        query = term
                                        keyboardController?.hide()
                                        executeSearch(term)
                                    }
                                ) {
                                    Text(
                                        text = term,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SakuColors.SagePrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(28.dp))

                        // Info card explaining Jisho flexibility
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SakuColors.SurfaceElevated,
                            border = BorderStroke(1.dp, SakuColors.Border),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = SakuColors.SagePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Universal Search",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = SakuColors.TextPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Type English words ('eat'), Romaji ('taberu'), Kanji ('食べる'), or Kana ('たべる'). You can also search by level, e.g. '#jlpt-n5'.",
                                    fontSize = 13.sp,
                                    color = SakuColors.TextSecondary,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            } else if (isLoading) {
                // Loading indicator
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = SakuColors.SagePrimary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Searching Jisho...",
                        fontSize = 14.sp,
                        color = SakuColors.TextSecondary
                    )
                }
            } else if (errorMessage != null) {
                // Error card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = errorMessage ?: "Failed to connect",
                        fontSize = 14.sp,
                        color = SakuColors.AccentAmber,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedButton(
                        onClick = { executeSearch(query) },
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = SakuColors.SagePrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry")
                    }
                }
            } else if (results.isEmpty()) {
                // Empty search results
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No results found for \"$query\"",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SakuColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try checking the spelling or searching using Romaji or plain English.",
                        fontSize = 13.sp,
                        color = SakuColors.TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            } else {
                // Results List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "${results.size} RESULTS FOR \"$query\"",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SakuColors.TextTertiary,
                            letterSpacing = 0.8.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(results, key = { it.slug + it.primaryReading }) { word ->
                        val isExpanded = expandedSlugs.contains(word.slug)
                        JishoWordCard(
                            word = word,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                if (isExpanded) {
                                    expandedSlugs.remove(word.slug)
                                } else {
                                    expandedSlugs.add(word.slug)
                                }
                            },
                            onPronounce = {
                                ttsHelper.speak(word.primaryWord.ifBlank { word.primaryReading })
                            },
                            onCopy = {
                                val copyContent = if (word.primaryReading.isNotBlank() && word.primaryReading != word.primaryWord) {
                                    "${word.primaryWord} [${word.primaryReading}]"
                                } else {
                                    word.primaryWord
                                }
                                clipboardManager.setText(AnnotatedString(copyContent))
                                Toast.makeText(context, "Copied: $copyContent", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun JishoWordCard(
    word: JishoWord,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPronounce: () -> Unit,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SakuColors.SurfaceElevated,
        border = BorderStroke(1.dp, SakuColors.Border),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Top Row: Word, Reading, Romaji & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Furigana / Kana reading above if kanji present
                    if (word.primaryReading.isNotBlank() && word.primaryReading != word.primaryWord) {
                        Text(
                            text = word.primaryReading,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SakuColors.SagePrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Main Word / Kanji
                    Text(
                        text = word.primaryWord,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SakuColors.TextPrimary
                    )

                    // Romaji reading
                    if (word.romaji.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = word.romaji,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SakuColors.TextTertiary
                        )
                    }
                }

                // Action Icons: Speak, Copy, Expand
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onPronounce,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speak",
                            tint = SakuColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            tint = SakuColors.TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = SakuColors.SagePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Badges Row: Common, JLPT, Wanikani
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (word.isCommon) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF43A047).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF43A047).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "common",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF43A047),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                word.jlptBadge?.let { badge ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SakuColors.SageContainer,
                        border = BorderStroke(1.dp, SakuColors.SagePrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SakuColors.SagePrimary,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                word.tags.filter { it.startsWith("wanikani", ignoreCase = true) }.take(1).forEach { wkTag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFF24E1E).copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color(0xFFF24E1E).copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = wkTag.replace("wanikani", "WK ", ignoreCase = true),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF24E1E),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = SakuColors.Border, thickness = 0.8.dp)

            Spacer(modifier = Modifier.height(10.dp))

            // Definitions section
            val sensesToShow = if (isExpanded) word.senses else word.senses.take(1)

            sensesToShow.forEachIndexed { sIdx, sense ->
                if (sIdx > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Column {
                    // Parts of Speech
                    if (sense.partsOfSpeech.isNotEmpty()) {
                        Text(
                            text = sense.partsOfSpeech.joinToString(", "),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SakuColors.SagePrimary,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    // English definitions
                    val defsText = sense.englishDefinitions.mapIndexed { dIdx, def ->
                        if (sense.englishDefinitions.size > 1) "${dIdx + 1}. $def" else def
                    }.joinToString("; ")

                    Text(
                        text = defsText,
                        fontSize = 14.sp,
                        color = SakuColors.TextPrimary,
                        lineHeight = 20.sp
                    )

                    // Additional sense tags or info (e.g. Usually written using kana alone)
                    if (isExpanded) {
                        val tagsAndInfo = (sense.tags + sense.info).distinct()
                        if (tagsAndInfo.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tagsAndInfo.joinToString(" • "),
                                fontSize = 11.sp,
                                color = SakuColors.TextTertiary,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }

                        if (sense.seeAlso.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "See also: " + sense.seeAlso.joinToString(", "),
                                fontSize = 11.sp,
                                color = SakuColors.TextTertiary
                            )
                        }
                    }
                }
            }

            // Other forms when expanded
            if (isExpanded && word.otherForms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = SakuColors.Border, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "OTHER WRITINGS: " + word.otherForms.joinToString(", "),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = SakuColors.TextTertiary
                )
            }
        }
    }
}
