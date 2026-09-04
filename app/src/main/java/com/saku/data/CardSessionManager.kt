package com.saku.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.saku.anki.AnkiDroidHelper
import com.saku.notification.LockScreenCardService
import com.saku.widget.SakuWidgetProvider
import java.util.concurrent.CopyOnWriteArrayList

object CardSessionManager {

    var currentCard: CardInfo? = null
        private set
    var previousCard: CardInfo? = null
        private set
    private var previousStats: Triple<Int, Int, Int>? = null
    var isRevealed: Boolean = false
        private set
    var currentStats: Triple<Int, Int, Int> = Triple(0, 0, 0)
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    fun getOrFetchCard(context: Context, forceRefresh: Boolean = false): CardInfo? {
        if (currentCard == null || forceRefresh) {
            val ankiHelper = AnkiDroidHelper(context)
            val prefs = PreferencesManager(context)
            val selectedDecks = prefs.getSelectedDeckIdsAsLongs()
            currentCard = ankiHelper.getNextDueCard(selectedDecks)
            currentStats = ankiHelper.getSelectedDeckStats(selectedDecks)
            if (forceRefresh) isRevealed = false
            notifyUi()
        }
        return currentCard
    }

    fun toggleReveal(context: Context) {
        isRevealed = !isRevealed
        notifyAllSurfaces(context)
    }

    fun reveal(context: Context) {
        if (!isRevealed) {
            isRevealed = true
            notifyAllSurfaces(context)
        }
    }

    fun hide(context: Context) {
        if (isRevealed) {
            isRevealed = false
            notifyAllSurfaces(context)
        }
    }

    fun gradeCard(
        context: Context,
        ease: Int,
        timeTaken: Long = 5000L,
        onComplete: ((CardInfo?) -> Unit)? = null
    ) {
        val card = currentCard ?: getOrFetchCard(context)
        val ankiHelper = AnkiDroidHelper(context)
        val prefs = PreferencesManager(context)
        val selectedDecks = prefs.getSelectedDeckIdsAsLongs()

        if (card != null) {
            val oldStats = currentStats
            previousCard = card
            previousStats = oldStats
            currentStats = when (ease) {
                1 -> Triple(
                    (oldStats.first - (if (card.cardType == 0) 1 else 0)).coerceAtLeast(0),
                    oldStats.second + 1,
                    (oldStats.third - (if (card.cardType == 2) 1 else 0)).coerceAtLeast(0)
                )
                else -> Triple(
                    (oldStats.first - (if (card.cardType == 0) 1 else 0)).coerceAtLeast(0),
                    (oldStats.second - (if (card.cardType == 1) 1 else 0)).coerceAtLeast(0),
                    (oldStats.third - (if (card.cardType == 2) 1 else 0)).coerceAtLeast(0)
                )
            }

            Thread {
                ankiHelper.answerCard(card.noteId, card.cardOrd, ease, timeTaken)
                val nextCard = ankiHelper.getNextDueCard(selectedDecks, excludeNoteId = card.noteId)
                val freshStats = ankiHelper.getSelectedDeckStats(selectedDecks)

                mainHandler.post {
                    currentCard = nextCard
                    currentStats = freshStats
                    isRevealed = false
                    notifyAllSurfaces(context)
                    onComplete?.invoke(nextCard)
                }
            }.start()
        } else {
            currentCard = null
            isRevealed = false
            notifyAllSurfaces(context)
            onComplete?.invoke(null)
        }
    }

    fun suspendCurrentCard(
        context: Context,
        onComplete: ((CardInfo?) -> Unit)? = null
    ) {
        val card = currentCard ?: getOrFetchCard(context)
        val ankiHelper = AnkiDroidHelper(context)
        val prefs = PreferencesManager(context)
        val selectedDecks = prefs.getSelectedDeckIdsAsLongs()

        if (card != null) {
            previousCard = card
            previousStats = currentStats
            Thread {
                ankiHelper.suspendCard(card.noteId, card.cardOrd)
                val nextCard = ankiHelper.getNextDueCard(selectedDecks, excludeNoteId = card.noteId)
                val freshStats = ankiHelper.getSelectedDeckStats(selectedDecks)

                mainHandler.post {
                    currentCard = nextCard
                    currentStats = freshStats
                    isRevealed = false
                    notifyAllSurfaces(context)
                    onComplete?.invoke(nextCard)
                }
            }.start()
        } else {
            onComplete?.invoke(null)
        }
    }

    fun undoLastReview(context: Context) {
        val prev = previousCard
        if (prev != null) {
            currentCard = prev
            if (previousStats != null) {
                currentStats = previousStats!!
            }
            isRevealed = false
            previousCard = null
            previousStats = null
            notifyAllSurfaces(context)
        }
    }

    fun refresh(context: Context) {
        Thread {
            val ankiHelper = AnkiDroidHelper(context)
            val prefs = PreferencesManager(context)
            val selectedDecks = prefs.getSelectedDeckIdsAsLongs()

            val card = ankiHelper.getNextDueCard(selectedDecks)
            val stats = ankiHelper.getSelectedDeckStats(selectedDecks)

            mainHandler.post {
                currentCard = card
                currentStats = stats
                isRevealed = false
                notifyAllSurfaces(context)
            }
        }.start()
    }

    private fun notifyUi() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            listeners.forEach { it.invoke() }
        } else {
            mainHandler.post { listeners.forEach { it.invoke() } }
        }
    }

    fun notifyAllSurfaces(context: Context) {
        notifyUi()
        if (PreferencesManager(context).isServiceEnabled) {
            LockScreenCardService.updateNotification(context)
        }
        SakuWidgetProvider.updateAllWidgets(context)
    }
}
