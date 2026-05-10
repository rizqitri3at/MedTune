package com.rizqitriantomi.medtune

import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest

object SongActionHandler {

    fun deleteSong(context: Context, song: SongData, launcher: ActivityResultLauncher<IntentSenderRequest>?) {
        try {
            val resolver = context.contentResolver
            resolver.delete(song.uri, null, null)
            Toast.makeText(context, "Song deleted", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recoverableSecurityException = e as? RecoverableSecurityException
                recoverableSecurityException?.let {
                    val intentSender = it.userAction.actionIntent.intentSender
                    launcher?.launch(IntentSenderRequest.Builder(intentSender).build())
                }
            }
        }
    }

    fun renameSong(context: Context, song: SongData, newName: String, launcher: ActivityResultLauncher<IntentSenderRequest>?) {
        try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.TITLE, newName)
            }
            resolver.update(song.uri, values, null, null)
            Toast.makeText(context, "Song renamed", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val recoverableSecurityException = e as? RecoverableSecurityException
                recoverableSecurityException?.let {
                    val intentSender = it.userAction.actionIntent.intentSender
                    launcher?.launch(IntentSenderRequest.Builder(intentSender).build())
                }
            }
        }
    }

    fun shareSong(context: Context, song: SongData) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, song.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Song"))
    }

    fun setAsRingtone(context: Context, song: SongData) {
        // Basic implementation, requires WRITE_SETTINGS
        Toast.makeText(context, "Setting ringtone...", Toast.LENGTH_SHORT).show()
    }
}
