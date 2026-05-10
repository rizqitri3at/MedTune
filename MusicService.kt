package com.rizqitriantomi.medtune

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import java.io.InputStream

class MusicService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaSession: MediaSessionCompat? = null
    private var lastUpdatedTitle = ""
    private var cachedBitmap: Bitmap? = null
    private var lastBitmapUri: Uri? = null

    override fun onBind(intent: Intent?): IBinder? = null

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        Log.d("MusicService", "onCreate")
        createNotificationChannel()

        // Initial foreground notification to prevent "Background Service Start" exception
        val notification = createPlaceholderNotification()
        startForeground(NOTIFICATION_ID, notification)

        mediaSession = MediaSessionCompat(this, "MedTuneSession").apply {
            isActive = true
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = MusicPlayer.toggle(this@MusicService)
                override fun onPause() = MusicPlayer.toggle(this@MusicService)
                override fun onSkipToNext() = MusicPlayer.next(this@MusicService)
                override fun onSkipToPrevious() = MusicPlayer.prev(this@MusicService)
                override fun onStop() = stopService()
                override fun onSeekTo(pos: Long) = MusicPlayer.seekTo(pos)
            })
        }
        
        serviceScope.launch {
            snapshotFlow { 
                Triple(PlayerState.isPlaying.value, PlayerState.currentTitle.value, PlayerState.albumArtUri.value)
            }.debounce(150).collectLatest { (playing, title, artUri) ->
                if (title == "No Song" && !playing) return@collectLatest

                if (artUri != lastBitmapUri) {
                    cachedBitmap = getAlbumArtBitmap(artUri)
                    lastBitmapUri = artUri
                }
                
                updatePlaybackState()
                if (title != lastUpdatedTitle) {
                    updateMetadata()
                    lastUpdatedTitle = title
                }
                updateNotification()
                
                val updateIntent = Intent("UPDATE_WIDGETS")
                updateIntent.setPackage(packageName)
                sendBroadcast(updateIntent)
            }
        }

        serviceScope.launch {
            while (isActive) {
                if (PlayerState.isPlaying.value) updatePlaybackState()
                delay(1000) 
            }
        }
    }

    private fun stopService() {
        Log.d("MusicService", "stopService")
        MusicPlayer.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun updatePlaybackState() {
        val state = if (PlayerState.isPlaying.value) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_STOP or
                PlaybackStateCompat.ACTION_SEEK_TO
        
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, PlayerState.currentPosition.value, PlayerState.playbackSpeed.value)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun updateMetadata() {
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, PlayerState.currentTitle.value)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, PlayerState.currentArtist.value)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, PlayerState.duration.value)
            .apply {
                if (cachedBitmap != null) putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, cachedBitmap)
            }
            .build()
        mediaSession?.setMetadata(metadata)
    }

    private suspend fun getAlbumArtBitmap(uri: Uri?): Bitmap? = withContext(Dispatchers.IO) {
        if (uri == null) return@withContext null
        return@withContext try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inSampleSize = 2 }
            BitmapFactory.decodeStream(inputStream, null, options)
        } catch (e: Exception) { null }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PLAY_PAUSE" -> MusicPlayer.toggle(this)
            "NEXT" -> MusicPlayer.next(this)
            "PREV" -> MusicPlayer.prev(this)
            "STOP" -> stopService()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channelId = "music_channel_v2"
        val manager = getSystemService(NotificationManager::class.java)
        if (manager != null) {
            val channel = NotificationChannel(channelId, "MedTune Player", NotificationManager.IMPORTANCE_LOW)
            channel.setShowBadge(false)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createPlaceholderNotification(): Notification {
        return NotificationCompat.Builder(this, "music_channel_v2")
            .setContentTitle("MedTune")
            .setContentText("Initializing...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val title = PlayerState.currentTitle.value
        if (title == "No Song") return

        val isPlaying = PlayerState.isPlaying.value
        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val pPlayPause = PendingIntent.getService(this, 0, Intent(this, MusicService::class.java).apply { action = "PLAY_PAUSE" }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val pNext = PendingIntent.getService(this, 1, Intent(this, MusicService::class.java).apply { action = "NEXT" }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val pPrev = PendingIntent.getService(this, 2, Intent(this, MusicService::class.java).apply { action = "PREV" }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val pOpen = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, "music_channel_v2")
            .setContentTitle(title)
            .setContentText(PlayerState.currentArtist.value)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(cachedBitmap)
            .setContentIntent(pOpen)
            .setColor(PlayerState.accentColor.value.toArgb())
            .setColorized(true)
            .addAction(android.R.drawable.ic_media_previous, "Prev", pPrev)
            .addAction(playPauseIcon, "Play/Pause", pPlayPause)
            .addAction(android.R.drawable.ic_media_next, "Next", pNext)
            .setStyle(MediaStyle().setMediaSession(mediaSession?.sessionToken).setShowActionsInCompactView(0, 1, 2))
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaSession?.release()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
