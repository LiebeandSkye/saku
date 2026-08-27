package com.saku.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.saku.anki.AnkiDroidClient
import com.saku.data.SakuPreferences
import com.saku.notification.LockScreenCardService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SakuNextCardAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withContext(Dispatchers.IO) {
            val prefs = SakuPreferences(context)
            val ankiClient = AnkiDroidClient(context)
            val dueCards = ankiClient.getDueCards(prefs.selectedDeckId, limit = 20)

            val currentId = prefs.getActiveCard().cardId
            val nextCard = dueCards.firstOrNull { it.cardId != currentId }
                ?: dueCards.firstOrNull()
                ?: ankiClient.getSamplePreviewCard()

            prefs.saveActiveCard(nextCard)
            prefs.isAnswerRevealed = false

            if (prefs.isLockScreenCardEnabled) {
                LockScreenCardService.updateNotification(context, nextCard, showAnswer = false)
            }

            SakuGlanceWidget().update(context, glanceId)
            SakuGlanceWidget().updateAll(context)
        }
    }
}

class SakuGradeCardAction : ActionCallback {
    companion object {
        val EASE_PARAM = ActionParameters.Key<Int>("ease_rating")
    }

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withContext(Dispatchers.IO) {
            val ease = parameters[EASE_PARAM] ?: 1
            val prefs = SakuPreferences(context)
            val ankiClient = AnkiDroidClient(context)
            val currentCard = prefs.getActiveCard()

            if (currentCard.noteId > 0) {
                ankiClient.answerCard(currentCard, ease)
            }

            val dueCards = ankiClient.getDueCards(prefs.selectedDeckId, limit = 20)
            val nextCard = dueCards.firstOrNull { it.cardId != currentCard.cardId }
                ?: dueCards.firstOrNull()
                ?: ankiClient.getSamplePreviewCard()

            prefs.saveActiveCard(nextCard)
            prefs.isAnswerRevealed = false

            if (prefs.isLockScreenCardEnabled) {
                LockScreenCardService.updateNotification(context, nextCard, showAnswer = false)
            }

            SakuGlanceWidget().update(context, glanceId)
            SakuGlanceWidget().updateAll(context)
        }
    }
}

class SakuToggleAnswerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withContext(Dispatchers.IO) {
            val prefs = SakuPreferences(context)
            val newRevealed = !prefs.isAnswerRevealed
            prefs.isAnswerRevealed = newRevealed

            if (prefs.isLockScreenCardEnabled) {
                LockScreenCardService.updateNotification(context, prefs.getActiveCard(), showAnswer = newRevealed)
            }

            SakuGlanceWidget().update(context, glanceId)
            SakuGlanceWidget().updateAll(context)
        }
    }
}

class SakuOpenAnkiAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        withContext(Dispatchers.IO) {
            val ankiClient = AnkiDroidClient(context)
            val prefs = SakuPreferences(context)
            val intent = ankiClient.getOpenAnkiIntent(prefs.getActiveCard().noteId)
            context.startActivity(intent)
        }
    }
}

