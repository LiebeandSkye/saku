package com.saku.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.saku.data.CardSessionManager
import com.saku.data.PreferencesManager

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REVEAL, ACTION_SHOW_ANSWER -> {
                CardSessionManager.toggleReveal(context)
            }
            ACTION_GRADE_AGAIN -> {
                val pendingResult = goAsync()
                CardSessionManager.gradeCard(context, 1) {
                    pendingResult.finish()
                }
            }
            ACTION_GRADE_GOOD -> {
                val pendingResult = goAsync()
                CardSessionManager.gradeCard(context, 3) {
                    pendingResult.finish()
                }
            }
            ACTION_GRADE -> {
                val pendingResult = goAsync()
                val ease = intent.getIntExtra(EXTRA_EASE, 3)
                CardSessionManager.gradeCard(context, ease) {
                    pendingResult.finish()
                }
            }
            ACTION_SUSPEND -> {
                val pendingResult = goAsync()
                CardSessionManager.suspendCurrentCard(context) {
                    pendingResult.finish()
                }
            }
            ACTION_UNDO -> {
                CardSessionManager.undoLastReview(context)
            }
            ACTION_SNOOZE -> {
                val prefs = PreferencesManager(context)
                val durationMs = prefs.snoozeDurationMinutes * 60 * 1000L
                val targetTime = System.currentTimeMillis() + durationMs
                prefs.snoozeUntil = targetTime
                scheduleAutoUnsnooze(context, targetTime)
                LockScreenCardService.updateNotification(context)
            }
            ACTION_UNSNOOZE -> {
                val prefs = PreferencesManager(context)
                prefs.snoozeUntil = 0L
                cancelAutoUnsnooze(context)
                LockScreenCardService.updateNotification(context)
            }
            ACTION_DISMISSED -> {
                val prefs = PreferencesManager(context)
                prefs.snoozeUntil = 0L
                cancelAutoUnsnooze(context)
                LockScreenCardService.updateNotification(context)
            }
        }
    }

    companion object {
        const val ACTION_REVEAL = "com.saku.action.REVEAL"
        const val ACTION_SHOW_ANSWER = "com.saku.action.SHOW_ANSWER"
        const val ACTION_GRADE_AGAIN = "com.saku.action.GRADE_AGAIN"
        const val ACTION_GRADE_GOOD = "com.saku.action.GRADE_GOOD"
        const val ACTION_GRADE = "com.saku.action.GRADE"
        const val ACTION_SUSPEND = "com.saku.action.SUSPEND"
        const val ACTION_UNDO = "com.saku.action.UNDO"
        const val ACTION_SNOOZE = "com.saku.action.SNOOZE"
        const val ACTION_UNSNOOZE = "com.saku.action.UNSNOOZE"
        const val ACTION_DISMISSED = "com.saku.action.DISMISSED"
        const val EXTRA_EASE = "extra_ease"

        private fun getUnsnoozePendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, NotificationActionReceiver::class.java).apply {
                action = ACTION_UNSNOOZE
            }
            return PendingIntent.getBroadcast(
                context,
                888,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        fun scheduleAutoUnsnooze(context: Context, triggerAtMillis: Long) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val pending = getUnsnoozePendingIntent(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                }
            } catch (e: Exception) {
                try {
                    val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                    val pending = getUnsnoozePendingIntent(context)
                    am?.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
                } catch (ignored: Exception) {
                }
            }
        }

        fun cancelAutoUnsnooze(context: Context) {
            try {
                val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
                val pending = getUnsnoozePendingIntent(context)
                am.cancel(pending)
            } catch (ignored: Exception) {
            }
        }
    }
}
