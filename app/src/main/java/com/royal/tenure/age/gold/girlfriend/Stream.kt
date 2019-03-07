package com.royal.tenure.age.gold.girlfriend

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil

data class Stream(
    val mediaId: String,
    var color: Int) {

    companion object {
        /**
         * Indicates [playbackRes] has changed.
         */
        const val PLAYING_COLOR = R.color.colorAccent

        val diffCallback = object : DiffUtil.ItemCallback<Stream>() {
            override fun areItemsTheSame(oldItem: Stream,
                                         newItem: Stream): Boolean =
                oldItem.mediaId == newItem.mediaId

            override fun areContentsTheSame(oldItem: Stream, newItem: Stream) =
                oldItem.mediaId == newItem.mediaId && oldItem.color == newItem.color

            override fun getChangePayload(oldItem: Stream, newItem: Stream) =
                if (oldItem.color != newItem.color) {
                    PLAYING_COLOR
                } else null
        }
    }
}