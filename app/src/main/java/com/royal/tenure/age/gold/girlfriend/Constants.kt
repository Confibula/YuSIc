package com.royal.tenure.age.gold.girlfriend

import android.support.v4.media.session.PlaybackStateCompat
import com.google.firebase.firestore.FirebaseFirestore

public class Constants{
    companion object {
        const val TAG : String = "my_tag"
        const val APP = "YUSIC"
        const val RC_SIGN_IN = 28657
        const val NOTIFICATION_ID = 10946
        const val NOTIFICATION_CHANNEL = "6765"
        const val POSITION = 17711
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