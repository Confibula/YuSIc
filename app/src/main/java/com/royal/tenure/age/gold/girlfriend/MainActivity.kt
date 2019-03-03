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
    private var playback: PlaybackStateCompat = PlaybackStateCompat.Builder().build()
    private lateinit var metadata : MediaMetadataCompat

    val controllerCallback = object : MediaControllerCompat.Callback(){

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            metadata?.also {

                Glide
                    .with(this@MainActivity)
                    .load(it.mediaUri)
                    .into(findViewById(R.id.image_view))
                supportActionBar?.title = it.title

                this@MainActivity.metadata = it
            }
        }

        override fun onPlaybackStateChanged(playback: PlaybackStateCompat?) {
            playback?.also {
                this@MainActivity.playback = it
            }

            val positionData = HashMap<String, Any?>()
            metadata.also {
                positionData.also { data ->
                    data["id"] = Integer.parseInt(it.id)
                    data["genre"] = it.genre } }
            updateToFireStoreThePositionData(positionData)

            super.onPlaybackStateChanged(playback)
        }
    }

    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()

            mediaBrowser.sessionToken.also { token ->
                val controller = MediaControllerCompat(
                    this@MainActivity, token)
                MediaControllerCompat.setMediaController(this@MainActivity, controller) }

            val controller = MediaControllerCompat
                .getMediaController(this@MainActivity)

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
        MenuInflater(this).inflate(R.menu.menu_main, menu)
        // Probably tells it to create. False means 'not create'

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId
        val controller = MediaControllerCompat.getMediaController(this)

        Log.e(Commons.TAG, "ran onOptionsItemSelected")
        when(id){
            R.id.play_button -> {

            }
            R.id.repeat_button -> {

            }
            else -> return true
        }

        return super.onOptionsItemSelected(item)
    }

    inner class StreamAdapter : ListAdapter<MediaBrowserCompat.MediaItem, StreamAdapter.MyViewHolder>(DIFF_CALLBACK) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val streamView = LayoutInflater.from(parent.context)
                .inflate(R.layout.stream, parent, false) as TextView

            Log.e(Commons.TAG, "ran onCreateView")

            return MyViewHolder(streamView)
        }

        override fun getItemCount(): Int {
            if(streams != null) {
                return streams!!.size
            }
            else {
                return 0
            }
        }

        fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val theStream = holder.stream
            val streamId = streams!![position].description.mediaId
            theStream.text = streamId

            onBindViewHolder(holder, position)

            theStream.setOnClickListener {
                if(playback.isPlayEnabled){
                    MediaControllerCompat
                        .getMediaController(this@MainActivity)
                        .transportControls.playFromMediaId(streamId, null)
                }
                if(playback.isPlaying){
                    MediaControllerCompat
                        .getMediaController(this@MainActivity)
                        .transportControls.playFromMediaId(streamId, null)
                }
            }
        }

        inner class MyViewHolder(val stream: TextView,
                                 val itemClickedListener: : (MediaItemData) -> Unit) : RecyclerView.ViewHolder(stream){
            val name = StreamModel.stream.text

            init {

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(StreamModel::class.java)
        setSupportActionBar(findViewById(R.id.toolbar_view))

        mediaBrowser = MediaBrowserCompat(this,
            ComponentName(this, MediaPlaybackService::class.java),
            connectionCallback,
            null)

        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view).apply {
            adapter = StreamAdapter()
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

    fun updateToFireStoreThePositionData(positionData: HashMap<String, Any?>){
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .collection("positions")
            .add(positionData) }


    override fun onStart() {
        super.onStart()


        Log.e(Commons.TAG, "current user email: " + auth.currentUser?.email)
        if(auth.currentUser == null)  startSignInProcess()
        else if(!mediaBrowser.isConnected) {
            startService(Intent(this, MediaPlaybackService::class.java))
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