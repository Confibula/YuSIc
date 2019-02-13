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

    // Todo: Teach yourself about session Items!
    // Now that probably all the different callback are established, teach yourself about how
    // to use items with your session. Items like QueueItem and MediaItem

    // Todo: Find out how to play the next song in the stream!
    // This is very related to the task just above

    override fun onSkipToNext(player: Player?) = Unit

    override fun getActiveQueueItemId(player: Player?): Long {
        return 1
    }

    override fun onSkipToPrevious(player: Player?) = Unit

    override fun getCommands(): Array<String>? = null

    override fun onTimelineChanged(player: Player?) = Unit

}