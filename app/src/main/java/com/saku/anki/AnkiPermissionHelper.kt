package com.saku.anki

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object AnkiPermissionHelper {
    const val REQUEST_CODE_ANKI_PERMISSION = 2001

    fun hasAnkiPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            AnkiDroidContract.PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestAnkiPermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(AnkiDroidContract.PERMISSION),
            REQUEST_CODE_ANKI_PERMISSION
        )
    }

    fun openPlayStoreForAnkiDroid(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=com.ichi2.anki")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.ichi2.anki")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        }
    }
}
