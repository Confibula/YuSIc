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
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.tasks.Task
import com.google.android.gms.common.api.ApiException






class MainActivity : AppCompatActivity() {

    lateinit var mMediaBrowserCompat : MediaBrowserCompat
    lateinit var mGoogleSignInClient : GoogleSignInClient
    lateinit var mControllerCallback: MyControllerCallback
    lateinit var playPause : ImageView

    val mConnectionCallback = object : MediaBrowserCompat.ConnectionCallback(){
        override fun onConnected() {
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
                val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
                mControllerCallback = MyControllerCallback(this@MainActivity)
                mediaController.registerCallback(mControllerCallback)
            }
        }

        override fun onConnectionFailed() {
            super.onConnectionFailed()
        }

        override fun onConnectionSuspended() {
            super.onConnectionSuspended()
        }

        private fun buildPlayPause(){
            val mediaController = MediaControllerCompat.getMediaController(this@MainActivity)
            playPause = findViewById<ImageView>(R.id.play_pause).apply {

                setOnClickListener {
                    var pbState = mediaController.playbackState.state
                    when(pbState){
                        PlaybackStateCompat.STATE_PLAYING -> {
                            mediaController.transportControls.pause()
                        }
                        PlaybackStateCompat.STATE_PAUSED -> {
                            mediaController.transportControls.play()
                        }
                        else -> { } } } }.also { it.setVisibility(View.INVISIBLE) }
        }
        private fun buildForwardBackward(){
            
        }
    }

    // TODO: google login functionality
    // Add a login functionality

    // Todo: add a notification compat
    // Also add a notificationCompat for when leaving the UI
    // Turns out, this functionality could actually be left out.
    // It's not imperative, thought perhaps necessary


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.royal.tenure.age.gold.girlfriend.R.layout.activity_main)

        mMediaBrowserCompat = MediaBrowserCompat(this,
            ComponentName(this, MediaPlaybackService::class.java),
            mConnectionCallback,
            null)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

    }

    override fun onStart() {
        super.onStart()

        if(mMediaBrowserCompat.isConnected != true){
            mMediaBrowserCompat.connect()
        }

        val account = GoogleSignIn.getLastSignedInAccount(this)
        //updateUI()


    }

    private fun signIn() {
        val signInIntent = mGoogleSignInClient.getSignInIntent()
        startActivityForResult(signInIntent, Constants.RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == Constants.RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Signed in successfully, show authenticated UI.
            //updateUI()

        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(Constants.TAG, "signInResult:failed code=" + e.statusCode)
            //updateUI()
        }
    }

    override fun onStop() {
        super.onStop()
    }
}
