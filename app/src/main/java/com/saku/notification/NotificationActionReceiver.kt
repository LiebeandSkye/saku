package com.saku.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saku.data.CardSessionManager
import com.saku.data.PreferencesManager

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REVEAL, ACTION_SHOW_ANSWER -> {
                CardSessionManager.toggleReveal(context)
            }
            ACTION_GRADE_AGAIN -> {
                CardSessionManager.gradeCard(context, 1)
            }
            ACTION_GRADE_GOOD -> {
                CardSessionManager.gradeCard(context, 3)
            }
            ACTION_GRADE -> {
                val ease = intent.getIntExtra(EXTRA_EASE, 3)
                CardSessionManager.gradeCard(context, ease)
            }
            ACTION_SUSPEND -> {
                CardSessionManager.suspendCurrentCard(context)
            }
            ACTION_UNDO -> {
                CardSessionManager.undoLastReview(context)
            }
            ACTION_SNOOZE -> {
                val prefs = PreferencesManager(context)
                val durationMs = prefs.snoozeDurationMinutes * 60 * 1000L
                prefs.snoozeUntil = System.currentTimeMillis() + durationMs
                LockScreenCardService.updateNotification(context)
            }
            ACTION_UNSNOOZE -> {
                val prefs = PreferencesManager(context)
                prefs.snoozeUntil = 0L
                LockScreenCardService.updateNotification(context)
            }
            ACTION_DISMISSED -> {
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
    }
}
