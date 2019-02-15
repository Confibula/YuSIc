package com.royal.tenure.age.gold.girlfriend.MediaSession

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.royal.tenure.age.gold.girlfriend.Constants
import com.royal.tenure.age.gold.girlfriend.R


@RequiresApi(Build.VERSION_CODES.O)
class MediaPlaybackService : MediaBrowserServiceCompat() {
    private val exoPlayer : ExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this)
    }

    private val notificationBuilder : NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(this, Constants.APP).apply {
            setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken))

            // Todo: Add play actions
            // Add play actions to the notification

            setContentIntent(controller.sessionActivity)
            setSmallIcon(R.drawable.exo_notification_small_icon)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setOnlyAlertOnce(true)

            // Creating the channel
            val name = getString(R.string.adjust)
            val descriptionText = getString(R.string.description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel(Constants.NOTIFICATION_CHANNEL, name, importance)
            mChannel.description = descriptionText
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
            setChannelId(mChannel.id)
        }
    }
    private lateinit var controller : MediaControllerCompat
    private lateinit var mediaSession : MediaSessionCompat
    private lateinit var sessionConnector: MediaSessionConnector
    private lateinit var playBackPreparer : MediaSessionConnector.PlaybackPreparer
    private lateinit var defaultDataSourceFactory: DefaultDataSourceFactory
    private lateinit var playbackController: MediaSessionConnector.PlaybackController

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(Constants.TAG, "ran onStartCommand")

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(Constants.TAG, "created MediaBrowserService")

        val sessionIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = PendingIntent.getActivity(this, 0, sessionIntent, 0)

        // Create a new MediaSession.
        mediaSession = object : MediaSessionCompat(this,
            Constants.TAG
        ){

        }.apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true
            }

        controller = mediaSession.controller.also {
            it.registerCallback(mCallback)
        }

        playbackController =
            MyPlaybackController()

        sessionConnector = MediaSessionConnector(mediaSession, playbackController).also {

            defaultDataSourceFactory = DefaultDataSourceFactory(
                this,
                Util.getUserAgent(this,
                    Constants.APP
                )
            )

            playBackPreparer =
                MyPlayBackPreparer(
                    mediaSession,
                    defaultDataSourceFactory,
                    exoPlayer,
                    this
                )

            it.setPlayer(exoPlayer, playBackPreparer)
        }

        // sets the token
        sessionToken = mediaSession.sessionToken
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.e(Constants.TAG, "ran onBind")

        return super.onBind(intent)
    }

    override fun onLoadItem(itemId: String?, result: Result<MediaBrowserCompat.MediaItem>) {
        super.onLoadItem(itemId, result)
    }

    override fun onLoadChildren(p0: String, p1: Result<MutableList<MediaBrowserCompat.MediaItem>>) {

    }

    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return BrowserRoot(Constants.APP, null)
    }

    override fun onDestroy() {
        Log.e(Constants.TAG, "ran service's onDestroy")
        exoPlayer.stop()
        super.onDestroy()
    }

    // As far as I know, having multiple controllers for a session is okay.
    // This is my second controller. I was worrying whether this is bad code,
    // am now fairly confident, it's completely fine!
    val mCallback = object : MediaControllerCompat.Callback(){
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            Log.e(Constants.TAG, "PlaybackState is: " + state?.state)

            when(state?.state){
                PlaybackStateCompat.STATE_PLAYING -> {
                    metaData = controller.metadata
                    if(needsBuild) buildNotification()
                    startService(Intent(this@MediaPlaybackService, MediaPlaybackService::class.java))
                    startForeground(Constants.NOTIFICATION_ID, fetchNotification())
                    needsBuild = false
                }
                PlaybackStateCompat.STATE_NONE -> {
                    needsBuild = true
                }
                else -> {
                    stopForeground(false)
                    stopSelf()
                }
            }
        }
        fun buildNotification(){
            notification = notificationBuilder.apply {
                val bitmap = metaData.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)
                setContentTitle(metaData.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                setContentText(metaData.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                setLargeIcon(bitmap)

            }.build()
        }
        fun fetchNotification(): Notification{
            return notification
        }
        lateinit var notification: Notification
        var needsBuild : Boolean = true
        lateinit var metaData : MediaMetadataCompat
    }
}

