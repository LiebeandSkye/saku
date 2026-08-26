package com.saku.anki

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.saku.data.AnkiDeck
import com.saku.data.CardModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap

class AnkiDroidClient(private val context: Context) {

    companion object {
        private const val TAG = "AnkiDroidClient"
    }

    // Cache note type field names by Model ID to minimize IPC ContentProvider calls
    private val modelFieldCache = ConcurrentHashMap<Long, List<String>>()

    fun getInstalledAnkiPackage(): String? {
        val pm = context.packageManager
        for (pkg in AnkiDroidContract.KNOWN_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (e: PackageManager.NameNotFoundException) {
                // Try next package
            }
        }
        return null
    }

    fun getAuthority(): String {
        val pkg = getInstalledAnkiPackage() ?: AnkiDroidContract.DEFAULT_PACKAGE
        return AnkiDroidContract.getAuthorityForPackage(pkg)
    }

    fun isAnkiDroidInstalled(): Boolean {
        return getInstalledAnkiPackage() != null
    }

    fun isPermissionGranted(): Boolean {
        return context.checkSelfPermission(AnkiDroidContract.PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getDecks(): List<AnkiDeck> = withContext(Dispatchers.IO) {
        if (!isPermissionGranted()) {
            Log.w(TAG, "Cannot get decks: permission not granted")
            return@withContext emptyList()
        }

        val authority = getAuthority()
        val decksUri = AnkiDroidContract.Decks.getContentUri(authority)
        val decks = mutableListOf<AnkiDeck>()
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(
                decksUri,
                AnkiDroidContract.Decks.DEFAULT_PROJECTION,
                null,
                null,
                null
            )

            cursor?.let { c ->
                val idIdx = c.getColumnIndex(AnkiDroidContract.Decks.DECK_ID)
                val nameIdx = c.getColumnIndex(AnkiDroidContract.Decks.DECK_NAME)
                val countIdx = c.getColumnIndex(AnkiDroidContract.Decks.DECK_COUNTS)

                while (c.moveToNext()) {
                    val id = if (idIdx != -1) c.getLong(idIdx) else 0L
                    val name = if (nameIdx != -1) c.getString(nameIdx) else "Unnamed Deck"
                    var dueCount = 0

                    if (countIdx != -1) {
                        val countJson = c.getString(countIdx)
                        if (!countJson.isNullOrEmpty()) {
                            try {
                                val jsonArray = JSONArray(countJson)
                                // Format: [learnCount, reviewCount, newCount]
                                val learn = jsonArray.optInt(0, 0)
                                val review = jsonArray.optInt(1, 0)
                                val newCards = jsonArray.optInt(2, 0)
                                dueCount = learn + review + newCards
                            } catch (e: Exception) {
                                // Ignore JSON parse errors for counts
                            }
                        }
                    }

                    decks.add(AnkiDeck(id = id, name = name, dueCardCount = dueCount))
                }
            }
            Log.d(TAG, "Successfully fetched ${decks.size} decks from AnkiDroid ($authority)")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching decks from AnkiDroid ($authority)", e)
        } finally {
            cursor?.close()
        }

        decks
    }

    suspend fun getDueCards(deckId: Long, limit: Int = 30): List<CardModel> = withContext(Dispatchers.IO) {
        if (!isPermissionGranted()) {
            return@withContext listOf(getSamplePreviewCard())
        }

        val authority = getAuthority()
        val cardsUri = AnkiDroidContract.Cards.getContentUri(authority)
        val cards = mutableListOf<CardModel>()

        // 1. Determine Anki search query string
        // AnkiDroid Cards ContentProvider accepts search syntax directly in the selection parameter.
        val searchQueries = mutableListOf<String>()
        if (deckId > 0) {
            searchQueries.add("did:$deckId is:due")
            searchQueries.add("did:$deckId (is:due or is:new or is:learn)")
            searchQueries.add("did:$deckId")
        } else {
            searchQueries.add("is:due")
            searchQueries.add("is:due or is:new or is:learn")
            searchQueries.add("")
        }

        for (query in searchQueries) {
            val queryCards = queryCardsWithSelection(cardsUri, query, limit)
            if (queryCards.isNotEmpty()) {
                cards.addAll(queryCards)
                break
            }
        }

        if (cards.isEmpty()) {
            Log.d(TAG, "No cards found for deck $deckId, returning sample card")
            cards.add(getSamplePreviewCard())
        }

        cards
    }

    private fun queryCardsWithSelection(cardsUri: Uri, selectionQuery: String, limit: Int): List<CardModel> {
        val result = mutableListOf<CardModel>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                cardsUri,
                AnkiDroidContract.Cards.DEFAULT_PROJECTION,
                selectionQuery.ifEmpty { null },
                null,
                null
            )

            cursor?.let { c ->
                val cardIdIdx = c.getColumnIndex(AnkiDroidContract.Cards._ID)
                val noteIdIdx = c.getColumnIndex(AnkiDroidContract.Cards.NOTE_ID)
                val cardOrdIdx = c.getColumnIndex(AnkiDroidContract.Cards.CARD_ORD)
                val deckIdIdx = c.getColumnIndex(AnkiDroidContract.Cards.DECK_ID)
                val ivlIdx = c.getColumnIndex(AnkiDroidContract.Cards.INTERVAL)
                val questionIdx = c.getColumnIndex(AnkiDroidContract.Cards.QUESTION_SIMPLE)
                val answerIdx = c.getColumnIndex(AnkiDroidContract.Cards.ANSWER_SIMPLE)

                var count = 0
                while (c.moveToNext() && count < limit) {
                    val cardId = if (cardIdIdx != -1) c.getLong(cardIdIdx) else 0L
                    val noteId = if (noteIdIdx != -1) c.getLong(noteIdIdx) else 0L
                    val cardOrd = if (cardOrdIdx != -1) c.getInt(cardOrdIdx) else 0
                    val dId = if (deckIdIdx != -1) c.getLong(deckIdIdx) else 0L
                    val interval = if (ivlIdx != -1) c.getInt(ivlIdx) else 0
                    val qSimple = if (questionIdx != -1) c.getString(questionIdx) ?: "" else ""
                    val aSimple = if (answerIdx != -1) c.getString(answerIdx) ?: "" else ""

                    val cardModel = fetchNoteDetails(noteId, cardId, cardOrd, dId, interval, qSimple, aSimple)
                    if (cardModel != null) {
                        result.add(cardModel)
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying cards with query '$selectionQuery'", e)
        } finally {
            cursor?.close()
        }
        return result
    }

    private fun fetchNoteDetails(
        noteId: Long,
        cardId: Long,
        cardOrd: Int,
        deckId: Long,
        interval: Int,
        fallbackQuestion: String,
        fallbackAnswer: String
    ): CardModel? {
        if (noteId <= 0) {
            // Fallback from card question/answer if noteId is not available
            val parsed = JapaneseFieldParser.mapFieldsToJapaneseCard(
                emptyList(),
                emptyList(),
                fallbackQuestion,
                fallbackAnswer
            )
            return CardModel(
                cardId = cardId,
                noteId = noteId,
                deckId = deckId,
                cardOrd = cardOrd,
                kanji = parsed.kanji,
                kana = parsed.kana,
                romaji = parsed.romaji,
                meaning = parsed.meaning,
                example = parsed.example,
                exampleSentence = parsed.exampleSentence,
                exampleTranslation = parsed.exampleTranslation,
                intervalDays = interval,
                isDue = true
            )
        }

        val authority = getAuthority()
        var noteCursor: Cursor? = null
        return try {
            val noteUri = Uri.withAppendedPath(AnkiDroidContract.Notes.getContentUri(authority), noteId.toString())
            noteCursor = context.contentResolver.query(
                noteUri,
                arrayOf(AnkiDroidContract.Notes._ID, AnkiDroidContract.Notes.MID, AnkiDroidContract.Notes.FLDS),
                null,
                null,
                null
            )

            if (noteCursor != null && noteCursor.moveToFirst()) {
                val midIdx = noteCursor.getColumnIndex(AnkiDroidContract.Notes.MID)
                val fldsIdx = noteCursor.getColumnIndex(AnkiDroidContract.Notes.FLDS)

                val modelId = if (midIdx != -1) noteCursor.getLong(midIdx) else -1L
                val rawFields = if (fldsIdx != -1) noteCursor.getString(fldsIdx) ?: "" else ""
                val fieldValues = rawFields.split("\u001f")

                val fieldNames = getModelFieldNames(authority, modelId)
                val parsed = JapaneseFieldParser.mapFieldsToJapaneseCard(
                    fieldNames = fieldNames,
                    fieldValues = fieldValues,
                    fallbackQuestion = fallbackQuestion,
                    fallbackAnswer = fallbackAnswer
                )

                CardModel(
                    cardId = cardId,
                    noteId = noteId,
                    deckId = deckId,
                    cardOrd = cardOrd,
                    kanji = parsed.kanji,
                    kana = parsed.kana,
                    romaji = parsed.romaji,
                    meaning = parsed.meaning,
                    example = parsed.example,
                    exampleSentence = parsed.exampleSentence,
                    exampleTranslation = parsed.exampleTranslation,
                    intervalDays = interval,
                    isDue = true
                )
            } else {
                // If direct note query returned empty, construct from card question/answer
                val parsed = JapaneseFieldParser.mapFieldsToJapaneseCard(
                    emptyList(),
                    emptyList(),
                    fallbackQuestion,
                    fallbackAnswer
                )
                CardModel(
                    cardId = cardId,
                    noteId = noteId,
                    deckId = deckId,
                    cardOrd = cardOrd,
                    kanji = parsed.kanji,
                    kana = parsed.kana,
                    romaji = parsed.romaji,
                    meaning = parsed.meaning,
                    example = parsed.example,
                    exampleSentence = parsed.exampleSentence,
                    exampleTranslation = parsed.exampleTranslation,
                    intervalDays = interval,
                    isDue = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading note fields for note $noteId", e)
            null
        } finally {
            noteCursor?.close()
        }
    }

    private fun getModelFieldNames(authority: String, modelId: Long): List<String> {
        if (modelId <= 0) return emptyList()
        modelFieldCache[modelId]?.let { return it }

        var modelCursor: Cursor? = null
        try {
            val modelUri = Uri.withAppendedPath(AnkiDroidContract.Models.getContentUri(authority), modelId.toString())
            modelCursor = context.contentResolver.query(
                modelUri,
                arrayOf(AnkiDroidContract.Models.FIELD_NAMES),
                null,
                null,
                null
            )
            if (modelCursor != null && modelCursor.moveToFirst()) {
                val fldNamesIdx = modelCursor.getColumnIndex(AnkiDroidContract.Models.FIELD_NAMES)
                val rawNames = if (fldNamesIdx != -1) modelCursor.getString(fldNamesIdx) ?: "" else ""
                if (rawNames.isNotEmpty()) {
                    val names = rawNames.split("\u001f")
                    modelFieldCache[modelId] = names
                    return names
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading model field names for model $modelId", e)
        } finally {
            modelCursor?.close()
        }
        return emptyList()
    }

    /**
     * Answers a card in AnkiDroid and updates FSRS / SM-2 spaced repetition state.
     * ease: 1 = Again, 2 = Hard, 3 = Good, 4 = Easy
     */
    suspend fun answerCard(noteId: Long, cardOrd: Int, ease: Int, timeTakenMs: Long = 5000L): Boolean = withContext(Dispatchers.IO) {
        if (!isPermissionGranted() || noteId <= 0) return@withContext false

        val authority = getAuthority()
        val scheduleUri = AnkiDroidContract.ReviewInfo.getContentUri(authority)

        return@withContext try {
            val values = ContentValues().apply {
                put(AnkiDroidContract.ReviewInfo.NOTE_ID, noteId)
                put(AnkiDroidContract.ReviewInfo.CARD_ORD, cardOrd)
                put(AnkiDroidContract.ReviewInfo.EASE, ease)
                put(AnkiDroidContract.ReviewInfo.TIME_TAKEN, timeTakenMs)
            }
            val updated = context.contentResolver.update(scheduleUri, values, null, null)
            Log.d(TAG, "Answered card noteId=$noteId ord=$cardOrd ease=$ease -> updated rows: $updated")
            updated > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error answering card noteId=$noteId ord=$cardOrd with ease $ease", e)
            false
        }
    }

    suspend fun answerCard(card: CardModel, ease: Int): Boolean {
        return answerCard(card.noteId, card.cardOrd, ease)
    }

    fun getSamplePreviewCard(): CardModel {
        return CardModel(
            cardId = -1L,
            noteId = -1L,
            deckId = -1L,
            cardOrd = 0,
            kanji = "日",
            kana = "ひ",
            romaji = "hi",
            meaning = "sun, day",
            example = "日本 • Japan",
            intervalDays = 4,
            isDue = true
        )
    }
}
