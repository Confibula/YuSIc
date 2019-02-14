package com.royal.tenure.age.gold.girlfriend.MediaSession

import android.app.PendingIntent
import android.content.Intent
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.royal.tenure.age.gold.girlfriend.Constants

//import android.support.annotation.RequiresApi


class MediaPlaybackService : MediaBrowserServiceCompat() {
    private val mExoPlayer : ExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this)
    }

    // Todo: create notification
    private val notificationBuilder : NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(this, Constants.APP)
    }
    private lateinit var mController : MediaControllerCompat
    private lateinit var mMediaMetadata : MediaMetadataCompat
    private lateinit var mDescription : MediaDescriptionCompat

    private lateinit var mMediaSessionCompat : MediaSessionCompat
    private lateinit var mMediaSessionConnector: MediaSessionConnector
    private lateinit var mPlayBackPreparer : MediaSessionConnector.PlaybackPreparer
    private lateinit var mDefaultDataSourceFactory: DefaultDataSourceFactory
    private lateinit var mPlaybackController: MediaSessionConnector.PlaybackController

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()

        val sessionIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = PendingIntent.getActivity(this, 0, sessionIntent, 0)

        // Create a new MediaSession.
        mMediaSessionCompat = object : MediaSessionCompat(this,
            Constants.TAG
        ){

        }.apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true
            }

        mPlaybackController =
            MyPlaybackController()

        mMediaSessionConnector = MediaSessionConnector(mMediaSessionCompat, mPlaybackController).also {

            mDefaultDataSourceFactory = DefaultDataSourceFactory(
                this,
                Util.getUserAgent(this,
                    Constants.APP
                )
            )

            mPlayBackPreparer =
                MyPlayBackPreparer(
                    mMediaSessionCompat,
                    mDefaultDataSourceFactory,
                    mExoPlayer,
                    this
                )

            it.setPlayer(mExoPlayer, mPlayBackPreparer)
        }

        // sets the token
        sessionToken = mMediaSessionCompat.sessionToken
    }

    override fun onLoadItem(itemId: String?, result: Result<MediaBrowserCompat.MediaItem>) {
        super.onLoadItem(itemId, result)
    }

    override fun onLoadChildren(p0: String, p1: Result<MutableList<MediaBrowserCompat.MediaItem>>) {

    }

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return BrowserRoot(Constants.APP, null)

    }
}
