package com.saku

import com.saku.data.PreferencesManager
import com.saku.util.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FishAudioServiceTest {

    private lateinit var prefsManager: PreferencesManager
    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        prefsManager = PreferencesManager(fakePrefs)
    }

    @Test
    fun testDefaultFishAudioConstants() {
        assertEquals("5b09815a54a04395bf6ad642d57ce12a", PreferencesManager.DEFAULT_FISH_AUDIO_VOICE_ID)
        assertEquals("https://fish.audio/m/5b09815a54a04395bf6ad642d57ce12a", PreferencesManager.DEFAULT_FISH_AUDIO_VOICE_URL)
        assertEquals("s2.1-pro-free", PreferencesManager.DEFAULT_FISH_AUDIO_MODEL)
        assertTrue(PreferencesManager.AVAILABLE_FISH_AUDIO_MODELS.any { it.id == "s2.1-pro-free" })
        assertTrue(PreferencesManager.AVAILABLE_FISH_AUDIO_MODELS.any { it.id == "s2.1-pro" })
    }

    @Test
    fun testExtractVoiceIdFromRawId() {
        val rawId = "5b09815a54a04395bf6ad642d57ce12a"
        assertEquals(rawId, PreferencesManager.extractVoiceId(rawId))
        assertEquals(rawId, PreferencesManager.extractVoiceId("  $rawId  "))
    }

    @Test
    fun testExtractVoiceIdFromUrl() {
        val url = "https://fish.audio/m/5b09815a54a04395bf6ad642d57ce12a"
        assertEquals("5b09815a54a04395bf6ad642d57ce12a", PreferencesManager.extractVoiceId(url))

        val urlWithTrailingSlash = "https://fish.audio/m/5b09815a54a04395bf6ad642d57ce12a/"
        assertEquals("5b09815a54a04395bf6ad642d57ce12a", PreferencesManager.extractVoiceId(urlWithTrailingSlash))

        val urlWithQuery = "https://fish.audio/m/5b09815a54a04395bf6ad642d57ce12a?share=1"
        assertEquals("5b09815a54a04395bf6ad642d57ce12a", PreferencesManager.extractVoiceId(urlWithQuery))

        val urlModelsPath = "https://fish.audio/models/5b09815a54a04395bf6ad642d57ce12a"
        assertEquals("5b09815a54a04395bf6ad642d57ce12a", PreferencesManager.extractVoiceId(urlModelsPath))
    }

    @Test
    fun testExtractVoiceIdBlankDefaults() {
        assertEquals("5b09815a54a04395bf6ad642d57ce12a", PreferencesManager.extractVoiceId(""))
        assertEquals("5b09815a54a04395bf6ad642d57ce12a", PreferencesManager.extractVoiceId("   "))
    }

    @Test
    fun testPreferencesManagerFishAudioApiKey() {
        assertNull(prefsManager.fishAudioApiKey)

        prefsManager.fishAudioApiKey = "fish_test_key_123"
        assertEquals("fish_test_key_123", prefsManager.fishAudioApiKey)

        prefsManager.fishAudioApiKey = "   "
        assertNull(prefsManager.fishAudioApiKey)
    }

    @Test
    fun testPreferencesManagerFishAudioVoiceIdAndUrlExtraction() {
        // Defaults to default voice ID
        assertEquals(PreferencesManager.DEFAULT_FISH_AUDIO_VOICE_ID, prefsManager.fishAudioVoiceId)

        // Setting a URL automatically extracts the Voice ID
        prefsManager.fishAudioVoiceId = "https://fish.audio/m/customVoiceId123"
        assertEquals("customVoiceId123", prefsManager.fishAudioVoiceId)

        // Setting empty resets to default
        prefsManager.fishAudioVoiceId = ""
        assertEquals(PreferencesManager.DEFAULT_FISH_AUDIO_VOICE_ID, prefsManager.fishAudioVoiceId)
    }

    @Test
    fun testPreferencesManagerFishAudioVoiceName() {
        assertNull(prefsManager.fishAudioVoiceName)

        prefsManager.fishAudioVoiceName = "Aoi (Storyteller)"
        assertEquals("Aoi (Storyteller)", prefsManager.fishAudioVoiceName)

        prefsManager.fishAudioVoiceName = ""
        assertNull(prefsManager.fishAudioVoiceName)
    }

    @Test
    fun testPreferencesManagerFishAudioModel() {
        // Defaults to s2.1-pro-free
        assertEquals(PreferencesManager.DEFAULT_FISH_AUDIO_MODEL, prefsManager.fishAudioModel)

        // Set to s2.1-pro
        prefsManager.fishAudioModel = "s2.1-pro"
        assertEquals("s2.1-pro", prefsManager.fishAudioModel)
    }
}
