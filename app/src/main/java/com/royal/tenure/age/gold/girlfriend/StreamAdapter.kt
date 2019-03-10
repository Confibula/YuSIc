package com.royal.tenure.age.gold.girlfriend

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.service.voice.AlwaysOnHotwordDetector
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide.init
import com.royal.tenure.age.gold.girlfriend.Stream.Companion.PLAYING_COLOR
import java.util.zip.Inflater

class StreamAdapter(val streamClickListener: (Stream) -> Unit, val context: Context) : ListAdapter<Stream, StreamViewHolder>(Stream.diffCallback) {
    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        super.onBindViewHolder(holder, position, mutableListOf())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.stream, parent, false)

        return StreamViewHolder(view, streamClickListener, context)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: StreamViewHolder, position: Int, payloads: MutableList<Any>) {
        val stream = getItem(position)

        holder.stream = stream
        holder.title.text = stream.mediaId.toUpperCase()
        holder.title.setTextColor(ContextCompat.getColor(context, stream.color))

        Log.e(Commons.TAG, "ran onBindViewHolder: " + stream.mediaId)

        payloads.forEach { payload ->
            when (payload) {
                PLAYING_COLOR -> {

                    // Todo: COLOR CODING for the stream !
                    // Make the color change when playback changes
                    Log.e(Commons.TAG, "changed the color")

                    holder.title.setTextColor(ContextCompat.getColor(context, PLAYING_COLOR))
                }
                // If the payload wasn't understood, refresh the full item (to be safe).
                else -> {
                    Log.e(Commons.TAG, "payload was not understood ?")
                }
            }
        }
    }
}

class StreamViewHolder(view: View, streamClickListener: (Stream) -> Unit, context: Context)
    : androidx.recyclerview.widget.RecyclerView.ViewHolder(view){

    val title: TextView = view.findViewById(R.id.text_view_stream)
    var stream : Stream? = null

    init {
        view.setOnClickListener {
            stream?.let {
                streamClickListener(it)
            }
        }
    }

}