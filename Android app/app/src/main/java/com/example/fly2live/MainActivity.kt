package com.example.fly2live


import android.content.Context
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import io.realm.Realm


class MainActivity : AppCompatActivity() {
    private var soundtrack: MediaPlayer? = null

    // Variable for handling the audio from the fragments
    private var playSoundtrack: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        //setSupportActionBar(toolbar)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Load soundtrack for all fragments
        soundtrack = MediaPlayer.create(this, R.raw.soundtrack_main)
        soundtrack?.setOnPreparedListener { mp ->
            mp.isLooping = true
        }

        // Initialize the Realm library.
        // Your application should initialize Realm just once each time the application runs
        Realm.init(this)
    }

    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }*/

    // Play soundtrack mediaplayer if is not playing
    private fun play() {
        if (soundtrack != null && !soundtrack!!.isPlaying)
            soundtrack!!.start()
    }

    // Stop soundtrack mediaplayer if is playing
    private fun stop() {
        if (soundtrack != null && soundtrack!!.isPlaying)
            soundtrack!!.pause()
    }

    // Play soundtrack (function for handling audio from fragments)
    fun startMainSoundtrack() {
        playSoundtrack = true
        play()
    }

    // Stop soundtrack(function for handling audio from fragments)
    fun stopMainSoundtrack() {
        playSoundtrack = false
        stop()
    }

    // Start main soundtrack based on audio preference (useful for login fragment)
    override fun onStart() {
        super.onStart()

        val sharedPref = getSharedPreferences(getString(R.string.shared_preferences_name), Context.MODE_PRIVATE)!!
        val isAudioEnabled = sharedPref.getBoolean(getString(R.string.shared_preference_audio), true)

        if (playSoundtrack && isAudioEnabled)
            play()
    }

    override fun onStop() {
        stop()

        super.onStop()
    }

}