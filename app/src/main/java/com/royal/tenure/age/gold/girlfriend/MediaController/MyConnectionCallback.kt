package com.royal.tenure.age.gold.girlfriend.MediaController

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.royal.tenure.age.gold.girlfriend.MediaController.MainActivity
import com.royal.tenure.age.gold.girlfriend.R

class MyConnectionCallback(private val context: Context,
                           private val myControllerCallback: MyControllerCallback
) : MediaBrowserCompat.ConnectionCallback(){

    lateinit var playPause : ImageView
    lateinit var image : ImageView
    lateinit var text : TextView

    override fun onConnected() {
        MainActivity.mMediaBrowserCompat.sessionToken.also { token ->
            val mMediaController = MediaControllerCompat(
                context, token
            )

            //set it for the activity for later retrieval
            MediaControllerCompat.setMediaController(context as MainActivity, mMediaController)

            // Todo: remember where user left
            // In the bundle, write song position. And in the id, write users specific ID
            // This is a task that will make it so that the playback recognizes where the user last left
            val bundle: Bundle = Bundle().also {
                it.putLong("position", 0)
            }
            mMediaController.transportControls.prepareFromMediaId("1", bundle)

            buildPlayPause()
            val mediaController = MediaControllerCompat.getMediaController(context as MainActivity)
            mediaController.registerCallback(myControllerCallback)
        }
    }

    override fun onConnectionFailed() {
        super.onConnectionFailed()
    }

    override fun onConnectionSuspended() {
        super.onConnectionSuspended()
    }

    private fun buildPlayPause(){
        val mediaController = MediaControllerCompat.getMediaController(context as MainActivity)
        playPause = context.findViewById<ImageView>(R.id.play_pause).apply {

            setOnClickListener {
                var pbState = mediaController.playbackState.state
                when(pbState){
                    PlaybackStateCompat.STATE_PLAYING -> {
                        mediaController.transportControls.pause()
                        playPause.setImageDrawable(
                            ContextCompat.getDrawable(context,
                                R.drawable.exo_controls_play
                            )
                        )
                    }
                    PlaybackStateCompat.STATE_PAUSED -> {
                        mediaController.transportControls.play()
                        playPause.setImageDrawable(
                            ContextCompat.getDrawable(context,
                                R.drawable.exo_controls_pause
                            )
                        )
                    }
                    else -> { } } } }.also { it.setVisibility(View.INVISIBLE) }
    }
}