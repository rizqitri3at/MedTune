package com.rizqitriantomi.medtune

import android.net.Uri

data class SongData(
    val title: String,
    val artist: String,
    val uri: Uri,
    val albumId: Long,
    val data: String,
    val dateAdded: Long = 0,
    val duration: Long = 0,
    val album: String = "Unknown",
    val year: String = "Unknown",
    val composer: String = "Unknown"
)
