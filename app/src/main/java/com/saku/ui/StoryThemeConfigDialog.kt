package com.saku.ui

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.saku.data.PreferencesManager
import com.saku.data.StoryThemes

@Composable
fun StoryThemeConfigDialog(
    prefs: PreferencesManager,
    onDismiss: () -> Unit,
    onConfigurationChanged: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(if (prefs.isCustomThemeModeActive) 1 else 0) }

    // Randomizer state
    var disabledThemes by remember { mutableStateOf(prefs.disabledStoryThemes.toMutableSet()) }
    var disabledTopics by remember { mutableStateOf(prefs.disabledStoryTopics.toMutableSet()) }
    val expandedThemes = remember { mutableStateOf(mutableSetOf<String>()) }

    // Custom story state
    var isCustomActive by remember { mutableStateOf(prefs.isCustomThemeModeActive) }
    var selectedCustomTheme by remember {
        mutableStateOf(prefs.customStoryTheme ?: StoryThemes.ALL_THEMES.first())
    }
    var selectedCustomTopic by remember {
        mutableStateOf(prefs.customStoryTopic) // null means "Any topic"
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SakuColors.Surface),
            border = BorderStroke(1.dp, SakuColors.Border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = SakuColors.SageContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = SakuColors.SagePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Theme & Topic Settings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = SakuColors.TextPrimary
                            )
                            Text(
                                text = if (isCustomActive) "Custom Mode Active" else "Randomizer Active",
                                fontSize = 12.sp,
                                color = if (isCustomActive) SakuColors.AccentLavender else SakuColors.SagePrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = SakuColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabs: Randomizer Settings vs Custom Story
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SakuColors.SurfaceElevated,
                    contentColor = SakuColors.TextPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SakuColors.SagePrimary,
                            height = 3.dp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                text = "Randomizer",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) SakuColors.SagePrimary else SakuColors.TextSecondary
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                text = "Custom Story",
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) SakuColors.SagePrimary else SakuColors.TextSecondary
                            )
                        }
                    )
                }

                // Scrollable Content Area
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (selectedTab == 0) {
                        // TAB 0: Randomizer Settings
                        RandomizerSettingsContent(
                            disabledThemes = disabledThemes,
                            disabledTopics = disabledTopics,
                            expandedThemes = expandedThemes.value,
                            onToggleTheme = { themeKey ->
                                val updated = disabledThemes.toMutableSet()
                                if (themeKey in updated) {
                                    updated.remove(themeKey)
                                } else {
                                    // Ensure at least one theme remains enabled
                                    val enabledCount = StoryThemes.ALL_THEMES.count { it !in updated }
                                    if (enabledCount <= 1) {
                                        Toast.makeText(context, "At least one theme must remain enabled", Toast.LENGTH_SHORT).show()
                                        return@RandomizerSettingsContent
                                    }
                                    updated.add(themeKey)
                                }
                                disabledThemes = updated
                                prefs.disabledStoryThemes = updated
                                onConfigurationChanged()
                            },
                            onToggleTopic = { topic ->
                                val updated = disabledTopics.toMutableSet()
                                if (topic in updated) {
                                    updated.remove(topic)
                                } else {
                                    updated.add(topic)
                                }
                                disabledTopics = updated
                                prefs.disabledStoryTopics = updated
                                onConfigurationChanged()
                            },
                            onToggleExpand = { themeKey ->
                                val updated = expandedThemes.value.toMutableSet()
                                if (themeKey in updated) {
                                    updated.remove(themeKey)
                                } else {
                                    updated.add(themeKey)
                                }
                                expandedThemes.value = updated
                            },
                            onEnableAll = {
                                disabledThemes = mutableSetOf()
                                disabledTopics = mutableSetOf()
                                prefs.disabledStoryThemes = emptySet()
                                prefs.disabledStoryTopics = emptySet()
                                onConfigurationChanged()
                                Toast.makeText(context, "All themes and topics enabled", Toast.LENGTH_SHORT).show()
                            },
                            onDisableOthers = { themeToKeep ->
                                val updatedThemes = StoryThemes.ALL_THEMES.filter { it != themeToKeep }.toMutableSet()
                                disabledThemes = updatedThemes
                                prefs.disabledStoryThemes = updatedThemes
                                onConfigurationChanged()
                            }
                        )
                    } else {
                        // TAB 1: Custom Story
                        CustomStoryContent(
                            isCustomActive = isCustomActive,
                            selectedTheme = selectedCustomTheme,
                            selectedTopic = selectedCustomTopic,
                            onThemeSelected = { newTheme ->
                                selectedCustomTheme = newTheme
                                selectedCustomTopic = null // Reset topic to "Any topic" when switching theme
                            },
                            onTopicSelected = { newTopic ->
                                selectedCustomTopic = newTopic
                            },
                            onApplyCustom = {
                                isCustomActive = true
                                prefs.isCustomThemeModeActive = true
                                prefs.customStoryTheme = selectedCustomTheme
                                prefs.customStoryTopic = selectedCustomTopic
                                onConfigurationChanged()
                                Toast.makeText(context, "Custom story configured and locked!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            onResetToRandom = {
                                isCustomActive = false
                                prefs.isCustomThemeModeActive = false
                                prefs.customStoryTheme = null
                                prefs.customStoryTopic = null
                                onConfigurationChanged()
                                Toast.makeText(context, "Switched back to Random mode", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Footer Done button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SakuColors.SagePrimary,
                        contentColor = SakuColors.OnSage
                    )
                ) {
                    Text(
                        text = "Done",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RandomizerSettingsContent(
    disabledThemes: Set<String>,
    disabledTopics: Set<String>,
    expandedThemes: Set<String>,
    onToggleTheme: (String) -> Unit,
    onToggleTopic: (String) -> Unit,
    onToggleExpand: (String) -> Unit,
    onEnableAll: () -> Unit,
    onDisableOthers: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Quick control header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Themes & Topics Pool",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SakuColors.TextSecondary
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = SakuColors.SurfaceElevated,
                border = BorderStroke(1.dp, SakuColors.BorderSubtle),
                modifier = Modifier.clickable { onEnableAll() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        tint = SakuColors.SagePrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Enable All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SakuColors.SagePrimary
                    )
                }
            }
        }

        Text(
            text = "Checked themes and topics will be randomly picked during story generation. Expand any theme to toggle specific topics.",
            fontSize = 12.sp,
            color = SakuColors.TextMuted,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Themes list
        StoryThemes.ALL_THEMES.forEach { themeKey ->
            val isThemeEnabled = themeKey !in disabledThemes
            val isExpanded = themeKey in expandedThemes
            val topics = StoryThemes.CATEGORIES[themeKey] ?: emptyList()
            val badgeColors = StoryThemes.getThemeBadgeColors(themeKey)

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isThemeEnabled) SakuColors.SurfaceElevated else SakuColors.SurfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(
                    1.dp,
                    if (isThemeEnabled) badgeColors.borderColor.copy(alpha = 0.35f) else SakuColors.BorderSubtle
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = isThemeEnabled,
                                onCheckedChange = { onToggleTheme(themeKey) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = badgeColors.contentColor,
                                    checkmarkColor = Color.Black,
                                    uncheckedColor = SakuColors.TextMuted
                                ),
                                modifier = Modifier.size(32.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            // Color badge
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = badgeColors.backgroundColor,
                                border = BorderStroke(1.dp, badgeColors.borderColor)
                            ) {
                                Text(
                                    text = StoryThemes.formatThemeName(themeKey),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColors.contentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "(${topics.size} topics)",
                                fontSize = 11.sp,
                                color = SakuColors.TextMuted
                            )
                        }

                        IconButton(
                            onClick = { onToggleExpand(themeKey) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Expand topics",
                                tint = SakuColors.TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Collapsible Topics List
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 28.dp, end = 4.dp, bottom = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            topics.forEach { topic ->
                                val isTopicEnabled = isThemeEnabled && topic !in disabledTopics
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = isThemeEnabled) { onToggleTopic(topic) }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isTopicEnabled,
                                        onCheckedChange = { onToggleTopic(topic) },
                                        enabled = isThemeEnabled,
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = badgeColors.contentColor,
                                            checkmarkColor = Color.Black,
                                            uncheckedColor = SakuColors.TextMuted
                                        ),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = topic,
                                        fontSize = 12.sp,
                                        color = if (isTopicEnabled) SakuColors.TextPrimary else SakuColors.TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
private fun CustomStoryContent(
    isCustomActive: Boolean,
    selectedTheme: String,
    selectedTopic: String?,
    onThemeSelected: (String) -> Unit,
    onTopicSelected: (String?) -> Unit,
    onApplyCustom: () -> Unit,
    onResetToRandom: () -> Unit
) {
    val topics = StoryThemes.CATEGORIES[selectedTheme] ?: emptyList()
    val badgeColors = StoryThemes.getThemeBadgeColors(selectedTheme)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Status indicator
        if (isCustomActive) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SakuColors.AccentLavenderContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, SakuColors.AccentLavender.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LOCKED CUSTOM STORY",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SakuColors.AccentLavender,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${StoryThemes.formatThemeName(selectedTheme)}: ${selectedTopic ?: "Any Topic"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = SakuColors.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onResetToRandom,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SakuColors.AccentRose),
                        border = BorderStroke(1.dp, SakuColors.AccentRose.copy(alpha = 0.5f)),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(text = "Clear", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Section 1: Choose Theme
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "1. Select Theme",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SakuColors.TextSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoryThemes.ALL_THEMES.forEach { themeKey ->
                    val isSelected = themeKey == selectedTheme
                    val style = StoryThemes.getThemeBadgeColors(themeKey)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) style.backgroundColor else SakuColors.SurfaceElevated,
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) style.contentColor else SakuColors.BorderSubtle
                        ),
                        modifier = Modifier.clickable { onThemeSelected(themeKey) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = style.contentColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = StoryThemes.formatThemeName(themeKey),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) style.contentColor else SakuColors.TextPrimary
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Choose Topic
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "2. Select Topic for ${StoryThemes.formatThemeName(selectedTheme)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SakuColors.TextSecondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // "Any topic" option
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selectedTopic == null) badgeColors.backgroundColor.copy(alpha = 0.6f) else SakuColors.SurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (selectedTopic == null) badgeColors.borderColor else SakuColors.BorderSubtle
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTopicSelected(null) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTopic == null,
                            onClick = { onTopicSelected(null) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = badgeColors.contentColor,
                                unselectedColor = SakuColors.TextMuted
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "✨ Any topic in ${StoryThemes.formatThemeName(selectedTheme)} (Random)",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTopic == null) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTopic == null) badgeColors.contentColor else SakuColors.TextPrimary
                        )
                    }
                }

                // Specific topics
                topics.forEach { topic ->
                    val isSelected = selectedTopic == topic
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) badgeColors.backgroundColor.copy(alpha = 0.6f) else SakuColors.SurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) badgeColors.borderColor else SakuColors.BorderSubtle
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTopicSelected(topic) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { onTopicSelected(topic) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = badgeColors.contentColor,
                                    unselectedColor = SakuColors.TextMuted
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = topic,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SakuColors.TextPrimary else SakuColors.TextSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Action Buttons
        Button(
            onClick = onApplyCustom,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = badgeColors.contentColor,
                contentColor = Color.Black
            )
        ) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Apply & Lock for Next Stories",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
