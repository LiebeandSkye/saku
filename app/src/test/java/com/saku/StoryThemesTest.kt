package com.saku

import com.saku.data.GeneratedStory
import com.saku.data.PreferencesManager
import com.saku.data.StoryThemes
import com.saku.reading.GeminiStoryService
import com.saku.util.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryThemesTest {

    @Test
    fun testAllTenCategoriesPresentAndPopulated() {
        val expectedCategories = listOf(
            "Happy",
            "Sad",
            "SliceOfLife",
            "Mystery",
            "Horror",
            "Adventure",
            "Inspirational",
            "Fantasy",
            "Comedy",
            "Nostalgic"
        )

        assertEquals(10, StoryThemes.ALL_THEMES.size)
        expectedCategories.forEach { category ->
            assertTrue("Category $category should be in ALL_THEMES", StoryThemes.ALL_THEMES.contains(category))
            val topics = StoryThemes.CATEGORIES[category]
            assertNotNull("Topics for $category should not be null", topics)
            assertTrue("Category $category should contain topics", topics!!.isNotEmpty())
        }

        // Spot-check specific topics from prompt
        assertTrue(StoryThemes.CATEGORIES["Happy"]!!.contains("Love story - first love"))
        assertTrue(StoryThemes.CATEGORIES["Sad"]!!.contains("Broken love story - breakup"))
        assertTrue(StoryThemes.CATEGORIES["SliceOfLife"]!!.contains("A day at school"))
        assertTrue(StoryThemes.CATEGORIES["Mystery"]!!.contains("A strange letter with no sender"))
        assertTrue(StoryThemes.CATEGORIES["Horror"]!!.contains("A haunted house on the edge of town"))
        assertTrue(StoryThemes.CATEGORIES["Adventure"]!!.contains("A treasure hunt with an old map"))
        assertTrue(StoryThemes.CATEGORIES["Inspirational"]!!.contains("An underdog story"))
        assertTrue(StoryThemes.CATEGORIES["Fantasy"]!!.contains("A hidden magic school"))
        assertTrue(StoryThemes.CATEGORIES["Comedy"]!!.contains("A case of mistaken identity"))
        assertTrue(StoryThemes.CATEGORIES["Nostalgic"]!!.contains("Childhood summer memories"))
    }

    @Test
    fun testFormatThemeName() {
        assertEquals("Slice of Life", StoryThemes.formatThemeName("SliceOfLife"))
        assertEquals("Happy", StoryThemes.formatThemeName("Happy"))
        assertEquals("Mystery", StoryThemes.formatThemeName("Mystery"))
    }

    @Test
    fun testRandomThemeAndTopicRespectsDisabledFilters() {
        // Disable 9 out of 10 categories -> Fantasy must be selected
        val disabledCategories = StoryThemes.ALL_THEMES.filter { it != "Fantasy" }.toSet()
        val (theme, topic) = StoryThemes.getRandomThemeAndTopic(disabledThemes = disabledCategories)
        assertEquals("Fantasy", theme)
        assertTrue(StoryThemes.CATEGORIES["Fantasy"]!!.contains(topic))

        // Disable all topics in Fantasy except one
        val fantasyTopics = StoryThemes.CATEGORIES["Fantasy"]!!
        val onlyTopic = fantasyTopics.first()
        val disabledTopics = fantasyTopics.drop(1).toSet()

        val (theme2, topic2) = StoryThemes.getRandomThemeAndTopic(
            disabledThemes = disabledCategories,
            disabledTopics = disabledTopics
        )
        assertEquals("Fantasy", theme2)
        assertEquals(onlyTopic, topic2)
    }

    @Test
    fun testRandomThemeAndTopicFallbackWhenAllDisabled() {
        // If user manages to disable all categories, safe fallback returns any valid category
        val allDisabled = StoryThemes.ALL_THEMES.toSet()
        val (theme, topic) = StoryThemes.getRandomThemeAndTopic(disabledThemes = allDisabled)
        assertTrue(StoryThemes.ALL_THEMES.contains(theme))
        assertNotNull(topic)
    }

    @Test
    fun testThemeBadgeColorsDefinedForAllThemes() {
        StoryThemes.ALL_THEMES.forEach { theme ->
            val style = StoryThemes.getThemeBadgeColors(theme)
            assertNotNull(style.backgroundColor)
            assertNotNull(style.contentColor)
            assertNotNull(style.borderColor)
        }
        val defaultStyle = StoryThemes.getThemeBadgeColors("UnknownTheme")
        assertNotNull(defaultStyle)
    }

    @Test
    fun testPreferencesManagerStoryThemeConfig() {
        val fakePrefs = FakeSharedPreferences()
        val prefs = PreferencesManager(fakePrefs)

        // Defaults
        assertTrue(prefs.disabledStoryThemes.isEmpty())
        assertTrue(prefs.disabledStoryTopics.isEmpty())
        assertNull(prefs.customStoryTheme)
        assertNull(prefs.customStoryTopic)
        assertFalse(prefs.isCustomThemeModeActive)

        // Set preferences
        prefs.disabledStoryThemes = setOf("Horror", "Sad")
        assertEquals(setOf("Horror", "Sad"), prefs.disabledStoryThemes)

        prefs.disabledStoryTopics = setOf("Broken love story - breakup")
        assertEquals(setOf("Broken love story - breakup"), prefs.disabledStoryTopics)

        prefs.customStoryTheme = "Mystery"
        prefs.customStoryTopic = "A strange letter with no sender"
        prefs.isCustomThemeModeActive = true

        assertEquals("Mystery", prefs.customStoryTheme)
        assertEquals("A strange letter with no sender", prefs.customStoryTopic)
        assertTrue(prefs.isCustomThemeModeActive)
    }

    @Test
    fun testPromptBuilderWithThemeAndTopic() {
        val service = GeminiStoryService()
        val prompt = service.buildJlptStoryPrompt(
            jlptLevel = "N3",
            wordPromptList = "手紙 (てがみ) [letter]",
            theme = "Mystery",
            topic = "A strange letter with no sender"
        )

        assertTrue(prompt.contains("[Story Theme & Narrative Premise]"))
        assertTrue(prompt.contains("Theme: Mystery"))
        assertTrue(prompt.contains("Topic / Premise: A strange letter with no sender"))
        assertTrue(prompt.contains("centered on the given theme and topic premise"))
        assertTrue(prompt.contains("手紙 (てがみ) [letter]"))
    }

    @Test
    fun testPromptBuilderThemeFormatting() {
        val service = GeminiStoryService()
        val prompt = service.buildJlptStoryPrompt(
            jlptLevel = "N4",
            wordPromptList = "",
            theme = "SliceOfLife",
            topic = "A day at school"
        )

        assertTrue(prompt.contains("Theme: Slice of Life"))
        assertTrue(prompt.contains("Topic / Premise: A day at school"))
    }

    @Test
    fun testGeneratedStoryModelThemeFields() {
        val storyWithTheme = GeneratedStory(
            title = "謎の手紙",
            content = "ある日、差出人のない手紙が届きました。",
            jlptLevel = "N3",
            theme = "Mystery",
            topic = "A strange letter with no sender"
        )

        assertEquals("Mystery", storyWithTheme.theme)
        assertEquals("A strange letter with no sender", storyWithTheme.topic)

        // Backward compatibility: default values are null
        val defaultStory = GeneratedStory(
            title = "古い物語",
            content = "昔々あるところに...",
            jlptLevel = "N5"
        )
        assertNull(defaultStory.theme)
        assertNull(defaultStory.topic)
    }
}
