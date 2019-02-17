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
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
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

            val clickedPlayIntent = Intent(this@MediaPlaybackService, BecomingNoisyReceiver::class.java).apply {
                action = "ACTION_PLAY" }
            val pendingClickedPlay: PendingIntent =
                PendingIntent.getBroadcast(this@MediaPlaybackService, 0, clickedPlayIntent, 0)
            addAction(R.drawable.exo_controls_play, "Play", pendingClickedPlay)

            val clickedPauseIntent = Intent(this@MediaPlaybackService, BecomingNoisyReceiver::class.java).apply {
                action = "ACTION_PAUSE" }
            val pendingClickedPause: PendingIntent =
                PendingIntent.getBroadcast(this@MediaPlaybackService, 0, clickedPauseIntent, 0)
            addAction(R.drawable.exo_controls_pause, "Pause", pendingClickedPause)

            val clickedRepeatIntent = Intent(this@MediaPlaybackService, BecomingNoisyReceiver::class.java).apply {
                action = "ACTION_SET_REPEAT_MODE" }
            val pendingRepeatIntent: PendingIntent =
                PendingIntent.getBroadcast(this@MediaPlaybackService, 0, clickedRepeatIntent, 0)
            addAction(R.drawable.exo_controls_repeat_one, "Repeat", pendingRepeatIntent)

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
    private lateinit var receiver: BecomingNoisyReceiver

    private var metadata: MediaMetadataCompat? = null
    private var metadataObjects : HashMap<String, MediaMetadataCompat> = HashMap()
    var positionData : HashMap<String, Any?> = HashMap()
    fun collectPositionData(){
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .get().addOnSuccessListener { document ->
                val value: Map<String, Any> = document.data!!
                val data: HashMap<String, Any?> = HashMap()
                val playPosition = value.get("playPosition") as Long
                val streamPosition = value.get("streamPosition") as String
                data["playPosition"] = playPosition
                data["streamPosition"] = streamPosition

                //positionData = data
            }
    }
    fun fetchData(){
        db.collection("stream")
            .get()
            .addOnSuccessListener {documents ->
                for(document in documents){
                    val value: Map<String, Any> = document.data
                    val bitmap = GetBitmap().execute(value.get("image") as String).get()
                    val source = value.get("source") as String
                    val creator = value.get("creator") as String
                    val title = value.get("title") as String
                    val id = value.get("id") as String
                    val genre = value.get("genre") as String

                    val metaData : MediaMetadataCompat =
                        MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, source)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, creator)
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                            .build()

                    //metadataObjects[id] = metaData
                }
            }.addOnFailureListener {exception ->
                Log.e(Constants.TAG, "failed to read from FireStore: " + exception) }
    }
    override fun onCreate() {
        super.onCreate()
        Log.e(Constants.TAG, "ran service's onCreate")

        val sessionIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = PendingIntent.getActivity(this, 0, sessionIntent, 0)

        mediaSession = MediaSessionCompat(this, Constants.TAG).apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true }
        sessionToken = mediaSession.sessionToken

        notificationManager = NotificationManagerCompat.from(this)
        notificationManager
            .notify(Constants.NOTIFICATION_ID, notificationBuilder.build())

        controller = mediaSession.controller.also {
            it.registerCallback(mCallback) }

        receiver = BecomingNoisyReceiver(controller)
        this.registerReceiver(receiver, IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction("ACTION_PLAY")
            addAction("ACTION_PAUSE")
            addAction("ACTION_SET_REPEAT_MODE")
        })

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


        var played : Boolean = false
        override fun onPlay(player: Player?) {

            if(!played) {
                playSong()
                played = true
            }

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

    var playbackPreparer = object : MediaSessionConnector.PlaybackPreparer {
        override fun onPrepareFromSearch(query: String?, extras: Bundle?) = Unit

        override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) = Unit

        override fun getSupportedPrepareActions(): Long {
            return PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
        }

        override fun getCommands(): Array<String>? = null


        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
            var song = metadataObjects[mediaId]
            metadata = song
            Log.e(Constants.TAG, "current metadata: "
                    + metadata?.getString(MediaMetadataCompat.METADATA_KEY_TITLE))

            val mediaUri = Uri.parse(song?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI))
            val videoSource = ExtractorMediaSource.Factory(dataFactory)
                .createMediaSource(mediaUri)
            exoPlayer.prepare(videoSource)
            exoPlayer.seekTo(
                extras!!.getLong("playPosition")
            )
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

                    var metadataId : Int = Integer.parseInt(
                        metadata!!.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))
                    val nextId = metadataId + 1
                    controller.transportControls.playFromMediaId(
                        Integer.toString(nextId),
                        null)
                    notificationManager
                        .notify(Constants.NOTIFICATION_ID, updateNotification())

                }
                else -> return
            }
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {

    }
    override fun onGetRoot(p0: String, p1: Int, p2: Bundle?): BrowserRoot? {
        return BrowserRoot(Constants.ROOT_ID, null)
    }
    override fun onDestroy() {
        this.unregisterReceiver(receiver)
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

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)


        }
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(state)

            positionData["playPosition"] = state?.position!!
            positionData["streamPosition"] = metadata?.getString(
                MediaMetadataCompat.METADATA_KEY_MEDIA_ID) ?: "1"
            writeToFireStoreThePositionData()


            when(state.state){
                PlaybackStateCompat.STATE_PLAYING -> {
                    startService(Intent(this@MediaPlaybackService, MediaPlaybackService::class.java))
                    startForeground(Constants.NOTIFICATION_ID, updateNotification())
                }
                PlaybackStateCompat.STATE_NONE ->{
                    stopForeground(false)
                    stopSelf()
                }
                else -> {
                    stopForeground(false)
                }
            }
        }
    }
    fun updateNotification(): Notification{
        return notificationBuilder.apply {
            setContentText(controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
            setContentTitle(controller.metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
            setLargeIcon(controller.metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART))

            setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken))
            setContentIntent(controller.sessionActivity)

        }.build()
    }
    fun writeToFireStoreThePositionData(){
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .set(positionData) }
    fun playSong(){
        controller.transportControls.playFromMediaId(
            positionData["streamPosition"] as String,
            Bundle().apply {
                putLong(
                    "playPosition",
                    positionData["playPosition"] as Long) }) }
}

