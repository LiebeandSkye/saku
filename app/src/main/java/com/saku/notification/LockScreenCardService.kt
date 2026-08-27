package com.saku.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.saku.R
import com.saku.anki.AnkiDroidClient
import com.saku.anki.FuriganaBitmapRenderer
import com.saku.anki.JapaneseFieldParser
import com.saku.data.CardModel
import com.saku.data.ReviewEase
import com.saku.data.SakuPreferences
import com.saku.ui.MainActivity

class LockScreenCardService : Service {

    constructor() : super()

    companion object {
        const val CHANNEL_ID = "saku_lockscreen_cards"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, LockScreenCardService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Foreground service start error handling
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LockScreenCardService::class.java)
            context.stopService(intent)
        }

        fun updateNotification(context: Context, card: CardModel, showAnswer: Boolean = false) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = buildCardNotification(context, card, showAnswer)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        fun buildCardNotification(context: Context, card: CardModel, showAnswer: Boolean = false): Notification {
            createNotificationChannel(context)

            val openAppIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Show Answer PendingIntent
            val showAnswerIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_SHOW_ANSWER
            }
            val showAnswerPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                showAnswerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Review Grade PendingIntents
            fun createGradePendingIntent(ease: ReviewEase, requestCode: Int): PendingIntent {
                val gradeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_GRADE
                    putExtra(NotificationActionReceiver.EXTRA_EASE, ease.value)
                }
                return PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    gradeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }

            val againPendingIntent = createGradePendingIntent(ReviewEase.AGAIN, 10)
            val goodPendingIntent = createGradePendingIntent(ReviewEase.GOOD, 12)

            // Open Anki PendingIntent
            val ankiClient = AnkiDroidClient(context)
            val openAnkiIntent = ankiClient.getOpenAnkiIntent(card.noteId)
            val openAnkiPendingIntent = PendingIntent.getActivity(
                context,
                20,
                openAnkiIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Counts
            val newCountStr = if (card.newCount > 0) card.newCount.toString() else "15"
            val learnCountStr = if (card.learnCount > 0) card.learnCount.toString() else "17"
            val reviewCountStr = if (card.reviewCount > 0) card.reviewCount.toString() else "21"

            val frontSentence = card.exampleSentence.ifEmpty {
                card.example.substringBefore("•").trim().ifEmpty { card.kanji }
            }
            val furiganaWord = card.furigana.ifEmpty { card.kana }
            val sentenceTrans = card.exampleTranslation.ifEmpty {
                val after = card.example.substringAfter("•", "").trim()
                after.ifEmpty { card.meaning }
            }

            // Compact View for Lockscreen / AOD
            val compactView = RemoteViews(context.packageName, R.layout.notification_saku_compact).apply {
                if (!showAnswer) {
                    setViewVisibility(R.id.layout_front, View.VISIBLE)
                    setViewVisibility(R.id.layout_back, View.GONE)

                    setTextViewText(R.id.tv_front_count_new, newCountStr)
                    setTextViewText(R.id.tv_front_count_learn, learnCountStr)
                    setTextViewText(R.id.tv_front_count_review, reviewCountStr)
                    setTextViewText(R.id.tv_front_kanji, card.kanji)
                    setTextViewText(R.id.tv_front_sentence, frontSentence)
                    setOnClickPendingIntent(R.id.btn_front_show_answer, showAnswerPendingIntent)
                } else {
                    setViewVisibility(R.id.layout_front, View.GONE)
                    setViewVisibility(R.id.layout_back, View.VISIBLE)

                    setTextViewText(R.id.tv_back_count_new, newCountStr)
                    setTextViewText(R.id.tv_back_count_learn, learnCountStr)
                    setTextViewText(R.id.tv_back_count_review, reviewCountStr)
                    setTextViewText(R.id.tv_back_furigana, furiganaWord)
                    setTextViewText(R.id.tv_back_kanji, card.kanji)
                    setTextViewText(R.id.tv_back_meaning, card.meaning)

                    // Render Furigana-aligned sentence
                    val sentenceSource = card.exampleFurigana.ifEmpty { card.exampleSentence }
                    val segments = JapaneseFieldParser.parseFuriganaSegments(sentenceSource, targetWord = card.kanji)
                    val compactBitmap = FuriganaBitmapRenderer.renderFuriganaSentenceBitmap(
                        context = context,
                        segments = segments,
                        kanjiTextSizeSp = 13f,
                        furiganaTextSizeSp = 9f
                    )

                    if (compactBitmap != null) {
                        setImageViewBitmap(R.id.iv_back_sentence_canvas, compactBitmap)
                        setViewVisibility(R.id.iv_back_sentence_canvas, View.VISIBLE)
                        setViewVisibility(R.id.tv_back_example_furigana, View.GONE)
                        setViewVisibility(R.id.tv_back_example_sentence, View.GONE)
                    } else {
                        setViewVisibility(R.id.iv_back_sentence_canvas, View.GONE)
                        if (card.exampleFuriganaLine.isNotEmpty()) {
                            setViewVisibility(R.id.tv_back_example_furigana, View.VISIBLE)
                            setTextViewText(R.id.tv_back_example_furigana, card.exampleFuriganaLine)
                        } else {
                            setViewVisibility(R.id.tv_back_example_furigana, View.GONE)
                        }
                        setTextViewText(R.id.tv_back_example_sentence, card.exampleSentenceLine.ifEmpty { frontSentence })
                    }

                    setTextViewText(R.id.tv_back_example_trans, sentenceTrans)

                    setOnClickPendingIntent(R.id.btn_back_again, againPendingIntent)
                    setOnClickPendingIntent(R.id.btn_back_good, goodPendingIntent)
                    setOnClickPendingIntent(R.id.btn_back_open_anki, openAnkiPendingIntent)
                }
            }

            // Expanded View for Notification Shade
            val expandedView = RemoteViews(context.packageName, R.layout.notification_saku_expanded).apply {
                if (!showAnswer) {
                    setViewVisibility(R.id.layout_exp_front, View.VISIBLE)
                    setViewVisibility(R.id.layout_exp_back, View.GONE)

                    setTextViewText(R.id.tv_exp_front_count_new, newCountStr)
                    setTextViewText(R.id.tv_exp_front_count_learn, learnCountStr)
                    setTextViewText(R.id.tv_exp_front_count_review, reviewCountStr)
                    setTextViewText(R.id.tv_exp_front_kanji, card.kanji)
                    setTextViewText(R.id.tv_exp_front_sentence, frontSentence)
                    setOnClickPendingIntent(R.id.btn_exp_front_show_answer, showAnswerPendingIntent)
                } else {
                    setViewVisibility(R.id.layout_exp_front, View.GONE)
                    setViewVisibility(R.id.layout_exp_back, View.VISIBLE)

                    setTextViewText(R.id.tv_exp_back_count_new, newCountStr)
                    setTextViewText(R.id.tv_exp_back_count_learn, learnCountStr)
                    setTextViewText(R.id.tv_exp_back_count_review, reviewCountStr)
                    setTextViewText(R.id.tv_exp_back_furigana, furiganaWord)
                    setTextViewText(R.id.tv_exp_back_kanji, card.kanji)
                    setTextViewText(R.id.tv_exp_back_meaning, card.meaning)

                    val sentenceSource = card.exampleFurigana.ifEmpty { card.exampleSentence }
                    val segments = JapaneseFieldParser.parseFuriganaSegments(sentenceSource, targetWord = card.kanji)
                    val expandedBitmap = FuriganaBitmapRenderer.renderFuriganaSentenceBitmap(
                        context = context,
                        segments = segments,
                        kanjiTextSizeSp = 15f,
                        furiganaTextSizeSp = 10f
                    )

                    if (expandedBitmap != null) {
                        setImageViewBitmap(R.id.iv_exp_back_sentence_canvas, expandedBitmap)
                        setViewVisibility(R.id.iv_exp_back_sentence_canvas, View.VISIBLE)
                        setViewVisibility(R.id.tv_exp_back_example_furigana, View.GONE)
                        setViewVisibility(R.id.tv_exp_back_example_sentence, View.GONE)
                    } else {
                        setViewVisibility(R.id.iv_exp_back_sentence_canvas, View.GONE)
                        if (card.exampleFuriganaLine.isNotEmpty()) {
                            setViewVisibility(R.id.tv_exp_back_example_furigana, View.VISIBLE)
                            setTextViewText(R.id.tv_exp_back_example_furigana, card.exampleFuriganaLine)
                        } else {
                            setViewVisibility(R.id.tv_exp_back_example_furigana, View.GONE)
                        }
                        setTextViewText(R.id.tv_exp_back_example_sentence, card.exampleSentenceLine.ifEmpty { frontSentence })
                    }

                    setTextViewText(R.id.tv_exp_back_example_trans, sentenceTrans)

                    setOnClickPendingIntent(R.id.btn_exp_back_again, againPendingIntent)
                    setOnClickPendingIntent(R.id.btn_exp_back_good, goodPendingIntent)
                    setOnClickPendingIntent(R.id.btn_exp_back_open_anki, openAnkiPendingIntent)
                }
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setCustomContentView(compactView)
                .setCustomBigContentView(expandedView)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setSilent(true)
                .setContentIntent(openAppIntent)

            return builder.build()
        }

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Saku Flashcards",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Displays minimal Japanese flashcards on Lock Screen & AOD"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(null, null)
                    enableVibration(false)
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundCompat()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        return START_STICKY
    }

    private fun startForegroundCompat() {
        val prefs = SakuPreferences(this)
        val card = prefs.getActiveCard()
        val notification = buildCardNotification(this, card, prefs.isAnswerRevealed)

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
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

