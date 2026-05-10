package com.rizqitriantomi.medtune

import android.content.Context
import android.net.Uri
import java.io.File

data class LyricLine(val time: Long, val text: String)

object LyricParser {
    fun parse(lrc: String?): List<LyricLine> {
        if (lrc.isNullOrBlank()) return emptyList()
        val regex = "\\[(\\d+):(\\d+\\.\\d+)](.*)".toRegex()
        return lrc.lines().mapNotNull { line ->
            val match = regex.find(line) ?: return@mapNotNull null
            val (min, sec, text) = match.destructured
            val time = min.toLong() * 60000 + (sec.toFloat() * 1000).toLong()
            LyricLine(time, text.trim())
        }.sortedBy { it.time }
    }

    fun loadLyrics(context: Context, filePath: String, uri: Uri): String? {
        // 1. Try .lrc file
        try {
            val lrcFile = File(filePath.substringBeforeLast(".") + ".lrc")
            if (lrcFile.exists()) {
                return lrcFile.readText()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Try embedded lyrics via MediaMetadataRetriever (if supported by system)
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            // Some devices/versions use key 1000 for lyrics
            val lyrics = retriever.extractMetadata(1000)
            retriever.release()
            if (!lyrics.isNullOrBlank()) return lyrics
        } catch (e: Exception) {
            // ignore
        }

        return null
    }
}
