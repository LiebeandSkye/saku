package com.saku.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.saku.anki.AnkiDroidClient
import com.saku.anki.AnkiDroidContract
import com.saku.anki.AnkiPermissionHelper
import com.saku.data.AnkiDeck
import com.saku.data.CardModel
import com.saku.data.ReviewEase
import com.saku.data.SakuPreferences
import com.saku.notification.LockScreenCardService
import com.saku.widget.SakuGlanceWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SakuPreferences
    private lateinit var ankiClient: AnkiDroidClient

    private var onAnkiPermissionResult: ((Boolean) -> Unit)? = null

    private val requestAnkiPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "AnkiDroid connected successfully!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Permission required to read Anki flashcards", Toast.LENGTH_LONG).show()
        }
        onAnkiPermissionResult?.invoke(isGranted)
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            LockScreenCardService.startService(this)
            Toast.makeText(this, "Lock screen card enabled!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = SakuPreferences(this)
        ankiClient = AnkiDroidClient(this)

        checkAndRequestNotificationPermission()

        if (prefs.isLockScreenCardEnabled) {
            LockScreenCardService.startService(this)
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0A0A0A),
                    surface = Color(0xFF141414),
                    primary = Color(0xFFFFFFFF),
                    onBackground = Color(0xFFFFFFFF),
                    onSurface = Color(0xFFFFFFFF)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var refreshCount by remember { mutableStateOf(0) }

                    DisposableEffect(Unit) {
                        onAnkiPermissionResult = { isGranted ->
                            if (isGranted) {
                                refreshCount++
                            }
                        }
                        onDispose {
                            onAnkiPermissionResult = null
                        }
                    }

                    SakuMainScreen(
                        prefs = prefs,
                        ankiClient = ankiClient,
                        refreshTrigger = refreshCount,
                        onRequestAnkiPermission = {
                            requestAnkiPermissionLauncher.launch(AnkiDroidContract.PERMISSION)
                        },
                        onRequestNotificationPermission = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onInstallAnkiDroid = {
                            AnkiPermissionHelper.openPlayStoreForAnkiDroid(this)
                        },
                        onToggleLockScreen = { enabled ->
                            prefs.isLockScreenCardEnabled = enabled
                            if (enabled) {
                                checkAndRequestNotificationPermission()
                                LockScreenCardService.startService(this)
                            } else {
                                LockScreenCardService.stopService(this)
                            }
                        },
                        onUpdateWidgets = { card ->
                            LockScreenCardService.updateNotification(this, card)
                            lifecycleScope.launch(Dispatchers.IO) {
                                try {
                                    SakuGlanceWidget().updateAll(this@MainActivity)
                                } catch (e: Exception) {
                                    // Widget update safety
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SakuMainScreen(
    prefs: SakuPreferences,
    ankiClient: AnkiDroidClient,
    refreshTrigger: Int = 0,
    onRequestAnkiPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onInstallAnkiDroid: () -> Unit,
    onToggleLockScreen: (Boolean) -> Unit,
    onUpdateWidgets: (CardModel) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isAnkiInstalled by remember { mutableStateOf(ankiClient.isAnkiDroidInstalled()) }
    var hasPermission by remember { mutableStateOf(ankiClient.isPermissionGranted()) }
    var decks by remember { mutableStateOf<List<AnkiDeck>>(emptyList()) }
    var selectedDeckId by remember { mutableStateOf(prefs.selectedDeckId) }
    var activeCard by remember { mutableStateOf(prefs.getActiveCard()) }
    var isLockScreenEnabled by remember { mutableStateOf(prefs.isLockScreenCardEnabled) }
    var isPreviewRevealed by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isAnsweringCard by remember { mutableStateOf(false) }

    fun refreshData(onlyCards: Boolean = false) {
        coroutineScope.launch {
            isLoading = true
            isAnkiInstalled = ankiClient.isAnkiDroidInstalled()
            hasPermission = ankiClient.isPermissionGranted()

            if (hasPermission) {
                if (!onlyCards) {
                    val fetchedDecks = ankiClient.getDecks()
                    decks = fetchedDecks
                }
                val dueCards = ankiClient.getDueCards(selectedDeckId)
                if (dueCards.isNotEmpty()) {
                    activeCard = dueCards.first()
                    prefs.saveActiveCard(activeCard)
                    onUpdateWidgets(activeCard)
                }
            }
            isPreviewRevealed = false
            isLoading = false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                activeCard = prefs.getActiveCard()
                isLockScreenEnabled = prefs.isLockScreenCardEnabled
                selectedDeckId = prefs.selectedDeckId
                if (ankiClient.isPermissionGranted()) {
                    refreshData(onlyCards = true)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(refreshTrigger) {
        refreshData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Saku • 咲く",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp
                    )
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 8.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    }
                    IconButton(onClick = { refreshData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Connection Status Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161616))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "ANKIDROID CONNECTION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF888888),
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        if (!isAnkiInstalled) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFA94D))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("AnkiDroid is not installed on this device", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onInstallAnkiDroid,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                            ) {
                                Text("Install AnkiDroid from Play Store")
                            }
                        } else if (!hasPermission) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFA94D))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Permission required to read your cards", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRequestAnkiPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                            ) {
                                Text("Connect to AnkiDroid (1-Tap)")
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF51CF66))
                                Spacer(modifier = Modifier.width(10.dp))
                                val pkgName = ankiClient.getInstalledAnkiPackage() ?: "AnkiDroid"
                                Text("Connected to $pkgName (FSRS / SM-2 synced)", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 2. Live Widget Preview
            item {
                Text(
                    text = "LIVE WIDGET PREVIEW",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF888888),
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141414), RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0xFF262626), RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dynamic Kanji Box & Font Scaling
                            val kanjiText = activeCard.kanji
                            val kanjiFontSize = when {
                                kanjiText.length > 4 -> 20.sp
                                kanjiText.length > 2 -> 26.sp
                                kanjiText.length == 2 -> 32.sp
                                else -> 38.sp
                            }
                            val kanjiBoxWidth = if (kanjiText.length > 2) (kanjiText.length * 22).dp.coerceIn(68.dp, 120.dp) else 68.dp

                            Box(
                                modifier = Modifier
                                    .size(width = kanjiBoxWidth, height = 68.dp)
                                    .background(Color(0xFF1F1F1F), RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = kanjiText,
                                    fontSize = kanjiFontSize,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            if (!isPreviewRevealed) {
                                // FRONT STATE PREVIEW: Example/Context Prompt
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "EXAMPLE / CONTEXT",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF888888),
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val frontExample = activeCard.exampleSentence.ifEmpty {
                                        activeCard.example.substringBefore("•").trim().ifEmpty { activeCard.kanji }
                                    }
                                    Text(
                                        text = frontExample,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            } else {
                                // BACK STATE PREVIEW: Reading, Meaning, and Full Example with Translation
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = activeCard.kana,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        if (activeCard.romaji.isNotEmpty()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = activeCard.romaji,
                                                fontSize = 14.sp,
                                                color = Color(0xFFAAAAAA)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = activeCard.meaning,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    if (activeCard.example.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = activeCard.example,
                                            fontSize = 13.sp,
                                            color = Color(0xFF888888)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (!isPreviewRevealed) {
                            // "Show Answer" Button (Front State)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF262626))
                                    .clickable { isPreviewRevealed = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Show Answer",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            // Review Ease Buttons: Again, Hard, Good, Easy (Back State)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReviewEase.entries.forEach { ease ->
                                    val color = when (ease) {
                                        ReviewEase.AGAIN -> Color(0xFFFF6B6B)
                                        ReviewEase.HARD -> Color(0xFFFFA94D)
                                        ReviewEase.GOOD -> Color(0xFF51CF66)
                                        ReviewEase.EASY -> Color(0xFF74C0FC)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isAnsweringCard) Color(0xFF1A1A1A) else Color(0xFF242424))
                                            .clickable(enabled = !isAnsweringCard) {
                                                coroutineScope.launch {
                                                    isAnsweringCard = true
                                                    try {
                                                        if (activeCard.noteId > 0) {
                                                            ankiClient.answerCard(activeCard, ease.value)
                                                        }
                                                        val dueCards = ankiClient.getDueCards(selectedDeckId)
                                                        activeCard = dueCards.firstOrNull { it.cardId != activeCard.cardId }
                                                            ?: dueCards.firstOrNull()
                                                            ?: ankiClient.getSamplePreviewCard()
                                                        prefs.saveActiveCard(activeCard)
                                                        isPreviewRevealed = false
                                                        onUpdateWidgets(activeCard)
                                                    } catch (e: Exception) {
                                                        // Error handling for card answer
                                                    } finally {
                                                        isAnsweringCard = false
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = ease.label,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAnsweringCard) color.copy(alpha = 0.4f) else color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Lock Screen & AOD Toggle
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161616))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Lock Screen & AOD Display",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Keep a minimal card pinned on your OxygenOS Lock Screen and Always-On Display",
                                fontSize = 12.sp,
                                color = Color(0xFF888888)
                            )
                        }
                        Switch(
                            checked = isLockScreenEnabled,
                            onCheckedChange = {
                                isLockScreenEnabled = it
                                onToggleLockScreen(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF333333)
                            )
                        )
                    }
                }
            }

            // 4. Deck Selection
            item {
                Text(
                    text = "SELECT DECK",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF888888),
                    letterSpacing = 1.2.sp
                )
            }

            if (decks.isEmpty()) {
                item {
                    Text(
                        text = if (hasPermission) "No decks found. Please make sure you have cards in AnkiDroid and that AnkiDroid Third-party API is accessible." else "Connect to AnkiDroid above to select your decks.",
                        fontSize = 13.sp,
                        color = Color(0xFF9E9E9E)
                    )
                }
            } else {
                // "All Decks" option
                item {
                    val isAllSelected = selectedDeckId == -1L
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedDeckId = -1L
                                prefs.selectedDeckId = -1L
                                prefs.selectedDeckName = "All Decks"
                                refreshData(onlyCards = true)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAllSelected) Color(0xFF222222) else Color(0xFF141414)
                        ),
                        border = if (isAllSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.White) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "All Decks (Combined Due)",
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                            if (isAllSelected) {
                                Text(
                                    text = "Active",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF51CF66)
                                )
                            }
                        }
                    }
                }

                items(decks, key = { it.id }) { deck ->
                    val isSelected = deck.id == selectedDeckId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedDeckId = deck.id
                                prefs.selectedDeckId = deck.id
                                prefs.selectedDeckName = deck.name
                                refreshData(onlyCards = true)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF222222) else Color(0xFF141414)
                        ),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.White) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = deck.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp
                                )
                                if (deck.dueCardCount > 0) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${deck.dueCardCount} cards due",
                                        fontSize = 11.sp,
                                        color = Color(0xFF888888)
                                    )
                                }
                            }
                            if (isSelected) {
                                Text(
                                    text = "Active",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF51CF66)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
