package com.saku

import com.saku.data.GeneratedStory
import com.saku.data.ReadingHistoryManager
import com.saku.reading.NekosService
import com.saku.util.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReadingHistoryTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var historyManager: ReadingHistoryManager

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        historyManager = ReadingHistoryManager(fakePrefs)
    }

    @Test
    fun testGeneratedStoryPinnedAndImageUrlDefaults() {
        val story = GeneratedStory(
            title = "公園の猫",
            content = "猫が歩いています。",
            jlptLevel = "N5"
        )
        assertFalse("Default story should not be pinned", story.isPinned)
        assertNull("Default story should not have image URL", story.imageUrl)
    }

    @Test
    fun testReadingHistorySaveAndRetrieveWithPinAndImageUrl() {
        val story = GeneratedStory(
            id = "story-1",
            title = "美味しいお寿司",
            content = "お寿司を食べます。",
            jlptLevel = "N5",
            isPinned = true,
            imageUrl = "https://cdn.nekosapi.com/images/sushi.webp"
        )

        historyManager.saveStory(story)
        val stories = historyManager.getStories()

        assertEquals(1, stories.size)
        val retrieved = stories.first()
        assertEquals("story-1", retrieved.id)
        assertTrue("Retrieved story should be pinned", retrieved.isPinned)
        assertEquals("https://cdn.nekosapi.com/images/sushi.webp", retrieved.imageUrl)
    }

    @Test
    fun testReadingHistorySortingPinnedFirst() {
        val story1 = GeneratedStory(
            id = "story-1",
            title = "Story 1",
            content = "Content 1",
            jlptLevel = "N5",
            createdAt = 1000L,
            isPinned = false
        )
        val story2 = GeneratedStory(
            id = "story-2",
            title = "Story 2",
            content = "Content 2",
            jlptLevel = "N5",
            createdAt = 2000L,
            isPinned = false
        )
        val story3 = GeneratedStory(
            id = "story-3",
            title = "Story 3 Pinned",
            content = "Content 3",
            jlptLevel = "N5",
            createdAt = 500L,
            isPinned = true
        )

        historyManager.saveStory(story1)
        historyManager.saveStory(story2)
        historyManager.saveStory(story3)

        val sorted = historyManager.getStories()
        assertEquals(3, sorted.size)
        // Pinned story should be first even though its createdAt is earlier
        assertEquals("story-3", sorted[0].id)
        assertTrue(sorted[0].isPinned)

        // The remaining stories should be sorted newest first (story2 before story1)
        assertEquals("story-2", sorted[1].id)
        assertEquals("story-1", sorted[2].id)
    }

    @Test
    fun testTogglePin() {
        val story = GeneratedStory(
            id = "story-toggle",
            title = "Toggle Test",
            content = "Content",
            jlptLevel = "N4",
            isPinned = false
        )
        historyManager.saveStory(story)

        assertFalse(historyManager.getStories().first().isPinned)

        val pinnedState = historyManager.togglePin("story-toggle")
        assertTrue("togglePin should return new pinned state (true)", pinnedState)
        assertTrue("Story should now be pinned in saved list", historyManager.getStories().first().isPinned)

        val unpinnedState = historyManager.togglePin("story-toggle")
        assertFalse("togglePin should return new pinned state (false)", unpinnedState)
        assertFalse("Story should now be unpinned in saved list", historyManager.getStories().first().isPinned)
    }

    @Test
    fun testUpdateStoryImageUrl() {
        val story = GeneratedStory(
            id = "story-image",
            title = "Image Update Test",
            content = "Content",
            jlptLevel = "N5"
        )
        historyManager.saveStory(story)
        assertNull(historyManager.getStories().first().imageUrl)

        historyManager.updateStoryImageUrl("story-image", "https://cdn.nekosapi.com/image.webp")
        val updated = historyManager.getStories().first()
        assertEquals("https://cdn.nekosapi.com/image.webp", updated.imageUrl)
    }

    @Test
    fun testNekosServiceTagMapping() {
        val tagFood = NekosService.determineTag(
            title = "美味しいラーメン屋に行きました",
            theme = "Food",
            topic = "Ramen"
        )
        assertEquals("cafe+scenery", tagFood)

        val tagSchool = NekosService.determineTag(
            title = "学校の教室で勉強",
            theme = "School Life",
            topic = "Classroom"
        )
        assertEquals("classroom+scenery", tagSchool)

        val tagShrine = NekosService.determineTag(
            title = "京都の神社を参拝する",
            theme = "Travel",
            topic = "Shrine"
        )
        assertEquals("shrine+scenery", tagShrine)

        val tagNature = NekosService.determineTag(
            title = "桜の花が咲いた",
            theme = "Nature",
            topic = "Cherry blossoms"
        )
        assertEquals("cherry_blossoms+scenery", tagNature)

        val tagNight = NekosService.determineTag(
            title = "東京の夜景",
            theme = "City",
            topic = "Night scene"
        )
        assertEquals("night+scenery", tagNight)

        val tagTrain = NekosService.determineTag(
            title = "新幹線で旅行",
            theme = "Travel",
            topic = "Train"
        )
        assertEquals("train+scenery", tagTrain)

        val tagDefault = NekosService.determineTag(
            title = "不思議な話",
            theme = null,
            topic = null
        )
        assertEquals("scenery", tagDefault)
    }
}
