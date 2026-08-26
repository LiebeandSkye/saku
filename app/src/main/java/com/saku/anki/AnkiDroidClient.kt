package com.saku.anki

import android.content.ContentValues
import android.content.Context
import android.content.Intent
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
                null,
                null,
                null,
                null
            )

            cursor?.let { c ->
                val idIdx = c.getColumnIndex(AnkiDroidContract.Decks.DECK_ID)
                val nameIdx = c.getColumnIndex(AnkiDroidContract.Decks.DECK_NAME)
                val countIdx = c.getColumnIndex(AnkiDroidContract.Decks.DECK_COUNTS).takeIf { it != -1 }
                    ?: c.getColumnIndex(AnkiDroidContract.Decks.DECK_COUNTS_LEGACY)

                while (c.moveToNext()) {
                    val id = if (idIdx != -1) c.getLong(idIdx) else 0L
                    val name = if (nameIdx != -1) c.getString(nameIdx) else "Unnamed Deck"
                    var dueCount = 0
                    var newCount = 0
                    var learnCount = 0
                    var reviewCount = 0

                    if (countIdx != -1) {
                        val countRaw = c.getString(countIdx)
                        if (!countRaw.isNullOrEmpty()) {
                            try {
                                if (countRaw.startsWith("[")) {
                                    val jsonArray = JSONArray(countRaw)
                                    learnCount = jsonArray.optInt(0, 0)
                                    reviewCount = jsonArray.optInt(1, 0)
                                    newCount = jsonArray.optInt(2, 0)
                                    dueCount = learnCount + reviewCount + newCount
                                } else {
                                    dueCount = countRaw.toIntOrNull() ?: 0
                                }
                            } catch (e: Exception) {
                                // Ignore parse error
                            }
                        }
                    }

                    decks.add(
                        AnkiDeck(
                            id = id,
                            name = name,
                            dueCardCount = dueCount,
                            newCount = newCount,
                            learnCount = learnCount,
                            reviewCount = reviewCount
                        )
                    )
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

        // 1. Determine Anki search query strings with explicit exclusion of suspended and buried cards
        val searchQueries = mutableListOf<String>()
        if (deckId > 0) {
            searchQueries.add("did:$deckId -is:suspended -is:buried (is:due or is:learn)")
            searchQueries.add("did:$deckId -is:suspended -is:buried is:new")
            searchQueries.add("did:$deckId -is:suspended -is:buried")
            searchQueries.add("did:$deckId")
        } else {
            searchQueries.add("-is:suspended -is:buried (is:due or is:learn)")
            searchQueries.add("-is:suspended -is:buried is:new")
            searchQueries.add("-is:suspended -is:buried")
            searchQueries.add("")
        }

        for (query in searchQueries) {
            val queryCards = queryCardsWithSelection(cardsUri, query, limit, deckId)
            if (queryCards.isNotEmpty()) {
                cards.addAll(queryCards)
                break
            }
        }

        // 2. Secondary Fallback: Query Notes ContentProvider directly if cards search was empty
        if (cards.isEmpty()) {
            val directNotes = queryNotesDirectly(authority, deckId, limit)
            if (directNotes.isNotEmpty()) {
                cards.addAll(directNotes)
            }
        }

        if (cards.isEmpty()) {
            Log.d(TAG, "No valid cards found for deck $deckId in AnkiDroid, returning preview placeholder")
            cards.add(getSamplePreviewCard())
        }

        cards
    }

    private fun queryCardsWithSelection(
        cardsUri: Uri,
        selectionQuery: String,
        limit: Int,
        selectedDeckId: Long = -1L
    ): List<CardModel> {
        val result = mutableListOf<CardModel>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                cardsUri,
                null,
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
                val questionSimpleIdx = c.getColumnIndex(AnkiDroidContract.Cards.QUESTION_SIMPLE)
                val questionIdx = c.getColumnIndex(AnkiDroidContract.Cards.QUESTION)
                val answerSimpleIdx = c.getColumnIndex(AnkiDroidContract.Cards.ANSWER_SIMPLE)
                val answerIdx = c.getColumnIndex(AnkiDroidContract.Cards.ANSWER)

                var count = 0
                while (c.moveToNext() && count < limit) {
                    val cardId = if (cardIdIdx != -1) c.getLong(cardIdIdx) else 0L
                    val noteId = if (noteIdIdx != -1) c.getLong(noteIdIdx) else 0L
                    val cardOrd = if (cardOrdIdx != -1) c.getInt(cardOrdIdx) else 0
                    val dId = if (deckIdIdx != -1) c.getLong(deckIdIdx) else selectedDeckId
                    val interval = if (ivlIdx != -1) c.getInt(ivlIdx) else 0

                    val qSimple = if (questionSimpleIdx != -1) c.getString(questionSimpleIdx) else null
                    val qStr = qSimple?.takeIf { it.isNotBlank() }
                        ?: (if (questionIdx != -1) c.getString(questionIdx) ?: "" else "")

                    val aSimple = if (answerSimpleIdx != -1) c.getString(answerSimpleIdx) else null
                    val aStr = aSimple?.takeIf { it.isNotBlank() }
                        ?: (if (answerIdx != -1) c.getString(answerIdx) ?: "" else "")

                    val cardModel = fetchNoteDetails(noteId, cardId, cardOrd, dId, interval, qStr, aStr)
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

    private fun queryNotesDirectly(authority: String, deckId: Long, limit: Int): List<CardModel> {
        val result = mutableListOf<CardModel>()
        val notesUri = AnkiDroidContract.Notes.getContentUri(authority)
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                notesUri,
                null,
                null,
                null,
                null
            )

            cursor?.let { c ->
                val idIdx = c.getColumnIndex(AnkiDroidContract.Notes._ID)
                val midIdx = c.getColumnIndex(AnkiDroidContract.Notes.MID)
                val fldsIdx = c.getColumnIndex(AnkiDroidContract.Notes.FLDS)

                var count = 0
                while (c.moveToNext() && count < limit) {
                    val noteId = if (idIdx != -1) c.getLong(idIdx) else 0L
                    val modelId = if (midIdx != -1) c.getLong(midIdx) else -1L
                    val rawFields = if (fldsIdx != -1) c.getString(fldsIdx) ?: "" else ""

                    if (rawFields.isNotEmpty()) {
                        val fieldValues = rawFields.split("\u001f")
                        val fieldNames = getModelFieldNames(authority, modelId)
                        val parsed = JapaneseFieldParser.mapFieldsToJapaneseCard(
                            fieldNames = fieldNames,
                            fieldValues = fieldValues
                        )

                        // Skip non-study instruction cards
                        if (JapaneseFieldParser.isInstructionOrInvalidCard(parsed.kanji, parsed.kana, parsed.meaning)) {
                            continue
                        }

                        result.add(
                            CardModel(
                                cardId = noteId,
                                noteId = noteId,
                                deckId = deckId,
                                cardOrd = 0,
                                kanji = parsed.kanji,
                                kana = parsed.kana,
                                furigana = parsed.furigana,
                                romaji = parsed.romaji,
                                meaning = parsed.meaning,
                                example = parsed.example,
                                exampleSentence = parsed.exampleSentence,
                                exampleFurigana = parsed.exampleFurigana,
                                exampleTranslation = parsed.exampleTranslation,
                                exampleFuriganaLine = parsed.exampleFuriganaLine,
                                exampleSentenceLine = parsed.exampleSentenceLine,
                                intervalDays = 0,
                                isDue = true
                            )
                        )
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying notes directly from AnkiDroid ($authority)", e)
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
            val parsed = JapaneseFieldParser.mapFieldsToJapaneseCard(
                emptyList(),
                emptyList(),
                fallbackQuestion,
                fallbackAnswer
            )
            if (JapaneseFieldParser.isInstructionOrInvalidCard(parsed.kanji, parsed.kana, parsed.meaning)) {
                return null
            }
            return CardModel(
                cardId = cardId,
                noteId = noteId,
                deckId = deckId,
                cardOrd = cardOrd,
                kanji = parsed.kanji,
                kana = parsed.kana,
                furigana = parsed.furigana,
                romaji = parsed.romaji,
                meaning = parsed.meaning,
                example = parsed.example,
                exampleSentence = parsed.exampleSentence,
                exampleFurigana = parsed.exampleFurigana,
                exampleTranslation = parsed.exampleTranslation,
                exampleFuriganaLine = parsed.exampleFuriganaLine,
                exampleSentenceLine = parsed.exampleSentenceLine,
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
                null,
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

                // Skip non-study instruction cards (e.g. Kaishi 1.5k Welcome Card)
                if (JapaneseFieldParser.isInstructionOrInvalidCard(parsed.kanji, parsed.kana, parsed.meaning)) {
                    return null
                }

                CardModel(
                    cardId = cardId,
                    noteId = noteId,
                    deckId = deckId,
                    cardOrd = cardOrd,
                    kanji = parsed.kanji,
                    kana = parsed.kana,
                    furigana = parsed.furigana,
                    romaji = parsed.romaji,
                    meaning = parsed.meaning,
                    example = parsed.example,
                    exampleSentence = parsed.exampleSentence,
                    exampleFurigana = parsed.exampleFurigana,
                    exampleTranslation = parsed.exampleTranslation,
                    exampleFuriganaLine = parsed.exampleFuriganaLine,
                    exampleSentenceLine = parsed.exampleSentenceLine,
                    intervalDays = interval,
                    isDue = true
                )
            } else {
                val parsed = JapaneseFieldParser.mapFieldsToJapaneseCard(
                    emptyList(),
                    emptyList(),
                    fallbackQuestion,
                    fallbackAnswer
                )
                if (JapaneseFieldParser.isInstructionOrInvalidCard(parsed.kanji, parsed.kana, parsed.meaning)) {
                    return null
                }
                CardModel(
                    cardId = cardId,
                    noteId = noteId,
                    deckId = deckId,
                    cardOrd = cardOrd,
                    kanji = parsed.kanji,
                    kana = parsed.kana,
                    furigana = parsed.furigana,
                    romaji = parsed.romaji,
                    meaning = parsed.meaning,
                    example = parsed.example,
                    exampleSentence = parsed.exampleSentence,
                    exampleFurigana = parsed.exampleFurigana,
                    exampleTranslation = parsed.exampleTranslation,
                    exampleFuriganaLine = parsed.exampleFuriganaLine,
                    exampleSentenceLine = parsed.exampleSentenceLine,
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
                null,
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

    fun getOpenAnkiIntent(noteId: Long = -1L): Intent {
        val pm = context.packageManager
        for (pkg in AnkiDroidContract.KNOWN_PACKAGES) {
            val launchIntent = pm.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                return launchIntent
            }
        }
        return Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.ichi2.anki")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getSamplePreviewCard(): CardModel {
        return CardModel(
            cardId = -1L,
            noteId = -1L,
            deckId = -1L,
            cardOrd = 0,
            kanji = "九",
            kana = "きゅう",
            furigana = "きゅう",
            romaji = "kyuu",
            meaning = "nine",
            example = "野球は九人で1チームです。 • In baseball there are nine people on one team.",
            exampleSentence = "野球は九人で1チームです。",
            exampleFurigana = "野球[やきゅう]は 九人[きゅうにん]で 1チームです。",
            exampleTranslation = "In baseball there are nine people on one team.",
            exampleFuriganaLine = "や きゅう   きゅうにん",
            exampleSentenceLine = "野球は九人で1チームです。",
            newCount = 15,
            learnCount = 17,
            reviewCount = 21,
            intervalDays = 4,
            isDue = true
        )
    }
}
