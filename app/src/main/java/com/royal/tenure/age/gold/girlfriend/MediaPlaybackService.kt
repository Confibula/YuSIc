package com.royal.tenure.age.gold.girlfriend

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.browse.MediaBrowser
import android.media.session.PlaybackState
import android.net.Uri
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.text.TextUtils.replace
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat.stopForeground
import androidx.core.graphics.get
import androidx.core.view.OneShotPreDrawListener.add
import androidx.core.view.accessibility.AccessibilityEventCompat.setAction
import androidx.media.MediaBrowserServiceCompat
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.offline.DownloadService.startForeground
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.gms.common.internal.service.Common
import com.google.android.gms.flags.Singletons
import com.google.firebase.firestore.FirebaseFirestore
import com.squareup.okhttp.Call
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL

@RequiresApi(Build.VERSION_CODES.O)
class MediaPlaybackService : MediaBrowserServiceCompat() {
    private val db : FirebaseFirestore = FirebaseFirestore.getInstance()

    private var browseTree: BrowseTree = BrowseTree()

    private val exoPlayer : ExoPlayer by lazy {
        ExoPlayerFactory.newSimpleInstance(this,
            DefaultRenderersFactory(this),
            DefaultTrackSelector(),
            DefaultLoadControl.Builder().setBufferDurationsMs(
                13000, 21000, 8000, 8000
            ).createDefaultLoadControl()).apply {
            setAudioAttributes(uAmpAudioAttributes, true)
        }
    }

    private val uAmpAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val notificationBuilder : NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(this, Commons.APP).apply {
            setSmallIcon(R.drawable.exo_notification_small_icon)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setOnlyAlertOnce(true)
            setAutoCancel(true)
            setContentIntent(controller.sessionActivity)

            setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setMediaSession(mediaSession.sessionToken)
                .setShowActionsInCompactView(1)
            )

