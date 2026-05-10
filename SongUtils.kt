package com.rizqitriantomi.medtune

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

fun fetchSongs(context: Context): List<SongData> {
    val songs = mutableListOf<SongData>()
    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DURATION
    )

    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

        while (cursor.moveToNext()) {
            val duration = cursor.getLong(durationCol)
            val data = cursor.getString(dataCol)
            
            // Apply filters from PlayerState
            if (duration < PlayerState.minDuration.value) continue
            if (PlayerState.excludedFolders.value.any { data.contains(it) }) continue

            val title = cursor.getString(titleCol)
            val artist = cursor.getString(artistCol)
            val id = cursor.getLong(idCol)
            val albumId = cursor.getLong(albumIdCol)
            val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

            songs.add(SongData(title, artist, contentUri, albumId, data))
        }
    }
    return songs
}
