package com.saku.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.saku.data.SakuPreferences
import com.saku.notification.LockScreenCardService
import com.saku.widget.SakuGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = SakuPreferences(context)
            if (prefs.isLockScreenCardEnabled) {
                LockScreenCardService.startService(context)
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    SakuGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    // Safe widget update on reboot
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
