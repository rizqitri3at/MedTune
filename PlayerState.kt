package com.rizqitriantomi.medtune

import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

object PlayerState {
    val currentTitle = mutableStateOf("No Song")
    val currentArtist = mutableStateOf("Unknown")
    val currentUri = mutableStateOf<Uri?>(null)
    val albumArtUri = mutableStateOf<Uri?>(null)
    val isPlaying = mutableStateOf(false)
    val isLyricsVisible = mutableStateOf(false)
    val dominantColor = mutableStateOf(Color.Black)
    val currentPosition = mutableStateOf(0L)
    val duration = mutableStateOf(0L)
    val playbackSpeed = mutableStateOf(1.0f)
    val isShuffle = mutableStateOf(false)
    val isRepeat = mutableStateOf(false)
    val wasPlaying = mutableStateOf(false)
    
    val playCounts = mutableStateOf<Map<String, Int>>(emptyMap())
    val currentPlaylist = mutableStateOf<List<SongData>>(emptyList())
    val lastQueue = mutableStateOf<List<String>>(emptyList()) // List of URIs
    val currentLyrics = mutableStateOf<String?>(null)
    val currentData = mutableStateOf<String?>(null)

    // Theme Engine
    val themeMode = mutableStateOf("DARK") // LIGHT, DARK, TRANSPARENT, ADAPTIVE, CUSTOM, EXPERIMENTAL
    val gradientPreset = mutableStateOf("Dynamic Auto")
    val accentColor = mutableStateOf(Color(0xFF1DB954))
    val subAccentColor = mutableStateOf(Color(0xFF1DB954).copy(alpha = 0.5f))
    val adaptiveAccent = mutableStateOf(true)
    
    // Custom Background
    val customBgUri = mutableStateOf<String?>(null)
    val bgBlur = mutableStateOf(10f)
    val bgOpacity = mutableStateOf(0.5f)
    val bgDim = mutableStateOf(0.4f)
    
    // App State
    val language = mutableStateOf("ID")
    val minDuration = mutableStateOf(30000L)
    val lastSelectedTab = mutableStateOf(1)
    val activeSubPage = mutableStateOf(-1) // -1: None, 0: Library, 1: Artists, 2: Albums, 3: Settings
    val rememberPosition = mutableStateOf(true)
    
    // Sleep Timer
    val sleepTimerActive = mutableStateOf(false)
    val sleepTimerMinutes = mutableStateOf(0)
    val sleepTimerRemaining = mutableStateOf(0L)
    val fadeOutBeforeStop = mutableStateOf(true)
    
    // Storage (SAF)
    val includedFolders = mutableStateOf(setOf<String>())
    val excludedFolders = mutableStateOf(setOf<String>())

    // View States (Persistable)
    val albumsViewMode = mutableStateOf("GRID")
    val artistsViewMode = mutableStateOf("LIST")
    val libraryViewMode = mutableStateOf("LIST")
    val gridDensity = mutableStateOf("Comfortable")
    val albumsGridColumns = mutableStateOf(2)
    
    val albumsSortOrder = mutableStateOf("A -> Z")
    val artistsSortOrder = mutableStateOf("A -> Z")
    val songsSortOrder = mutableStateOf("A -> Z")
    
    // Accessibility & Visuals
    val contrastSensitivity = mutableStateOf(0.5f)
    val glowIntensity = mutableStateOf(0.8f)
}
