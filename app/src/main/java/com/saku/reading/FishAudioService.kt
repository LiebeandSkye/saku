package com.saku.reading

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.saku.data.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class FishAudioService(context: Context) {
    private val context: Context = context.applicationContext

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private var mediaPlayer: MediaPlayer? = null
    private var onPlaybackStateCallback: ((Boolean) -> Unit)? = null
    private var onCompletionCallback: (() -> Unit)? = null

    /**
     * Queries Fish Audio API to fetch the title/name of the voice model associated with [voiceId].
     * Validates that the voice exists and the API key is active.
     */
    suspend fun fetchVoiceName(apiKey: String, voiceId: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Fish Audio API key is required"))
        }
        val cleanVoiceId = PreferencesManager.extractVoiceId(voiceId)
        if (cleanVoiceId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Voice ID is required"))
        }

        val request = Request.Builder()
            .url("https://api.fish.audio/model/$cleanVoiceId")
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(bodyStr, response.code)
                    return@withContext Result.failure(IOException(errorMsg))
                }

                val json = JSONObject(bodyStr)
                val title = json.optString("title", "").ifBlank {
                    json.optString("name", "Custom Voice")
                }
                val author = json.optJSONObject("author")?.optString("nickname", "") ?: ""
                val displayName = if (author.isNotBlank()) "$title (by $author)" else title
                Result.success(displayName.ifBlank { "Voice Model ($cleanVoiceId)" })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Synthesizes story text into MP3 audio or retrieves it from cache if already synthesized.
     */
    suspend fun synthesizeStoryAudio(
        apiKey: String,
        voiceId: String,
        model: String = PreferencesManager.DEFAULT_FISH_AUDIO_MODEL,
        storyId: String,
        text: String
    ): Result<File> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Fish Audio API key is required"))
        }
        val cleanVoiceId = PreferencesManager.extractVoiceId(voiceId)
        if (cleanVoiceId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Voice ID is required"))
        }
        if (text.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Text cannot be empty"))
        }

        val audioFile = getAudioFile(context, storyId)
        if (audioFile.exists() && audioFile.length() > 0) {
            return@withContext Result.success(audioFile)
        }

        val cleanModel = model.ifBlank { PreferencesManager.DEFAULT_FISH_AUDIO_MODEL }

        val requestJson = JSONObject().apply {
            put("text", text)
            put("reference_id", cleanVoiceId)
            put("format", "mp3")
        }

        val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("https://api.fish.audio/v1/tts")
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("model", cleanModel)
            .header("Accept", "audio/mpeg")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val errorMsg = parseErrorMessage(errorBody, response.code)
                    return@withContext Result.failure(IOException(errorMsg))
                }

                val body = response.body ?: return@withContext Result.failure(IOException("Empty response body from Fish Audio"))
                val dir = getAudioDir(context)
                if (!dir.exists()) {
                    dir.mkdirs()
                }

                val tempFile = File(dir, "temp_${storyId}_${System.currentTimeMillis()}.mp3")
                FileOutputStream(tempFile).use { fos ->
                    body.byteStream().use { input ->
                        input.copyTo(fos)
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (audioFile.exists()) {
                        audioFile.delete()
                    }
                    if (tempFile.renameTo(audioFile)) {
                        Result.success(audioFile)
                    } else {
                        // In case rename fails across partitions
                        tempFile.copyTo(audioFile, overwrite = true)
                        tempFile.delete()
                        Result.success(audioFile)
                    }
                } else {
                    tempFile.delete()
                    Result.failure(IOException("Failed to write audio file"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Synthesizes conversational speech text into MP3 audio or retrieves from cache.
     */
    suspend fun synthesizeSpeechAudio(
        apiKey: String,
        voiceId: String,
        model: String = PreferencesManager.DEFAULT_FISH_AUDIO_MODEL,
        text: String
    ): Result<File> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Fish Audio API key is required"))
        }
        val cleanVoiceId = PreferencesManager.extractVoiceId(voiceId)
        if (cleanVoiceId.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Voice ID is required"))
        }
        if (text.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Text cannot be empty"))
        }

        val cleanModel = model.ifBlank { PreferencesManager.DEFAULT_FISH_AUDIO_MODEL }
        val speechFile = getSpeakAudioFile(context, "$cleanVoiceId-$cleanModel-$text")
        if (speechFile.exists() && speechFile.length() > 0) {
            return@withContext Result.success(speechFile)
        }
        val requestJson = JSONObject().apply {
            put("text", text)
            put("reference_id", cleanVoiceId)
            put("format", "mp3")
        }

        val requestBody = requestJson.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("https://api.fish.audio/v1/tts")
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("model", cleanModel)
            .header("Accept", "audio/mpeg")
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val errorMsg = parseErrorMessage(errorBody, response.code)
                    return@withContext Result.failure(IOException(errorMsg))
                }

                val body = response.body ?: return@withContext Result.failure(IOException("Empty response body from Fish Audio"))
                val dir = getSpeakAudioDir(context)
                if (!dir.exists()) {
                    dir.mkdirs()
                }

                val tempFile = File(dir, "temp_speak_${System.currentTimeMillis()}.mp3")
                FileOutputStream(tempFile).use { fos ->
                    body.byteStream().use { input ->
                        input.copyTo(fos)
                    }
                }

                if (tempFile.exists() && tempFile.length() > 0) {
                    if (speechFile.exists()) {
                        speechFile.delete()
                    }
                    if (tempFile.renameTo(speechFile)) {
                        Result.success(speechFile)
                    } else {
                        tempFile.copyTo(speechFile, overwrite = true)
                        tempFile.delete()
                        Result.success(speechFile)
                    }
                } else {
                    tempFile.delete()
                    Result.failure(IOException("Failed to write audio file"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Plays the audio file using Android's MediaPlayer.
     */
    fun playAudio(
        file: File,
        onPlaybackStateChanged: (isPlaying: Boolean) -> Unit,
        onCompletion: () -> Unit
    ) {
        stopAudio()

        try {
            onPlaybackStateCallback = onPlaybackStateChanged
            onCompletionCallback = onCompletion

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                    onPlaybackStateCallback?.invoke(true)
                }
                setOnCompletionListener {
                    val completion = onCompletionCallback
                    stopAudio()
                    completion?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    val completion = onCompletionCallback
                    stopAudio()
                    completion?.invoke()
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            e.printStackTrace()
            stopAudio()
        }
    }

    /**
     * Stops audio playback and releases the MediaPlayer.
     */
    fun stopAudio() {
        val mp = mediaPlayer
        mediaPlayer = null
        mp?.let { player ->
            try {
                try {
                    player.stop()
                } catch (ignored: Exception) {
                }
                player.reset()
                player.release()
            } catch (ignored: Exception) {
                try {
                    player.release()
                } catch (e: Exception) {
                }
            }
        }
        onPlaybackStateCallback?.invoke(false)
        onPlaybackStateCallback = null
        onCompletionCallback = null
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (ignored: Exception) {
            false
        }
    }

    private fun parseErrorMessage(body: String, code: Int): String {
        return try {
            val json = JSONObject(body)
            val message = json.optString("message").ifBlank {
                json.optJSONObject("detail")?.optString("message") ?: json.optString("detail")
            }
            if (message.isNotBlank()) {
                message
            } else {
                "Fish Audio API Error (HTTP $code)"
            }
        } catch (ignored: Exception) {
            if (body.isNotBlank()) body.take(150) else "Fish Audio request failed (HTTP $code)"
        }
    }

    companion object {
        fun getAudioDir(context: Context): File {
            return File(context.cacheDir, "story_audio")
        }

        fun getAudioFile(context: Context, storyId: String): File {
            val safeId = storyId.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            return File(getAudioDir(context), "story_${safeId}.mp3")
        }

        fun deleteAudioForStory(context: Context, storyId: String): Boolean {
            val file = getAudioFile(context, storyId)
            return if (file.exists()) file.delete() else false
        }

        fun clearAllAudio(context: Context): Boolean {
            val dir = getAudioDir(context)
            return if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { it.delete() }
                true
            } else {
                false
            }
        }

        fun getSpeakAudioDir(context: Context): File {
            return File(context.cacheDir, "speak_audio")
        }

        fun getSpeakAudioFile(context: Context, key: String): File {
            val hash = key.hashCode().toString().replace("-", "n")
            return File(getSpeakAudioDir(context), "speak_${hash}.mp3")
        }

        fun clearSpeakAudio(context: Context): Boolean {
            val dir = getSpeakAudioDir(context)
            return if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { it.delete() }
                true
            } else {
                false
            }
        }
    }
}
