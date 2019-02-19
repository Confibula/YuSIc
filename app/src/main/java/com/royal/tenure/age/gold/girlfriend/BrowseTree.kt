package com.royal.tenure.age.gold.girlfriend

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat

class BrowseTree(metadatas: HashMap<String, MediaMetadataCompat>, info: Bundle) {

    // This is the ingenious code
    private val children = mutableMapOf<String, MutableList<MediaMetadataCompat>>()
    operator fun get(node: String?) = children[node]


    init {
        metadatas.forEach { mediaItem ->
            val streamName = mediaItem.value.getString(MediaMetadataCompat.METADATA_KEY_GENRE)
            val streamies : MutableList<MediaMetadataCompat>
                    = children[streamName] ?: buildStreamies(mediaItem.value)
            streamies.add(mediaItem.value)
        }

    }
    fun buildStreamies(metadata: MediaMetadataCompat) : MutableList<MediaMetadataCompat>{
        val streamName = metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE)
        val stream = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, streamName)
            .build()

        val root = children[Constants.ROOT_ID] ?: mutableListOf()
        root.add(stream)

        return mutableListOf<MediaMetadataCompat>().also { streamies ->
            children[streamName] = streamies
        }
    }
}