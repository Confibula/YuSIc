package com.royal.tenure.age.gold.girlfriend

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.service.voice.AlwaysOnHotwordDetector
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

class StreamAdapter(val streamClickListener: (Stream) -> Unit) : ListAdapter<Stream, StreamViewHolder>(Stream.diffCallback) {
    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        super.onBindViewHolder(holder, position, mutableListOf())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.stream, parent, false)

        return StreamViewHolder(view, streamClickListener)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onBindViewHolder(holder: StreamViewHolder, position: Int, payloads: MutableList<Any>) {
        val stream = getItem(position)

        holder.stream = stream
        holder.title.text = stream.mediaId

        payloads.forEach{payload ->
            when (payload) {
                PLAYING_COLOR -> {
                    holder.title.setTextColor(
                        ContextCompat.getColor(Application().baseContext, PLAYING_COLOR)
                    )
                }
                // If the payload wasn't understood, refresh the full item (to be safe).
                else -> return
            }
        }
    }
}

class StreamViewHolder(view: View, streamClickListener: (Stream) -> Unit)
    : androidx.recyclerview.widget.RecyclerView.ViewHolder(view){

    val title: TextView = view.findViewById(R.id.text_view_stream)
    var stream : Stream? = null

    init {
        view.setOnClickListener {
            stream?.let { streamClickListener(it) }
        }
    }

}