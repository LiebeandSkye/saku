package com.saku

import com.saku.data.PreferencesManager
import com.saku.util.FakeSharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ElevenLabsAudioServiceTest {

    private lateinit var prefsManager: PreferencesManager
    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        prefsManager = PreferencesManager(fakePrefs)
    }

    @Test
    fun testDefaultElevenLabsConstants() {
        assertEquals("JTlYtJrcTzPC71hMLOxo", PreferencesManager.DEFAULT_ELEVENLABS_VOICE_ID)
        assertEquals("https://elevenlabs.io/voices/JTlYtJrcTzPC71hMLOxo", PreferencesManager.DEFAULT_ELEVENLABS_VOICE_URL)
        assertEquals("eleven_multilingual_v2", PreferencesManager.DEFAULT_ELEVENLABS_MODEL_ID)
    }

    @Test
    fun testExtractVoiceIdFromRawId() {
        val rawId = "JTlYtJrcTzPC71hMLOxo"
        assertEquals(rawId, PreferencesManager.extractVoiceId(rawId))
        assertEquals(rawId, PreferencesManager.extractVoiceId("  $rawId  "))
    }

    @Test
    fun testExtractVoiceIdFromUrl() {
        val url = "https://elevenlabs.io/voices/JTlYtJrcTzPC71hMLOxo"
        assertEquals("JTlYtJrcTzPC71hMLOxo", PreferencesManager.extractVoiceId(url))

        val urlWithTrailingSlash = "https://elevenlabs.io/voices/JTlYtJrcTzPC71hMLOxo/"
        assertEquals("JTlYtJrcTzPC71hMLOxo", PreferencesManager.extractVoiceId(urlWithTrailingSlash))

        val urlWithQuery = "https://elevenlabs.io/voices/JTlYtJrcTzPC71hMLOxo?share=1"
        assertEquals("JTlYtJrcTzPC71hMLOxo", PreferencesManager.extractVoiceId(urlWithQuery))
    }

    @Test
    fun testExtractVoiceIdBlankDefaults() {
        assertEquals("JTlYtJrcTzPC71hMLOxo", PreferencesManager.extractVoiceId(""))
        assertEquals("JTlYtJrcTzPC71hMLOxo", PreferencesManager.extractVoiceId("   "))
    }

    @Test
    fun testPreferencesManagerElevenLabsApiKey() {
        assertNull(prefsManager.elevenLabsApiKey)

        prefsManager.elevenLabsApiKey = "sk_test123456"
        assertEquals("sk_test123456", prefsManager.elevenLabsApiKey)

        prefsManager.elevenLabsApiKey = "   "
        assertNull(prefsManager.elevenLabsApiKey)
    }

    @Test
    fun testPreferencesManagerElevenLabsVoiceIdAndUrlExtraction() {
        // Defaults to default voice ID
        assertEquals(PreferencesManager.DEFAULT_ELEVENLABS_VOICE_ID, prefsManager.elevenLabsVoiceId)

        // Setting a URL automatically extracts the Voice ID
        prefsManager.elevenLabsVoiceId = "https://elevenlabs.io/voices/customVoiceId123"
        assertEquals("customVoiceId123", prefsManager.elevenLabsVoiceId)

        // Setting empty resets to default
        prefsManager.elevenLabsVoiceId = ""
        assertEquals(PreferencesManager.DEFAULT_ELEVENLABS_VOICE_ID, prefsManager.elevenLabsVoiceId)
    }

    @Test
    fun testPreferencesManagerElevenLabsVoiceName() {
        assertNull(prefsManager.elevenLabsVoiceName)

        prefsManager.elevenLabsVoiceName = "Aoi (Storyteller)"
        assertEquals("Aoi (Storyteller)", prefsManager.elevenLabsVoiceName)

        prefsManager.elevenLabsVoiceName = ""
        assertNull(prefsManager.elevenLabsVoiceName)
    }
}
