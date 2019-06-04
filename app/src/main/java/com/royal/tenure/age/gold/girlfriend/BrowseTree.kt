package com.royal.tenure.age.gold.girlfriend

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.util.Log

class BrowseTree {

    // This is the ingenious code
    private val children = mutableMapOf<String, MutableList<MediaMetadataCompat>>()
    operator fun get(node: String?) = children[node]


    fun update(metadatas: MutableList<MediaMetadataCompat>, positions : MutableList<HashMap<String, Any>>){

        metadatas.shuffle()
        metadatas.forEach { song ->
            val genre : String = song.genre
            val streamies : MutableList<MediaMetadataCompat>
                    = children[genre] ?: buildStreamies(song)

            if(streamies.size < 34) {
                streamies.add(song)
            }
        }
    }

    fun buildStreamies(metadata: MediaMetadataCompat) : MutableList<MediaMetadataCompat>{
        val genre = metadata.genre
        val stream = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, genre)
            .build()

        val streams = children[Commons.ROOT_ID] ?: mutableListOf()
        streams.add(stream)
        children[Commons.ROOT_ID] = streams

        return mutableListOf<MediaMetadataCompat>().also { streamies ->
            children[genre] = streamies
        }
    }
}