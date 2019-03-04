package com.royal.tenure.age.gold.girlfriend

import android.content.Context
import android.drm.DrmStore
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.bumptech.glide.Glide.init

class StreamModel : ViewModel() {
    private val _streams = MutableLiveData<List<Stream>>()
        .apply { postValue(emptyList()) }

    val streams: LiveData<List<Stream>> = _streams

    val playbackState = MutableLiveData<PlaybackStateCompat>()
        .apply { postValue(
            PlaybackStateCompat.Builder().build()
        ) }

    val nowPlaying = MutableLiveData<MediaMetadataCompat>()
        .apply { postValue(
            MediaMetadataCompat.Builder().build()
        ) }

    fun putStreams(streams: MutableList<MediaBrowserCompat.MediaItem>){
        val list = streams.map { stream ->
            Stream(stream.mediaId as String, R.color.colorAccent)
        }

        this._streams.postValue(list)

        Log.e(Commons.TAG, "ran putStreams: " + streams.size)
    }

    fun putMetadata(metadata: MediaMetadataCompat){
        nowPlaying.postValue(metadata)


    }

    fun putPlayback(playback: PlaybackStateCompat){
        playbackState.postValue(playback)
    }

}