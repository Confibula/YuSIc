package com.royal.tenure.age.gold.girlfriend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.PlaybackState
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.util.Log

class BecomingNoisyReceiver(private val controller: MediaControllerCompat) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.e(Constants.TAG, "ran onReceive in my BroadcastReceiver")

        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            controller.transportControls.pause() }

        if (intent.action == "ACTION_PLAY"){
            controller.transportControls.play()
        }

        if (intent.action == "ACTION_PAUSE"){
            controller.transportControls.pause()
        }

        if (intent.action == "ACTION_SET_REPEAT_MODE"){

            if(controller.playbackState.state == PlaybackStateCompat.REPEAT_MODE_ONE){
                controller.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
            }

            if(controller.playbackState.state == PlaybackStateCompat.REPEAT_MODE_NONE){
                controller.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
            }

        }
    }
}