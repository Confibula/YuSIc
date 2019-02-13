package com.royal.tenure.age.gold.girlfriend.MediaSession

import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.royal.tenure.age.gold.girlfriend.Constants

class MyPlaybackController: MediaSessionConnector.PlaybackController{
    override fun onRewind(player: Player?) = Unit

    override fun onSeekTo(player: Player?, position: Long) = Unit

    override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) = Unit

    override fun onPause(player: Player?) {
        player!!.setPlayWhenReady(false)
    }

    override fun onFastForward(player: Player?) = Unit

    override fun onPlay(player: Player?) {
        player!!.setPlayWhenReady(true)
    }

    override fun onStop(player: Player?) {
    }

    override fun onSetShuffleMode(player: Player?, shuffleMode: Int) = Unit

    override fun getSupportedPlaybackActions(player: Player?): Long {
        return  PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE
    }

    override fun getCommands(): Array<String>? = null

    override fun onSetRepeatMode(player: Player?, repeatMode: Int) = Unit

}