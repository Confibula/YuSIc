package com.royal.tenure.age.gold.girlfriend

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog.show
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
import android.content.res.Configuration.ORIENTATION_LANDSCAPE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.media.session.PlaybackState
import android.os.AsyncTask
import android.os.Bundle
import android.provider.ContactsContract
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.Layout
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.scale
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
import com.google.ads.consent.*
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.internal.service.Common
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.stream.Stream

class MainActivity : AppCompatActivity() {
    private var db : FirebaseFirestore? = null
    private lateinit var googleSignInClient : GoogleSignInClient
    private lateinit var mediaBrowser : MediaBrowserCompat
    private lateinit var viewModel: StreamModel
    private var metadata : MediaMetadataCompat? = null
    private var analytics : FirebaseAnalytics? = null
    private var consentInformation: ConsentInformation? = null
    private var adRequestBuilder: AdRequest.Builder? = null
    private var form : ConsentForm? = null
    private var adView: AdView? = null

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

        analytics = FirebaseAnalytics.getInstance(this)
        MobileAds.initialize(applicationContext, getString(R.string.ad_id))
        consentInformation = ConsentInformation.getInstance(this)
        adRequestBuilder = AdRequest.Builder()
        db = FirebaseFirestore.getInstance()
        adView = findViewById(R.id.adView)

        adView?.adListener = object : AdListener() {
            override fun onAdFailedToLoad(p0: Int) {
                super.onAdFailedToLoad(p0)
                Log.e(Commons.TAG, "failed to load: " +  p0)
            }
        }

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

        viewModel.nowPlaying.observe(this@MainActivity, Observer<MediaMetadataCompat> { data ->
            Log.e(Commons.TAG, "ran observercode for the image with: ")
            val bitmap = data.bitmap
            findViewById<ConstraintLayout>(R.id.layout_view_main).apply {
                background = BitmapDrawable(null, bitmap)
            }
        })


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


        val publisherIds = arrayOf(getString(R.string.publish_id))
        consentInformation!!.requestConsentInfoUpdate(publisherIds, object : ConsentInfoUpdateListener {
            override fun onConsentInfoUpdated(consentStatus: ConsentStatus?) {
                Log.e(Commons.TAG, "userinfo updated: " + consentStatus)

                when(consentInformation!!.consentStatus){
                    ConsentStatus.UNKNOWN -> {
                        form?.load()
                    }
                    ConsentStatus.NON_PERSONALIZED -> {
                        val bundle = Bundle().apply { putString("npa", "1") }
                        adView?.loadAd(adRequestBuilder?.addNetworkExtrasBundle(AdMobAdapter::class.java, bundle)!!.build())
                    }
                    ConsentStatus.PERSONALIZED -> {
                        adView?.loadAd(adRequestBuilder?.build())
                    }
                    else -> {

                    }
                }
            }
            override fun onFailedToUpdateConsentInfo(reason: String?) {
                Log.e(Commons.TAG, "userinfo failed to update: " + reason)
            }
        })

        var privacyUrl : URL? = null
        try { privacyUrl = URL("http://yusic.droppages.com/") }
        catch (e : Throwable) { Log.e(Commons.TAG, "problem %e ", e) }

        form = ConsentForm.Builder(this@MainActivity, privacyUrl)
            .withListener(object : ConsentFormListener() {
                override fun onConsentFormClosed(consentStatus: ConsentStatus?, userPrefersAdFree: Boolean?) {
                    super.onConsentFormClosed(consentStatus, userPrefersAdFree)
                    consentInformation?.consentStatus = consentStatus
                    Log.e(Commons.TAG, "new consentStatus is:" + consentStatus)
                }

                override fun onConsentFormError(reason: String?) {
                    super.onConsentFormError(reason)
                    Log.e(Commons.TAG, "formerror: " + reason)
                }

                override fun onConsentFormLoaded() {
                    form?.show()
                    super.onConsentFormLoaded()
                    Log.e(Commons.TAG, "consent form is loaded!!")

                }
                override fun onConsentFormOpened() {
                    super.onConsentFormOpened()
                }

            })
            .withNonPersonalizedAdsOption()
            .withPersonalizedAdsOption()
            .build()

    }

    override fun onStart() {
        super.onStart()
        connecting()
        // Todo: Ads-implementation in the future ?

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