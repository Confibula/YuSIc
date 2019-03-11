package com.royal.tenure.age.gold.girlfriend

import android.app.Activity
import android.content.Context
import android.drm.DrmStore
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide.init
import com.google.android.gms.common.internal.service.Common

class StreamModel : ViewModel() {

    private val _streams = MutableLiveData<List<Stream>>()
        .apply { postValue(emptyList()) }
    val streams: LiveData<List<Stream>> = _streams

    val playbackState = MutableLiveData<PlaybackStateCompat>().apply {
        this.postValue(PlaybackStateCompat.Builder().build())
    }
    val controller = MutableLiveData<MediaControllerCompat>()

    private val _nowPlaying = MutableLiveData<MediaMetadataCompat>().apply {
        this.postValue(MediaMetadataCompat.Builder().build())
    }
    val nowPlaying : LiveData<MediaMetadataCompat> = _nowPlaying

    val playbutton_res = MutableLiveData<Int>()
        .apply { postValue(R.drawable.exo_notification_play) }

    fun putStreams(streams: MutableList<MediaBrowserCompat.MediaItem>){
        val list = streams.map { stream ->
            Log.e(Commons.TAG, "stream: " + stream.description.title)

            Stream(stream.mediaId!!, getStreamColor(stream.mediaId!!))
        }

        this._streams.postValue(list)

        Log.e(Commons.TAG, "ran putStreams: " + streams.size)
    }

    fun putController(controller : MediaControllerCompat){
        this.controller.postValue(controller)
    }

    fun putMetadata(metadata: MediaMetadataCompat){
        _nowPlaying.postValue(metadata)

        // Todo: bitmap for the image!
        // Receive the bitmap for the metadata
    }

    fun putPlayback(playback: PlaybackStateCompat){
        playbackState.postValue(playback)

        if(playback.isPlaying) playbutton_res.postValue(R.drawable.exo_notification_pause)
        else if(playback.isPlayEnabled) playbutton_res.postValue(R.drawable.exo_notification_play)
    }

    fun getStreamColor(stream: String) : Int {
        if(nowPlaying.value!!.genre == stream && playbackState.value!!.isPlaying) {
            Log.e(Commons.TAG, "gave the stream object dark color")

            return Stream.PLAYING_COLOR
        }
        else {
            Log.e(Commons.TAG, "gave the stream object accented color")

            return R.color.colorPrimary
        }
    }

    fun play(stream: Stream){

        if (stream.mediaId == nowPlaying.value!!.genre) {
            when {
                playbackState.value!!.isPlaying -> {
                    controller.value!!.transportControls.pause()
                    Log.e(Commons.TAG, "paused playback")
                }
                playbackState.value!!.isPlayEnabled -> {
                    controller.value!!.transportControls.play()
                    Log.e(Commons.TAG, "played playback")
                }
                else -> {
                    Log.w(Commons.TAG,
                        "Playable item clicked but neither play nor pause are enabled!"
                                + " (mediaId=${stream.mediaId})") }
            }
        } else {
            controller.value!!.transportControls.playFromMediaId(stream.mediaId, null)
            Log.e(Commons.TAG, "played playback anew")
        }
    }
}
