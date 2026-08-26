package com.saku.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.saku.anki.AnkiDroidClient
import com.saku.data.ReviewEase
import com.saku.data.SakuPreferences
import com.saku.widget.SakuGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_NEXT = "com.saku.action.NEXT_CARD"
        const val ACTION_GRADE = "com.saku.action.GRADE_CARD"
        const val EXTRA_EASE = "extra_ease"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val prefs = SakuPreferences(context)
        val ankiClient = AnkiDroidClient(context)
        val currentCard = prefs.getActiveCard()

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_GRADE -> {
                        val ease = intent.getIntExtra(EXTRA_EASE, ReviewEase.GOOD.value)
                        if (currentCard.noteId > 0) {
                            ankiClient.answerCard(currentCard, ease)
                        }
                        loadNextCardAndUpdate(context, prefs, ankiClient)
                    }
                    ACTION_NEXT -> {
                        loadNextCardAndUpdate(context, prefs, ankiClient)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun loadNextCardAndUpdate(
        context: Context,
        prefs: SakuPreferences,
        ankiClient: AnkiDroidClient
    ) {
        val dueCards = ankiClient.getDueCards(prefs.selectedDeckId, limit = 20)
        val nextCard = dueCards.firstOrNull { it.cardId != prefs.getActiveCard().cardId }
            ?: dueCards.firstOrNull()
            ?: ankiClient.getSamplePreviewCard()

        prefs.saveActiveCard(nextCard)

        // Update Lock Screen Notification
        LockScreenCardService.updateNotification(context, nextCard)

        // Update Home Screen Glance Widget
        try {
            SakuGlanceWidget().updateAll(context)
        } catch (e: Exception) {
            // Glance update exception handling
        }
    }
}
