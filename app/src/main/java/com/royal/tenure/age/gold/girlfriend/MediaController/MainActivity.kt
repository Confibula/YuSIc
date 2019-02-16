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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.royal.tenure.age.gold.girlfriend.GetBitmap
import java.text.FieldPosition


val db : FirebaseFirestore = FirebaseFirestore.getInstance()

class MainActivity : AppCompatActivity() {

    // Todo: future improvements
    // Create a Queue for the mediaSession to allow skipping forward or backwards in the stream

    // Also do create a list of different streams. I'm fairly certain this code will revolve around
    // communication between the BrowserService and Browser my way of mediaChildren and root nodes.

    lateinit var googleSignInClient : GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    lateinit var mediaBrowser : MediaBrowserCompat

    val mControllerCallback = object : MediaControllerCompat.Callback(){
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            super.onMetadataChanged(metadata)
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
        }
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            Log.e(Constants.TAG, "PlaybackState: " + state?.state)
            super.onPlaybackStateChanged(state)
        }
    }

    private val mConnectionCallback = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            super.onConnected()
            Log.e(Constants.TAG, "ran onConnected")

            mediaBrowser.sessionToken.also { token ->
                val mMediaController = MediaControllerCompat(
                    this@MainActivity, token)
                MediaControllerCompat.setMediaController(this@MainActivity, mMediaController) }

            val mediaController = MediaControllerCompat
                .getMediaController(this@MainActivity)

            mediaController.registerCallback(mControllerCallback) }
    }

    fun recyclerView(){
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view).apply {
            adapter = recyclerViewAdapter
        }
    }

    var recyclerViewAdapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            TODO()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): RecyclerView.ViewHolder {
            val view : View = LayoutInflater.from(this@MainActivity)
                .inflate(R.layout.media_item, parent, false) as View

            TODO()
        }

        override fun getItemCount(): Int {
            TODO()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        fetchPositionData()
    }

    override fun onStart() {
        super.onStart()

        if(auth.currentUser == null) startSignInProcess()
        mediaBrowser.connect()
    }

    // Todo:
    // currently you are not using your position data.
    // You have it now neatly placed in your variable "globalPositionData",
    // but you haven't implemented it's usage yet.
    lateinit var globalPositionData : HashMap<String, Any>
    fun fetchPositionData(){
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .get().addOnSuccessListener { document ->
                val value: Map<String, Any> = document.data!!
                val positionData: HashMap<String, Any> = HashMap()
                val playPosition = value.get("playPosition") as Long
                val streamPosition = value.get("streamPosition") as String
                positionData["playPosition"] = playPosition
                positionData["streamPosition"] = streamPosition

                globalPositionData = positionData }
    }

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

    fun writeToFireStoreThePositionData(){
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .set(globalPositionData)
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
        writeToFireStoreThePositionData()
        super.onDestroy()
    }
}