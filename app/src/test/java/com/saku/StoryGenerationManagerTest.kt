package com.saku

import com.saku.data.GeneratedStory
import com.saku.reading.GenerationStatus
import com.saku.reading.StoryGenerationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StoryGenerationManagerTest {

    @Before
    fun setUp() {
        StoryGenerationManager.reset()
    }

    @Test
    fun testInitialState() {
        assertEquals(GenerationStatus.Idle, StoryGenerationManager.status)
        assertFalse(StoryGenerationManager.isGenerating)
        assertNull(StoryGenerationManager.latestStory)
    }

    @Test
    fun testGeneratingStateProperties() {
        assertFalse(StoryGenerationManager.isGenerating)

        val genStatus = GenerationStatus.Generating("N3", "Mystery", "A strange letter")
        assertEquals("N3", genStatus.jlptLevel)
        assertEquals("Mystery", genStatus.theme)
        assertEquals("A strange letter", genStatus.topic)
    }

    @Test
    fun testSuccessStateAndDismiss() {
        val story = GeneratedStory(
            id = "test-123",
            title = "テスト物語",
            content = "これはテストです。",
            jlptLevel = "N4",
            targetWords = listOf("テスト")
        )

        val successStatus = GenerationStatus.Success(story)
        assertEquals(story, successStatus.story)

        StoryGenerationManager.dismissStatus()
        assertEquals(GenerationStatus.Idle, StoryGenerationManager.status)
        assertFalse(StoryGenerationManager.isGenerating)
    }

    @Test
    fun testErrorStateAndDismiss() {
        val errorStatus = GenerationStatus.Error("Network timeout")
        assertEquals("Network timeout", errorStatus.message)

        StoryGenerationManager.dismissStatus()
        assertEquals(GenerationStatus.Idle, StoryGenerationManager.status)
        assertFalse(StoryGenerationManager.isGenerating)
    }

    @Test
    fun testResetClearsState() {
        StoryGenerationManager.reset()
        assertEquals(GenerationStatus.Idle, StoryGenerationManager.status)
        assertNull(StoryGenerationManager.latestStory)
        assertFalse(StoryGenerationManager.isGenerating)
    }
}
