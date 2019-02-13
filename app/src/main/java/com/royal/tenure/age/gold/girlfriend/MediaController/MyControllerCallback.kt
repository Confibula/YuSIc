package com.royal.tenure.age.gold.girlfriend.MediaController

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.royal.tenure.age.gold.girlfriend.Constants
import com.royal.tenure.age.gold.girlfriend.R

class MyControllerCallback(private val context: Context) : MediaControllerCompat.Callback(){
    val mContext = context as MainActivity
    var imageView : ImageView = mContext.findViewById<ImageView>(R.id.image)
    var textView : TextView = mContext.findViewById<TextView>(R.id.text_and_info)
    var playPause : ImageView = mContext.findViewById<ImageView>(R.id.play_pause)

    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {

        if(metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) != null){
            val title : String = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            val creator: String = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
            val bitmap : Bitmap = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)

            imageView.setImageBitmap(bitmap)
            textView.setText(title + "\n" + creator)
            playPause.setVisibility(View.VISIBLE)
        }

        super.onMetadataChanged(metadata)
    }

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
        Log.e(Constants.TAG, "PlaybackState changed to: " + state?.state)

        when(state!!.state){
            PlaybackStateCompat.STATE_PAUSED -> {
                playPause.setImageDrawable(
                    ContextCompat.getDrawable(context,
                        R.drawable.exo_controls_play)) }
            PlaybackStateCompat.STATE_PLAYING -> {
                playPause.setImageDrawable(
                    ContextCompat.getDrawable(context,
                        R.drawable.exo_controls_pause)) }
            else -> null
        }

        super.onPlaybackStateChanged(state)
    }

    override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
        super.onQueueChanged(queue)
    }
}