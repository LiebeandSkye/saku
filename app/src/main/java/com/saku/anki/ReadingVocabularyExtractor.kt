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
        val notesUri = AnkiDroidContract.Notes.getContentUri(authority)
        val studiedMap = mutableMapOf<String, AnkiVocabularyItem>()
        val suspendedMap = mutableMapOf<String, AnkiVocabularyItem>()

        // 1. Resolve deck search filter using Anki's Browser search syntax
        val allDecks = ankiClient.getDecks()
        val targetDecks = if (selectedDeckIds.isNotEmpty()) {
            allDecks.filter { it.id in selectedDeckIds }
        } else {
            emptyList()
        }

        val deckFilter = if (selectedDeckIds.isNotEmpty()) {
            val clauses = mutableListOf<String>()
            targetDecks.forEach { deck ->
                val cleanName = deck.name.replace("\"", "\\\"")
                clauses.add("deck:\"$cleanName\"")
            }
            selectedDeckIds.forEach { id ->
                clauses.add("did:$id")
            }
            if (clauses.isNotEmpty()) {
                "(" + clauses.distinct().joinToString(" or ") + ")"
            } else {
                "(" + selectedDeckIds.joinToString(" or ") { "did:$it" } + ")"
            }
        } else {
            ""
        }

        // 2. Query Studied Cards: (-is:new or prop:reps>0) -is:suspended
        val studiedSearchQuery = if (deckFilter.isNotBlank()) {
            "$deckFilter (-is:new or prop:reps>0) -is:suspended"
        } else {
            "(-is:new or prop:reps>0) -is:suspended"
        }

        // 3. Query Suspended Cards: is:suspended
        val suspendedSearchQuery = if (deckFilter.isNotBlank()) {
            "$deckFilter is:suspended"
        } else {
            "is:suspended"
        }

        // Extract studied vocabulary
        extractFromNotesQuery(
            authority = authority,
            notesUri = notesUri,
            searchQuery = studiedSearchQuery,
            isSuspended = false,
            outMap = studiedMap
        )

        // Extract suspended vocabulary
        extractFromNotesQuery(
            authority = authority,
            notesUri = notesUri,
            searchQuery = suspendedSearchQuery,
            isSuspended = true,
            outMap = suspendedMap
        )

        // Fallback 1: If studiedMap is empty and targetDecks were found, try with simple deck names
        if (studiedMap.isEmpty() && targetDecks.isNotEmpty()) {
            val simpleDeckFilter = "(" + targetDecks.joinToString(" or ") { "deck:\"${it.name.replace("\"", "\\\"")}\"" } + ")"
            extractFromNotesQuery(
                authority = authority,
                notesUri = notesUri,
                searchQuery = "$simpleDeckFilter -is:new",
                isSuspended = false,
                outMap = studiedMap
            )
        }

        // Fallback 2: If both maps are still completely empty, fallback to Cards content provider
        if (studiedMap.isEmpty() && suspendedMap.isEmpty()) {
            extractFromCardsFallback(
                authority = authority,
                deckFilter = deckFilter,
                studiedMap = studiedMap,
                suspendedMap = suspendedMap
            )
        }

        // If a word is already in studiedMap, remove it from suspendedMap so it counts as studied
        studiedMap.keys.forEach { suspendedMap.remove(it) }

        val allWords = (studiedMap.values + suspendedMap.values).sortedBy { it.displayWord }
        Log.d(TAG, "Extracted ${studiedMap.size} studied words and ${suspendedMap.size} suspended cards (total: ${allWords.size})")

        ReadingVocabularySummary(
            studiedCount = studiedMap.size,
            suspendedCount = suspendedMap.size,
            words = allWords
        )
    }

    private fun extractFromNotesQuery(
        authority: String,
        notesUri: Uri,
        searchQuery: String,
        isSuspended: Boolean,
        outMap: MutableMap<String, AnkiVocabularyItem>
    ) {
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                notesUri,
                null,
                searchQuery,
                null,
                null
            )

            cursor?.let { c ->
                val midIdx = c.getColumnIndex(AnkiDroidContract.Notes.MID)
                val fldsIdx = c.getColumnIndex(AnkiDroidContract.Notes.FLDS)

                while (c.moveToNext()) {
                    val modelId = if (midIdx != -1) c.getLong(midIdx) else -1L
                    val rawFields = if (fldsIdx != -1) c.getString(fldsIdx) ?: "" else ""

                    if (rawFields.isNotEmpty()) {
                        val fieldValues = rawFields.split("\u001f")
                        val fieldNames = getModelFieldNames(authority, modelId)
                        val parsed = JapaneseFieldParser.mapFieldsToJapaneseCard(
                            fieldNames = fieldNames,
                            fieldValues = fieldValues
                        )

                        if (!JapaneseFieldParser.isInstructionOrInvalidCard(parsed.kanji, parsed.kana, parsed.meaning)) {
                            val word = parsed.kanji.ifBlank { parsed.kana }
                            if (word.isNotBlank()) {
                                val item = AnkiVocabularyItem(
                                    kanji = parsed.kanji,
                                    reading = parsed.kana,
                                    meaning = parsed.meaning,
                                    isSuspended = isSuspended
                                )
                                outMap[item.displayWord] = item
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query notes with \"$searchQuery\": ${e.message}")
        } finally {
            cursor?.close()
        }
    }

    private fun extractFromCardsFallback(
        authority: String,
        deckFilter: String,
        studiedMap: MutableMap<String, AnkiVocabularyItem>,
        suspendedMap: MutableMap<String, AnkiVocabularyItem>
    ) {
        val cardsUri = AnkiDroidContract.Cards.getContentUri(authority)
        var cursor: Cursor? = null
        try {
            val query = if (deckFilter.isNotBlank()) deckFilter else ""
            cursor = context.contentResolver.query(
                cardsUri,
                null,
                query.ifBlank { null },
                null,
                null
            )

            cursor?.let { c ->
                val noteIdIdx = c.getColumnIndex(AnkiDroidContract.Cards.NOTE_ID)
                val queueIdx = c.getColumnIndex(AnkiDroidContract.Cards.RAW_QUEUE)
                val ivlIdx = c.getColumnIndex(AnkiDroidContract.Cards.INTERVAL)
                val repsIdx = c.getColumnIndex(AnkiDroidContract.Cards.REPS)
                val processedNoteIds = mutableSetOf<Long>()

                while (c.moveToNext()) {
                    val noteId = if (noteIdIdx != -1) c.getLong(noteIdIdx) else 0L
                    val queue = if (queueIdx != -1) c.getInt(queueIdx) else 0
                    val interval = if (ivlIdx != -1) c.getInt(ivlIdx) else 0
                    val reps = if (repsIdx != -1) c.getInt(repsIdx) else 0

                    val isSuspended = (queue == -1)
                    val isStudied = (queue in 1..3 || interval > 0 || reps > 0)

                    if (!isSuspended && !isStudied) continue

                    if (noteId > 0 && !processedNoteIds.contains(noteId)) {
                        processedNoteIds.add(noteId)
                        val item = extractNoteVocabularyById(authority, noteId, isSuspended)
                        if (item != null) {
                            if (isSuspended) {
                                if (!studiedMap.containsKey(item.displayWord)) {
                                    suspendedMap[item.displayWord] = item
                                }
                            } else {
                                studiedMap[item.displayWord] = item
                                suspendedMap.remove(item.displayWord)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cards fallback query also failed", e)
        } finally {
            cursor?.close()
        }
    }

    private fun extractNoteVocabularyById(
        authority: String,
        noteId: Long,
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
                        fieldValues = fieldValues
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
            Log.d(TAG, "Failed reading note $noteId: ${e.message}")
        } finally {
            noteCursor?.close()
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