            // Creating the channel
            if (shouldCreateNowPlayingChannel()) {
                val mChannel = NotificationChannel(
                    Commons.NOTIFICATION_CHANNEL,
                    getString(R.string.notification_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = getString(R.string.description) }
                notificationManager.createNotificationChannel(mChannel)
                setChannelId(Commons.NOTIFICATION_CHANNEL)
            }
        }
    }

    private fun shouldCreateNowPlayingChannel() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowPlayingChannelExists() =
        notificationManager.getNotificationChannel(Commons.NOTIFICATION_CHANNEL) != null

    private lateinit var controller : MediaControllerCompat
    private lateinit var mediaSession : MediaSessionCompat
    private lateinit var sessionConnector: MediaSessionConnector
    private lateinit var dataFactory: DefaultDataSourceFactory
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var receiver: MyReceiver
    private var playback: PlaybackStateCompat = PlaybackStateCompat.Builder().build()
    private var metadata: MediaMetadataCompat = MediaMetadataCompat.Builder().build()

    // Fetched data
    private val metadatas: MutableList<MediaMetadataCompat> = mutableListOf()
    private val positions : MutableList<HashMap<String, Any>> = mutableListOf()
    fun fetch(){

        db.collection("stream")
            .get()
            .addOnSuccessListener {datas ->
                for(data in datas){
                    val song: Map<String, Any> = data.data
                    val bitmap = song["image"] as String
                    val source = song["source"] as String
                    val creator = song["creator"] as String
                    val title = song["title"] as String
                    val id = song["id"] as String
                    val genre = song["genre"] as String

                    val metadata : MediaMetadataCompat =
                        MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, source)
                            .putString(MediaMetadataCompat.METADATA_KEY_ART_URI, bitmap)
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, creator)
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, id)
                            .putString(MediaMetadataCompat.METADATA_KEY_GENRE, genre)
                            .build()
                    metadatas.add(metadata)
                }

            }.addOnFailureListener {
                Log.e(Commons.TAG, "it failed loading: " + it)
            }.continueWith {
                Log.e(Commons.TAG, "ran updating")
                browseTree.update(metadatas, positions)
                notifyChildrenChanged(Commons.ROOT_ID)
            }
    }

    override fun onCreate() {
        super.onCreate()
        fetch()

        // Todo: Sonos Project
        // ContentProvider for Sonos and for your apps latest metadata

        val sessionIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val sessionActivityPendingIntent = PendingIntent.getActivity(this, 0, sessionIntent, 0)

        mediaSession = MediaSessionCompat(this, Commons.TAG).apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true }

        controller = mediaSession.controller.also {
            it.registerCallback(mCallback) }

        sessionToken = mediaSession.sessionToken

        notificationManager = NotificationManagerCompat.from(this)

        receiver = MyReceiver(controller)

        this@MediaPlaybackService
            .registerReceiver(receiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

        exoPlayer.addListener(playerEventListener)

        sessionConnector = MediaSessionConnector(mediaSession, playbackController).also {
            dataFactory = DefaultDataSourceFactory(
                this,
                Util.getUserAgent(this, Commons.APP))

            it.setPlayer(exoPlayer, playbackPreparer)

            it.setQueueNavigator(object : MediaSessionConnector.QueueNavigator{
                override fun onSkipToQueueItem(player: Player?, id: Long) {}

                override fun onCurrentWindowIndexChanged(player: Player?) {}

                override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) {}

                override fun getSupportedQueueNavigatorActions(player: Player?): Long {
                    return ACTION_SKIP_TO_NEXT or ACTION_SKIP_TO_PREVIOUS
                }

                override fun getActiveQueueItemId(player: Player?): Long = 0

                override fun onSkipToPrevious(player: Player?) {
                    if(queueInt > 1){
                        queueInt = queueInt - 2
                        queueAndMetadataAndPlay()
                    }
                }

                override fun getCommands(): Array<String>? = null

                override fun onTimelineChanged(player: Player?) {}

                override fun onSkipToNext(player: Player?) {
                    queueAndMetadataAndPlay()
                }
            })
        }


    }


    var playbackController = object : MediaSessionConnector.PlaybackController{
        override fun onRewind(player: Player?) = Unit

        override fun onSeekTo(player: Player?, position: Long) = Unit

        override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?){
        }

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
            return  ACTION_PLAY or
                    ACTION_PAUSE or
                    ACTION_SET_REPEAT_MODE
        }

        override fun getCommands(): Array<String>? = null

        override fun onSetRepeatMode(player: Player?, repeatMode: Int){
            player!!.repeatMode = repeatMode
        }
    }

    var playbackPreparer = object : MediaSessionConnector.PlaybackPreparer {
        override fun onPrepareFromSearch(query: String?, extras: Bundle?) = Unit

        override fun onCommand(player: Player?, command: String?, extras: Bundle?, cb: ResultReceiver?) = Unit

        override fun getSupportedPrepareActions(): Long {
            return PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
        }

        override fun getCommands(): Array<String>? = null

        fun playlist(streamies: MutableList<MediaMetadataCompat>?) : ConcatenatingMediaSource{
            var theStream = ConcatenatingMediaSource()
            streamies?.apply { sortBy { it.id.toLong() } }?.map {song ->
                val videoSource = ExtractorMediaSource.Factory(dataFactory)
                    .createMediaSource(Uri.parse(song.mediaUri))
                theStream = ConcatenatingMediaSource(theStream, videoSource) }
            return theStream
        }

        fun queue(mediaId : String): MutableList<MediaSessionCompat.QueueItem>{
            val streamies : MutableList<MediaSessionCompat.QueueItem> = mutableListOf()
            browseTree[mediaId]!!.map { streamie ->
                val theStremie = MediaSessionCompat.QueueItem(streamie.description, streamie.id.toLong())
                streamies.add(theStremie)
            }
            return streamies
        }

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
            val list = queue(mediaId!!)
            mediaSession.setQueue(list)
            Log.e(Commons.TAG, "metadata was initially set to: " + metadata.title)
            queueInt = 0
            queueAndMetadataAndPlay()
        }

        override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) = Unit

        override fun onPrepare() {
        }
    }

    var queueInt : Int = 28657

    fun queueAndMetadataAndPlay(){
        if(queueInt < controller.queue.size){
            val use = controller.queue[queueInt]
            val data = metadatas.find { it.id.toLong() == use.queueId }
            metadata = data!!

            val uri = use.description.mediaUri
            val videoSource = ExtractorMediaSource.Factory(dataFactory).createMediaSource(uri)
            exoPlayer.prepare(videoSource)
            queueInt++

            GetBitmap().execute(metadata.artUri)
        }
    }

    var playerEventListener = object : Player.EventListener{
        override fun onLoadingChanged(isLoading: Boolean) {
        }
        override fun onPositionDiscontinuity(reason: Int) {

        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            mediaSession.setMetadata(metadata)
            if(playbackState == Player.STATE_ENDED) queueAndMetadataAndPlay()
            Log.e(Commons.TAG, "metadata is: " + metadata.title)
        }
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {

        }

        override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {

        }
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        // My app doesn't need browsing. But I understand everything now about browsing, the root, ViewModels, etc.
        // Really. I understand it all now. Don't even need to show it.

        // Support fetching the list of items belonging to the root!


        val streams : MutableList<MediaBrowserCompat.MediaItem> = mutableListOf()
        browseTree[Commons.ROOT_ID]?.map { stream ->
            streams.add(MediaBrowserCompat.MediaItem(stream.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
        }
        result.sendResult(streams)

    }

    override fun onGetRoot(thePackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        if(thePackageName == packageName) {
            return BrowserRoot(Commons.ROOT_ID, null)
        } else {
            return BrowserRoot("EMPTY", null)
        }
    }

    override fun onDestroy() {
        this@MediaPlaybackService
            .unregisterReceiver(receiver)

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
        override fun onPlaybackStateChanged(playback: PlaybackStateCompat?) {
            super.onPlaybackStateChanged(playback)

            playback?.let {
                this@MediaPlaybackService.playback = it

            }

            when(playback?.state){
                PlaybackStateCompat.STATE_PLAYING -> {
                    startForeground(Commons.NOTIFICATION_ID, notification()) }
                else -> {
                    if(playback?.state == PlaybackStateCompat.STATE_PAUSED){
                        stopForeground(false)
                        notificationManager.notify(Commons.NOTIFICATION_ID, notification())}
                    else if (playback?.state == PlaybackStateCompat.STATE_NONE){
                        stopForeground(true)
                    }
                }
            }
        }
    }

    fun notification(): Notification{
        return notificationBuilder.apply {
            setContentText(metadata.title)
            setContentTitle(metadata.artist)
            setLargeIcon(metadata.bitmap)

            Log.e(Commons.TAG, "ran notification creation")

            mActions.clear()

            if(playback.isSkipToPreviousEnabled){
                addAction(NotificationCompat.Action(
                    R.drawable.exo_controls_previous,
                    getString(R.string.description),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext, ACTION_SKIP_TO_PREVIOUS))) }

            if(playback.isPlaying){
                addAction(NotificationCompat.Action(
                    R.drawable.exo_controls_pause,
                    this@MediaPlaybackService.getString(R.string.pause),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext, ACTION_PAUSE))) }

            else if(playback.isPlayEnabled){
                addAction(NotificationCompat.Action(
                    R.drawable.exo_controls_play,
                    this@MediaPlaybackService.getString(R.string.play),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext, ACTION_PLAY))) }

            if(playback.isSkipToNextEnabled){
                addAction(NotificationCompat.Action(
                    R.drawable.exo_controls_next,
                    getString(R.string.description),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        applicationContext, ACTION_SKIP_TO_NEXT))) }


        }.build() }

    inner class GetBitmap : AsyncTask<String, Void, MutableList<Bitmap>>(){
        override fun doInBackground(vararg bitmapUris: String?): MutableList<Bitmap> {
            var inputStream: InputStream? = null
            val bitmaps : MutableList<Bitmap> = mutableListOf()

            try {
                val url : URL = URL(bitmapUris.first())
                val urlConnection : HttpURLConnection = url.openConnection() as HttpURLConnection
                inputStream = BufferedInputStream(urlConnection.inputStream)

                val bitmapOptions = BitmapFactory.Options()
                bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888
                val bitmap = BitmapFactory.decodeStream(inputStream, null, bitmapOptions)!!
                bitmaps.add(bitmap)

            } catch (e: IOException){
                return mutableListOf()
            } finally {
                inputStream?.close()
            }
            return bitmaps
        }
        override fun onPostExecute(result: MutableList<Bitmap>) {
            super.onPostExecute(result)
            var bitmap : Bitmap? = null
            if(!result.isEmpty()) {
                bitmap = result.first()
            }
            val data = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI,
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI))
                .putString(MediaMetadataCompat.METADATA_KEY_ART_URI,
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_ART_URI))
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE))
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID,
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))
                .putString(MediaMetadataCompat.METADATA_KEY_GENRE,
                    metadata.getString(MediaMetadataCompat.METADATA_KEY_GENRE))
                .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
                .build()

            metadata = data
            mediaSession.setMetadata(metadata)
            notificationManager.notify(Commons.NOTIFICATION_ID, notification())
        }
    }

}

