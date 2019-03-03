package com.royal.tenure.age.gold.girlfriend

import android.content.Context
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
    private val _streams = MutableLiveData<List<MediaBrowserCompat.MediaItem>>()
        .apply { postValue(emptyList()) }

    val streams: LiveData<List<MediaBrowserCompat.MediaItem>> = _streams

    fun fetch() : LiveData<List<MediaBrowserCompat.MediaItem>> {
        return streams
    }

    fun putStreams(streams: MutableList<MediaBrowserCompat.MediaItem>){
        this._streams.postValue(streams)

        Log.e(Commons.TAG, "ran putStreams: " + streams.size)
    }
}