package com.royal.tenure.age.gold.girlfriend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

public class BecomingNoisyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            // Pause the playback
        }
    }
}