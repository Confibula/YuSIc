package com.royal.tenure.age.gold.girlfriend.MediaController

import android.content.ComponentName
import android.os.*
import android.support.v4.media.MediaBrowserCompat
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.royal.tenure.age.gold.girlfriend.Constants
import com.royal.tenure.age.gold.girlfriend.MediaSession.MediaPlaybackService
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.royal.tenure.age.gold.girlfriend.R
import android.content.Intent
import android.graphics.Bitmap
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore


val db : FirebaseFirestore = FirebaseFirestore.getInstance()
class MainActivity : AppCompatActivity() {

    // Todo: future improvements
    // Create a Queue for the mediaSession to allow skipping forward or backwards in the stream

    // Also do create a list of different streams. I'm fairly certain this code will revolve around
    // communication between the BrowserService and Browser my way of mediaChildren and root nodes.

    lateinit var googleSignInClient : GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    var user: FirebaseUser? = null
    lateinit var mediaBrowser : MediaBrowserCompat
    lateinit var playPause : ImageView
    var playPosition: Long = 0
    var streamPosition: String? = "1"

    val mControllerCallback = object : MediaControllerCompat.Callback(){
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            var imageView : ImageView = findViewById<ImageView>(R.id.image)
            var textView : TextView = findViewById<TextView>(R.id.text_and_info)
            playPause = findViewById<ImageView>(R.id.play_pause)

            streamPosition = metadata?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).takeIf {
                it != null
            }?.apply {
                val title : String = metadata!!.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
                val creator: String = metadata!!.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
                val bitmap : Bitmap = metadata!!.getBitmap(MediaMetadataCompat.METADATA_KEY_ART)

                imageView.setImageBitmap(bitmap)
                textView.setText(title + "\n" + creator)
                playPause.setVisibility(View.VISIBLE)
            }

            super.onMetadataChanged(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playPosition = state?.position!!

            val whenUserLeftData = HashMap<String, Any?>()
            whenUserLeftData["playPosition"] = playPosition
            whenUserLeftData["streamPosition"] = streamPosition
            db.collection("users")
                .document(auth.currentUser!!.uid)
                .set(whenUserLeftData)

            when(state!!.state){
                PlaybackStateCompat.STATE_PAUSED -> {
                    playPause.setImageDrawable(
                        ContextCompat.getDrawable(this@MainActivity,
                            R.drawable.exo_controls_play))
                }

                PlaybackStateCompat.STATE_PLAYING -> {
                    playPause.setImageDrawable(
                        ContextCompat.getDrawable(this@MainActivity,
                            R.drawable.exo_controls_pause)) }
                else -> {
                    playPause.setVisibility(View.INVISIBLE)
                }
            }
            super.onPlaybackStateChanged(state)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
        }
    }

    private val mConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            Log.e(Constants.TAG, "ran onConnected")

            mediaBrowser.sessionToken.also { token ->
                val mMediaController = MediaControllerCompat(
                    this@MainActivity, token
                )

                //set it for the activity for later retrieval
                MediaControllerCompat.setMediaController(this@MainActivity, mMediaController)

                val bundle: Bundle = Bundle().also {
                    it.putLong("position", playPosition)
                }
                mMediaController.transportControls.prepareFromMediaId(streamPosition, bundle)
                buildPlayPause()
                mMediaController.registerCallback(mControllerCallback)

                var titleWhenStarted : TextView = findViewById(R.id.title_when_started)
                titleWhenStarted.setVisibility(View.INVISIBLE)
            }
        }

        private fun buildPlayPause() {
            val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
            playPause = findViewById<ImageView>(R.id.play_pause).apply {

                setOnClickListener {
                    var pbState = mediaController.playbackState.state
                    when (pbState) {
                        PlaybackStateCompat.STATE_PLAYING -> {
                            mediaController.transportControls.pause()
                        }
                        PlaybackStateCompat.STATE_PAUSED -> {
                            mediaController.transportControls.play()
                        }
                        else -> { } } } }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(Constants.TAG, "ran onCreate")

        setContentView(R.layout.activity_main)
        mediaBrowser = MediaBrowserCompat(this,
            ComponentName(this, MediaPlaybackService::class.java),
            mConnectionCallback,
            null)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser

    }

    override fun onStart() {
        super.onStart()

        Log.e(Constants.TAG, "ran onStart. User data: ")
        if(user == null) signIn()
        else {
            db.collection("users")
                .document(auth.currentUser!!.uid)
                .get().addOnSuccessListener { document ->
                    Log.e(Constants.TAG, "collected play position data")
                    val value: Map<String, Any> = document.data!!
                    playPosition = value.get("playPosition") as Long
                    streamPosition = value.get("streamPosition") as String

                    if(!mediaBrowser.isConnected) mediaBrowser.connect()
                }.addOnFailureListener{
                    Log.e(Constants.TAG, "failed to load position data: " + it)
                }
        }

    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, Constants.RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Constants.RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data!!)
            try {
                val account : GoogleSignInAccount? = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException){
                Log.e(Constants.TAG, "ApiException: " + e)
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount){
        val credential : AuthCredential = GoogleAuthProvider.getCredential(acct.idToken, null)

        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(Constants.TAG, "signInWithCredential:success")
                mediaBrowser.connect()
                user = auth.currentUser
            } else {
                Log.e(Constants.TAG, "Signing in was unsuccessful: " + task.exception) } }
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        mediaBrowser.disconnect()
        stopService(Intent(this, MediaPlaybackService::class.java))
        super.onDestroy()
    }
}