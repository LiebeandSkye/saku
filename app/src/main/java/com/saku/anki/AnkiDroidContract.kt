package com.saku.anki

import android.net.Uri

object AnkiDroidContract {
    const val AUTHORITY = "com.ichi2.anki.flashcards"
    val AUTHORITY_URI: Uri = Uri.parse("content://$AUTHORITY")

    const val PERMISSION = "com.ichi2.anki.permission.READ_WRITE_DATABASE"

    object Decks {
        val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, "decks")
        const val _ID = "_id"
        const val DECK_NAME = "deck_name"
        const val DECK_ID = "deck_id"
    }

    object Models {
        val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, "models")
        const val _ID = "_id"
        const val NAME = "name"
        const val FIELD_NAMES = "field_names"
    }

    object Notes {
        val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, "notes")
        const val _ID = "_id"
        const val MODEL_ID = "mid"
        const val FIELDS = "flds"
        const val TAGS = "tags"
    }

    object Cards {
        val CONTENT_URI: Uri = Uri.withAppendedPath(AUTHORITY_URI, "cards")
        const val _ID = "_id"
        const val NOTE_ID = "nid"
        const val DECK_ID = "did"
        const val ORD = "ord"
        const val TYPE = "type"
        const val QUEUE = "queue"
        const val DUE = "due"
        const val INTERVAL = "ivl"
        const val FACTOR = "factor"
        const val REPS = "reps"
        const val LAPSES = "lapses"
    }

    // Card Answer Ease Constants (Standard Anki ratings)
    const val EASE_AGAIN = 1
    const val EASE_HARD = 2
    const val EASE_GOOD = 3
    const val EASE_EASY = 4
}
