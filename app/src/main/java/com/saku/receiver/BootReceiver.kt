package com.saku.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.saku.data.PreferencesManager
import com.saku.notification.LockScreenCardService
import com.saku.widget.SakuWidgetProvider
import com.saku.worker.DueCountWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = PreferencesManager(context)
            if (prefs.isServiceEnabled) {
                LockScreenCardService.startService(context)
                DueCountWorker.schedule(context, prefs.updateIntervalMinutes.toLong())
            }
            SakuWidgetProvider.updateAllWidgets(context)
        }
    }
}
