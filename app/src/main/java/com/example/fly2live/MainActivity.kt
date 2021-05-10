package com.example.fly2live


import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import java.io.IOException
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.net.Uri


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






/*import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.media.MediaPlayer


class MainActivity : AppCompatActivity() {
    private lateinit var mView: View
    private lateinit var mThread: Thread

    private lateinit var soundtrack: MediaPlayer
    private lateinit var game_over_sound: MediaPlayer

    private var game_over_ready = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mView = GameView(this)
        setContentView(mView)

        // Load soundtrack
        soundtrack = MediaPlayer.create(this, R.raw.soundtrack)

        if (soundtrack != null)
            soundtrack.setOnPreparedListener { mp ->
                mp.isLooping = true
                mp.start()
            }

        // Load game over sound
        game_over_sound = MediaPlayer.create(this, R.raw.game_over)
        if (game_over_sound != null)
            game_over_sound.setOnPreparedListener { _ ->
                game_over_ready = true
            }
    }

    public fun gameOver() {
        if (soundtrack != null && soundtrack.isPlaying)
            soundtrack.pause()

        if (game_over_ready)
            game_over_sound.start()

        mThread.interrupt()
    }

    override fun onStart() {
        super.onStart()

        if (soundtrack != null && !soundtrack.isPlaying)
            soundtrack.start()

        mThread = object : Thread() {
            override fun run() {
                // wait and invalidate view until interrupted
                while (true) {
                    try {
                        Thread.sleep(50)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                        break // get out if interrupted
                    }
                    mView.postInvalidate()
                }
            }
        }

        mThread.start()
    }

    override fun onStop() {
        mThread.interrupt()

        if (soundtrack != null && soundtrack.isPlaying)
            soundtrack.pause()

        if (game_over_sound != null && game_over_sound.isPlaying)
            game_over_sound.pause()

        super.onStop()
    }
}*/