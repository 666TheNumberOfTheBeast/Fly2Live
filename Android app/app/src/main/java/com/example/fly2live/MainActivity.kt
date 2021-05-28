package com.example.fly2live


import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager


class MainActivity : AppCompatActivity() {
    private var soundtrack: MediaPlayer? = null
    private var play_soundtrack: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        //setSupportActionBar(toolbar)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Load soundtrack for all fragments
        soundtrack = MediaPlayer.create(this, R.raw.soundtrack_main)
        soundtrack?.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.start()
        }
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

    // Play soundtrack mediaplayer
    private fun play() {
        if (soundtrack != null && !soundtrack!!.isPlaying)
            soundtrack!!.start()
    }

    // Stop soundtrack mediaplayer
    private fun stop() {
        if (soundtrack != null && soundtrack!!.isPlaying)
            soundtrack!!.pause()
    }

    // Play soundtrack (function for handling audio from fragments)
    fun startMainSoundtrack() {
        play_soundtrack = true
        play()
    }

    // Stop soundtrack(function for handling audio from fragments)
    fun stopMainSoundtrack() {
        play_soundtrack = false
        stop()
    }

    override fun onStart() {
        super.onStart()

        if (play_soundtrack)
            play()

    }

    override fun onStop() {
        stop()

        super.onStop()
    }

}