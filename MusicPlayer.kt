package com.rizqitriantomi.medtune

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log

object MusicPlayer {
    private var mediaPlayer: MediaPlayer? = null
    var index = 0
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressAction = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    PlayerState.currentPosition.value = it.currentPosition.toLong()
                    PlayerState.duration.value = it.duration.toLong()
                    handler.postDelayed(this, 1000)
                } else {
                    handler.postDelayed(this, 2000)
                }
            } ?: handler.postDelayed(this, 2000)
        }
    }

    init {
        handler.post(updateProgressAction)
    }

    private fun updateLastQueue() {
        PlayerState.lastQueue.value = playlist.map { it.uri.toString() }
    }

    var playlist: List<SongData>
        get() = PlayerState.currentPlaylist.value
        set(value) {
            PlayerState.currentPlaylist.value = value
            updateLastQueue()
        }

    fun play(context: Context, song: SongData, startPosition: Long = 0L, autoPlay: Boolean = true) {
        try {
            Log.d("MusicPlayer", "Play request: ${song.title}")
            
            if (playlist.isEmpty()) {
                playlist = listOf(song)
                index = 0
            } else {
                val newIndex = playlist.indexOf(song)
                if (newIndex != -1) {
                    index = newIndex
                } else {
                    playlist = playlist + song
                    index = playlist.size - 1
                }
            }

            if (autoPlay) {
                context.startForegroundService(Intent(context, MusicService::class.java))
            }
            
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, song.uri)
                setOnPreparedListener {
                    if (android.os.Build.VERSION.SDK_INT >= 23) {
                        try {
                            it.playbackParams = it.playbackParams.setSpeed(PlayerState.playbackSpeed.value)
                        } catch (e: Exception) { Log.e("MusicPlayer", "Params error", e) }
                    }
                    if (startPosition > 0) it.seekTo(startPosition.toInt())
                    if (autoPlay) {
                        it.start()
                        PlayerState.isPlaying.value = true
                    } else {
                        PlayerState.isPlaying.value = false
                    }
                    PlayerState.duration.value = it.duration.toLong()
                }
                setOnCompletionListener {
                    if (PlayerState.isRepeat.value) {
                        play(context, song)
                    } else {
                        next(context)
                    }
                }
                setOnErrorListener { _, what, extra -> 
                    Log.e("MusicPlayer", "MediaPlayer Error: $what, $extra")
                    stop()
                    true 
                }
                prepareAsync()
            }
            
            PlayerState.currentTitle.value = song.title
            PlayerState.currentArtist.value = song.artist
            PlayerState.currentUri.value = song.uri
            PlayerState.currentData.value = song.data
            PlayerState.albumArtUri.value = Uri.parse("content://media/external/audio/albumart/${song.albumId}")
            
            PlayerState.currentLyrics.value = LyricParser.loadLyrics(context, song.data, song.uri)
            Persistence.savePlayerState(context)
        } catch (e: Exception) {
            Log.e("MusicPlayer", "Error playing song", e)
        }
    }

    suspend fun restoreSession(context: Context) {
        val lastUri = PlayerState.currentUri.value ?: return
        val lastQueueUris = PlayerState.lastQueue.value
        if (lastQueueUris.isEmpty()) return

        val songs = MediaScanner.getSongs(context)
        val sessionQueue = lastQueueUris.mapNotNull { uriStr ->
            songs.find { it.uri.toString() == uriStr }
        }
        
        if (sessionQueue.isNotEmpty()) {
            playlist = sessionQueue
            val currentSong = sessionQueue.find { it.uri == lastUri } ?: sessionQueue.first()
            // Restore state but don't play. Start service to show notification.
            play(context, currentSong, startPosition = PlayerState.currentPosition.value, autoPlay = false)
            updateService(context)
        }
    }

    fun startSleepTimer(minutes: Int, fadeOut: Boolean) {
        PlayerState.sleepTimerActive.value = true
        PlayerState.sleepTimerMinutes.value = minutes
        PlayerState.fadeOutBeforeStop.value = fadeOut
        PlayerState.sleepTimerRemaining.value = minutes * 60 * 1000L
        
        handler.removeCallbacks(sleepTimerRunnable)
        handler.postDelayed(sleepTimerRunnable, 1000)
    }

    private val sleepTimerRunnable = object : Runnable {
        override fun run() {
            if (!PlayerState.sleepTimerActive.value) return
            
            PlayerState.sleepTimerRemaining.value -= 1000
            if (PlayerState.sleepTimerRemaining.value <= 0) {
                stop()
                PlayerState.sleepTimerActive.value = false
            } else {
                // Fade out logic
                if (PlayerState.fadeOutBeforeStop.value && PlayerState.sleepTimerRemaining.value < 30000) {
                    val volume = PlayerState.sleepTimerRemaining.value / 30000f
                    mediaPlayer?.setVolume(volume, volume)
                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    fun toggle(context: Context) {
        if (mediaPlayer == null && PlayerState.currentUri.value != null) {
            val lastUri = PlayerState.currentUri.value ?: return
            val song = SongData(
                PlayerState.currentTitle.value,
                PlayerState.currentArtist.value,
                lastUri,
                PlayerState.albumArtUri.value?.lastPathSegment?.toLongOrNull() ?: -1L,
                PlayerState.currentData.value ?: ""
            )
            play(context, song)
            return
        }
        
        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.pause()
                    PlayerState.isPlaying.value = false
                } else {
                    it.start()
                    PlayerState.isPlaying.value = true
                }
                updateService(context)
            } catch (e: Exception) {
                Log.e("MusicPlayer", "Toggle error", e)
            }
        }
    }

    private fun updateService(context: Context) {
        val intent = Intent(context, MusicService::class.java)
        context.startForegroundService(intent)
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        PlayerState.isPlaying.value = false
    }

    fun next(context: Context) {
        val currentPlaylist = playlist
        if (currentPlaylist.isEmpty()) return
        
        index = if (PlayerState.isShuffle.value) {
            (currentPlaylist.indices).random()
        } else {
            (index + 1) % currentPlaylist.size
        }
        
        if (index in currentPlaylist.indices) {
            play(context, currentPlaylist[index])
        }
    }

    fun prev(context: Context) {
        val currentPlaylist = playlist
        if (currentPlaylist.isEmpty()) return
        index = if (index <= 0) currentPlaylist.lastIndex else index - 1
        if (index in currentPlaylist.indices) {
            play(context, currentPlaylist[index])
        }
    }

    fun seekTo(pos: Long) {
        mediaPlayer?.seekTo(pos.toInt())
        PlayerState.currentPosition.value = pos
    }

    fun toggleShuffle() { PlayerState.isShuffle.value = !PlayerState.isShuffle.value }
    fun toggleRepeat() { PlayerState.isRepeat.value = !PlayerState.isRepeat.value }

    fun setPlaybackSpeed(speed: Float) {
        PlayerState.playbackSpeed.value = speed
        mediaPlayer?.let {
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                try {
                    val wasPlaying = it.isPlaying
                    it.playbackParams = it.playbackParams.setSpeed(speed)
                    if (!wasPlaying) it.pause()
                } catch (e: Exception) {}
            }
        }
    }
}
