package com.royal.tenure.age.gold.girlfriend.MediaController

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import com.royal.tenure.age.gold.girlfriend.Constants
import com.royal.tenure.age.gold.girlfriend.R

class MyControllerCallback(private val context: Context) : MediaControllerCompat.Callback(){
    override fun onMetadataChanged(metadata: MediaMetadataCompat?) {

        Log.e(Constants.TAG, "Metadata changed to: " + metadata!!.getString(MediaMetadataCompat.METADATA_KEY_TITLE))

        // Todo: showcase metadata
        // Take the metadata from the session and showcase it

        val mContext = context as MainActivity
        var imageView : ImageView = mContext.findViewById<ImageView>(R.id.image)
        var textView : TextView = mContext.findViewById<TextView>(R.id.text_and_info)

        if(metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) != null){
            val title : String = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
            val bitmap : Bitmap = metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)
            imageView.setImageBitmap(bitmap)
            textView.setText(title)
        }

        super.onMetadataChanged(metadata)
    }

    override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
        Log.e(Constants.TAG, "PlaybackState changed to: " + state)


        // Todo: create PlaybackStateCompat
        // there is currently no PlaybackStateCompat!

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