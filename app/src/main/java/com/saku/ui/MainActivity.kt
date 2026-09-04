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
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
            containerColor = SakuColors.Background,
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
                        containerColor = SakuColors.Background,
                        titleContentColor = SakuColors.TextPrimary,
                        actionIconContentColor = SakuColors.TextSecondary
                    )
                )
            },
            bottomBar = {
                BubblyFloatingNav(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
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
                        hasAnkiPermission = hasPermissionState
                    )
                }
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
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = SakuColors.Surface.copy(alpha = 0.96f),
                border = BorderStroke(1.dp, SakuColors.Border),
                shadowElevation = 8.dp,
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

                    // Bubbly sliding indicator pill
                    Box(
                        modifier = Modifier
                            .offset(x = indicatorOffset)
                            .width(tabWidth)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(26.dp))
                            .background(SakuColors.SageContainer)
                            .border(
                                BorderStroke(1.dp, SakuColors.SageContainerBorder),
                                RoundedCornerShape(26.dp)
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
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = SakuColors.Surface
            ),
            border = BorderStroke(
                1.dp,
                if (isEnabled) SakuColors.SageContainerBorder
                else SakuColors.Border
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = if (isEnabled) listOf(
                                SakuColors.SurfaceElevated,
                                SakuColors.SageContainer.copy(alpha = 0.5f)
                            ) else listOf(
                                SakuColors.Surface,
                                SakuColors.SurfaceElevated.copy(alpha = 0.5f)
                            )
                        )
                    )
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
                    eraseColor(android.graphics.Color.parseColor("#15171C"))
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = SakuColors.Surface
            ),
            border = BorderStroke(1.dp, SakuColors.Border)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
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

                Spacer(modifier = Modifier.height(12.dp))

                Image(
                    bitmap = previewArtwork.asImageBitmap(),
                    contentDescription = "Live Card Artwork",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(BorderStroke(1.dp, SakuColors.BorderSubtle), RoundedCornerShape(16.dp))
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SakuColors.AccentRose.copy(alpha = 0.85f),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Again", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onToggleReveal,
                        modifier = Modifier.weight(1.2f).height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SakuColors.SagePrimary,
                            contentColor = SakuColors.OnSage
                        )
                    ) {
                        Text(
                            if (isRevealed) "Hide" else "Reveal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onGood,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SakuColors.SageLight.copy(alpha = 0.85f),
                            contentColor = SakuColors.OnSage
                        )
                    ) {
                        Text("Good", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onOpenAnki,
                        modifier = Modifier.weight(0.9f).height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SakuColors.SurfaceElevated
                        ),
                        border = BorderStroke(1.dp, SakuColors.BorderHighlight)
                    ) {
                        Text("Anki", fontSize = 12.sp, color = SakuColors.TextSecondary)
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
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
            border = BorderStroke(1.dp, SakuColors.Border)
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
            border = BorderStroke(1.dp, SakuColors.Border)
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
            border = BorderStroke(1.dp, SakuColors.Border)
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
            border = BorderStroke(1.dp, SakuColors.Border)
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
            border = BorderStroke(1.dp, SakuColors.Border)
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
