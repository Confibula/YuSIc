package com.royal.tenure.age.gold.girlfriend

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter

class StreamAdapter : ListAdapter<Stream, StreamViewHolder>(Stream.diffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreamViewHolder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBindViewHolder(holder: StreamViewHolder, position: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class StreamViewHolder(view: View): androidx.recyclerview.widget.RecyclerView.ViewHolder(view){

}