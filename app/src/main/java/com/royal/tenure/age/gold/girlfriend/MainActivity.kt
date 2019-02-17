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
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

val db : FirebaseFirestore = FirebaseFirestore.getInstance()
val auth: FirebaseAuth = FirebaseAuth.getInstance()

class MainActivity : AppCompatActivity() {

    lateinit var googleSignInClient : GoogleSignInClient
    lateinit var mediaBrowser : MediaBrowserCompat

    val controllerCallback = object : MediaControllerCompat.Callback(){
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)

            Log.e(Constants.TAG, "metadata: " + metadata
                ?.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID))
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
        }
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            Log.e(Constants.TAG, "PlaybackState: " + state?.state)



            super.onPlaybackStateChanged(state)
        }
    }

    private val connectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            Log.e(Constants.TAG, "ran onConnected")

            mediaBrowser.sessionToken.also { token ->
                val controller = MediaControllerCompat(
                    this@MainActivity, token)
                MediaControllerCompat.setMediaController(this@MainActivity, controller) }

            val controller = MediaControllerCompat
                .getMediaController(this@MainActivity)

            controller.registerCallback(controllerCallback) }
    }

    val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback(){
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
        }
    }

    fun recyclerView(){
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view).apply {

            // Needs DOM knowledge
            adapter
            layoutManager
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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



    override fun onStart() {
        super.onStart()

        if(auth.currentUser == null) startSignInProcess()
        startService(Intent(this, MediaPlaybackService::class.java))
        mediaBrowser.connect() }

    override fun onStop() {
        mediaBrowser.disconnect()
        super.onStop() }

    override fun onPause() {
        mediaBrowser.disconnect()
        super.onPause() }

    private fun startSignInProcess() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, Constants.RC_SIGN_IN)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // if the launch request is a google sign in
        if (requestCode == Constants.RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data!!)
            try {
                val account : GoogleSignInAccount? = task.getResult(ApiException::class.java)
                firebaseAuthenticationWithGoogle(account!!)
            } catch (e: ApiException){
                Log.e(Constants.TAG, "ApiException: " + e) } }
    }

    private fun firebaseAuthenticationWithGoogle(acct: GoogleSignInAccount){
        val credential : AuthCredential = GoogleAuthProvider.getCredential(acct.idToken, null)

        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                mediaBrowser.connect()
            }
            else { } }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}