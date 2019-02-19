package com.royal.tenure.age.gold.girlfriend

import android.app.*
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource

@RequiresApi(Build.VERSION_CODES.O)
class MediaPlaybackService : MediaBrowserServiceCompat() {
    private val exoPlayer : ExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this) }

    private val notificationBuilder : NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(this, Constants.APP).apply {
            setSmallIcon(R.drawable.exo_notification_small_icon)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setOnlyAlertOnce(true)

            setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(0))
            setContentIntent(controller.sessionActivity)

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
    private lateinit var dataFactory: DefaultDataSourceFactory
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var receiver: MyReceiver

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)

        exoPlayer.stop(true)
    }

    private var metadata: MediaMetadataCompat? = null
    private var metadatas: HashMap<String, MediaMetadataCompat> = HashMap()
    val streamCount = 55
    fun createMetadatas(){
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .collection("positions")
            .get().addOnSuccessListener { positions ->
                for(position in positions){
                    val value: Map<String, Any> = position.data
                    val streamPoint = value.get("streamPosition") as Int
                    val streamName = value.get("streamName") as String

                    // Start the fetching
                    db.collection("stream")
                        .orderBy("id")
                        .startAt(streamPoint)
                        .whereEqualTo("genre", streamName)
                        .endAt(streamPoint + streamCount)
                        .get()
                        .addOnSuccessListener {documents ->
                            for(document in documents){
                                val value: Map<String, Any> = document.data
                                val bitmap = value.get("image") as String
                                val source = value.get("source") as String
                                val creator = value.get("creator") as String
                                val title = value.get("title") as String
                                val id = value.get("id") as String
                                val genre = value.get("genre") as String

                                val data : MediaMetadataCompat =
                                    MediaMetadataCompat.Builder()
                                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, source)
                                        .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, bitmap)
                                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, creator)
                                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                                        .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                                        .build()
                                metadatas[id] = data
                            }
                        }.addOnFailureListener {exception ->
                            Log.e(Constants.TAG, "failed to read from FireStore: " + exception) }
                }
            }
    }

    override fun onCreate() {
        super.onCreate()
        createMetadatas()

        // Todo:
        // When the service is created, add support for fetching the last metadata
        // that was showing when the user left the app and show it.
        // This might work best by storing the metadata in a ContentResolver

        val sessionIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = PendingIntent.getActivity(this, 0, sessionIntent, 0)

        mediaSession = MediaSessionCompat(this, Constants.TAG).apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true }
        sessionToken = mediaSession.sessionToken

        controller = mediaSession.controller.also {
            it.registerCallback(mCallback) }

        notificationManager = NotificationManagerCompat.from(this)

        receiver = MyReceiver(controller)

        exoPlayer.addListener(playerEventListener)

        sessionConnector = MediaSessionConnector(mediaSession, playbackController).also {
            dataFactory = DefaultDataSourceFactory(
                this,
                Util.getUserAgent(this, Constants.APP))

            it.setPlayer(exoPlayer, playbackPreparer) }
    }


    var playbackController = object : MediaSessionConnector.PlaybackController{
        override fun onRewind(player: Player?) = Unit

        override fun onSeekTo(player: Player?, position: Long) = Unit

        override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) = Unit

        override fun onPause(player: Player?) {
            player!!.playWhenReady = false

        }

        override fun onFastForward(player: Player?) = Unit

        override fun onPlay(player: Player?) {
            player!!.playWhenReady = true

        }

        override fun onStop(player: Player?) {

        }

        override fun onSetShuffleMode(player: Player?, shuffleMode: Int) = Unit

        override fun getSupportedPlaybackActions(player: Player?): Long {
            return  PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SET_REPEAT_MODE
        }

        override fun getCommands(): Array<String>? = null

        override fun onSetRepeatMode(player: Player?, repeatMode: Int){
            player!!.repeatMode = Player.REPEAT_MODE_ONE

        }

    }

    private val browseTree: BrowseTree by lazy {
        BrowseTree(metadatas, Bundle())
    }

    var playbackPreparer = object : MediaSessionConnector.PlaybackPreparer {
        override fun onPrepareFromSearch(query: String?, extras: Bundle?) = Unit

        override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) = Unit

        override fun getSupportedPrepareActions(): Long {
            return PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
        }

        override fun getCommands(): Array<String>? = null


        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {

            val streamies : MutableList<MediaMetadataCompat>? = browseTree[mediaId]

            // Todo:
            // Here a playlist is actually created. You also need to figure out what intent-filter
            // the "Sonos" application uses to allow playing music from "Sonos". Add support,
            // for playing one of the streams on your app with "Sonos".

            var theStream = ConcatenatingMediaSource()
            streamies?.forEach {song ->
                val mediaUri = Uri.parse(song.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI))
                val videoSource = ExtractorMediaSource.Factory(dataFactory)
                    .createMediaSource(mediaUri)
                theStream = ConcatenatingMediaSource(theStream, videoSource)
            }
            exoPlayer.prepare(theStream)

        }
        override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) = Unit

        override fun onPrepare() {

        }
    }

    var playerEventListener = object : Player.EventListener{
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {

            when(playbackState){
                Player.STATE_READY -> {
                    mediaSession.setMetadata(metadata)
                }
                Player.STATE_ENDED -> {

                }
                else -> return
            }
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        // My app doesn't need browsing. But I understand everything now about browsing, the root, ViewModels, etc.
        // Really. I understand it all now. Don't even need to show it.

        // Support fetching the list of items belonging to the root!


    }
    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return BrowserRoot(Constants.ROOT_ID, null)
    }
    override fun onDestroy() {
        super.onDestroy()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
    override fun onUnbind(intent: Intent?): Boolean {
        return super.onUnbind(intent)
    }
    override fun onBind(intent: Intent?): IBinder? {
        return super.onBind(intent)
    }

    // As far as I know, having multiple controllers for a session is okay.
    // This is my second controller. I was worrying whether this is bad code,
    // am now fairly confident, it's completely fine!
    val mCallback = object : MediaControllerCompat.Callback(){
        lateinit var state : PlaybackStateCompat

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)


        }
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)

            if(state!=null) this.state = state

            when(state?.state){
                PlaybackStateCompat.STATE_PLAYING -> {
                    startForeground(Constants.NOTIFICATION_ID, notification())
                    this@MediaPlaybackService
                        .registerReceiver(receiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
                }
                else -> {
                    stopForeground(false)
                    this@MediaPlaybackService
                        .unregisterReceiver(receiver)
                    if(state?.state == PlaybackStateCompat.STATE_NONE){
                        stopSelf()
                    }
                }
            }
        }
        fun notification(): Notification{
            return notificationBuilder.apply {
                setContentText(controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                setContentTitle(controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))

                // Todo:
                // right now the notification can't show any image.
                // How do you fix this without making your app run slowly,
                // doing ASyncTasks for bitmaps . . .

                if(state.isPlaying){
                    addAction(R.drawable.exo_controls_pause, applicationContext.getString(R.string.pause), MediaButtonReceiver
                        .buildMediaButtonPendingIntent(this@MediaPlaybackService, PlaybackStateCompat.ACTION_PAUSE))
                }
                if(state.isPlayEnabled){
                    addAction(R.drawable.exo_controls_play, applicationContext.getString(R.string.play), MediaButtonReceiver
                        .buildMediaButtonPendingIntent(this@MediaPlaybackService, PlaybackStateCompat.ACTION_PLAY))
                }

            }.build()
        }
    }
}

