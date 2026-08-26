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

class AnkiDroidClient(private val context: Context) {

    companion object {
        private const val TAG = "AnkiDroidClient"
    }

    fun isAnkiDroidInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.ichi2.anki", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun isPermissionGranted(): Boolean {
        return context.checkSelfPermission(AnkiDroidContract.PERMISSION) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getDecks(): List<AnkiDeck> = withContext(Dispatchers.IO) {
        if (!isPermissionGranted()) return@withContext emptyList()

        val decks = mutableListOf<AnkiDeck>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                AnkiDroidContract.Decks.CONTENT_URI,
                arrayOf(AnkiDroidContract.Decks._ID, AnkiDroidContract.Decks.DECK_NAME),
                null,
                null,
                "${AnkiDroidContract.Decks.DECK_NAME} ASC"
            )

            cursor?.let {
                val idIdx = it.getColumnIndex(AnkiDroidContract.Decks._ID)
                val nameIdx = it.getColumnIndex(AnkiDroidContract.Decks.DECK_NAME)
                while (it.moveToNext()) {
                    val id = if (idIdx != -1) it.getLong(idIdx) else 0L
                    val name = if (nameIdx != -1) it.getString(nameIdx) else "Unnamed Deck"
                    decks.add(AnkiDeck(id = id, name = name))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching decks from AnkiDroid", e)
        } finally {
            cursor?.close()
        }
        decks
    }

    suspend fun getDueCards(deckId: Long, limit: Int = 30): List<CardModel> = withContext(Dispatchers.IO) {
        if (!isPermissionGranted()) {
            return@withContext listOf(getSamplePreviewCard())
        }

        val cards = mutableListOf<CardModel>()
        var cursor: Cursor? = null
        try {
            // Query due cards from AnkiDroid
            val selection = if (deckId > 0) "${AnkiDroidContract.Cards.DECK_ID} = ?" else null
            val selectionArgs = if (deckId > 0) arrayOf(deckId.toString()) else null

            cursor = context.contentResolver.query(
                AnkiDroidContract.Cards.CONTENT_URI,
                arrayOf(
                    AnkiDroidContract.Cards._ID,
                    AnkiDroidContract.Cards.NOTE_ID,
                    AnkiDroidContract.Cards.DECK_ID,
                    AnkiDroidContract.Cards.INTERVAL,
                    AnkiDroidContract.Cards.QUEUE
                ),
                selection,
                selectionArgs,
                "${AnkiDroidContract.Cards.DUE} ASC LIMIT $limit"
            )

            cursor?.let { c ->
                val cardIdIdx = c.getColumnIndex(AnkiDroidContract.Cards._ID)
                val noteIdIdx = c.getColumnIndex(AnkiDroidContract.Cards.NOTE_ID)
                val deckIdIdx = c.getColumnIndex(AnkiDroidContract.Cards.DECK_ID)
                val ivlIdx = c.getColumnIndex(AnkiDroidContract.Cards.INTERVAL)

                while (c.moveToNext()) {
                    val cardId = if (cardIdIdx != -1) c.getLong(cardIdIdx) else 0L
                    val noteId = if (noteIdIdx != -1) c.getLong(noteIdIdx) else 0L
                    val dId = if (deckIdIdx != -1) c.getLong(deckIdIdx) else 0L
                    val interval = if (ivlIdx != -1) c.getInt(ivlIdx) else 0

                    val cardModel = fetchNoteDetails(noteId, cardId, dId, interval)
                    if (cardModel != null) {
                        cards.add(cardModel)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching due cards", e)
        } finally {
            cursor?.close()
        }

        if (cards.isEmpty()) {
            cards.add(getSamplePreviewCard())
        }

        cards
    }

    private fun fetchNoteDetails(noteId: Long, cardId: Long, deckId: Long, interval: Int): CardModel? {
        var cursor: Cursor? = null
        return try {
            cursor = context.contentResolver.query(
                Uri.withAppendedPath(AnkiDroidContract.Notes.CONTENT_URI, noteId.toString()),
                arrayOf(AnkiDroidContract.Notes.FIELDS),
                null,
                null,
                null
            )

            if (cursor != null && cursor.moveToFirst()) {
                val fldsIdx = cursor.getColumnIndex(AnkiDroidContract.Notes.FIELDS)
                val rawFields = if (fldsIdx != -1) cursor.getString(fldsIdx) else ""
                val fields = rawFields.split("\u001f")

                val rawKanji = fields.getOrNull(0) ?: ""
                val rawKana = fields.getOrNull(1) ?: ""
                val rawMeaning = fields.getOrNull(2) ?: ""
                val rawExample = fields.getOrNull(3) ?: ""

                val (kanji, kanaFromExpr) = JapaneseFieldParser.extractKanjiAndKana(rawKanji)
                val cleanKana = if (rawKana.isNotEmpty()) JapaneseFieldParser.cleanHtml(rawKana) else kanaFromExpr
                val romaji = JapaneseFieldParser.kanaToRomaji(cleanKana)
                val cleanMeaning = JapaneseFieldParser.cleanHtml(rawMeaning)
                val cleanExample = JapaneseFieldParser.cleanHtml(rawExample)

                CardModel(
                    cardId = cardId,
                    noteId = noteId,
                    deckId = deckId,
                    kanji = kanji.ifEmpty { "日" },
                    kana = cleanKana.ifEmpty { "ひ" },
                    romaji = romaji.ifEmpty { "hi" },
                    meaning = cleanMeaning.ifEmpty { "sun, day" },
                    example = cleanExample.ifEmpty { "日本 • Japan" },
                    intervalDays = interval,
                    isDue = true
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error reading note fields for note $noteId", e)
            null
        } finally {
            cursor?.close()
        }
    }

    /**
     * Answers a card in AnkiDroid and preserves FSRS/SM-2 spaced repetition state.
     * ease: 1 = Again, 2 = Hard, 3 = Good, 4 = Easy
     */
    suspend fun answerCard(cardId: Long, ease: Int): Boolean = withContext(Dispatchers.IO) {
        if (!isPermissionGranted() || cardId <= 0) return@withContext false

        return@withContext try {
            val answerUri = Uri.parse("${AnkiDroidContract.Cards.CONTENT_URI}/$cardId/answer")
            val values = ContentValues().apply {
                put("ease", ease)
            }
            val updated = context.contentResolver.update(answerUri, values, null, null)
            updated > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error answering card $cardId with ease $ease", e)
            false
        }
    }

    fun getSamplePreviewCard(): CardModel {
        return CardModel(
            cardId = -1L,
            noteId = -1L,
            deckId = -1L,
            kanji = "日",
            kana = "ひ",
            romaji = "hi",
            meaning = "sun",
            example = "日本 • Japan",
            intervalDays = 4,
            isDue = true
        )
    }
}
