package com.saku.anki

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import com.saku.data.AnkiVocabularyItem
import com.saku.data.ReadingVocabularySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReadingVocabularyExtractor(private val context: Context) {

    companion object {
        private const val TAG = "ReadingVocabExtractor"
    }

    private val ankiClient = AnkiDroidClient(context)

    suspend fun extractVocabulary(selectedDeckIds: Set<Long> = emptySet()): ReadingVocabularySummary = withContext(Dispatchers.IO) {
        if (!ankiClient.isPermissionGranted()) {
            return@withContext ReadingVocabularySummary()
        }

        val authority = ankiClient.getAuthority()
        val cardsUri = AnkiDroidContract.Cards.getContentUri(authority)
        val studiedMap = mutableMapOf<String, AnkiVocabularyItem>()
        val suspendedMap = mutableMapOf<String, AnkiVocabularyItem>()

        val selection = if (selectedDeckIds.isNotEmpty()) {
            "${AnkiDroidContract.Cards.DECK_ID} IN (${selectedDeckIds.joinToString(",")})"
        } else {
            null
        }

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                cardsUri,
                null,
                selection,
                null,
                null
            )

            cursor?.let { c ->
                val noteIdIdx = c.getColumnIndex(AnkiDroidContract.Cards.NOTE_ID)
                val queueIdx = c.getColumnIndex(AnkiDroidContract.Cards.RAW_QUEUE)
                val ivlIdx = c.getColumnIndex(AnkiDroidContract.Cards.INTERVAL)
                val qSimpleIdx = c.getColumnIndex(AnkiDroidContract.Cards.QUESTION_SIMPLE)
                val qIdx = c.getColumnIndex(AnkiDroidContract.Cards.QUESTION)
                val aSimpleIdx = c.getColumnIndex(AnkiDroidContract.Cards.ANSWER_SIMPLE)
                val aIdx = c.getColumnIndex(AnkiDroidContract.Cards.ANSWER)

                val processedNoteIds = mutableSetOf<Long>()

                while (c.moveToNext()) {
                    val noteId = if (noteIdIdx != -1) c.getLong(noteIdIdx) else 0L
                    val queue = if (queueIdx != -1) c.getInt(queueIdx) else 0
                    val interval = if (ivlIdx != -1) c.getInt(ivlIdx) else 0

                    val isSuspended = (queue == -1)
                    val isStudied = (queue in 1..3 || interval > 0)

                    if (!isSuspended && !isStudied) {
                        // Unstudied new card, skip
                        continue
                    }

                    if (noteId > 0 && !processedNoteIds.contains(noteId)) {
                        processedNoteIds.add(noteId)

                        val qFallback = if (qSimpleIdx != -1) c.getString(qSimpleIdx) else null
                            ?: (if (qIdx != -1) c.getString(qIdx) ?: "" else "")
                        val aFallback = if (aSimpleIdx != -1) c.getString(aSimpleIdx) else null
                            ?: (if (aIdx != -1) c.getString(aIdx) ?: "" else "")

                        val vocabItem = extractNoteVocabulary(
                            authority = authority,
                            noteId = noteId,
                            fallbackQuestion = qFallback,
                            fallbackAnswer = aFallback,
                            isSuspended = isSuspended
                        )

                        if (vocabItem != null) {
                            val key = vocabItem.displayWord
                            if (isSuspended) {
                                if (!studiedMap.containsKey(key)) {
                                    suspendedMap[key] = vocabItem
                                }
                            } else {
                                studiedMap[key] = vocabItem
                                suspendedMap.remove(key)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting vocabulary from AnkiDroid cards", e)
        } finally {
            cursor?.close()
        }

        val allWords = (studiedMap.values + suspendedMap.values).sortedBy { it.displayWord }
        ReadingVocabularySummary(
            studiedCount = studiedMap.size,
            suspendedCount = suspendedMap.size,
            words = allWords
        )
    }

    private fun extractNoteVocabulary(
        authority: String,
        noteId: Long,
        fallbackQuestion: String,
        fallbackAnswer: String,
        isSuspended: Boolean
    ): AnkiVocabularyItem? {
        val noteUri = Uri.withAppendedPath(AnkiDroidContract.Notes.getContentUri(authority), noteId.toString())
        var noteCursor: Cursor? = null
        try {
            noteCursor = context.contentResolver.query(noteUri, null, null, null, null)
            if (noteCursor != null && noteCursor.moveToFirst()) {
                val midIdx = noteCursor.getColumnIndex(AnkiDroidContract.Notes.MID)
                val fldsIdx = noteCursor.getColumnIndex(AnkiDroidContract.Notes.FLDS)
                val modelId = if (midIdx != -1) noteCursor.getLong(midIdx) else -1L
                val rawFields = if (fldsIdx != -1) noteCursor.getString(fldsIdx) ?: "" else ""

                if (rawFields.isNotEmpty()) {
                    val fieldValues = rawFields.split("\u001f")
                    val fieldNames = getModelFieldNames(authority, modelId)
                    val parsed = JapaneseFieldParser.mapFieldsToJapaneseCard(
                        fieldNames = fieldNames,
                        fieldValues = fieldValues,
                        fallbackQuestion = fallbackQuestion,
                        fallbackAnswer = fallbackAnswer
                    )

                    if (!JapaneseFieldParser.isInstructionOrInvalidCard(parsed.kanji, parsed.kana, parsed.meaning)) {
                        val word = parsed.kanji.ifBlank { parsed.kana }
                        if (word.isNotBlank()) {
                            return AnkiVocabularyItem(
                                kanji = parsed.kanji,
                                reading = parsed.kana,
                                meaning = parsed.meaning,
                                isSuspended = isSuspended
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed reading note details for noteId $noteId: ${e.message}")
        } finally {
            noteCursor?.close()
        }

        // Fallback to simple question/answer parsing
        val fallbackParsed = JapaneseFieldParser.mapFieldsToJapaneseCard(
            emptyList(),
            emptyList(),
            fallbackQuestion,
            fallbackAnswer
        )
        if (!JapaneseFieldParser.isInstructionOrInvalidCard(fallbackParsed.kanji, fallbackParsed.kana, fallbackParsed.meaning)) {
            val word = fallbackParsed.kanji.ifBlank { fallbackParsed.kana }
            if (word.isNotBlank()) {
                return AnkiVocabularyItem(
                    kanji = fallbackParsed.kanji,
                    reading = fallbackParsed.kana,
                    meaning = fallbackParsed.meaning,
                    isSuspended = isSuspended
                )
            }
        }

        return null
    }

    private val modelFieldCache = mutableMapOf<Long, List<String>>()

    private fun getModelFieldNames(authority: String, modelId: Long): List<String> {
        if (modelId <= 0) return emptyList()
        modelFieldCache[modelId]?.let { return it }

        var modelCursor: Cursor? = null
        try {
            val modelUri = Uri.withAppendedPath(AnkiDroidContract.Models.getContentUri(authority), modelId.toString())
            modelCursor = context.contentResolver.query(modelUri, null, null, null, null)
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
            Log.e(TAG, "Error fetching model fields for $modelId", e)
        } finally {
            modelCursor?.close()
        }
        return emptyList()
    }
}
