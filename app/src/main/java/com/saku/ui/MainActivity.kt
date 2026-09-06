package com.saku.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.util.lerp
import kotlin.math.absoluteValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.saku.anki.AnkiDroidHelper
import com.saku.data.CardInfo
import com.saku.data.CardSessionManager
import com.saku.data.DeckInfo
import com.saku.data.PreferencesManager
import com.saku.notification.LockScreenCardService
import com.saku.util.MediaArtworkGenerator
import com.saku.widget.SakuWidgetProvider
import com.saku.worker.DueCountWorker
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private lateinit var ankiHelper: AnkiDroidHelper
    private lateinit var prefs: PreferencesManager

    private var isAnkiInstalledState by mutableStateOf(false)
    private var hasPermissionState by mutableStateOf(false)
    private var decksState by mutableStateOf<List<DeckInfo>>(emptyList())
    private var backgroundTypeState by mutableStateOf("anki_lock")
    private var customImageUriState by mutableStateOf<String?>(null)
    private var savedImageUrisState by mutableStateOf<Set<String>>(emptySet())
    private var blurRadiusState by mutableFloatStateOf(20f)
    private var dimOpacityState by mutableFloatStateOf(0.10f)
    private var artworkOpacityState by mutableFloatStateOf(0.5f)

    private val ankiPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermissionState = granted
        if (granted) {
            Toast.makeText(this, "AnkiDroid connected successfully!", Toast.LENGTH_SHORT).show()
            refreshData()
            if (prefs.isServiceEnabled) {
                LockScreenCardService.updateNotification(this)
            }
        } else {
            Toast.makeText(this, "Permission required to read Anki flashcards", Toast.LENGTH_LONG).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && prefs.isServiceEnabled) {
            LockScreenCardService.startService(this)
        }
        checkAndRequestAnkiPermission()
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val savedPath = saveCustomImageToInternalStorage(uri)
            if (savedPath != null) {
                prefs.addSavedImageUri(savedPath)
                prefs.customImageUri = savedPath
                prefs.backgroundType = "custom"

                savedImageUrisState = prefs.savedImageUris
                customImageUriState = savedPath
                backgroundTypeState = "custom"

                CardSessionManager.notifyAllSurfaces(this)
                Toast.makeText(this, "Custom wallpaper added!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveCustomImageToInternalStorage(uri: Uri): String? {
        return try {
            val backgroundsDir = File(filesDir, "custom_backgrounds").apply { mkdirs() }
            val destFile = File(backgroundsDir, "bg_${System.currentTimeMillis()}.jpg")

            // First read image bounds without loading into memory
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val maxDimension = 1280
            var inSampleSize = 1
            if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
                val halfWidth = options.outWidth / 2
                val halfHeight = options.outHeight / 2
                while ((halfWidth / inSampleSize) >= maxDimension && (halfHeight / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decodedBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val finalBitmap = if (decodedBitmap.width > maxDimension || decodedBitmap.height > maxDimension) {
                val ratio = maxDimension.toFloat() / max(decodedBitmap.width, decodedBitmap.height)
                val targetW = (decodedBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetH = (decodedBitmap.height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decodedBitmap, targetW, targetH, true)
                if (scaled != decodedBitmap) {
                    decodedBitmap.recycle()
                }
                scaled
            } else {
                decodedBitmap
            }

            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            finalBitmap.recycle()

            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ankiHelper = AnkiDroidHelper(this)
        prefs = PreferencesManager(this)

        backgroundTypeState = prefs.backgroundType
        customImageUriState = prefs.customImageUri
        savedImageUrisState = prefs.savedImageUris
        blurRadiusState = prefs.blurRadius.toFloat()
        dimOpacityState = prefs.dimOpacity
        artworkOpacityState = prefs.artworkOpacity

        requestInitialPermissions()

        if (prefs.isServiceEnabled) {
            LockScreenCardService.startService(this)
            DueCountWorker.schedule(this, prefs.updateIntervalMinutes.toLong())
        }

        setContent {
            SakuTheme {
                MainContainer()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    private fun refreshData() {
        isAnkiInstalledState = ankiHelper.isAnkiDroidInstalled()
        hasPermissionState = ankiHelper.hasApiPermission()
        if (hasPermissionState) {
            decksState = ankiHelper.getDeckList()
            CardSessionManager.getOrFetchCard(this, forceRefresh = true)
        }
        backgroundTypeState = prefs.backgroundType
        customImageUriState = prefs.customImageUri
        savedImageUrisState = prefs.savedImageUris
        blurRadiusState = prefs.blurRadius.toFloat()
        dimOpacityState = prefs.dimOpacity
        artworkOpacityState = prefs.artworkOpacity
    }

    private fun syncCardSession(card: CardInfo?) {
        if (card == null) return
        try {
            val field = CardSessionManager::class.java.getDeclaredField("currentCard")
            field.isAccessible = true
            field.set(null, card)
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }

    private fun requestInitialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
                return
            }
        }
        checkAndRequestAnkiPermission()
    }

    private fun checkAndRequestAnkiPermission() {
        if (!ankiHelper.hasApiPermission()) {
            ankiPermissionLauncher.launch(
                AnkiDroidHelper.PERMISSION_READ_WRITE_DATABASE
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainContainer() {
        var isRefreshing by remember { mutableStateOf(false) }
        var currentTab by remember { mutableIntStateOf(0) }
        var openHistoryTrigger by remember { mutableIntStateOf(0) }
        var isDockVisible by remember { mutableStateOf(true) }
        val coroutineScope = rememberCoroutineScope()

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < -12f) {
                        isDockVisible = false
                    } else if (available.y > 12f) {
                        isDockVisible = true
                    }
                    return Offset.Zero
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SakuColors.Background)
                .nestedScroll(nestedScrollConnection)
        ) {
            // Opened book backdrop anchored at top for the Reading tab
            if (currentTab == 1) {
                Image(
                    painter = painterResource(id = com.saku.R.drawable.bg_reading_book),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                        .graphicsLayer { alpha = 0.42f }
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp)
                        .background(
                            Brush.verticalGradient(
                                0.0f to SakuColors.Background.copy(alpha = 0.15f),
                                0.60f to SakuColors.Background.copy(alpha = 0.70f),
                                1.0f to SakuColors.Background
                            )
                        )
                )
            }

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (prefs.isServiceEnabled) SakuColors.SagePrimary
                                             else SakuColors.AccentRose
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    if (currentTab == 0) "Saku • 咲く" else "Saku • 読書",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    letterSpacing = 0.6.sp,
                                    color = SakuColors.TextPrimary
                                )
                            }
                        },
                        actions = {
                            // Paper icon for Reading History on the Reading tab
                            if (currentTab == 1) {
                                IconButton(onClick = {
                                    openHistoryTrigger++
                                }) {
                                    Icon(
                                        Icons.Filled.Description,
                                        contentDescription = "Reading History",
                                        tint = SakuColors.TextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .padding(end = 6.dp),
                                    color = SakuColors.SagePrimary,
                                    strokeWidth = 2.dp
                                )
                            }
                            IconButton(onClick = {
                                coroutineScope.launch {
                                    isRefreshing = true
                                    refreshData()
                                    CardSessionManager.refresh(this@MainActivity)
                                    isRefreshing = false
                                }
                            }) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Refresh",
                                    tint = SakuColors.TextSecondary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = if (currentTab == 1) Color.Transparent else SakuColors.Background,
                            titleContentColor = SakuColors.TextPrimary,
                            actionIconContentColor = SakuColors.TextSecondary
                        )
                    )
                }
            ) { padding ->
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        val direction = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> direction * (fullWidth * 0.45f).toInt() },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + scaleIn(
                            initialScale = 0.90f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(
                            animationSpec = tween(160, easing = FastOutSlowInEasing)
                        )).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -direction * (fullWidth * 0.28f).toInt() },
                                animationSpec = tween(140, easing = FastOutSlowInEasing)
                            ) + scaleOut(
                                targetScale = 0.95f,
                                animationSpec = tween(140)
                            ) + fadeOut(
                                animationSpec = tween(110)
                            )
                        )
                    },
                    label = "ScreenSwitchBubbly"
                ) { tab ->
                    if (tab == 0) {
                        ModernSettingsScreen(padding)
                    } else {
                        ReadingScreen(
                            padding = padding,
                            prefs = prefs,
                            hasAnkiPermission = hasPermissionState,
                            openHistoryTrigger = openHistoryTrigger,
                            onHistoryTriggerConsumed = { openHistoryTrigger = 0 }
                        )
                    }
                }
            }

            // Floating Bottom Dock Pill: statically floating, hides on scroll down, reappears on scroll up
            AnimatedVisibility(
                visible = isDockVisible,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight + 120 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(180)),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight + 120 },
                    animationSpec = tween(220, easing = FastOutLinearInEasing)
                ) + fadeOut(animationSpec = tween(150)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                BubblyFloatingNav(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            }
        }
    }

    @Composable
    fun BubblyFloatingNav(
        currentTab: Int,
        onTabSelected: (Int) -> Unit
    ) {
        val tabs = listOf(
            Pair("Cards", Icons.Filled.Style),
            Pair("Reading", Icons.Filled.AutoStories)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidGlassBox(
                shape = RoundedCornerShape(32.dp),
                cornerRadius = 32.dp,
                tintColor = Color.White.copy(alpha = 0.16f),
                darkBaseAlpha = 0.60f,
                specularAlpha = 0.60f,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(5.dp)
                ) {
                    val tabWidth = maxWidth / tabs.size
                    val indicatorOffset by animateDpAsState(
                        targetValue = tabWidth * currentTab,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "BubblyNavIndicatorOffset"
                    )

                    // Bubbly sliding indicator pill with liquid glass sheen
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(tabWidth)
                            .fillMaxHeight()
                            .liquidGlass(
                                shape = RoundedCornerShape(26.dp),
                                cornerRadius = 26.dp,
                                tintColor = SakuColors.SagePrimary.copy(alpha = 0.40f),
                                darkBaseAlpha = 0.45f,
                                specularAlpha = 0.70f,
                                shadowElevation = 4.dp
                            )
                    )

                    // Nav Tab items
                    Row(modifier = Modifier.fillMaxSize()) {
                        tabs.forEachIndexed { index, (label, icon) ->
                            val isSelected = currentTab == index
                            val interactionSource = remember { MutableInteractionSource() }
                            val isPressed by interactionSource.collectIsPressedAsState()

                            val scale by animateFloatAsState(
                                targetValue = if (isPressed) 0.88f else if (isSelected) 1.03f else 1.0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "BubblyTabScale"
                            )

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(26.dp))
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = null
                                    ) {
                                        onTabSelected(index)
                                    }
                                    .scale(scale),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) SakuColors.SageLight else SakuColors.TextMuted,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) SakuColors.TextPrimary else SakuColors.TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ModernSettingsScreen(padding: PaddingValues) {
        var isEnabled by remember { mutableStateOf(prefs.isServiceEnabled) }
        var classicRevealedAction by remember { mutableStateOf(prefs.classicRevealedAction) }
        val selectedDeckIds = remember { mutableStateListOf<String>() }
        var updateInterval by remember { mutableIntStateOf(prefs.updateIntervalMinutes) }
        var snoozeDuration by remember { mutableIntStateOf(prefs.snoozeDurationMinutes) }

        var activeCard by remember { mutableStateOf(CardSessionManager.getOrFetchCard(this@MainActivity)) }
        var isRevealed by remember { mutableStateOf(CardSessionManager.isRevealed) }
        var stats by remember { mutableStateOf(CardSessionManager.currentStats) }

        DisposableEffect(Unit) {
            val listener = {
                activeCard = CardSessionManager.currentCard
                isRevealed = CardSessionManager.isRevealed
                stats = CardSessionManager.currentStats
            }
            CardSessionManager.addListener(listener)
            onDispose {
                CardSessionManager.removeListener(listener)
            }
        }

        remember {
            selectedDeckIds.clear()
            selectedDeckIds.addAll(prefs.selectedDeckIds)
            true
        }

        val coroutineScope = rememberCoroutineScope()
        val selectedDecksList = remember(decksState, selectedDeckIds.toList()) {
            val selectedIds = selectedDeckIds.mapNotNull { it.toLongOrNull() }.toSet()
            if (selectedIds.isNotEmpty()) {
                decksState.filter { it.id in selectedIds }
            } else {
                decksState
            }.ifEmpty {
                listOf(
                    DeckInfo(
                        id = -1L,
                        name = if (!hasPermissionState) "Connect AnkiDroid" else "No Decks Selected",
                        newCount = 0,
                        learnCount = 0,
                        reviewCount = 0
                    )
                )
            }
        }

        val deckCardsCache = remember { mutableStateMapOf<Long, CardInfo?>() }
        var currentCenteredDeckId by remember { mutableStateOf(selectedDecksList.firstOrNull()?.id ?: -1L) }

        LaunchedEffect(selectedDecksList) {
            withContext(Dispatchers.IO) {
                for (deck in selectedDecksList) {
                    if (deck.id > 0 && !deckCardsCache.containsKey(deck.id)) {
                        val card = ankiHelper.getNextDueCard(setOf(deck.id))
                        withContext(Dispatchers.Main) {
                            deckCardsCache[deck.id] = card
                        }
                    }
                }
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
            // 1. Connection Status (if not connected)
            if (!isAnkiInstalledState || !hasPermissionState) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
                    border = BorderStroke(1.dp, SakuColors.AccentAmber.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = SakuColors.AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ANKIDROID SYNC",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SakuColors.AccentAmber,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (!isAnkiInstalledState) {
                            Text(
                                "AnkiDroid is required to sync flashcards and algorithm progress.",
                                fontSize = 13.5.sp,
                                color = SakuColors.TextSecondary,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.ichi2.anki")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    try {
                                        startActivity(intent)
                                    } catch (e: Exception) {
                                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.ichi2.anki")))
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SakuColors.SagePrimary,
                                    contentColor = SakuColors.OnSage
                                )
                            ) {
                                Text("Install AnkiDroid", fontWeight = FontWeight.SemiBold)
                            }
                        } else if (!hasPermissionState) {
                            Text(
                                "Grant read/write permission so Saku can display your scheduled reviews.",
                                fontSize = 13.5.sp,
                                color = SakuColors.TextSecondary,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { checkAndRequestAnkiPermission() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SakuColors.SagePrimary,
                                    contentColor = SakuColors.OnSage
                                )
                            ) {
                                Text("Connect AnkiDroid (1-Tap)", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // 2. Hero Card: Lockscreen Service Active Switch
            ModernHeroCard(
                isEnabled = isEnabled,
                onToggle = { enabled ->
                    isEnabled = enabled
                    prefs.isServiceEnabled = enabled
                    if (enabled) {
                        requestInitialPermissions()
                        LockScreenCardService.startService(this@MainActivity)
                        DueCountWorker.schedule(this@MainActivity, updateInterval.toLong())
                    } else {
                        LockScreenCardService.stopService(this@MainActivity)
                        DueCountWorker.cancel(this@MainActivity)
                    }
                    SakuWidgetProvider.updateAllWidgets(this@MainActivity)
                }
            )

            // 3. Flashcard Deck Carousel
            DeckCarouselCard(
                decks = selectedDecksList,
                activeCard = activeCard,
                stats = stats,
                isRevealed = isRevealed,
                deckCardsCache = deckCardsCache,
                onToggleReveal = { CardSessionManager.toggleReveal(this@MainActivity) },
                onRefresh = {
                    CardSessionManager.refresh(this@MainActivity)
                    deckCardsCache.clear()
                    coroutineScope.launch(Dispatchers.IO) {
                        for (deck in selectedDecksList) {
                            if (deck.id > 0) {
                                val card = ankiHelper.getNextDueCard(setOf(deck.id))
                                withContext(Dispatchers.Main) {
                                    deckCardsCache[deck.id] = card
                                }
                            }
                        }
                    }
                },
                onAgain = {
                    val deckId = currentCenteredDeckId
                    val oldCard = deckCardsCache[deckId] ?: activeCard
                    if (oldCard != null && deckId > 0) {
                        syncCardSession(oldCard)
                        CardSessionManager.gradeCard(this@MainActivity, 1) {
                            Thread {
                                val nextCard = ankiHelper.getNextDueCard(setOf(deckId), excludeNoteId = oldCard.noteId)
                                runOnUiThread {
                                    deckCardsCache[deckId] = nextCard
                                    if (nextCard != null) {
                                        syncCardSession(nextCard)
                                    }
                                }
                            }.start()
                        }
                    }
                },
                onGood = {
                    val deckId = currentCenteredDeckId
                    val oldCard = deckCardsCache[deckId] ?: activeCard
                    if (oldCard != null && deckId > 0) {
                        syncCardSession(oldCard)
                        CardSessionManager.gradeCard(this@MainActivity, 3) {
                            Thread {
                                val nextCard = ankiHelper.getNextDueCard(setOf(deckId), excludeNoteId = oldCard.noteId)
                                runOnUiThread {
                                    deckCardsCache[deckId] = nextCard
                                    if (nextCard != null) {
                                        syncCardSession(nextCard)
                                    }
                                }
                            }.start()
                        }
                    }
                },
                onOpenAnki = {
                    val launchIntent = ankiHelper.getAnkiLaunchIntent()
                    startActivity(launchIntent)
                },
                onDeckChanged = { deckId ->
                    currentCenteredDeckId = deckId
                    CardSessionManager.hide(this@MainActivity)
                    if (deckId > 0) {
                        if (!deckCardsCache.containsKey(deckId)) {
                            coroutineScope.launch(Dispatchers.IO) {
                                val card = ankiHelper.getNextDueCard(setOf(deckId))
                                withContext(Dispatchers.Main) {
                                    deckCardsCache[deckId] = card
                                    syncCardSession(card)
                                }
                            }
                        } else {
                            syncCardSession(deckCardsCache[deckId])
                        }
                    }
                }
            )

            // 4. Background Studio Card (Customizable Widget & Artwork Wallpapers)
            ModernBackgroundStudioCard(
                currentType = backgroundTypeState,
                blurRadius = blurRadiusState,
                dimOpacity = dimOpacityState,
                artworkOpacity = artworkOpacityState,
                savedUris = savedImageUrisState,
                currentUri = customImageUriState,
                onSelectType = { type ->
                    backgroundTypeState = type
                    prefs.backgroundType = type
                    CardSessionManager.notifyAllSurfaces(this@MainActivity)
                },
                onSelectSavedUri = { uriStr ->
                    customImageUriState = uriStr
                    prefs.customImageUri = uriStr
                    backgroundTypeState = "custom"
                    prefs.backgroundType = "custom"
                    CardSessionManager.notifyAllSurfaces(this@MainActivity)
                },
                onRemoveSavedUri = { uriStr ->
                    try {
                        val file = File(uriStr)
                        if (file.exists() && file.absolutePath.contains("custom_backgrounds")) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                    }
                    prefs.removeSavedImageUri(uriStr)
                    savedImageUrisState = prefs.savedImageUris
                    customImageUriState = prefs.customImageUri
                    backgroundTypeState = prefs.backgroundType
                    CardSessionManager.notifyAllSurfaces(this@MainActivity)
                },
                onPickNewImage = {
                    imagePickerLauncher.launch("image/*")
                },
                onBlurChange = { newRadius ->
                    blurRadiusState = newRadius
                },
                onBlurCommit = {
                    prefs.blurRadius = blurRadiusState.toInt()
                    CardSessionManager.notifyAllSurfaces(this@MainActivity)
                },
                onOpacityChange = { newOpacity ->
                    dimOpacityState = newOpacity
                },
                onOpacityCommit = {
                    prefs.dimOpacity = dimOpacityState
                    CardSessionManager.notifyAllSurfaces(this@MainActivity)
                },
                onArtworkOpacityChange = { newArtOpacity ->
                    artworkOpacityState = newArtOpacity
                },
                onArtworkOpacityCommit = {
                    prefs.artworkOpacity = artworkOpacityState
                    CardSessionManager.notifyAllSurfaces(this@MainActivity)
                }
            )

            // 5. Classic Action on Revealed Card
            ModernClassicActionCard(classicRevealedAction) { action ->
                classicRevealedAction = action
                prefs.classicRevealedAction = action
                if (isEnabled) LockScreenCardService.updateNotification(this@MainActivity)
            }

            // 6. Deck Selection Card
            if (decksState.isNotEmpty()) {
                ModernDeckSelectorCard(decksState, selectedDeckIds) { deckId, checked ->
                    if (checked) {
                        selectedDeckIds.add(deckId)
                    } else {
                        selectedDeckIds.remove(deckId)
                    }
                    prefs.selectedDeckIds = selectedDeckIds.toSet()
                    CardSessionManager.refresh(this@MainActivity)
                }
            }

            // 7. Interval & Snooze Settings
            ModernIntervalCard(
                updateMinutes = updateInterval,
                snoozeMinutes = snoozeDuration,
                onUpdateSelect = { minutes ->
                    updateInterval = minutes
                    prefs.updateIntervalMinutes = minutes
                    if (isEnabled) DueCountWorker.schedule(this@MainActivity, minutes.toLong())
                },
                onSnoozeSelect = { minutes ->
                    snoozeDuration = minutes
                    prefs.snoozeDurationMinutes = minutes
                }
            )

            // 8. Gemini AI & Reading Settings
            ModernGeminiSettingsCard()

            Spacer(modifier = Modifier.height(96.dp))
        }
    }

    @Composable
    fun ModernHeroCard(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
        LiquidGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            cornerRadius = 24.dp,
            tintColor = if (isEnabled) SakuColors.SagePrimary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.12f),
            darkBaseAlpha = 0.58f,
            specularAlpha = 0.60f,
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isEnabled) "Lock Screen Card Active" else "Lock Screen Card Paused",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = SakuColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (isEnabled) "Flashcards active on Lock Screen & AOD"
                            else "Display flashcards when phone turns on",
                            fontSize = 12.5.sp,
                            color = SakuColors.TextSecondary
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = SakuColors.SagePrimary,
                            checkedThumbColor = SakuColors.OnSage,
                            uncheckedTrackColor = SakuColors.SurfaceVariant,
                            uncheckedThumbColor = SakuColors.TextMuted,
                            uncheckedBorderColor = SakuColors.Border
                        )
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun DeckCarouselCard(
        decks: List<DeckInfo>,
        activeCard: CardInfo?,
        stats: Triple<Int, Int, Int>,
        isRevealed: Boolean,
        deckCardsCache: SnapshotStateMap<Long, CardInfo?>,
        onToggleReveal: () -> Unit,
        onRefresh: () -> Unit,
        onAgain: () -> Unit,
        onGood: () -> Unit,
        onOpenAnki: () -> Unit,
        onDeckChanged: (Long) -> Unit
    ) {
        val actualPageCount = decks.size.coerceAtLeast(1)
        val virtualPageCount = actualPageCount * 10000
        val initialPage = (virtualPageCount / 2) - ((virtualPageCount / 2) % actualPageCount)
        val pagerState = rememberPagerState(
            initialPage = initialPage,
            pageCount = { virtualPageCount }
        )

        val decksKey = remember(decks) { decks.map { it.id } }
        LaunchedEffect(decksKey) {
            if (pagerState.currentPage >= virtualPageCount) {
                pagerState.scrollToPage(initialPage)
            }
        }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }
                .collect { page ->
                    val actualIndex = page % actualPageCount
                    val deck = decks.getOrNull(actualIndex)
                    if (deck != null) {
                        onDeckChanged(deck.id)
                    }
                }
        }

        val currentCenterIndex = pagerState.currentPage % actualPageCount
        val currentCenterDeck = decks.getOrNull(currentCenterIndex)
        val centerCard = if (currentCenterDeck != null) {
            deckCardsCache[currentCenterDeck.id] ?: if (currentCenterIndex == 0) activeCard else null
        } else null

        LiquidGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            cornerRadius = 24.dp,
            tintColor = Color.White.copy(alpha = 0.12f),
            darkBaseAlpha = 0.65f,
            specularAlpha = 0.50f,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                // Header row with title and refresh
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = SakuColors.SagePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Live Card Preview",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SakuColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = SakuColors.TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // === THE CAROUSEL ===
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds()
                ) {
                    // Ambient colorful glow behind the cards to give liquid glass rich refraction
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.90f)
                            .height(290.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        SakuColors.SagePrimary.copy(alpha = 0.22f),
                                        SakuColors.AccentSlateBlue.copy(alpha = 0.12f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    HorizontalPager(
                        state = pagerState,
                        contentPadding = PaddingValues(horizontal = 48.dp),
                        pageSpacing = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val actualIndex = page % actualPageCount
                        val deck = decks[actualIndex]
                        val cardForDeck = deckCardsCache[deck.id] ?: if (actualIndex == 0) activeCard else null
                        val isCenterPage = pagerState.currentPage == page

                        val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
                        val scale = lerp(0.85f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
                        val alpha = lerp(0.45f, 1f, 1f - pageOffset.coerceIn(0f, 1f))

                        GlassFlashcard(
                            deckName = deck.name,
                            deckId = deck.id,
                            card = cardForDeck,
                            isRevealed = isRevealed && isCenterPage,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                                .clickable(
                                    enabled = isCenterPage && cardForDeck != null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onToggleReveal()
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // === ACTION BUTTONS ===
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiquidGlassButton(
                        onClick = onAgain,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(14.dp),
                        cornerRadius = 14.dp,
                        tintColor = SakuColors.AccentRose.copy(alpha = 0.40f),
                        darkBaseAlpha = 0.55f,
                        specularAlpha = 0.60f,
                        enabled = centerCard != null
                    ) {
                        Text("Again", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    LiquidGlassButton(
                        onClick = onToggleReveal,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(40.dp),
                        shape = RoundedCornerShape(14.dp),
                        cornerRadius = 14.dp,
                        tintColor = SakuColors.SagePrimary.copy(alpha = 0.45f),
                        darkBaseAlpha = 0.50f,
                        specularAlpha = 0.70f,
                        enabled = centerCard != null
                    ) {
                        Text(
                            if (isRevealed) "Hide" else "Reveal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = SakuColors.TextPrimary
                        )
                    }
                    LiquidGlassButton(
                        onClick = onGood,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(14.dp),
                        cornerRadius = 14.dp,
                        tintColor = SakuColors.SageLight.copy(alpha = 0.35f),
                        darkBaseAlpha = 0.55f,
                        specularAlpha = 0.60f,
                        enabled = centerCard != null
                    ) {
                        Text("Good", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    LiquidGlassButton(
                        onClick = onOpenAnki,
                        modifier = Modifier
                            .weight(0.9f)
                            .height(40.dp),
                        shape = RoundedCornerShape(14.dp),
                        cornerRadius = 14.dp,
                        tintColor = Color.White.copy(alpha = 0.16f),
                        darkBaseAlpha = 0.60f,
                        specularAlpha = 0.50f
                    ) {
                        Text("Anki", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = SakuColors.TextSecondary)
                    }
                }
            }
        }
    }

    @Composable
    fun GlassFlashcard(
        deckName: String,
        deckId: Long,
        card: CardInfo?,
        isRevealed: Boolean,
        modifier: Modifier = Modifier
    ) {
        LiquidGlassBox(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(0.78f),
            shape = RoundedCornerShape(24.dp),
            cornerRadius = 24.dp,
            tintColor = Color.White.copy(alpha = 0.22f),
            darkBaseAlpha = 0.50f,
            specularAlpha = 0.65f,
            borderAlpha = 0.55f,
            shadowElevation = 10.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Deck name at top
                    Text(
                        text = "Deck: $deckName",
                        fontSize = 12.sp,
                        color = SakuColors.TextSecondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (card == null) {
                        if (deckId == -1L) {
                            Text(
                                text = "Connect AnkiDroid",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SakuColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "✓ All caught up!",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SakuColors.SagePrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No cards due",
                                fontSize = 12.sp,
                                color = SakuColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Furigana (shown when revealed)
                        if (isRevealed && !card.kanjiFurigana.isNullOrBlank()) {
                            Text(
                                text = card.kanjiFurigana,
                                fontSize = 12.5.sp,
                                color = SakuColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Main kanji / word
                        val mainWord = card.kanji.ifBlank { card.question }.ifEmpty { "—" }
                        Text(
                            text = mainWord,
                            fontSize = if (mainWord.length > 5) 26.sp else if (mainWord.length > 3) 30.sp else 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // English meaning (shown when revealed)
                        if (isRevealed && !card.kanjiMeaning.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = card.kanjiMeaning,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SakuColors.TextPrimary,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Divider line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.42f)
                                .height(1.dp)
                                .background(Color.White.copy(alpha = 0.18f))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Example sentence
                        val sentence = card.sentence
                        if (sentence.isNotBlank()) {
                            Text(
                                text = sentence,
                                fontSize = 14.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 19.sp
                            )

                            // Sentence meaning (shown when revealed)
                            if (isRevealed && !card.sentenceMeaning.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = card.sentenceMeaning,
                                    fontSize = 12.sp,
                                    color = SakuColors.TextSecondary,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ModernClassicActionCard(currentAction: String, onSelect: (String) -> Unit) {
        val options = listOf("suspend", "open_anki", "undo", "open_app")
        val labels = listOf("Suspend", "Open Anki", "Undo", "Open App")
        val selectedIndex = options.indexOf(currentAction).coerceAtLeast(0)

        LiquidGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            cornerRadius = 24.dp,
            tintColor = Color.White.copy(alpha = 0.12f),
            darkBaseAlpha = 0.65f,
            specularAlpha = 0.45f,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = SakuColors.SagePrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Action on Answer Revealed",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = SakuColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, action ->
                        SegmentedButton(
                            selected = index == selectedIndex,
                            onClick = { onSelect(action) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = SakuColors.SageContainer,
                                activeContentColor = SakuColors.SageLight,
                                inactiveContainerColor = SakuColors.SurfaceElevated,
                                inactiveContentColor = SakuColors.TextSecondary,
                                activeBorderColor = SakuColors.SageContainerBorder,
                                inactiveBorderColor = SakuColors.BorderSubtle
                            )
                        ) {
                            Text(labels[index], fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ModernBackgroundStudioCard(
        currentType: String,
        blurRadius: Float,
        dimOpacity: Float,
        artworkOpacity: Float,
        savedUris: Set<String>,
        currentUri: String?,
        onSelectType: (String) -> Unit,
        onSelectSavedUri: (String) -> Unit,
        onRemoveSavedUri: (String) -> Unit,
        onPickNewImage: () -> Unit,
        onBlurChange: (Float) -> Unit,
        onBlurCommit: () -> Unit,
        onOpacityChange: (Float) -> Unit,
        onOpacityCommit: () -> Unit,
        onArtworkOpacityChange: (Float) -> Unit,
        onArtworkOpacityCommit: () -> Unit
    ) {
        val context = LocalContext.current
        val options = listOf("anki_lock", "dark_blur", "sunset", "transparent", "custom")
        val labels = listOf("Default", "Dark Blur", "Sunset", "Glass", "Gallery")
        val selectedIndex = options.indexOf(currentType).coerceAtLeast(0)

        LiquidGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            cornerRadius = 24.dp,
            tintColor = Color.White.copy(alpha = 0.12f),
            darkBaseAlpha = 0.65f,
            specularAlpha = 0.45f,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = null,
                        tint = SakuColors.SagePrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Widget Background",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = SakuColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SakuColors.SageContainer,
                        border = BorderStroke(1.dp, SakuColors.SageContainerBorder)
                    ) {
                        Text(
                            labels[selectedIndex],
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SakuColors.SageLight,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clean style presets row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(options.indices.toList()) { index ->
                        val type = options[index]
                        val isSelected = (index == selectedIndex)
                        Surface(
                            onClick = { onSelectType(type) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) SakuColors.SagePrimary.copy(alpha = 0.22f)
                            else SakuColors.SurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) SakuColors.SagePrimary
                                else SakuColors.BorderSubtle
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = labels[index],
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) SakuColors.SageLight else SakuColors.TextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Fine-tuning range sliders (compact, minimal, clean)
                if (currentType != "transparent") {
                    Spacer(modifier = Modifier.height(14.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(SakuColors.SurfaceElevated.copy(alpha = 0.6f))
                            .border(BorderStroke(1.dp, SakuColors.BorderSubtle), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CompactSliderRow(
                            icon = Icons.Filled.BlurOn,
                            label = "Blur Radius",
                            valueDisplay = "${blurRadius.toInt()}px",
                            value = blurRadius,
                            valueRange = 5f..60f,
                            onValueChange = { newRadius ->
                                onBlurChange(((newRadius / 5f).roundToInt() * 5f).coerceIn(5f, 60f))
                            },
                            onValueChangeFinished = onBlurCommit
                        )
                        CompactSliderRow(
                            icon = Icons.Filled.Opacity,
                            label = "Dimming Tint",
                            valueDisplay = "${(dimOpacity * 100).toInt()}%",
                            value = dimOpacity,
                            valueRange = 0.0f..0.9f,
                            onValueChange = { newOpacity ->
                                onOpacityChange(((newOpacity * 20f).roundToInt() / 20f).coerceIn(0f, 0.9f))
                            },
                            onValueChangeFinished = onOpacityCommit
                        )
                        CompactSliderRow(
                            icon = Icons.Filled.AutoAwesome,
                            label = "Artwork Opacity",
                            valueDisplay = "${(artworkOpacity * 100).toInt()}%",
                            value = artworkOpacity,
                            valueRange = 0.1f..1.0f,
                            onValueChange = { newArtOpacity ->
                                onArtworkOpacityChange(((newArtOpacity * 20f).roundToInt() / 20f).coerceIn(0.1f, 1.0f))
                            },
                            onValueChangeFinished = onArtworkOpacityCommit
                        )
                    }
                }

                // Custom Gallery section
                if (currentType == "custom") {
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Inline "+ Add" button tile
                        item {
                            Surface(
                                onClick = onPickNewImage,
                                shape = RoundedCornerShape(14.dp),
                                color = SakuColors.SurfaceElevated,
                                border = BorderStroke(1.dp, SakuColors.SagePrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.size(64.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Filled.AddPhotoAlternate,
                                        contentDescription = "Add wallpaper",
                                        tint = SakuColors.SagePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Add",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SakuColors.SageLight
                                    )
                                }
                            }
                        }

                        items(savedUris.toList()) { uriStr ->
                            val isSelected = (uriStr == currentUri)
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        2.dp,
                                        if (isSelected) SakuColors.SagePrimary
                                        else SakuColors.BorderSubtle,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectSavedUri(uriStr) }
                            ) {
                                UriThumbnail(
                                    context = context,
                                    uriStr = uriStr,
                                    modifier = Modifier.fillMaxSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(3.dp)
                                        .size(17.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xCC000000))
                                        .clickable { onRemoveSavedUri(uriStr) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(3.dp)
                                            .size(17.dp)
                                            .clip(CircleShape)
                                            .background(SakuColors.SagePrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = null,
                                            tint = SakuColors.OnSage,
                                            modifier = Modifier.size(11.dp)
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

    @Composable
    fun ModernDeckSelectorCard(
        decks: List<DeckInfo>,
        selectedIds: List<String>,
        onDeckToggle: (String, Boolean) -> Unit
    ) {
        LiquidGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            cornerRadius = 24.dp,
            tintColor = Color.White.copy(alpha = 0.12f),
            darkBaseAlpha = 0.65f,
            specularAlpha = 0.45f,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Style,
                        contentDescription = null,
                        tint = SakuColors.SagePrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Anki Decks",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = SakuColors.TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                decks.forEach { deck ->
                    val deckIdStr = deck.id.toString()
                    val isSelected = deckIdStr in selectedIds || (selectedIds.isEmpty())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDeckToggle(deckIdStr, !(deckIdStr in selectedIds)) }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    ) {
                        Checkbox(
                            checked = deckIdStr in selectedIds,
                            onCheckedChange = { onDeckToggle(deckIdStr, it) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = SakuColors.SagePrimary,
                                uncheckedColor = SakuColors.BorderHighlight,
                                checkmarkColor = SakuColors.OnSage
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            deck.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = SakuColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${deck.newCount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SakuColors.AccentSlateBlue
                            )
                            Text(
                                " · ",
                                style = MaterialTheme.typography.bodySmall,
                                color = SakuColors.TextMuted
                            )
                            Text(
                                "${deck.learnCount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SakuColors.AccentRose
                            )
                            Text(
                                " · ",
                                style = MaterialTheme.typography.bodySmall,
                                color = SakuColors.TextMuted
                            )
                            Text(
                                "${deck.reviewCount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = SakuColors.AccentSage
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ModernIntervalCard(
        updateMinutes: Int,
        snoozeMinutes: Int,
        onUpdateSelect: (Int) -> Unit,
        onSnoozeSelect: (Int) -> Unit
    ) {
        val options = listOf(30, 60, 120)
        val labels = listOf("30m", "1h", "2h")
        val updateIdx = options.indexOf(updateMinutes).coerceAtLeast(0)
        val snoozeIdx = options.indexOf(snoozeMinutes).coerceAtLeast(0)

        LiquidGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            cornerRadius = 24.dp,
            tintColor = Color.White.copy(alpha = 0.12f),
            darkBaseAlpha = 0.65f,
            specularAlpha = 0.45f,
            shadowElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "Sync & Snooze Frequency",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = SakuColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Background Card Refresh",
                    fontSize = 12.sp,
                    color = SakuColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, min ->
                        SegmentedButton(
                            selected = index == updateIdx,
                            onClick = { onUpdateSelect(min) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = SakuColors.SageContainer,
                                activeContentColor = SakuColors.SageLight,
                                inactiveContainerColor = SakuColors.SurfaceElevated,
                                inactiveContentColor = SakuColors.TextSecondary,
                                activeBorderColor = SakuColors.SageContainerBorder,
                                inactiveBorderColor = SakuColors.BorderSubtle
                            )
                        ) {
                            Text(labels[index], fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Snooze Interval",
                    fontSize = 12.sp,
                    color = SakuColors.TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, min ->
                        SegmentedButton(
                            selected = index == snoozeIdx,
                            onClick = { onSnoozeSelect(min) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = SakuColors.SageContainer,
                                activeContentColor = SakuColors.SageLight,
                                inactiveContainerColor = SakuColors.SurfaceElevated,
                                inactiveContentColor = SakuColors.TextSecondary,
                                activeBorderColor = SakuColors.SageContainerBorder,
                                inactiveBorderColor = SakuColors.BorderSubtle
                            )
                        ) {
                            Text(labels[index], fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun CompactSliderRow(
        icon: ImageVector,
        label: String,
        valueDisplay: String,
        value: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        onValueChange: (Float) -> Unit,
        onValueChangeFinished: (() -> Unit)? = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = SakuColors.SageLight,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = SakuColors.TextSecondary
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SakuColors.SurfaceVariant
                ) {
                    Text(
                        valueDisplay,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SakuColors.SageLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                valueRange = valueRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                colors = SliderDefaults.colors(
                    thumbColor = SakuColors.SagePrimary,
                    activeTrackColor = SakuColors.SagePrimary,
                    inactiveTrackColor = SakuColors.Border
                )
            )
        }
    }

    @Composable
    fun ModernGeminiSettingsCard() {
        var showDialog by remember { mutableStateOf(false) }
        var showModelDialog by remember { mutableStateOf(false) }
        var currentKey by remember { mutableStateOf(prefs.geminiApiKey ?: "") }
        var currentModel by remember { mutableStateOf(prefs.geminiModel) }

        LiquidGlassBox(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            cornerRadius = 24.dp,
            tintColor = SakuColors.AccentLavender.copy(alpha = 0.14f),
            darkBaseAlpha = 0.65f,
            specularAlpha = 0.50f,
            shadowElevation = 6.dp
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
                            tint = SakuColors.AccentLavender,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GEMINI AI STORIES",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SakuColors.TextSecondary,
                            letterSpacing = 1.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (currentKey.isNotBlank()) SakuColors.SageContainer else SakuColors.AccentRoseContainer,
                        border = BorderStroke(
                            1.dp,
                            if (currentKey.isNotBlank()) SakuColors.SageContainerBorder else SakuColors.AccentRose.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = if (currentKey.isNotBlank()) "Connected" else "Not set",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (currentKey.isNotBlank()) SakuColors.SageLight else SakuColors.AccentRose,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "AI-generated Japanese reading passages personalized from your due and studied Anki flashcards.",
                    fontSize = 13.sp,
                    color = SakuColors.TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Active Model Selection Tile
                Text(
                    text = "ACTIVE MODEL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SakuColors.TextMuted,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    onClick = { showModelDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    color = SakuColors.AccentLavenderContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, SakuColors.AccentLavender.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = SakuColors.AccentLavender,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = PreferencesManager.getModelDisplayName(currentModel),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = SakuColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentModel,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = SakuColors.AccentLavender
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SakuColors.AccentLavenderContainer,
                            border = BorderStroke(1.dp, SakuColors.AccentLavender.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "Change",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SakuColors.AccentLavender,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SakuColors.SurfaceElevated,
                            contentColor = SakuColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SakuColors.BorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp), tint = SakuColors.SageLight)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (currentKey.isNotBlank()) "Change API Key" else "Set API Key", fontSize = 12.5.sp)
                    }

                    Button(
                        onClick = { showModelDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SakuColors.SurfaceElevated,
                            contentColor = SakuColors.TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SakuColors.BorderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = SakuColors.AccentLavender)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick Model", fontSize = 12.5.sp)
                    }
                }
            }
        }

        if (showDialog) {
            ApiKeySetupDialog(
                currentKey = currentKey,
                onSave = { newKey ->
                    currentKey = newKey
                    prefs.geminiApiKey = newKey
                    showDialog = false
                    Toast.makeText(this@MainActivity, "Gemini API key saved!", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showDialog = false }
            )
        }

        if (showModelDialog) {
            GeminiModelDialog(
                currentModel = currentModel,
                onSave = { newModel ->
                    currentModel = newModel
                    prefs.geminiModel = newModel
                    showModelDialog = false
                    Toast.makeText(this@MainActivity, "Gemini model set to $newModel", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { showModelDialog = false }
            )
        }
    }
}

@Composable
fun UriThumbnail(context: Context, uriStr: String, modifier: Modifier = Modifier) {
    val bitmap = remember(uriStr) {
        try {
            val uri = Uri.parse(uriStr)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeStream(stream, null, options)
            }
        } catch (e: Exception) {
            null
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(SakuColors.SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Image, contentDescription = null, tint = SakuColors.TextMuted)
        }
    }
}
