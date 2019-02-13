package com.royal.tenure.age.gold.girlfriend.MediaSession

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.util.Log
import com.royal.tenure.age.gold.girlfriend.Constants

class MyEventListener(private val exoPlayer: ExoPlayer,
                      private val mediaSession: MediaSessionCompat): Player.EventListener{
    override fun onLoadingChanged(isLoading: Boolean) {
        Log.e(Constants.TAG, "Loading changed: " + isLoading)
    }

    override fun onPositionDiscontinuity(reason: Int) {
        Log.e(Constants.TAG, "Reached position discontinuity: " + reason)

    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        if(playbackState == Player.STATE_ENDED){
            var id = mediaSession.controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
            var nextId = Integer.parseInt(id) + 1
            var nextIdAsString = Integer.toString(nextId)
            var finalId = nextIdAsString

            mediaSession.controller.transportControls.playFromMediaId(finalId, null)
        }
    }
}