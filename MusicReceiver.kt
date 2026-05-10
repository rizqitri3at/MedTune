package com.rizqitriantomi.medtune

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MusicReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "PLAY" -> MusicPlayer.toggle(context)
            "NEXT" -> MusicPlayer.next(context)
            "PREV" -> MusicPlayer.prev(context)
        }
    }
}