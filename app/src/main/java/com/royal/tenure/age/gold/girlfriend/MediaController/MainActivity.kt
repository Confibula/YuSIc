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
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.ImageView
import androidx.core.app.ActivityCompat.startActivityForResult
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.FirebaseAuthCredentialsProvider
import java.io.IOException


class MainActivity : AppCompatActivity() {

    // Todo: general
    // Create a Queue for the mediaSession to allow skipping forward or backwards in the stream
    
    // Also do create a list of different streams. I'm fairly certain this code will revolve around
    // communication between the BrowserService and Browser my way of mediaChildren and root nodes.

    lateinit var mGoogleSignInClient : GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    var user: FirebaseUser? = null
    val db : FirebaseFirestore = FirebaseFirestore.getInstance()

    lateinit var mMediaBrowserCompat : MediaBrowserCompat
    lateinit var mControllerCallback: MyControllerCallback
    lateinit var playPause : ImageView

    private val mConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()

            mMediaBrowserCompat.sessionToken.also { token ->
                val mMediaController = MediaControllerCompat(
                    this@MainActivity, token
                )

                //set it for the activity for later retrieval
                MediaControllerCompat.setMediaController(this@MainActivity, mMediaController)

                // Todo: remember where user left
                // In the bundle, write song position. And in the id, write users specific ID
                // This is a task that will make it so that the playback recognizes where the user last left
                val bundle: Bundle = Bundle().also {
                    it.putLong("position", 0)
                }
                mMediaController.transportControls.prepareFromMediaId("1", bundle)

                buildPlayPause()
                mControllerCallback = MyControllerCallback(this@MainActivity)
                mMediaController.registerCallback(mControllerCallback)
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.royal.tenure.age.gold.girlfriend.R.layout.activity_main)

        mMediaBrowserCompat = MediaBrowserCompat(this,
            ComponentName(this, MediaPlaybackService::class.java),
            mConnectionCallback,
            null)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_id))
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        auth = FirebaseAuth.getInstance()

    }

    override fun onStart() {
        super.onStart()

        user = auth.currentUser
        if(user != null)
            if(mMediaBrowserCompat.isConnected != true)
                mMediaBrowserCompat.connect()
        else signIn()
    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, Constants.RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
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
                mMediaBrowserCompat.connect()
                user = auth.currentUser
            } else {
                Log.e(Constants.TAG, "Signing in was unsuccessful: " + task.exception)
            }
        }
    }

    override fun onStop() {
        super.onStop()
    }
}
