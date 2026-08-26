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
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.saku.R
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, LockScreenCardService::class.java)
            context.stopService(intent)
        }

        fun updateNotification(context: Context, card: CardModel) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notification = buildCardNotification(context, card)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }

        fun buildCardNotification(context: Context, card: CardModel): Notification {
            createNotificationChannel(context)

            val openAppIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Next card PendingIntent
            val nextIntent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_NEXT
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                nextIntent,
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

            // Compact View for Lockscreen / AOD
            val compactView = RemoteViews(context.packageName, R.layout.notification_saku_compact).apply {
                setTextViewText(R.id.tv_notification_kanji, card.kanji)
                setTextViewText(R.id.tv_notification_kana, card.kana)
                setTextViewText(R.id.tv_notification_romaji, card.romaji)
                setTextViewText(R.id.tv_notification_meaning, card.meaning)
                setTextViewText(R.id.tv_notification_example, card.example)
                setOnClickPendingIntent(R.id.btn_notification_next, nextPendingIntent)
            }

            // Expanded View for Notification Shade with Review Grading
            val expandedView = RemoteViews(context.packageName, R.layout.notification_saku_expanded).apply {
                setTextViewText(R.id.tv_exp_kanji, card.kanji)
                setTextViewText(R.id.tv_exp_kana, card.kana)
                setTextViewText(R.id.tv_exp_romaji, card.romaji)
                setTextViewText(R.id.tv_exp_meaning, card.meaning)
                setTextViewText(R.id.tv_exp_example, card.example)

                setOnClickPendingIntent(R.id.btn_exp_again, createGradePendingIntent(ReviewEase.AGAIN, 10))
                setOnClickPendingIntent(R.id.btn_exp_hard, createGradePendingIntent(ReviewEase.HARD, 11))
                setOnClickPendingIntent(R.id.btn_exp_good, createGradePendingIntent(ReviewEase.GOOD, 12))
                setOnClickPendingIntent(R.id.btn_exp_easy, createGradePendingIntent(ReviewEase.EASY, 13))
            }

            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setCustomContentView(compactView)
                .setCustomBigContentView(expandedView)
                .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setContentIntent(openAppIntent)
                .build()
        }

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Saku Lock Screen Cards",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Displays minimal Japanese flashcards on Lock Screen & AOD"
                    setShowBadge(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val prefs = SakuPreferences(this)
        val card = prefs.getActiveCard()
        startForeground(NOTIFICATION_ID, buildCardNotification(this, card))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = SakuPreferences(this)
        val card = prefs.getActiveCard()
        startForeground(NOTIFICATION_ID, buildCardNotification(this, card))
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
