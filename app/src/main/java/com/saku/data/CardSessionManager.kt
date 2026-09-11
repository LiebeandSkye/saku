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

    val isGradingInProgress = java.util.concurrent.atomic.AtomicBoolean(false)

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private val surfaceExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
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

    fun setActiveCard(card: CardInfo?, notify: Boolean = false, context: Context? = null) {
        currentCard = card
        if (notify) {
            if (context != null) notifyAllSurfaces(context) else notifyUi()
        }
    }

    fun gradeCard(
        context: Context,
        ease: Int,
        timeTaken: Long = 5000L,
        specificCard: CardInfo? = null,
        targetDeckId: Long? = null,
        onComplete: ((CardInfo?) -> Unit)? = null
    ) {
        val card = specificCard ?: currentCard ?: getOrFetchCard(context)
        if (card == null) {
            currentCard = null
            isRevealed = false
            notifyAllSurfaces(context)
            onComplete?.invoke(null)
            return
        }

        if (!isGradingInProgress.compareAndSet(false, true)) {
            // Drop rapid repeated clicks while a grading operation is actively running
            onComplete?.invoke(currentCard)
            return
        }

        val ankiHelper = AnkiDroidHelper(context)
        val prefs = PreferencesManager(context)
        val selectedDecks = prefs.getSelectedDeckIdsAsLongs()

        val oldStats = currentStats
        previousCard = card
        previousStats = oldStats
        currentStats = when (ease) {
            1 -> Triple(
                (oldStats.first - (if (card.cardType == 0) 1 else 0)).coerceAtLeast(0),
                if (card.cardType == 1) oldStats.second else oldStats.second + 1,
                (oldStats.third - (if (card.cardType == 2) 1 else 0)).coerceAtLeast(0)
            )
            else -> Triple(
                (oldStats.first - (if (card.cardType == 0) 1 else 0)).coerceAtLeast(0),
                (oldStats.second - (if (card.cardType == 1) 1 else 0)).coerceAtLeast(0),
                (oldStats.third - (if (card.cardType == 2) 1 else 0)).coerceAtLeast(0)
            )
        }

        val effectiveDeckId = targetDeckId?.takeIf { it > 0 } ?: card.deckId.takeIf { it > 0 }

        Thread {
            try {
                ankiHelper.answerCard(card.noteId, card.cardOrd, ease, timeTaken, effectiveDeckId)
                val deckQueryIds = if (effectiveDeckId != null && effectiveDeckId > 0) setOf(effectiveDeckId) else selectedDecks
                var nextDeckCard = ankiHelper.getNextDueCard(deckQueryIds, excludeNoteId = card.noteId)
                if (nextDeckCard == null && targetDeckId == null && selectedDecks.isNotEmpty()) {
                    nextDeckCard = ankiHelper.getNextDueCard(selectedDecks, excludeNoteId = card.noteId)
                }
                val freshStats = ankiHelper.getSelectedDeckStats(selectedDecks)

                mainHandler.post {
                    try {
                        if (currentCard?.noteId == card.noteId || specificCard == null) {
                            currentCard = nextDeckCard
                        }
                        currentStats = freshStats
                        isRevealed = false
                        notifyAllSurfaces(context)
                        onComplete?.invoke(nextDeckCard)
                    } finally {
                        isGradingInProgress.set(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isGradingInProgress.set(false)
                mainHandler.post { onComplete?.invoke(null) }
            }
        }.start()
    }

    fun suspendCurrentCard(
        context: Context,
        targetDeckId: Long? = null,
        specificCard: CardInfo? = null,
        onComplete: ((CardInfo?) -> Unit)? = null
    ) {
        val card = specificCard ?: currentCard ?: getOrFetchCard(context)
        if (card == null) {
            onComplete?.invoke(null)
            return
        }

        if (!isGradingInProgress.compareAndSet(false, true)) {
            onComplete?.invoke(currentCard)
            return
        }

        val ankiHelper = AnkiDroidHelper(context)
        val prefs = PreferencesManager(context)
        val selectedDecks = prefs.getSelectedDeckIdsAsLongs()

        previousCard = card
        previousStats = currentStats
        val effectiveDeckId = targetDeckId?.takeIf { it > 0 } ?: card.deckId.takeIf { it > 0 }
        Thread {
            try {
                ankiHelper.suspendCard(card.noteId, card.cardOrd, effectiveDeckId)
                val deckQueryIds = if (effectiveDeckId != null && effectiveDeckId > 0) setOf(effectiveDeckId) else selectedDecks
                var nextCard = ankiHelper.getNextDueCard(deckQueryIds, excludeNoteId = card.noteId)
                if (nextCard == null && targetDeckId == null && selectedDecks.isNotEmpty()) {
                    nextCard = ankiHelper.getNextDueCard(selectedDecks, excludeNoteId = card.noteId)
                }
                val freshStats = ankiHelper.getSelectedDeckStats(selectedDecks)

                mainHandler.post {
                    try {
                        if (currentCard?.noteId == card.noteId || specificCard == null) {
                            currentCard = nextCard
                        }
                        currentStats = freshStats
                        isRevealed = false
                        notifyAllSurfaces(context)
                        onComplete?.invoke(nextCard)
                    } finally {
                        isGradingInProgress.set(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isGradingInProgress.set(false)
                mainHandler.post { onComplete?.invoke(null) }
            }
        }.start()
    }

    /**
     * Rolls back the in-memory card session and statistics to the previously reviewed card.
     *
     * Note: The public AnkiDroid ContentProvider API does not provide an endpoint to undo a review
     * once committed in AnkiDroid's database. This method restores the card in Saku's active
     * session memory and notifies all UI surfaces (lockscreen, widget, main activity).
     */
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

    fun refresh(context: Context, onComplete: (() -> Unit)? = null) {
        AnkiDroidHelper.invalidateDeckCache()
        Thread {
            try {
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
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                mainHandler.post { onComplete?.invoke() }
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
        surfaceExecutor.execute {
            try {
                if (PreferencesManager(context).isServiceEnabled) {
                    LockScreenCardService.updateNotification(context)
                }
                SakuWidgetProvider.updateAllWidgets(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
