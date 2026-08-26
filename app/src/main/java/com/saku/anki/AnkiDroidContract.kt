package com.saku.anki

import android.net.Uri

object AnkiDroidContract {
    const val DEFAULT_PACKAGE = "com.ichi2.anki"
    const val DEFAULT_AUTHORITY = "com.ichi2.anki.flashcards"
    const val PERMISSION = "com.ichi2.anki.permission.READ_WRITE_DATABASE"

    val KNOWN_PACKAGES = listOf(
        "com.ichi2.anki",
        "com.ichi2.anki.a",
        "com.ichi2.anki.b",
        "com.ichi2.anki.debug"
    )

    fun getAuthorityForPackage(packageName: String): String {
        return "$packageName.flashcards"
    }

    fun getAuthorityUri(authority: String = DEFAULT_AUTHORITY): Uri {
        return Uri.parse("content://$authority")
    }

    // Default URIs using default authority
    val AUTHORITY_URI: Uri = getAuthorityUri(DEFAULT_AUTHORITY)

    object Decks {
        fun getContentUri(authority: String = DEFAULT_AUTHORITY): Uri =
            Uri.withAppendedPath(getAuthorityUri(authority), "decks")

        fun getSelectedDeckUri(authority: String = DEFAULT_AUTHORITY): Uri =
            Uri.withAppendedPath(getAuthorityUri(authority), "selected_deck")

        val CONTENT_URI: Uri = getContentUri()
        val CONTENT_SELECTED_URI: Uri = getSelectedDeckUri()

        const val DECK_ID = "deck_id"
        const val DECK_NAME = "deck_name"
        const val DECK_COUNTS = "deck_count"
        const val OPTIONS = "options"
        const val DECK_DYN = "deck_dyn"
        const val DECK_DESC = "deck_desc"

        val DEFAULT_PROJECTION = arrayOf(
            DECK_NAME,
            DECK_ID,
            DECK_COUNTS,
            OPTIONS,
            DECK_DYN,
            DECK_DESC
        )
    }

    object Models {
        fun getContentUri(authority: String = DEFAULT_AUTHORITY): Uri =
            Uri.withAppendedPath(getAuthorityUri(authority), "models")

        val CONTENT_URI: Uri = getContentUri()

        const val _ID = "_id"
        const val NAME = "name"
        const val FIELD_NAMES = "field_names"
        const val NUM_CARDS = "num_cards"
        const val CSS = "css"
        const val DECK_ID = "deck_id"
        const val SORT_FIELD_INDEX = "sort_field_index"
        const val TYPE = "type"

        val DEFAULT_PROJECTION = arrayOf(
            _ID,
            NAME,
            FIELD_NAMES,
            NUM_CARDS,
            CSS,
            DECK_ID,
            SORT_FIELD_INDEX,
            TYPE
        )
    }

    object Notes {
        fun getContentUri(authority: String = DEFAULT_AUTHORITY): Uri =
            Uri.withAppendedPath(getAuthorityUri(authority), "notes")

        fun getContentUriV2(authority: String = DEFAULT_AUTHORITY): Uri =
            Uri.withAppendedPath(getAuthorityUri(authority), "notes_v2")

        val CONTENT_URI: Uri = getContentUri()
        val CONTENT_URI_V2: Uri = getContentUriV2()

        const val _ID = "_id"
        const val GUID = "guid"
        const val MID = "mid"
        const val MOD = "mod"
        const val USN = "usn"
        const val TAGS = "tags"
        const val FLDS = "flds"
        const val SFLD = "sfld"
        const val CSUM = "csum"
        const val FLAGS = "flags"
        const val DATA = "data"

        val DEFAULT_PROJECTION = arrayOf(
            _ID,
            GUID,
            MID,
            MOD,
            USN,
            TAGS,
            FLDS,
            SFLD,
            CSUM,
            FLAGS,
            DATA
        )
    }

    object Cards {
        fun getContentUri(authority: String = DEFAULT_AUTHORITY): Uri =
            Uri.withAppendedPath(getAuthorityUri(authority), "cards")

        val CONTENT_URI: Uri = getContentUri()

        const val _ID = "_id"
        const val NOTE_ID = "note_id"
        const val CARD_ORD = "ord"
        const val CARD_NAME = "card_name"
        const val DECK_ID = "deck_id"
        const val QUESTION = "question"
        const val ANSWER = "answer"
        const val QUESTION_SIMPLE = "question_simple"
        const val ANSWER_SIMPLE = "answer_simple"
        const val ANSWER_PURE = "answer_pure"
        const val RAW_QUEUE = "queue"
        const val RAW_DUE = "due"
        const val INTERVAL = "interval"
        const val RAW_SM2_FACTOR = "sm2_factor"
        const val REPS = "reps"
        const val LAPSES = "lapses"
        const val TYPE = "type"
        const val FLAGS = "flags"

        val DEFAULT_PROJECTION = arrayOf(
            _ID,
            NOTE_ID,
            CARD_ORD,
            CARD_NAME,
            DECK_ID,
            QUESTION,
            ANSWER,
            QUESTION_SIMPLE,
            ANSWER_SIMPLE,
            RAW_QUEUE,
            RAW_DUE,
            INTERVAL,
            FLAGS
        )
    }

    object ReviewInfo {
        fun getContentUri(authority: String = DEFAULT_AUTHORITY): Uri =
            Uri.withAppendedPath(getAuthorityUri(authority), "schedule")

        val CONTENT_URI: Uri = getContentUri()

        const val NOTE_ID = "note_id"
        const val CARD_ORD = "ord"
        const val BUTTON_COUNT = "button_count"
        const val NEXT_REVIEW_TIMES = "next_review_times"
        const val MEDIA_FILES = "media_files"
        const val EASE = "answer_ease"
        const val TIME_TAKEN = "time_taken"
        const val BURY = "buried"
        const val SUSPEND = "suspended"

        val DEFAULT_PROJECTION = arrayOf(
            NOTE_ID,
            CARD_ORD,
            BUTTON_COUNT,
            NEXT_REVIEW_TIMES,
            MEDIA_FILES
        )
    }

    // Card Answer Ease Constants (Standard Anki ratings)
    const val EASE_AGAIN = 1
    const val EASE_HARD = 2
    const val EASE_GOOD = 3
    const val EASE_EASY = 4
}
