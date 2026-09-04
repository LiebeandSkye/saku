package com.saku.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import android.util.Log
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import com.saku.R
import com.saku.anki.AnkiDroidHelper
import com.saku.data.CardSessionManager
import com.saku.util.MediaArtworkGenerator

class SakuWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_WIDGET_REVEAL -> {
                CardSessionManager.toggleReveal(context)
            }
            ACTION_WIDGET_AGAIN -> {
                CardSessionManager.gradeCard(context, 1)
            }
            ACTION_WIDGET_GOOD -> {
                CardSessionManager.gradeCard(context, 3)
            }
            ACTION_WIDGET_REFRESH -> {
                CardSessionManager.refresh(context)
            }
        }
    }

    companion object {
        const val ACTION_WIDGET_REVEAL = "com.saku.widget.ACTION_REVEAL"
        const val ACTION_WIDGET_AGAIN = "com.saku.widget.ACTION_AGAIN"
        const val ACTION_WIDGET_GOOD = "com.saku.widget.ACTION_GOOD"
        const val ACTION_WIDGET_REFRESH = "com.saku.widget.ACTION_REFRESH"

        fun updateAllWidgets(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisWidget = ComponentName(context, SakuWidgetProvider::class.java)
                val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
                for (widgetId in allWidgetIds) {
                    updateWidget(context, appWidgetManager, widgetId)
                }
            } catch (t: Throwable) {
                Log.e("SakuWidgetProvider", "Failed to update all widgets", t)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int
        ) {
            try {
                val ankiHelper = AnkiDroidHelper(context)
                val card = CardSessionManager.getOrFetchCard(context)
                val isRevealed = CardSessionManager.isRevealed
                val stats = CardSessionManager.currentStats

                val imageBitmap = if (!card?.imageFileName.isNullOrBlank()) {
                    ankiHelper.getCardImageBitmap(card!!.imageFileName)
                } else {
                    null
                }

                val options = appWidgetManager.getAppWidgetOptions(widgetId)
                val minWidthDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) ?: 0
                val minHeightDp = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT) ?: 0
                val density = context.resources.displayMetrics.density

                val widthDp = if (minWidthDp > 0) minWidthDp else 280
                val heightDp = if (minHeightDp > 0) minHeightDp else 320

                val scaleTier = when {
                    widthDp >= 320 -> 1.20f
                    widthDp >= 250 -> 1.00f
                    widthDp >= 180 -> 0.85f
                    else -> 0.75f
                }

                val targetW = (widthDp * density).toInt().coerceAtLeast(300)
                val targetH = (heightDp * density).toInt().coerceAtLeast(300)

                val artwork = MediaArtworkGenerator.generateArtwork(
                    context = context,
                    card = card,
                    stats = stats,
                    isRevealed = isRevealed,
                    imageBitmap = imageBitmap,
                    showBottomControls = false,
                    targetWidth = targetW,
                    targetHeight = targetH,
                    fontScaleMultiplier = scaleTier
                )

                val rv = RemoteViews(context.packageName, R.layout.widget_card)
                rv.setImageViewBitmap(R.id.iv_widget_artwork, artwork)

                rv.setTextViewText(
                    R.id.btn_widget_reveal,
                    if (isRevealed) "Hide" else "Reveal"
                )

                val revealIntent = Intent(context, SakuWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_REVEAL
                }
                val revealPending = PendingIntent.getBroadcast(
                    context,
                    301,
                    revealIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                rv.setOnClickPendingIntent(R.id.btn_widget_reveal, revealPending)
                rv.setOnClickPendingIntent(R.id.iv_widget_artwork, revealPending)

                val againIntent = Intent(context, SakuWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_AGAIN
                }
                val againPending = PendingIntent.getBroadcast(
                    context,
                    302,
                    againIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                rv.setOnClickPendingIntent(R.id.btn_widget_again, againPending)

                val goodIntent = Intent(context, SakuWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_GOOD
                }
                val goodPending = PendingIntent.getBroadcast(
                    context,
                    303,
                    goodIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                rv.setOnClickPendingIntent(R.id.btn_widget_good, goodPending)

                val ankiIntent = ankiHelper.getAnkiLaunchIntent()
                val ankiPending = PendingIntent.getActivity(
                    context,
                    304,
                    ankiIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                rv.setOnClickPendingIntent(R.id.btn_widget_open, ankiPending)

                appWidgetManager.updateAppWidget(widgetId, rv)
            } catch (t: Throwable) {
                Log.e("SakuWidgetProvider", "Failed to update widget $widgetId", t)
            }
        }
    }
}
