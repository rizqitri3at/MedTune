package com.rizqitriantomi.medtune

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import java.io.InputStream

abstract class BaseMusicWidgetProvider(private val layoutId: Int) : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "UPDATE_WIDGETS") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, this.javaClass)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (id in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }
    }

    protected open fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, layoutId)
        
        val playPauseIntent = Intent(context, MusicService::class.java).apply { action = "PLAY_PAUSE" }
        val nextIntent = Intent(context, MusicService::class.java).apply { action = "NEXT" }
        val prevIntent = Intent(context, MusicService::class.java).apply { action = "PREV" }
        val openAppIntent = Intent(context, MainActivity::class.java)

        views.setOnClickPendingIntent(R.id.btn_play_pause, PendingIntent.getService(context, 0, playPauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        views.setOnClickPendingIntent(R.id.btn_next, PendingIntent.getService(context, 1, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        views.setOnClickPendingIntent(R.id.btn_prev, PendingIntent.getService(context, 2, prevIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        
        val pOpenApp = PendingIntent.getActivity(context, 5, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        views.setOnClickPendingIntent(R.id.txt_title, pOpenApp)
        views.setOnClickPendingIntent(R.id.txt_artist, pOpenApp)

        views.setTextViewText(R.id.txt_title, PlayerState.currentTitle.value)
        views.setTextViewText(R.id.txt_artist, PlayerState.currentArtist.value)
        views.setImageViewResource(R.id.btn_play_pause, if (PlayerState.isPlaying.value) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)

        val artUri = PlayerState.albumArtUri.value
        if (artUri != null) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(artUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                views.setImageViewBitmap(R.id.img_album_art, bitmap)
            } catch (e: Exception) {
                views.setImageViewResource(R.id.img_album_art, R.drawable.mediaplay_about)
            }
        } else {
            views.setImageViewResource(R.id.img_album_art, R.drawable.mediaplay_about)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}

class MusicWidgetProvider3x1 : BaseMusicWidgetProvider(R.layout.player_widget_3x1)
