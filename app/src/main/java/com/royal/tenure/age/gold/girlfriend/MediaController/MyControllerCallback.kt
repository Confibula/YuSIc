package com.royal.tenure.age.gold.girlfriend.MediaController

import android.app.Activity
import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.TextView
import com.royal.tenure.age.gold.girlfriend.Constants

class MyControllerCallback(private val context: Context) : MediaControllerCompat.Callback(){
    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {

        // Todo: showcase metadata
        // Take the metadata from the session and showcase it!

        super.onMetadataChanged(metadata)
    }

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
        Log.e(Constants.TAG, "playback state changed in the session to: " + state)

        // Just a tiny bit questionable since its duplicate code
        when(state!!.playbackState){
            PlaybackStateCompat.STATE_PAUSED -> null

            PlaybackStateCompat.STATE_PLAYING -> null

            else -> null
        }

        super.onPlaybackStateChanged(state)
    }

    override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
        super.onQueueChanged(queue)
    }
}