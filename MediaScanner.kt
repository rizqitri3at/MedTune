package com.rizqitriantomi.medtune

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaScanner {
    private var cachedSongs: List<SongData>? = null

    suspend fun getSongs(context: Context, forceRefresh: Boolean = false): List<SongData> = withContext(Dispatchers.IO) {
        if (!forceRefresh && cachedSongs != null) return@withContext cachedSongs!!

        val list = mutableListOf<SongData>()
        val projection = arrayOf(
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.COMPOSER
        )

        // If specific folders are included via SAF, we might want to filter by paths
        // However, MediaStore is global. We filter the results based on the allowed SAF tree paths.
        val allowedPaths = PlayerState.includedFolders.value.mapNotNull { uriString ->
            val uri = Uri.parse(uriString)
            DocumentFile.fromTreeUri(context, uri)?.let { getFullPathFromTreeUri(it) }
        }

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null
        )?.use { cursor ->
            val t = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val a = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val alb = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val d = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val dur = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val date = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albName = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
            val compCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.COMPOSER)

            while (cursor.moveToNext()) {
                val filePath = cursor.getString(d) ?: ""
                
        // SAF Filtering Logic: Only include if in allowedPaths (if any allowedPaths exist)
                val isPathAllowed = if (allowedPaths.isEmpty()) {
                    true
                } else {
                    allowedPaths.any { filePath.contains(it) }
                }

                if (!isPathAllowed) continue
                
                // Exclude Logic
                if (PlayerState.excludedFolders.value.isNotEmpty() && PlayerState.excludedFolders.value.any { filePath.contains(it) }) continue

                val song = SongData(
                    title = cursor.getString(t) ?: "Unknown",
                    artist = cursor.getString(a) ?: "Unknown",
                    uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(id)),
                    albumId = cursor.getLong(alb),
                    data = filePath,
                    dateAdded = cursor.getLong(date),
                    duration = cursor.getLong(dur),
                    album = cursor.getString(albName) ?: "Unknown",
                    year = cursor.getString(yearCol) ?: "Unknown",
                    composer = cursor.getString(compCol) ?: "Unknown"
                )
                list.add(song)
            }
        }
        cachedSongs = list
        list
    }

    private fun getFullPathFromTreeUri(documentFile: DocumentFile): String? {
        // This is a simplified helper; in real scoped storage, 
        // we usually match by relative path or use DocumentFile directly.
        // For MediaStore compatibility, we'll try to extract path segments.
        return documentFile.uri.path?.split(":")?.lastOrNull()
    }
}
