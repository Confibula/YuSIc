package com.royal.tenure.age.gold.girlfriend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.support.v4.media.session.MediaControllerCompat
import com.google.android.exoplayer2.util.Log

class MyReceiver(private val controller: MediaControllerCompat) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.e(Commons.TAG, "ran onReceive in my BroadcastReceiver")

        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            controller.transportControls.pause()
        }

    }
}