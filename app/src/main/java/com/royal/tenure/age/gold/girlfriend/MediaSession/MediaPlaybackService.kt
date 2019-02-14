package com.royal.tenure.age.gold.girlfriend.MediaSession

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.opengl.Visibility
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.ImageView
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.media.MediaBrowserServiceCompat
import androidx.media.app.*
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.offline.DownloadService.startForeground
import com.google.firebase.firestore.FirebaseFirestore
import com.royal.tenure.age.gold.girlfriend.Constants
import com.royal.tenure.age.gold.girlfriend.MediaController.MainActivity
import com.royal.tenure.age.gold.girlfriend.R

//import android.support.annotation.RequiresApi


@RequiresApi(Build.VERSION_CODES.O)
class MediaPlaybackService : MediaBrowserServiceCompat() {
    private val mExoPlayer : ExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this)
    }

    private val notificationBuilder : NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(this, Constants.APP).apply {
            setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mMediaSessionCompat.sessionToken))
            // Todo: Add play actions
            // Add play actions to the notification


            // Todo: contentIntent
            // Find out how to deal with the fact that the contentIntent triggers onCreate in MainActivity
            // Is this a fault with the contentIntent
            // or actually a wrong implementation of MainActivity and MediaBrowserCompat
            //setContentIntent(mController.sessionActivity)
            setSmallIcon(R.drawable.exo_notification_small_icon)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

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
    private lateinit var mController : MediaControllerCompat

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
        Log.e(Constants.TAG, "created MediaBrowserService")

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

        mController = mMediaSessionCompat.controller.also {
            it.registerCallback(mCallback)
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

    // As far as I know, having multiple controllers for a session is okay.
    // This is my second controller. I was worrying whether this is bad code,
    // am now fairly confident, it's completely fine!
    val mCallback = object : MediaControllerCompat.Callback(){
        @RequiresApi(Build.VERSION_CODES.O)

        lateinit var metaData : MediaMetadataCompat

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)
            Log.e(Constants.TAG, "PlaybackState is: " + state?.state)

            when(state?.state){
                PlaybackStateCompat.STATE_PLAYING -> {
                    metaData = mController.metadata
                    startForeground(Constants.NOTIFICATION_ID, buildNotification())
                }
                else -> {
                    stopForeground(false)
                }
            }
        }
        fun buildNotification(): Notification{
            val mNotification = notificationBuilder.apply {
                val bitmap = metaData.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)

                setContentTitle(metaData.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                setContentText(metaData.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                setLargeIcon(bitmap)

            }.build()
            return mNotification
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
