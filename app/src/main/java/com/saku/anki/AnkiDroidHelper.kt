package com.saku.anki

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.ContextCompat
import com.saku.data.CardInfo
import com.saku.data.DeckInfo
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class AnkiDroidHelper(private val context: Context) {

    private val resolver: ContentResolver get() = context.contentResolver

    val authority: String
        get() = "${getInstalledPackage()}.flashcards"

    val scheduleUri: Uri
        get() = Uri.parse("content://$authority/schedule")

    val selectedDeckUri: Uri
        get() = Uri.parse("content://$authority/selected_deck")

    val decksUri: Uri
        get() = Uri.parse("content://$authority/decks")

    val notesUri: Uri
        get() = Uri.parse("content://$authority/notes")

    fun selectDeck(deckId: Long): Boolean {
        if (deckId <= 0) return false
        return synchronized(ankiIpcLock) {
            try {
                val values = ContentValues().apply {
                    put(COL_DECK_ID, deckId)
                }
                resolver.update(selectedDeckUri, values, null, null) > 0
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun isAnkiDroidInstalled(): Boolean {
        for (pkg in ANKI_PACKAGES) {
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                return true
            } catch (e: PackageManager.NameNotFoundException) {
                // Continue checking other packages
            }
        }
        return false
    }

    fun getInstalledPackage(): String {
        for (pkg in ANKI_PACKAGES) {
            try {
                context.packageManager.getPackageInfo(pkg, 0)
                return pkg
            } catch (e: Exception) {
            }
        }
        return ANKI_PACKAGE
    }

    fun hasApiPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            PERMISSION_READ_WRITE_DATABASE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getDeckList(forceRefresh: Boolean = false): List<DeckInfo> {
        if (!forceRefresh) {
            val cached = cachedDeckList
            if (cached != null && (System.currentTimeMillis() - cachedDeckListTimestamp < DECK_CACHE_TTL_MS)) {
                return cached
            }
        }

        return synchronized(ankiIpcLock) {
            if (!forceRefresh) {
                val cached = cachedDeckList
                if (cached != null && (System.currentTimeMillis() - cachedDeckListTimestamp < DECK_CACHE_TTL_MS)) {
                    return@synchronized cached
                }
            }

            val decks = mutableListOf<DeckInfo>()
            val uris = listOf(decksUri, Uri.parse("content://$authority/decks/"))

            for (uri in uris) {
                try {
                    val cursor = resolver.query(uri, null, null, null, null)
                    cursor?.use { cur ->
                        val nameIdx = cur.getColumnIndex(COL_DECK_NAME)
                        val idIdx = cur.getColumnIndex(COL_DECK_ID)
                        val countsIdx = cur.getColumnIndex(COL_DECK_COUNTS)

                        while (cur.moveToNext()) {
                            val name = cur.getString(nameIdx) ?: continue
                            val id = cur.getLong(idIdx)
                            var newC = 0
                            var learnC = 0
                            var revC = 0

                            if (countsIdx >= 0) {
                                val countsStr = cur.getString(countsIdx) ?: ""
                                val nums = Regex("\\d+").findAll(countsStr).map { it.value.toInt() }.toList()
                                if (nums.size >= 3) {
                                    learnC = nums[0]
                                    revC = nums[1]
                                    newC = nums[2]
                                }
                            }

                            if (newC > 100) {
                                val dueNew = getNotesDueCountInternal("deck:\"$name\" is:new is:due")
                                if (dueNew > 0) newC = dueNew
                            }

                            if (newC == 0 && learnC == 0 && revC == 0) {
                                val s = getDeckStatsForDeckInternal(name)
                                newC = s.first
                                learnC = s.second
                                revC = s.third
                            }

                            decks.add(
                                DeckInfo(
                                    id = id,
                                    name = name,
                                    newCount = newC,
                                    learnCount = learnC,
                                    reviewCount = revC
                                )
                            )
                        }
                    }
                    if (decks.isNotEmpty()) break
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (decks.isNotEmpty()) {
                cachedDeckList = decks
                cachedDeckListTimestamp = System.currentTimeMillis()
            }
            decks
        }
    }

    fun getAnkiLaunchIntent(deckId: Long? = null): Intent {
        val pkg = getInstalledPackage()
        if (deckId != null && deckId > 0) {
            val reviewerIntent = Intent(Intent.ACTION_VIEW).apply {
                setClassName(pkg, "com.ichi2.anki.Reviewer")
                putExtra("deckId", deckId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            if (reviewerIntent.resolveActivity(context.packageManager) != null) {
                return reviewerIntent
            }
        }

        return context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
            if (deckId != null && deckId > 0) {
                putExtra("deckId", deckId)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        } ?: Intent(Intent.ACTION_MAIN).apply {
            setPackage(pkg)
            addCategory(Intent.CATEGORY_LAUNCHER)
            if (deckId != null && deckId > 0) {
                putExtra("deckId", deckId)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
    }

    fun getSelectedDeckStats(selectedDeckIds: Set<Long> = emptySet()): Triple<Int, Int, Int> {
        val decks = getDeckList()
        if (decks.isEmpty()) return Triple(0, 0, 0)

        val targetDecks = if (selectedDeckIds.isNotEmpty()) {
            decks.filter { it.id in selectedDeckIds }
        } else {
            decks
        }

        var newC = 0
        var learnC = 0
        var revC = 0
        for (deck in targetDecks) {
            newC += deck.newCount
            learnC += deck.learnCount
            revC += deck.reviewCount
        }
        return Triple(newC, learnC, revC)
    }

    fun getDeckStatsForDeck(deckName: String): Triple<Int, Int, Int> {
        return synchronized(ankiIpcLock) {
            getDeckStatsForDeckInternal(deckName)
        }
    }

    private fun getDeckStatsForDeckInternal(deckName: String): Triple<Int, Int, Int> {
        val cleanDeck = deckName.trim()
        val newNotes = getNotesDueCountInternal("deck:\"$cleanDeck\" is:new is:due").takeIf { it > 0 }
            ?: getNotesDueCountInternal("deck:\"$cleanDeck\" is:new")
        val learnNotes = getNotesDueCountInternal("deck:\"$cleanDeck\" is:learn")
        val dueNotes = getNotesDueCountInternal("deck:\"$cleanDeck\" is:due -is:learn -is:new")
        if (newNotes > 0 || learnNotes > 0 || dueNotes > 0) {
            return Triple(newNotes, learnNotes, dueNotes)
        }
        return Triple(0, 0, 0)
    }

    fun getNotesDueCount(searchQuery: String): Int {
        return synchronized(ankiIpcLock) {
            getNotesDueCountInternal(searchQuery)
        }
    }

    private fun getNotesDueCountInternal(searchQuery: String): Int {
        return try {
            val cursor = resolver.query(notesUri, arrayOf("_id"), searchQuery, null, null)
            cursor?.use { it.count } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getNextDueCard(deckIds: Set<Long> = emptySet(), excludeNoteId: Long? = null): CardInfo? {
        val decks = getDeckList()
        val queryDeckIds = if (deckIds.isNotEmpty()) deckIds.toList() else listOf<Long?>(null)

        for (deckId in queryDeckIds) {
            val card = queryNextDueCardForDeck(deckId, excludeNoteId, decks)
            if (card != null) return card
        }
        return null
    }

    fun getNextDueCard(deckId: Long?, excludeNoteId: Long? = null): CardInfo? {
        return getNextDueCard(if (deckId != null) setOf(deckId) else emptySet(), excludeNoteId)
    }

    private fun queryNextDueCardForDeck(
        deckId: Long?,
        excludeNoteId: Long?,
        decks: List<DeckInfo>
    ): CardInfo? {
        return synchronized(ankiIpcLock) {
            try {
                val hasSpecificDeck = deckId != null && deckId > 0
                val (selection, selectionArgs) = if (hasSpecificDeck) {
                    Pair("limit=?, deckID=?", arrayOf("10", deckId.toString()))
                } else {
                    Pair("limit=?", arrayOf("10"))
                }

                var cursor = try {
                    resolver.query(
                        scheduleUri,
                        null,
                        selection,
                        selectionArgs,
                        null
                    )
                } catch (e: Exception) {
                    null
                }

                // Fallback for legacy AnkiDroid builds that expect inlined selection string
                if (cursor == null) {
                    val rawSelection = if (hasSpecificDeck) "deckID=$deckId, limit=10" else "limit=10"
                    cursor = try {
                        resolver.query(scheduleUri, null, rawSelection, null, null)
                    } catch (e: Exception) {
                        null
                    }
                }

                cursor?.use { cur ->
                    while (cur.moveToNext()) {
                        val noteId = cur.getLong(
                            cur.getColumnIndexOrThrow(COL_NOTE_ID)
                        )
                        if (excludeNoteId != null && noteId == excludeNoteId) {
                            continue
                        }
                        val cardOrd = cur.getInt(
                            cur.getColumnIndexOrThrow(COL_CARD_ORD)
                        )
                        val buttonCount = cur.getInt(
                            cur.getColumnIndexOrThrow(COL_BUTTON_COUNT)
                        )
                        val nextTimes = cur.getString(
                            cur.getColumnIndexOrThrow(COL_NEXT_REVIEW_TIMES)
                        ) ?: ""

                        val cursorDeckId = cur.getColumnIndex(COL_DECK_ID).takeIf { it >= 0 }?.let { cur.getLong(it) }
                            ?: cur.getColumnIndex("did").takeIf { it >= 0 }?.let { cur.getLong(it) }
                        val resolvedDeckId = if (deckId != null && deckId > 0) {
                            deckId
                        } else {
                            cursorDeckId?.takeIf { it > 0 } ?: (decks.firstOrNull()?.id ?: 0L)
                        }
                        val deckName = decks.find { it.id == resolvedDeckId }?.name
                            ?: (if (deckId != null && deckId > 0) decks.find { it.id == deckId }?.name else null)
                            ?: (decks.firstOrNull()?.name ?: "")

                        val typeCol = cur.getColumnIndex("type").takeIf { it >= 0 }
                            ?: cur.getColumnIndex("card_type").takeIf { it >= 0 }
                        val queueCol = cur.getColumnIndex("queue").takeIf { it >= 0 }

                        val cardType = if (typeCol != null) {
                            val rawType = cur.getInt(typeCol)
                            when (rawType) {
                                0 -> 0
                                1, 3 -> 1
                                2 -> 2
                                else -> 0
                            }
                        } else if (queueCol != null) {
                            val rawQueue = cur.getInt(queueCol)
                            when (rawQueue) {
                                0 -> 0
                                1, 3 -> 1
                                2 -> 2
                                else -> 0
                            }
                        } else {
                            0
                        }

                        val parsed = getCardContentInternal(noteId)

                        return@synchronized parsed.copy(
                            noteId = noteId,
                            cardOrd = cardOrd,
                            deckId = resolvedDeckId,
                            deckName = deckName.ifEmpty { decks.firstOrNull()?.name ?: "" },
                            buttonCount = buttonCount,
                            nextReviewTimes = nextTimes,
                            cardType = cardType
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            null
        }
    }

    fun getCardContent(noteId: Long): CardInfo {
        return synchronized(ankiIpcLock) {
            getCardContentInternal(noteId)
        }
    }

    private val modelFieldCache = ConcurrentHashMap<Long, List<String>>()

    fun getModelFieldNames(modelId: Long): List<String> {
        if (modelId <= 0) return emptyList()
        modelFieldCache[modelId]?.let { return it }

        var modelCursor: Cursor? = null
        try {
            val modelUri = Uri.withAppendedPath(AnkiDroidContract.Models.getContentUri(authority), modelId.toString())
            modelCursor = resolver.query(
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
            // ignore
        } finally {
            modelCursor?.close()
        }
        return emptyList()
    }

    private fun getCardContentInternal(noteId: Long): CardInfo {
        try {
            val noteUri = Uri.withAppendedPath(notesUri, noteId.toString())
            resolver.query(
                noteUri,
                null,
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val fldsIdx = cursor.getColumnIndex(COL_FLDS)
                    val midIdx = cursor.getColumnIndex(COL_MID)
                    val modelId = if (midIdx >= 0) cursor.getLong(midIdx) else -1L
                    val fieldNames = if (modelId > 0) getModelFieldNames(modelId) else emptyList()
                    if (fldsIdx >= 0) {
                        val fields = cursor.getString(fldsIdx) ?: ""
                        return parseCardContent(fields, fieldNames)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return CardInfo(noteId = noteId, cardOrd = 0, question = "", answer = "", deckName = "")
    }

    fun parseCardContent(fields: String, fieldNames: List<String> = emptyList()): CardInfo {
        return Companion.parseCardContent(fields, fieldNames)
    }

    fun getCardImageBitmap(imageFileName: String, maxDimension: Int = 600): Bitmap? {
        if (imageFileName.isBlank()) return null
        val cleanName = imageFileName.trim()

        try {
            val mediaUri = Uri.parse("content://$authority/media/" + Uri.encode(cleanName))
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(mediaUri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            if (boundsOptions.outWidth > 0 && boundsOptions.outHeight > 0) {
                var sampleSize = 1
                val maxEdge = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
                while (maxEdge / sampleSize > maxDimension) {
                    sampleSize *= 2
                }
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize.coerceAtLeast(1)
                }
                resolver.openInputStream(mediaUri)?.use { stream ->
                    return BitmapFactory.decodeStream(stream, null, decodeOptions)
                }
            }
        } catch (e: Exception) {
        }

        val installedPkg = getInstalledPackage()
        val possibleFolders = listOf(
            File("/storage/emulated/0/Android/data/$installedPkg/files/AnkiDroid/collection.media"),
            File("/storage/emulated/0/AnkiDroid/collection.media"),
            File(context.filesDir.parentFile?.parentFile, "$installedPkg/files/AnkiDroid/collection.media")
        )

        for (folder in possibleFolders) {
            val file = File(folder, cleanName)
            if (file.exists()) {
                try {
                    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
                    if (boundsOptions.outWidth > 0 && boundsOptions.outHeight > 0) {
                        var sampleSize = 1
                        val maxEdge = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)
                        while (maxEdge / sampleSize > maxDimension) {
                            sampleSize *= 2
                        }
                        val decodeOptions = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize.coerceAtLeast(1)
                        }
                        return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                    }
                } catch (e: Exception) {
                }
            }
        }
        return null
    }

    private fun isJapanese(text: String): Boolean {
        return text.any { char ->
            (char in '\u3040'..'\u309F') ||
            (char in '\u30A0'..'\u30FF') ||
            (char in '\u4E00'..'\u9FAF')
        }
    }

    private fun isEnglish(text: String): Boolean {
        return text.any { char -> char in 'a'..'z' || char in 'A'..'Z' }
    }

    private fun cleanHtml(html: String): String {
        return html.replace(Regex("\\[sound:[^\\]]+\\]"), "")
            .replace(Regex("<style[\\s\\S]*?</style>"), "")
            .replace(Regex("<script[\\s\\S]*?</script>"), "")
            .replace(Regex("<br\\s*/?>"), " ")
            .replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .trim()
    }

    private fun cleanFuriganaToKanji(furiganaField: String): String {
        var text = furiganaField.replace(Regex("([^\\[\\s]+)\\[([^\\]]+)\\]"), "$1")
        text = text.replace(Regex("<ruby>([^<]+)<rt>[^<]+</rt></ruby>"), "$1")
        return cleanHtml(text)
    }

    private fun getDeckNameForNote(noteId: Long): String {
        try {
            val decks = getDeckList()
            if (decks.isNotEmpty()) return decks.first().name
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun answerCard(noteId: Long, cardOrd: Int, ease: Int, timeTaken: Long = 5000L, deckId: Long? = null): Boolean {
        invalidateDeckCache()
        return synchronized(ankiIpcLock) {
            try {
                if (deckId != null && deckId > 0) {
                    try {
                        val selValues = ContentValues().apply {
                            put(COL_DECK_ID, deckId)
                        }
                        resolver.update(selectedDeckUri, selValues, null, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val values = ContentValues().apply {
                    put(COL_NOTE_ID, noteId)
                    put(COL_CARD_ORD, cardOrd)
                    put(COL_EASE, ease)
                    put(COL_TIME_TAKEN, timeTaken)
                }
                val count = resolver.update(scheduleUri, values, null, null)
                count > 0
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun suspendCard(noteId: Long, cardOrd: Int, deckId: Long? = null): Boolean {
        invalidateDeckCache()
        return synchronized(ankiIpcLock) {
            try {
                if (deckId != null && deckId > 0) {
                    try {
                        val selValues = ContentValues().apply {
                            put(COL_DECK_ID, deckId)
                        }
                        resolver.update(selectedDeckUri, selValues, null, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                val values = ContentValues().apply {
                    put(COL_NOTE_ID, noteId)
                    put(COL_CARD_ORD, cardOrd)
                    put(COL_SUSPEND, 1)
                }
                val count = resolver.update(scheduleUri, values, null, null)
                count > 0
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    companion object {
        private val ankiIpcLock = Any()

        @Volatile
        private var cachedDeckList: List<DeckInfo>? = null
        @Volatile
        private var cachedDeckListTimestamp: Long = 0L
        private const val DECK_CACHE_TTL_MS = 20_000L // 20-second cache to prevent IPC storms

        fun invalidateDeckCache() {
            synchronized(ankiIpcLock) {
                cachedDeckList = null
                cachedDeckListTimestamp = 0L
            }
        }

        const val ANKI_PACKAGE = "com.ichi2.anki"
        val ANKI_PACKAGES = listOf(
            "com.ichi2.anki",
            "com.ichi2.anki.a",
            "com.ichi2.anki.b",
            "com.ichi2.anki.debug"
        )
        const val PERMISSION_READ_WRITE_DATABASE = "com.ichi2.anki.permission.READ_WRITE_DATABASE"
        private const val AUTHORITY = "com.ichi2.anki.flashcards"

        val DECKS_URI: Uri get() = Uri.parse("content://$AUTHORITY/decks/")
        val SCHEDULE_URI: Uri get() = Uri.parse("content://$AUTHORITY/schedule/")
        val SELECTED_DECK_URI: Uri get() = Uri.parse("content://$AUTHORITY/selected_deck")
        val NOTES_URI: Uri get() = Uri.parse("content://$AUTHORITY/notes")

        private const val COL_DECK_NAME = "deck_name"
        private const val COL_DECK_ID = "deck_id"
        private const val COL_DECK_COUNTS = "deck_counts"
        private const val COL_NOTE_ID = "note_id"
        private const val COL_CARD_ORD = "ord"
        private const val COL_BUTTON_COUNT = "button_count"
        private const val COL_NEXT_REVIEW_TIMES = "next_review_times"
        private const val COL_FLDS = "flds"
        private const val COL_MID = "mid"
        private const val COL_EASE = "answer_ease"
        private const val COL_TIME_TAKEN = "time_taken"
        private const val COL_SUSPEND = "suspended"

        fun parseCardContent(fields: String, fieldNames: List<String> = emptyList()): CardInfo {
            val rawParts = fields.split("\u001f")
            if (rawParts.isEmpty()) {
                return CardInfo(noteId = 0L, cardOrd = 0, question = "", answer = "", deckName = "")
            }

            var detectedImage = ""
            val imgRegex = Regex("<img[^>]+src=[\"']?([^\"'>\\s]+)[\"']?")
            for (part in rawParts) {
                val match = imgRegex.find(part)
                if (match != null && detectedImage.isEmpty()) {
                    detectedImage = match.groupValues[1]
                    break
                }
            }

            val parsed = JapaneseFieldParser.mapFieldsToJapaneseCard(
                fieldNames = fieldNames,
                fieldValues = rawParts
            )

            val kanji = parsed.kanji
            val furigana = if (parsed.furigana.isNotBlank() && parsed.furigana != kanji) {
                parsed.furigana
            } else if (parsed.kana.isNotBlank() && parsed.kana != kanji) {
                parsed.kana
            } else {
                ""
            }

            val meaning = parsed.meaning
            val sentence = parsed.exampleSentence.ifBlank {
                JapaneseFieldParser.cleanHtml(parsed.example)
            }
            val sentenceFurigana = parsed.exampleFurigana.ifBlank { parsed.example }
            val sentenceMeaning = parsed.exampleTranslation

            return CardInfo(
                noteId = 0L,
                cardOrd = 0,
                question = kanji,
                answer = meaning,
                deckName = "",
                kanji = kanji,
                kanjiFurigana = furigana,
                kanjiMeaning = meaning,
                sentence = sentence,
                sentenceFurigana = sentenceFurigana,
                sentenceMeaning = sentenceMeaning,
                imageFileName = detectedImage
            )
        }
    }
}
