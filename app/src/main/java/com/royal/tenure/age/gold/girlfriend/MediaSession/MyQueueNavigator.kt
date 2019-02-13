package com.royal.tenure.age.gold.girlfriend.MediaSession

import android.os.Bundle
import android.os.ResultReceiver
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector

class MyQueueNavigator : MediaSessionConnector.QueueNavigator{
    override fun onSkipToQueueItem(player: Player?, id: Long) = Unit

    override fun onCurrentWindowIndexChanged(player: Player?) = Unit

    override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) = Unit

    override fun getSupportedQueueNavigatorActions(player: Player?): Long {
        return 1
    }

    override fun onSkipToNext(player: Player?) = Unit

    override fun getActiveQueueItemId(player: Player?): Long {
        return 1
    }

    override fun onSkipToPrevious(player: Player?) = Unit

    override fun getCommands(): Array<String>? = null

    override fun onTimelineChanged(player: Player?) = Unit

}