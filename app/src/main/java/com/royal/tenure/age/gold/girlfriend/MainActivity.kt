package com.royal.tenure.age.gold.girlfriend

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import android.content.Intent
import android.content.Intent.ACTION_PICK
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.session.PlaybackState
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.init
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.stream.Stream

class MainActivity : AppCompatActivity() {
    private val db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var googleSignInClient : GoogleSignInClient
    private lateinit var mediaBrowser : MediaBrowserCompat
    private lateinit var viewModel: StreamModel
    private var metadata : MediaMetadataCompat? = null
    private var analytics : FirebaseAnalytics? = null

    val controllerCallback = object : MediaControllerCompat.Callback(){

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            metadata?.id?.also {
                this@MainActivity.metadata = metadata
                viewModel.putMetadata(metadata)
            }
        }

        override fun onPlaybackStateChanged(playback: PlaybackStateCompat?) {
            playback?.let {
                viewModel.putPlayback(it)
            }
            super.onPlaybackStateChanged(playback)
        }
    }

    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()

            mediaBrowser.sessionToken.also { token ->
                val controller = MediaControllerCompat(
                    this@MainActivity, token)
                MediaControllerCompat.setMediaController(this@MainActivity, controller)
            }

            // updating for every return to the Main UI
            val controller = MediaControllerCompat
                .getMediaController(this@MainActivity)
            viewModel.putController(controller)
            val data = controller.metadata?.let {
                it
            } ?: MediaMetadataCompat.Builder().build()
            viewModel.putMetadata(data)
            viewModel.putPlayback(controller.playbackState)

            controller.registerCallback(controllerCallback)
            mediaBrowser.subscribe(Commons.ROOT_ID, subscriptionCallback)
        }
    }

    val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback(){
        override fun onChildrenLoaded(parentId: String, streams: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, streams)

            viewModel.putStreams(streams)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        MenuInflater(this).inflate(R.menu.menu_main, menu)
        val playButt = menu!!.findItem(R.id.play_button)
        val repeatButt = menu.findItem(R.id.repeat_button)
        viewModel.playbutton_res.observe(this, Observer { res ->
            playButt.icon = getDrawable(res)
        })
        viewModel.repeat_res.observe(this, Observer {res ->
            repeatButt.icon = getDrawable(res)
        })
        Log.e(Commons.TAG, "button was: " + viewModel.playbutton_res.value!!)
        playButt.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.findItem(R.id.forward_button).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        repeatButt.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)

        // Probably tells it to create. False means 'not create'

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val controller = viewModel.controller.value
        when(item?.itemId){
            R.id.play_button -> {
                if(controller!!.playbackState.isPlaying) {
                    controller.transportControls!!.pause()
                    item.icon = getDrawable(R.drawable.exo_controls_pause)
                }
                else if(controller.playbackState.isPlayEnabled) {
                    controller.transportControls!!.play()
                    item.icon = getDrawable(R.drawable.exo_controls_play)
                }
            }
            R.id.forward_button -> {
                if(controller!!.playbackState.state != PlaybackStateCompat.STATE_NONE) {
                    controller.transportControls?.skipToNext()
                }
            }
            R.id.repeat_button -> {
                if(controller!!.playbackState.state != PlaybackStateCompat.STATE_NONE){
                    if(controller.repeatMode == Player.REPEAT_MODE_OFF){
                        Log.e(Commons.TAG, "IF")
                        controller.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
                        item.icon = getDrawable(R.drawable.exo_controls_repeat_one)
                    } else if (controller.repeatMode == Player.REPEAT_MODE_ONE){
                        Log.e(Commons.TAG, "ELSE IF")
                        controller.transportControls?.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
                        item.icon = getDrawable(R.drawable.exo_controls_repeat_off)
                    }
                    Log.e(Commons.TAG, "STATE CHECK")
                }
            }
            else -> return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(StreamModel::class.java)
        setSupportActionBar(findViewById(R.id.toolbar_view))

        supportActionBar!!.also {actionBar ->
            viewModel.nowPlaying.observe(this@MainActivity, Observer<MediaMetadataCompat>{ data ->
                actionBar.apply {
                    title = data?.artist
                    subtitle = data?.title
                    Log.e(Commons.TAG, "writes titles to the toolbar: " + data?.title)
                }
            })
        }


        findViewById<ImageView>(R.id.image_view).also { view ->
            viewModel.nowPlaying.observe(this@MainActivity, Observer<MediaMetadataCompat> { data ->
                Log.e(Commons.TAG, "ran observercode for the image with: ")
                view.setImageBitmap(data.bitmap)
            })
        }


        mediaBrowser = MediaBrowserCompat(this,
            ComponentName(this, MediaPlaybackService::class.java),
            connectionCallback,
            null)

        findViewById<RecyclerView>(R.id.recycler_view).apply {
            adapter = StreamAdapter({ stream -> viewModel.play(stream) }, this@MainActivity).also {adapter ->
                viewModel.streams.observe(this@MainActivity, Observer { list ->
                    adapter.submitList(list)

                    Log.e(Commons.TAG, "submitted the list to the adapter ")
                })
            }
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false)
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        analytics = FirebaseAnalytics.getInstance(this)
        MobileAds.initialize(this, getString(R.string.ad_id))

        val adView = findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        adView.adListener = object : AdListener(){
            override fun onAdClicked() {
                Log.e(Commons.TAG, "I'm in the add")
                super.onAdClicked()
            }

            override fun onAdClosed() {
                Log.e(Commons.TAG, "I'm in the add")
                super.onAdClosed()
            }

            override fun onAdFailedToLoad(p0: Int) {
                Log.e(Commons.TAG, "I'm in the add")
                super.onAdFailedToLoad(p0)
            }

            override fun onAdImpression() {
                Log.e(Commons.TAG, "I'm in the add")
                super.onAdImpression()
            }

            override fun onAdLeftApplication() {
                Log.e(Commons.TAG, "I'm in the add")
                super.onAdLeftApplication()
            }

            override fun onAdLoaded() {
                Log.e(Commons.TAG, "I'm in the add")
                super.onAdLoaded()
            }

            override fun onAdOpened() {
                Log.e(Commons.TAG, "I'm in the add")
                super.onAdOpened()
            }
        }

    }

    override fun onStart() {
        super.onStart()

        val consent = getSharedPreferences(Commons.PRIVACY_INFO, Context.MODE_PRIVATE)
            .getBoolean(Commons.CONSENT_CHOICE, false)

        if(!consent){
            Log.e(Commons.TAG, "started running consent false code")
            startActivityForResult(
                Intent(this, ConsentActivity::class.java),
                Commons.PRIVACY_REQUEST
            )
        }
        else if(consent){
            connecting()
        }
    }

    override fun onStop() {
        if(mediaBrowser.isConnected) mediaBrowser.disconnect()
        super.onStop()
    }

    private fun startSignInProcess() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, Commons.SIGN_IN_REQUEST)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // if the launch request is a google sign in
        if (requestCode == Commons.SIGN_IN_REQUEST) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data!!)
            try {
                val account : GoogleSignInAccount? = task.getResult(ApiException::class.java)
                firebaseAuthenticationWithGoogle(account!!)
            } catch (e: ApiException){
                Log.e(Commons.TAG, "ApiException: " + e)
            }
        }

        if (requestCode == Commons.PRIVACY_REQUEST){
            Log.e(Commons.TAG, "Reach the REQUEST dealer")
        }
    }

    private fun firebaseAuthenticationWithGoogle(acct: GoogleSignInAccount){
        val credential : AuthCredential = GoogleAuthProvider.getCredential(acct.idToken, null)

        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                connecting()
            }
        }
    }

    private fun connecting(){
        if(!mediaBrowser.isConnected){
            mediaBrowser.connect()
            volumeControlStream = C.CONTENT_TYPE_MUSIC
            startService(Intent(this, MediaPlaybackService::class.java))
        }
    }
}