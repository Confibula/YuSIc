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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    val db : FirebaseFirestore = FirebaseFirestore.getInstance()

    // Todo:
    // Add support for a toolbar that shows a PlayButton and a looping option for the current song.
    // Let this toolbar be on the bottom of the MainActivity

    private lateinit var googleSignInClient : GoogleSignInClient
    private lateinit var mediaBrowser : MediaBrowserCompat

    val controllerCallback = object : MediaControllerCompat.Callback(){
        lateinit var metadata : MediaMetadataCompat

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
            metadata?.let { this.metadata = it}
        }

        override fun onPlaybackStateChanged(playback: PlaybackStateCompat?) {

            val positionData = HashMap<String, Any?>()
            metadata.let {
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



        }
    }

    val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback(){
        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
            super.onChildrenLoaded(parentId, children)
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

    override fun onPause() {
        super.onPause()
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

    override fun onDestroy() {
        super.onDestroy()
    }
}