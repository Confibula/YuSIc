package com.royal.tenure.age.gold.girlfriend

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.MediaController
import com.google.firebase.auth.FirebaseAuth

class Commons{
    companion object {
        const val TAG : String = "TAG"
        const val APP = "YUSIC"
        const val SIGN_IN_REQUEST = 28657
        const val NOTIFICATION_ID = 10946
        const val NOTIFICATION_CHANNEL = "6765"
        const val ROOT_ID = "610"
    }
}

val PlaybackStateCompat.isPlaying
    get() = (state == PlaybackStateCompat.STATE_PLAYING) ||
            (state == PlaybackStateCompat.STATE_BUFFERING)

val PlaybackStateCompat.isPlayEnabled
    get() = (actions and PlaybackStateCompat.ACTION_PLAY != 0L) ||
            ((actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L) &&
                    (state == PlaybackStateCompat.STATE_PAUSED))

val MediaMetadataCompat.id
    get() = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)

val MediaMetadataCompat.genre
    get() = getString(MediaMetadataCompat.METADATA_KEY_GENRE)

val MediaMetadataCompat.mediaUri
    get() = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI)

val MediaMetadataCompat.artist
    get() = getString(MediaMetadataCompat.METADATA_KEY_ARTIST)

val MediaMetadataCompat.title
    get() = getString(MediaMetadataCompat.METADATA_KEY_TITLE)

val auth: FirebaseAuth = FirebaseAuth.getInstance()

