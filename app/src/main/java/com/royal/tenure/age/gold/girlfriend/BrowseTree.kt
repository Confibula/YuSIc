package com.royal.tenure.age.gold.girlfriend

import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.util.Log

class BrowseTree {

    // This is the ingenious code
    private val children = mutableMapOf<String, MutableList<MediaMetadataCompat>>()
    operator fun get(node: String?) = children[node]


    fun update(metadatas: MutableList<MediaMetadataCompat>, positions : MutableList<HashMap<String, Any>>){
        Log.e(Commons.TAG, "I HAVE REACHED UPDATE metadatas" + positions)
        Log.e(Commons.TAG, "I HAVE REACHED UPDATE metadatas" + metadatas.toList())

        // Todo: do something with incoming position data

        metadatas.forEach { song ->
            val genre : String = song.genre
            val streamies : MutableList<MediaMetadataCompat>
                    = children[genre] ?: buildStreamies(song)

            Log.e(Commons.TAG, song.description.title.toString())
            val position = positions.find {
                it.containsValue(genre) }
            val id = position!!["id"] as Number
            val theGenre = position!!["genre"]
            if(streamies.size < 55 && song.id.toLong() >= id.toLong() && genre == theGenre){
                streamies.add(song) }

            // Todo: Most important fix. Return player to the right place in the stream!

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