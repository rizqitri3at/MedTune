package com.rizqitriantomi.medtune

import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject

object Persistence {
    private const val PREFS_NAME = "medtune_prefs_v2"

    fun savePlayerState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("theme_mode", PlayerState.themeMode.value)
            putString("gradient_preset", PlayerState.gradientPreset.value)
            putInt("accent_color", PlayerState.accentColor.value.toArgb())
            putInt("sub_accent_color", PlayerState.subAccentColor.value.toArgb())
            putBoolean("adaptive_accent", PlayerState.adaptiveAccent.value)
            
            putString("custom_bg_uri", PlayerState.customBgUri.value)
            putFloat("bg_blur", PlayerState.bgBlur.value)
            putFloat("bg_opacity", PlayerState.bgOpacity.value)
            putFloat("bg_dim", PlayerState.bgDim.value)
            
            putString("language", PlayerState.language.value)
            putLong("min_duration", PlayerState.minDuration.value)
            putStringSet("included_folders", PlayerState.includedFolders.value)
            putStringSet("excluded_folders", PlayerState.excludedFolders.value)
            
            putString("albums_view_mode", PlayerState.albumsViewMode.value)
            putString("artists_view_mode", PlayerState.artistsViewMode.value)
            putString("library_view_mode", PlayerState.libraryViewMode.value)
            putString("grid_density", PlayerState.gridDensity.value)
            
            putString("albums_sort_order", PlayerState.albumsSortOrder.value)
            putString("artists_sort_order", PlayerState.artistsSortOrder.value)
            putString("songs_sort_order", PlayerState.songsSortOrder.value)
            putInt("last_selected_tab", PlayerState.lastSelectedTab.value)
            
            putFloat("playback_speed", PlayerState.playbackSpeed.value)
            putBoolean("is_shuffle", PlayerState.isShuffle.value)
            putBoolean("is_repeat", PlayerState.isRepeat.value)
            putBoolean("was_playing", PlayerState.isPlaying.value)
            putLong("last_position", PlayerState.currentPosition.value)
            putString("last_song_uri", PlayerState.currentUri.value?.toString())
            putStringSet("last_queue", PlayerState.lastQueue.value.toSet())
            putBoolean("remember_position", PlayerState.rememberPosition.value)
            
            apply()
        }
    }

    fun loadPlayerState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        PlayerState.themeMode.value = prefs.getString("theme_mode", "DARK") ?: "DARK"
        PlayerState.gradientPreset.value = prefs.getString("gradient_preset", "Dynamic Auto") ?: "Dynamic Auto"
        PlayerState.accentColor.value = Color(prefs.getInt("accent_color", Color(0xFF1DB954).toArgb()))
        PlayerState.subAccentColor.value = Color(prefs.getInt("sub_accent_color", PlayerState.accentColor.value.copy(alpha = 0.5f).toArgb()))
        PlayerState.adaptiveAccent.value = prefs.getBoolean("adaptive_accent", true)
        
        PlayerState.customBgUri.value = prefs.getString("custom_bg_uri", null)
        PlayerState.bgBlur.value = prefs.getFloat("bg_blur", 10f)
        PlayerState.bgOpacity.value = prefs.getFloat("bg_opacity", 0.5f)
        PlayerState.bgDim.value = prefs.getFloat("bg_dim", 0.4f)
        
        PlayerState.language.value = prefs.getString("language", "ID") ?: "ID"
        PlayerState.minDuration.value = prefs.getLong("min_duration", 30000L)
        PlayerState.includedFolders.value = prefs.getStringSet("included_folders", emptySet()) ?: emptySet()
        PlayerState.excludedFolders.value = prefs.getStringSet("excluded_folders", emptySet()) ?: emptySet()
        
        PlayerState.albumsViewMode.value = prefs.getString("albums_view_mode", "GRID") ?: "GRID"
        PlayerState.artistsViewMode.value = prefs.getString("artists_view_mode", "LIST") ?: "LIST"
        PlayerState.libraryViewMode.value = prefs.getString("library_view_mode", "LIST") ?: "LIST"
        PlayerState.gridDensity.value = prefs.getString("grid_density", "Comfortable") ?: "Comfortable"
        
        PlayerState.albumsSortOrder.value = prefs.getString("albums_sort_order", "A -> Z") ?: "A -> Z"
        PlayerState.artistsSortOrder.value = prefs.getString("artists_sort_order", "A -> Z") ?: "A -> Z"
        PlayerState.songsSortOrder.value = prefs.getString("songs_sort_order", "A -> Z") ?: "A -> Z"
        PlayerState.lastSelectedTab.value = prefs.getInt("last_selected_tab", 1)
        
        PlayerState.playbackSpeed.value = prefs.getFloat("playback_speed", 1.0f)
        PlayerState.isShuffle.value = prefs.getBoolean("is_shuffle", false)
        PlayerState.isRepeat.value = prefs.getBoolean("is_repeat", false)
        PlayerState.wasPlaying.value = prefs.getBoolean("was_playing", false)
        PlayerState.currentPosition.value = prefs.getLong("last_position", 0L)
        PlayerState.rememberPosition.value = prefs.getBoolean("remember_position", true)
        
        val lastSongUriStr = prefs.getString("last_song_uri", null)
        PlayerState.currentUri.value = if (lastSongUriStr != null) Uri.parse(lastSongUriStr) else null
        PlayerState.lastQueue.value = (prefs.getStringSet("last_queue", emptySet()) ?: emptySet()).toList()
    }
}
