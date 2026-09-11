package com.saku.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.text.Html
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.saku.R
import com.saku.anki.AnkiDroidHelper
import com.saku.data.CardInfo
import com.saku.data.CardSessionManager
import com.saku.data.PreferencesManager
import com.saku.ui.MainActivity
import com.saku.util.RubyTextRenderer

class LockScreenCardService : Service() {

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE -> {
                updateNotification(this)
            }
            ACTION_STOP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    notificationManager.cancel(NOTIFICATION_ID)
                }
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                startForegroundCompat()
            }
        }
        return START_STICKY
    }

    private fun startForegroundCompat() {
        val notification = buildNotification(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "saku_lockscreen_cards"
        const val NOTIFICATION_ID = 1001
        const val ACTION_UPDATE = "com.saku.ACTION_UPDATE"
        const val ACTION_STOP = "com.saku.ACTION_STOP"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.notification_channel_description)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.createNotificationChannel(channel)
            }
        }

        fun startService(context: Context) {
            try {
                val intent = Intent(context, LockScreenCardService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun updateNotification(context: Context) {
            val prefs = PreferencesManager(context)
            if (!prefs.isServiceEnabled) return
            try {
                createNotificationChannel(context)
                val notification = buildNotification(context)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIFICATION_ID, notification)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, LockScreenCardService::class.java).apply {
                    action = ACTION_STOP
                }
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun buildNotification(context: Context): Notification {
            val prefs = PreferencesManager(context)
            val ankiHelper = AnkiDroidHelper(context)

            if (prefs.isSnoozed) {
                return buildSnoozedNotification(context, prefs, ankiHelper)
            }

            if (!ankiHelper.isAnkiDroidInstalled()) {
                return buildErrorNotification(context, context.getString(R.string.ankidroid_not_installed))
            }

            if (!ankiHelper.hasApiPermission()) {
                return buildErrorNotification(context, context.getString(R.string.api_permission_needed))
            }

            val card = CardSessionManager.getOrFetchCard(context)
            val stats = CardSessionManager.currentStats

            if (card == null) {
                return buildAllCaughtUpNotification(context, ankiHelper)
            }

            return buildFlashcardNotification(context, card, stats, prefs, ankiHelper)
        }

        private fun buildFlashcardNotification(
            context: Context,
            card: CardInfo?,
            stats: Triple<Int, Int, Int>,
            prefs: PreferencesManager,
            ankiHelper: AnkiDroidHelper
        ): Notification {
            val isRevealed = CardSessionManager.isRevealed
            val cardType = card?.cardType ?: 0
            val cardColor = when (cardType) {
                1 -> Color.parseColor("#EF5350")
                2 -> Color.parseColor("#66BB6A")
                else -> Color.parseColor("#42A5F5")
            }
            val smallIconRes = when (cardType) {
                1 -> R.drawable.ic_card_learn
                2 -> R.drawable.ic_card_review
                else -> R.drawable.ic_card_new
            }

            val collapsedViews = RemoteViews(context.packageName, R.layout.notification_card_collapsed)
            val expandedViews = RemoteViews(context.packageName, R.layout.notification_card_expanded)

            val revealIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_REVEAL
            }
            val revealPending = PendingIntent.getBroadcast(
                context,
                101,
                revealIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val againIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_GRADE_AGAIN
            }
            val againPending = PendingIntent.getBroadcast(
                context,
                102,
                againIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val goodIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_GRADE_GOOD
            }
            val goodPending = PendingIntent.getBroadcast(
                context,
                103,
                goodIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_SNOOZE
            }
            val snoozePending = PendingIntent.getBroadcast(
                context,
                104,
                snoozeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val openAnkiIntent = ankiHelper.getAnkiLaunchIntent(card?.deckId?.takeIf { it > 0 })
            val openAnkiPending = PendingIntent.getActivity(
                context,
                105,
                openAnkiIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val suspendIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_SUSPEND
            }
            val suspendPending = PendingIntent.getBroadcast(
                context,
                106,
                suspendIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val undoIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_UNDO
            }
            val undoPending = PendingIntent.getBroadcast(
                context,
                107,
                undoIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPending = PendingIntent.getActivity(
                context,
                108,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_DISMISSED
            }
            val dismissPending = PendingIntent.getBroadcast(
                context,
                999,
                dismissIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val revealedActionPending = when (prefs.classicRevealedAction) {
                "suspend" -> suspendPending
                "undo" -> undoPending
                "open_app" -> openAppPending
                else -> openAnkiPending
            }
            val revealedActionLabel = when (prefs.classicRevealedAction) {
                "suspend" -> "Suspend"
                "undo" -> "Undo"
                "open_app" -> "Open App"
                else -> "Open Anki"
            }

            val deckName = card?.deckName?.ifEmpty { "Saku" } ?: "Saku"
            val newC = stats.first
            val learnC = stats.second
            val revC = stats.third
            val statsHtml = "<font color='#8AB4F8'>$newC</font> · <font color='#F28B82'>$learnC</font> · <font color='#81C995'>$revC</font>"
            val statsSpanned = fromHtmlCompat(statsHtml)

            val kanjiText = card?.kanji?.ifEmpty { card.question } ?: "Review Deck"
            val kanjiFurigana = card?.kanjiFurigana ?: ""
            val kanjiMeaning = card?.kanjiMeaning?.ifEmpty { card.answer } ?: ""
            val rawSentence = card?.sentence ?: ""
            val highlightedSentence = highlightWordInSentence(rawSentence, kanjiText)
            val sentenceSpanned = fromHtmlCompat(highlightedSentence)
            val sentenceFurigana = card?.sentenceFurigana ?: ""
            val sentenceMeaning = card?.sentenceMeaning ?: ""

            val imageBitmap = if (!card?.imageFileName.isNullOrBlank()) {
                ankiHelper.getCardImageBitmap(card!!.imageFileName, maxDimension = 260)
            } else {
                null
            }

            val rubyBitmapExpanded = if (isRevealed && sentenceFurigana.isNotBlank()) {
                RubyTextRenderer.renderRubyBitmap(
                    context = context,
                    rawText = sentenceFurigana,
                    highlightWord = kanjiText,
                    baseTextSizeSp = 15f,
                    rubyTextSizeSp = 8.5f,
                    isCentered = true
                )
            } else {
                null
            }

            val rubyBitmapCollapsed = if (isRevealed && sentenceFurigana.isNotBlank()) {
                RubyTextRenderer.renderRubyBitmap(
                    context = context,
                    rawText = sentenceFurigana,
                    highlightWord = kanjiText,
                    baseTextSizeSp = 13f,
                    rubyTextSizeSp = 7.5f,
                    isCentered = true
                )
            } else {
                null
            }

            val viewsAndRuby = listOf(
                Pair(collapsedViews, rubyBitmapCollapsed),
                Pair(expandedViews, rubyBitmapExpanded)
            )

            viewsAndRuby.forEach { (rv, rubyBitmap) ->
                rv.setTextViewText(R.id.tv_deck_name, deckName)
                rv.setTextViewText(R.id.tv_deck_stats, statsSpanned)
                rv.setTextViewText(R.id.tv_kanji, kanjiText)

                if (imageBitmap != null) {
                    rv.setImageViewBitmap(R.id.iv_card_image, imageBitmap)
                    rv.setViewVisibility(R.id.iv_card_image, View.VISIBLE)
                } else {
                    rv.setViewVisibility(R.id.iv_card_image, View.GONE)
                }

                rv.setOnClickPendingIntent(R.id.notification_root, revealPending)
                rv.setOnClickPendingIntent(R.id.btn_reveal, revealPending)
                rv.setOnClickPendingIntent(R.id.btn_again, againPending)
                rv.setOnClickPendingIntent(R.id.btn_good, goodPending)
                rv.setOnClickPendingIntent(R.id.btn_snooze, snoozePending)

                if (!isRevealed) {
                    rv.setViewVisibility(R.id.btn_reveal, View.VISIBLE)
                    rv.setViewVisibility(R.id.btn_snooze, View.VISIBLE)
                    rv.setViewVisibility(R.id.btn_open_anki, View.VISIBLE)
                    rv.setTextViewText(R.id.btn_open_anki, "Anki")
                    rv.setOnClickPendingIntent(R.id.btn_open_anki, openAnkiPending)
                    rv.setViewVisibility(R.id.btn_again, View.GONE)
                    rv.setViewVisibility(R.id.btn_good, View.GONE)

                    rv.setViewVisibility(R.id.tv_kanji_furigana, View.GONE)
                    rv.setViewVisibility(R.id.tv_kanji_meaning, View.GONE)
                    rv.setViewVisibility(R.id.tv_sentence_furigana, View.GONE)
                    rv.setViewVisibility(R.id.iv_sentence_ruby, View.GONE)
                    rv.setViewVisibility(R.id.tv_sentence_meaning, View.GONE)

                    if (rawSentence.isNotBlank()) {
                        rv.setTextViewText(R.id.tv_sentence, sentenceSpanned)
                        rv.setViewVisibility(R.id.tv_sentence, View.VISIBLE)
                    } else {
                        rv.setViewVisibility(R.id.tv_sentence, View.GONE)
                    }
                } else {
                    rv.setViewVisibility(R.id.btn_reveal, View.GONE)
                    rv.setViewVisibility(R.id.btn_snooze, View.GONE)
                    rv.setViewVisibility(R.id.btn_open_anki, View.VISIBLE)
                    rv.setTextViewText(R.id.btn_open_anki, revealedActionLabel)
                    rv.setOnClickPendingIntent(R.id.btn_open_anki, revealedActionPending)
                    rv.setViewVisibility(R.id.btn_again, View.VISIBLE)
                    rv.setViewVisibility(R.id.btn_good, View.VISIBLE)

                    if (kanjiFurigana.isNotBlank()) {
                        rv.setTextViewText(R.id.tv_kanji_furigana, kanjiFurigana)
                        rv.setViewVisibility(R.id.tv_kanji_furigana, View.VISIBLE)
                    } else {
                        rv.setViewVisibility(R.id.tv_kanji_furigana, View.GONE)
                    }

                    if (kanjiMeaning.isNotBlank()) {
                        rv.setTextViewText(R.id.tv_kanji_meaning, kanjiMeaning)
                        rv.setViewVisibility(R.id.tv_kanji_meaning, View.VISIBLE)
                    } else {
                        rv.setViewVisibility(R.id.tv_kanji_meaning, View.GONE)
                    }

                    rv.setViewVisibility(R.id.tv_sentence_furigana, View.GONE)

                    if (rubyBitmap != null) {
                        rv.setImageViewBitmap(R.id.iv_sentence_ruby, rubyBitmap)
                        rv.setViewVisibility(R.id.iv_sentence_ruby, View.VISIBLE)
                        rv.setViewVisibility(R.id.tv_sentence, View.GONE)
                    } else if (rawSentence.isNotBlank()) {
                        rv.setViewVisibility(R.id.iv_sentence_ruby, View.GONE)
                        rv.setTextViewText(R.id.tv_sentence, sentenceSpanned)
                        rv.setViewVisibility(R.id.tv_sentence, View.VISIBLE)
                    } else {
                        rv.setViewVisibility(R.id.iv_sentence_ruby, View.GONE)
                        rv.setViewVisibility(R.id.tv_sentence, View.GONE)
                    }

                    if (sentenceMeaning.isNotBlank()) {
                        rv.setTextViewText(R.id.tv_sentence_meaning, sentenceMeaning)
                        rv.setViewVisibility(R.id.tv_sentence_meaning, View.VISIBLE)
                    } else {
                        rv.setViewVisibility(R.id.tv_sentence_meaning, View.GONE)
                    }
                }
            }

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(smallIconRes)
                .setColor(cardColor)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(collapsedViews)
                .setCustomBigContentView(expandedViews)
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(revealPending)
                .setDeleteIntent(dismissPending)
                .setSilent(true)
                .build()
        }

        private fun highlightWordInSentence(sentence: String, kanji: String): String {
            if (sentence.isBlank()) return ""
            if (sentence.contains("<b>") || sentence.contains("<strong>")) {
                return sentence
                    .replace("<b>", "<font color='#8AB4F8'><b>")
                    .replace("</b>", "</b></font>")
                    .replace("<strong>", "<font color='#8AB4F8'><strong>")
                    .replace("</strong>", "</strong></font>")
            }
            val cleanKanji = kanji.trim()
            if (cleanKanji.isNotEmpty() && sentence.contains(cleanKanji)) {
                return sentence.replace(cleanKanji, "<font color='#8AB4F8'>$cleanKanji</font>")
            }
            val rootKanji = cleanKanji.filter { it in '\u4E00'..'\u9FAF' }
            if (rootKanji.isNotEmpty() && sentence.contains(rootKanji)) {
                val regex = Regex("(${Regex.escape(rootKanji)}[\u3040-\u309F]*)")
                return regex.replace(sentence, "<font color='#8AB4F8'>$1</font>")
            }
            return sentence
        }

        private fun fromHtmlCompat(html: String): CharSequence {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
        }

        private fun buildAllCaughtUpNotification(context: Context, ankiHelper: AnkiDroidHelper): Notification {
            val prefs = PreferencesManager(context)
            val singleDeckId = prefs.getSelectedDeckIdsAsLongs().let { if (it.size == 1) it.first() else null }
            val ankiIntent = ankiHelper.getAnkiLaunchIntent(singleDeckId)
            val openAnkiPending = PendingIntent.getActivity(
                context,
                201,
                ankiIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("お疲れ様でした！ 🎉")
                .setContentText("All reviews complete for today! Come back tomorrow.")
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(openAnkiPending)
                .addAction(R.drawable.ic_notification, "Open Anki", openAnkiPending)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(true)
                .build()
        }

        private fun buildSnoozedNotification(
            context: Context,
            prefs: PreferencesManager,
            ankiHelper: AnkiDroidHelper
        ): Notification {
            val remainingMs = prefs.snoozeUntil - System.currentTimeMillis()
            val remainingMin = (remainingMs / 60000).coerceAtLeast(1)

            val singleDeckId = prefs.getSelectedDeckIdsAsLongs().let { if (it.size == 1) it.first() else null }
            val ankiIntent = ankiHelper.getAnkiLaunchIntent(singleDeckId)
            val openAnkiPending = PendingIntent.getActivity(
                context,
                202,
                ankiIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val unsnoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_UNSNOOZE
            }
            val unsnoozePending = PendingIntent.getBroadcast(
                context,
                204,
                unsnoozeIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val dismissIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_DISMISSED
            }
            val dismissPending = PendingIntent.getBroadcast(
                context,
                999,
                dismissIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Saku Snoozed")
                .setContentText("Snoozed for ${remainingMin}m · Tap to resume")
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(unsnoozePending)
                .setDeleteIntent(dismissPending)
                .addAction(R.drawable.btn_good_bg, "Resume Now", unsnoozePending)
                .addAction(R.drawable.ic_notification, "Open Anki", openAnkiPending)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setSilent(true)
                .build()
        }

        private fun buildErrorNotification(context: Context, message: String): Notification {
            val mainIntent = Intent(context, MainActivity::class.java)
            val mainPending = PendingIntent.getActivity(
                context,
                203,
                mainIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Saku")
                .setContentText(message)
                .setOngoing(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(mainPending)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSilent(true)
                .build()
        }
    }
}
