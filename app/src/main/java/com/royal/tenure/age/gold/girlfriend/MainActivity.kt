package com.royal.tenure.age.gold.girlfriend

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
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import java.util.stream.Stream

class MainActivity : AppCompatActivity() {
    private val db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var googleSignInClient : GoogleSignInClient
    private lateinit var mediaBrowser : MediaBrowserCompat
    private lateinit var viewModel: StreamModel
    private var metadata : MediaMetadataCompat? = null


    val controllerCallback = object : MediaControllerCompat.Callback(){

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            metadata?.id?.let {
                this@MainActivity.metadata = metadata
                    viewModel.putMetadata(metadata)
                    Log.e(Commons.TAG, "metadata sent to the viewmodel: " + metadata.title)
                }
        }

        override fun onPlaybackStateChanged(playback: PlaybackStateCompat?) {
            playback?.let {
                viewModel.putPlayback(it)
            }

            val positionData = HashMap<String, Any>()

                metadata?.let {
                    positionData.also { data ->
                        Log.e(Commons.TAG, "the metadata: " + it.genre)
                        data["id"] = it.id.toDouble()
                        data["genre"] = it.genre as String }
                    updateToFireStoreThePositionData(positionData)
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

            val controller = MediaControllerCompat
                .getMediaController(this@MainActivity)
            viewModel.putController(controller)
            viewModel.putMetadata(controller.metadata)
            viewModel.putPlayback(controller.playbackState)

            controller.registerCallback(controllerCallback)
            mediaBrowser.subscribe(Commons.ROOT_ID, subscriptionCallback)
        }
    }

    val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback(){
        override fun onChildrenLoaded(parentId: String, streams: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, streams)

            Log.e(Commons.TAG, "streams loaded")
            viewModel.putStreams(streams)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        MenuInflater(this).inflate(R.menu.menu_main, menu).also {
            val playButt = menu!!.findItem(R.id.play_button)
            playButt.icon = getDrawable(viewModel.playbutton_res.value!!)
            playButt.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        }
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
                    title = data.artist
                    subtitle = data.title
                    Log.e(Commons.TAG, "writes titles to the toolbar: " + data.title)
                }
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

    }

    fun updateToFireStoreThePositionData(positionData: HashMap<String, Any>){
        val info = HashMap<String, Any>()
        info["id"] = positionData["id"] as Number
        info["genre"] = positionData["genre"]!!

        db.collection("users")
            .document(auth.currentUser!!.uid)
            .collection("positions")
            .document(positionData["genre"] as String)
            .set(info)
            .addOnFailureListener{
                Log.e(Commons.TAG, "failed to write position data: %e", it)
            }}


    override fun onStart() {
        super.onStart()


        Log.e(Commons.TAG, "current user email: " + auth.currentUser?.email)
        if(auth.currentUser == null)  startSignInProcess()
        else if(!mediaBrowser.isConnected) {
            mediaBrowser.connect()
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
    }

    private fun firebaseAuthenticationWithGoogle(acct: GoogleSignInAccount){
        val credential : AuthCredential = GoogleAuthProvider.getCredential(acct.idToken, null)

        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                mediaBrowser.connect()
            }
        }
    }
}