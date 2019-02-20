package com.royal.tenure.age.gold.girlfriend

import android.content.ComponentName
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import android.content.Intent
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.*
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.stream.view.*

class MainActivity : AppCompatActivity() {
    private val db : FirebaseFirestore = FirebaseFirestore.getInstance()
    private lateinit var googleSignInClient : GoogleSignInClient
    private lateinit var mediaBrowser : MediaBrowserCompat
    private lateinit var viewModel: MyViewModel
    private lateinit var playback: PlaybackStateCompat
    private lateinit var metadata : MediaMetadataCompat

    val controllerCallback = object : MediaControllerCompat.Callback(){

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            metadata?.also {
                Glide
                    .with(this@MainActivity)
                    .load(it.mediaUri)
                    .into(findViewById(R.id.image_view))

                supportActionBar?.apply {
                    title = it.title
                }

                this@MainActivity.metadata = it
            }
        }

        override fun onPlaybackStateChanged(playback: PlaybackStateCompat?) {
            playback?.also { this@MainActivity.playback = it }

            val positionData = HashMap<String, Any?>()
            metadata.also {
                positionData.also { data ->
                    data["streamPoint"] = Integer.parseInt(it.id)
                    data["streamName"] = it.genre } }
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
            viewModel.put(streams)

        }
    }

    inner class MyViewModel : ViewModel() {
        private lateinit var streams: MutableLiveData<MutableList<MediaBrowserCompat.MediaItem>>

        fun fetch() : MutableLiveData<MutableList<MediaBrowserCompat.MediaItem>> {
            return streams
        }


        override fun onCleared() {
            super.onCleared()
            mediaBrowser.unsubscribe(Commons.ROOT_ID)
        }

        fun put(streams: MutableList<MediaBrowserCompat.MediaItem>){
            this.streams.postValue(streams)
        }
    }

    inner class MyAdapter(private val model: MyViewModel,
                    private val context: LifecycleOwner) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
        private lateinit var streams: MutableList<MediaBrowserCompat.MediaItem>

        init {
            model.fetch().observe(
                context,
                Observer<MutableList<MediaBrowserCompat.MediaItem>>{
                    streams = it
            })
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            val streamView = LayoutInflater.from(parent.context)
                .inflate(R.layout.stream, parent, false) as TextView

            return MyViewHolder(streamView)
        }

        override fun getItemCount(): Int {
            return streams.size
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val theStream = holder.stream
            val streamId = streams[position].description.mediaId
            theStream.text = streamId
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

        inner class MyViewHolder(val stream: TextView) : RecyclerView.ViewHolder(stream)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        MenuInflater(this).inflate(R.menu.menu_main, menu)
        // Probably tells it to create. False means 'not create'
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item?.itemId

        val controller = MediaControllerCompat.getMediaController(this)
        when(id){
            R.id.play_button -> {
                if(playback.isPlayEnabled){
                    controller.transportControls.play()
                    item.icon = getDrawable(R.drawable.exo_controls_pause)
                }
                if(playback.isPlaying){
                    controller.transportControls.pause()
                    item.icon = getDrawable(R.drawable.exo_controls_play)
                }
            }
            R.id.repeat_button -> {
                if(playback.state == PlaybackStateCompat.REPEAT_MODE_ONE) {
                    controller.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_NONE)
                    item.icon = getDrawable(R.drawable.exo_controls_repeat_off)
                } else {
                    controller.transportControls.setRepeatMode(PlaybackStateCompat.REPEAT_MODE_ONE)
                    item.icon = getDrawable(R.drawable.exo_controls_repeat_one)
                }
            }
            else -> return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(MyViewModel::class.java)

        findViewById<RecyclerView>(R.id.recycler_view).apply {
            adapter = MyAdapter(viewModel, this@MainActivity)
            layoutManager = LinearLayoutManager(
                this@MainActivity,
                LinearLayoutManager.HORIZONTAL,
                false)
        }

        setSupportActionBar(findViewById(R.id.toolbar_view))


        mediaBrowser = MediaBrowserCompat(this,
            ComponentName(this, MediaPlaybackService::class.java),
            connectionCallback,
            null)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

    }

    fun updateToFireStoreThePositionData(positionData: HashMap<String, Any?>){
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .set(positionData) }


    override fun onStart() {
        super.onStart()

        auth.currentUser?.let { startSignInProcess() }

        if(!mediaBrowser.isConnected) {
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