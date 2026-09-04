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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0A0A0A),
                    surface = Color(0xFF141414),
                    surfaceVariant = Color(0xFF1E293B),
                    primary = Color(0xFF38BDF8),
                    secondary = Color(0xFF8AB4F8),
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
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
        val coroutineScope = rememberCoroutineScope()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (prefs.isServiceEnabled) Color(0xFF10B981)
                                        else Color(0xFFEF4444)
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                if (currentTab == 0) "Saku • 咲く" else "Saku • 読書 (Reading)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = 0.8.sp
                            )
                        }
                    },
                    actions = {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp),
                                color = Color.White,
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
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0A0A0A),
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF101010),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = {
                            Icon(Icons.Filled.Style, contentDescription = "Flashcards")
                        },
                        label = {
                            Text("Flashcards", fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF38BDF8),
                            selectedTextColor = Color(0xFF38BDF8),
                            indicatorColor = Color(0xFF1E293B),
                            unselectedIconColor = Color(0xFF888888),
                            unselectedTextColor = Color(0xFF888888)
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = {
                            Icon(Icons.Filled.AutoStories, contentDescription = "Reading")
                        },
                        label = {
                            Text("Reading", fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF38BDF8),
                            selectedTextColor = Color(0xFF38BDF8),
                            indicatorColor = Color(0xFF1E293B),
                            unselectedIconColor = Color(0xFF888888),
                            unselectedTextColor = Color(0xFF888888)
                        )
                    )
                }
            }
        ) { padding ->
            if (currentTab == 0) {
                ModernSettingsScreen(padding)
            } else {
                ReadingScreen(
                    padding = padding,
                    prefs = prefs,
                    hasAnkiPermission = hasPermissionState
                )
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
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                    border = BorderStroke(1.dp, Color(0xFF262626))
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

                        if (!isAnkiInstalledState) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFFA94D))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("AnkiDroid is not installed on this device", fontSize = 14.sp)
                            }
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                            ) {
                                Text("Install AnkiDroid from Play Store")
                            }
                        } else if (!hasPermissionState) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFFFA94D))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Permission required to read your cards", fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { checkAndRequestAnkiPermission() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
                            ) {
                                Text("Connect to AnkiDroid (1-Tap)")
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

            // 3. Live Card & Widget Preview Card
            ModernPreviewCard(
                card = activeCard,
                stats = stats,
                isRevealed = isRevealed,
                onToggleReveal = { CardSessionManager.toggleReveal(this@MainActivity) },
                onRefresh = { CardSessionManager.refresh(this@MainActivity) },
                onAgain = { CardSessionManager.gradeCard(this@MainActivity, 1) },
                onGood = { CardSessionManager.gradeCard(this@MainActivity, 3) },
                onOpenAnki = {
                    val launchIntent = ankiHelper.getAnkiLaunchIntent()
                    startActivity(launchIntent)
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

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    @Composable
    fun ModernHeroCard(isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            border = BorderStroke(
                1.dp,
                if (isEnabled) Color(0xFF38BDF8).copy(alpha = 0.4f)
                else Color.White.copy(alpha = 0.1f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = if (isEnabled) listOf(
                                Color(0xFF0F172A),
                                Color(0xFF1E293B),
                                Color(0xFF0C4A6E).copy(alpha = 0.4f)
                            ) else listOf(
                                Color(0xFF1E1E2E),
                                Color(0xFF181825)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isEnabled) "Lockscreen Card Active" else "Lockscreen Card Inactive",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (isEnabled) "Review Japanese flashcards on Lock Screen & AOD"
                            else "Turn on to display flashcards when your screen turns on",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = Color(0xFF38BDF8),
                            checkedThumbColor = Color.White
                        )
                    )
                }
            }
        }
    }

    @Composable
    fun ModernPreviewCard(
        card: CardInfo?,
        stats: Triple<Int, Int, Int>,
        isRevealed: Boolean,
        onToggleReveal: () -> Unit,
        onRefresh: () -> Unit,
        onAgain: () -> Unit,
        onGood: () -> Unit,
        onOpenAnki: () -> Unit
    ) {
        val context = LocalContext.current
        val imageBitmap = remember(card) {
            if (!card?.imageFileName.isNullOrBlank()) {
                ankiHelper.getCardImageBitmap(card!!.imageFileName)
            } else {
                null
            }
        }

        val previewArtwork = remember(
            card,
            stats,
            isRevealed,
            imageBitmap,
            backgroundTypeState,
            customImageUriState,
            blurRadiusState,
            dimOpacityState,
            artworkOpacityState
        ) {
            try {
                MediaArtworkGenerator.generateArtwork(
                    context = context,
                    card = card,
                    stats = stats,
                    isRevealed = isRevealed,
                    imageBitmap = imageBitmap,
                    showBottomControls = false,
                    targetWidth = 512,
                    targetHeight = 512
                )
            } catch (t: Throwable) {
                t.printStackTrace()
                Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply {
                    eraseColor(android.graphics.Color.parseColor("#0F172A"))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0F172A).copy(alpha = 0.8f)
            ),
            border = BorderStroke(1.dp, Color(0xFF334155))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Live Card & Widget Preview",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Image(
                    bitmap = previewArtwork.asImageBitmap(),
                    contentDescription = "Live Card Artwork",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, Color(0xFF475569).copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                        .clickable { onToggleReveal() },
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onAgain,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF5350).copy(alpha = 0.85f)
                        )
                    ) {
                        Text("Again", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Button(
                        onClick = onToggleReveal,
                        modifier = Modifier.weight(1.2f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38BDF8).copy(alpha = 0.85f)
                        )
                    ) {
                        Text(
                            if (isRevealed) "Hide" else "Reveal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Button(
                        onClick = onGood,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981).copy(alpha = 0.85f)
                        )
                    ) {
                        Text("Good", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = onOpenAnki,
                        modifier = Modifier.weight(0.9f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color(0xFF334155).copy(alpha = 0.5f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF64748B).copy(alpha = 0.5f))
                    ) {
                        Text("Anki", fontSize = 12.sp, color = Color(0xFFE2E8F0))
                    }
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Action on Answer Revealed",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
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
        val options = listOf("anki_lock", "dark_blur", "sunset", "custom", "transparent")
        val labels = listOf("Default", "Dark Blur", "Sunset", "Gallery", "Glass")
        val selectedIndex = options.indexOf(currentType).coerceAtLeast(0)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Widget Background Studio",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            else Color(0xFF1E293B).copy(alpha = 0.6f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color(0xFF475569).copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = labels[index],
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                if (currentType != "transparent") {
                    Spacer(modifier = Modifier.height(8.dp))
                    StudioSliderRow(
                        icon = Icons.Filled.BlurOn,
                        label = "Blur Radius: ${blurRadius.toInt()}px",
                        value = blurRadius,
                        valueRange = 5f..60f,
                        steps = 10,
                        onValueChange = { newRadius ->
                            onBlurChange(((newRadius / 5f).roundToInt() * 5f).coerceIn(5f, 60f))
                        },
                        onValueChangeFinished = onBlurCommit
                    )
                    StudioSliderRow(
                        icon = Icons.Filled.Opacity,
                        label = "Dark Dimming Tint: ${(dimOpacity * 100).toInt()}%",
                        value = dimOpacity,
                        valueRange = 0.0f..0.9f,
                        steps = 17,
                        onValueChange = { newOpacity ->
                            onOpacityChange(((newOpacity * 20f).roundToInt() / 20f).coerceIn(0f, 0.9f))
                        },
                        onValueChangeFinished = onOpacityCommit
                    )
                    StudioSliderRow(
                        icon = Icons.Filled.AutoAwesome,
                        label = "Artwork Opacity: ${(artworkOpacity * 100).toInt()}%",
                        value = artworkOpacity,
                        valueRange = 0.1f..1.0f,
                        steps = 17,
                        onValueChange = { newArtOpacity ->
                            onArtworkOpacityChange(((newArtOpacity * 20f).roundToInt() / 20f).coerceIn(0.1f, 1.0f))
                        },
                        onValueChangeFinished = onArtworkOpacityCommit
                    )
                }

                if (currentType == "custom") {
                    Spacer(modifier = Modifier.height(14.dp))

                    if (savedUris.isNotEmpty()) {
                        Text(
                            "Saved Gallery Wallpapers (${savedUris.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(savedUris.toList()) { uriStr ->
                                val isSelected = (uriStr == currentUri)
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            2.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                            RoundedCornerShape(12.dp)
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
                                            .padding(4.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xCC000000))
                                            .clickable { onRemoveSavedUri(uriStr) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(4.dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = onPickNewImage,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Filled.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Picture from Phone / Gallery")
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Style,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Anki Decks",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
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
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            deck.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${deck.newCount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8AB4F8)
                            )
                            Text(
                                " · ",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                "${deck.learnCount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF28B82)
                            )
                            Text(
                                " · ",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                "${deck.reviewCount}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C995)
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "Frequency & Snooze",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Background Sync Interval",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, min ->
                        SegmentedButton(
                            selected = index == updateIdx,
                            onClick = { onUpdateSelect(min) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                        ) {
                            Text(labels[index], fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Snooze Duration",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
                Spacer(modifier = Modifier.height(6.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, min ->
                        SegmentedButton(
                            selected = index == snoozeIdx,
                            onClick = { onSnoozeSelect(min) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                        ) {
                            Text(labels[index], fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun StudioSliderRow(
        icon: ImageVector,
        label: String,
        value: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int = 0,
        onValueChange: (Float) -> Unit,
        onValueChangeFinished: (() -> Unit)? = null
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                label,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }

    @Composable
    fun ModernGeminiSettingsCard() {
        var showDialog by remember { mutableStateOf(false) }
        var showModelDialog by remember { mutableStateOf(false) }
        var currentKey by remember { mutableStateOf(prefs.geminiApiKey ?: "") }
        var currentModel by remember { mutableStateOf(prefs.geminiModel) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
            border = BorderStroke(1.dp, Color(0xFF262626))
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
                            tint = Color(0xFFA855F7),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "GEMINI AI READING",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF888888),
                            letterSpacing = 1.2.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (currentKey.isNotBlank()) Color(0xFF162520) else Color(0xFF2D1B1B)
                    ) {
                        Text(
                            text = if (currentKey.isNotBlank()) "Connected" else "Not set",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (currentKey.isNotBlank()) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Configure API key and AI model used to craft Japanese reading stories from your studied and suspended Anki flashcards.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Active Model Selection Tile
                Text(
                    text = "ACTIVE MODEL",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF888888),
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    onClick = { showModelDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E1829),
                    border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.45f)),
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
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = PreferencesManager.getModelDisplayName(currentModel),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = currentModel,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFFC084FC)
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF3B1E5A)
                        ) {
                            Text(
                                text = "Switch",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD8B4FE),
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
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (currentKey.isNotBlank()) "Change API Key" else "Set API Key")
                    }

                    Button(
                        onClick = { showModelDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E1F42),
                            contentColor = Color(0xFFD8B4FE)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFA855F7))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick Model")
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
            modifier = modifier.background(Color(0xFF334155)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Image, contentDescription = null, tint = Color(0xFF94A3B8))
        }
    }
}
