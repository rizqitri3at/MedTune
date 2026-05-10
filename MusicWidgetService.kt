package com.rizqitriantomi.medtune

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.io.InputStream

class MusicWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return PlaylistRemoteViewsFactory(this.applicationContext)
    }
}

class PlaylistRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var playlist: List<SongData> = emptyList()

    override fun onCreate() {
        playlist = PlayerState.currentPlaylist.value
    }

    override fun onDataSetChanged() {
        playlist = PlayerState.currentPlaylist.value
    }

    override fun onDestroy() {}

    override fun getCount(): Int = playlist.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.playlist_item)
        if (position in playlist.indices) {
            val song = playlist[position]
            views.setTextViewText(R.id.item_title, song.title)
            views.setTextViewText(R.id.item_artist, song.artist)
            
            val albumArtUri = Uri.parse("content://media/external/audio/albumart/${song.albumId}")
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(albumArtUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    views.setImageViewBitmap(R.id.item_art, bitmap)
                } else {
                    views.setImageViewResource(R.id.item_art, R.drawable.mediaplay_about)
                }
            } catch (e: Exception) {
                views.setImageViewResource(R.id.item_art, R.drawable.mediaplay_about)
            }

            val fillInIntent = Intent().apply {
                putExtra("position", position)
            }
            views.setOnClickFillInIntent(R.id.item_title, fillInIntent)
            views.setOnClickFillInIntent(R.id.item_artist, fillInIntent)
        }
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
