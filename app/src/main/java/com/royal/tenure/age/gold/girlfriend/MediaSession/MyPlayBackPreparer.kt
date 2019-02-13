package com.royal.tenure.age.gold.girlfriend.MediaSession

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
                         private val exoPlayer: ExoPlayer) : MediaSessionConnector.PlaybackPreparer, MyListener {

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
        mediaSession.setMetadata(mMediaMetadata)

        Log.e(Constants.TAG, "state changed to true, and now the media can be prepared")
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

                val bitmap = GetBitmap().execute(value.get("image") as String).get()
                val source = value.get("source") as String
                val creator = value.get("creator") as String
                val title = value.get("title") as String

                mMediaMetadata =
                    MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, source)
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                        .putString(MediaMetadataCompat.METADATA_KEY_COMPOSER, creator)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                        .build()

                caller.notifySomethingHappened()
            }
    }

    override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {

        // Todo: slow response
        // right now the app runs slowly when mediaItem is fetched since there is so much load!
        // find a way to reduce to load, or load more intelligently

        // Todo: efficiency
        // Creating the MediaMetadata could be run in the onCreate instead. This would make your app run faster.
        // Its shouldn't need to be created here, when the user wants to play the media. It can be created earlier.
        // Read more about custom callbacks!

        mMediaId = mediaId!!
        mBundle = extras!!
        createMediaMetadata(mediaId!!)

        Log.e(Constants.TAG, "exoplayer's state is: " + exoPlayer.playbackState.toString())
    }

    override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) = Unit


    override fun onPrepare() {

    }

}

interface MyListener {
    fun somethingHappened()
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
}