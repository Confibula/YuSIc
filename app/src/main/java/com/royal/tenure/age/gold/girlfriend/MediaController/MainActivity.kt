package com.royal.tenure.age.gold.girlfriend.MediaController

import android.content.ComponentName
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.royal.tenure.age.gold.girlfriend.Constants
import com.royal.tenure.age.gold.girlfriend.MediaSession.MediaPlaybackService
import com.royal.tenure.age.gold.girlfriend.R


class MainActivity : AppCompatActivity() {
    companion object {
        lateinit var mMediaBrowserCompat: MediaBrowserCompat
    }

    val mConnectionCallback: MyConnectionCallback by lazy {
        MyConnectionCallback(
            this,
            mControllerCallback
        )
    }

    val mControllerCallback: MyControllerCallback by lazy {
        MyControllerCallback(context = this)
    }

    // TODO: google login functionality
    // Add a login functionality

    // Todo: add a notification compat
    // Also add a notificationCompat for when leaving the UI



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mMediaBrowserCompat = MediaBrowserCompat(this,
            ComponentName(this, MediaPlaybackService::class.java),
            mConnectionCallback,
            null)
    }

    override fun onStart() {
        super.onStart()

        mMediaBrowserCompat.connect()
        Log.e(Constants.TAG, "ran onStart on MainActivity")

    }

    override fun onStop() {
        super.onStop()
        mMediaBrowserCompat.disconnect()
    }
}
