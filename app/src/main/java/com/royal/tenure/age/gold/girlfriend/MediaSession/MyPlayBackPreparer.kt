package com.royal.tenure.age.gold.girlfriend.MediaSession

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.royal.tenure.age.gold.girlfriend.Constants
import com.royal.tenure.age.gold.girlfriend.GetBitmap
import java.io.IOException


class MyPlayBackPreparer(private val mediaSession: MediaSessionCompat,
                         private val dataFactory: DefaultDataSourceFactory,
                         private val exoPlayer: ExoPlayer,
                         private val context: Context) : MediaSessionConnector.PlaybackPreparer, MyListener {

    override fun onSuccess() {

    }

    var state : Boolean = false
    lateinit var mMediaId : String
    lateinit var mMediaMetadata : MediaMetadataCompat
    lateinit var mBundle: Bundle
    var caller : Caller
    val db = FirebaseFirestore.getInstance()

    init {
        this.caller = Caller()
        caller.registerListener(this)
    }

    override fun somethingHappened() {
        state = true

        val mediaUri: Uri = Uri.parse(mMediaMetadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI))
        val videoSource = ExtractorMediaSource.Factory(dataFactory)
            .createMediaSource(mediaUri)
        exoPlayer.prepare(videoSource)

        exoPlayer.seekTo(mBundle!!.getLong("position"))

        exoPlayer.addListener(object : Player.EventListener{
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                Log.e(Constants.TAG, "playbackState: " + playbackState)
                if(playbackState == Player.STATE_READY){
                    mediaSession.setMetadata(mMediaMetadata)
                }
            }
        })

    }

    override fun onPrepareFromSearch(query: String?, extras: Bundle?) = Unit

    override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) = Unit

    override fun getSupportedPrepareActions(): Long {
        return  PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
    }

    override fun getCommands(): Array<String>? = null

    fun createMediaMetadata(mediaId: String) {

        db.collection("songs")
            .document(mediaId!!)
            .get().addOnSuccessListener { document ->
                val value: Map<String, Any> = document.data!!

                val bitmap = GetBitmap(context).execute(value.get("image") as String).get()
                val source = value.get("source") as String
                val creator = value.get("creator") as String
                val title = value.get("title") as String

                mMediaMetadata =
                    MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, source)
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                        .putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, creator)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, mediaId)
                        .build()

                caller.notifySomethingHappened()
            }
    }

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {

        mMediaId = mediaId!!
        mBundle = extras!!
        createMediaMetadata(mediaId!!)
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) = Unit


    override fun onPrepare() {

    }

}

interface MyListener {
    fun somethingHappened()

    fun onSuccess()
}

class Caller {
    private val listeners = ArrayList<MyListener>()

    fun registerListener(listener: MyListener) {
        listeners.add(listener)
    }

    fun notifySomethingHappened() {
        for (listener in listeners) {
            listener.somethingHappened()
        }
    }

    fun notifyOnSuccess(){
        for (listener in listeners) {
            listener.onSuccess()
        }
    }
}