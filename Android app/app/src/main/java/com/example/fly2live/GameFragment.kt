package com.example.fly2live

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import android.media.MediaPlayer
import android.util.Log
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.example.fly2live.configuration.Configuration.Companion.MULTIPLAYER
import com.example.fly2live.configuration.Configuration.Companion.WINNER_UNDEFINED
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.lang.Thread.sleep

class GameFragment : Fragment() {
    private var account: GoogleSignInAccount? = null

    private var soundtrack: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access control
        account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account == null) {
            // Attempt to pop the controller's back stack back to a specific destination
            findNavController().popBackStack(R.id.mainFragment, false)
            return
        } // DISABLE DURING DEVELOPING TO SAVE API CALLS
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Access control (continue)
        if (account == null)
            return null

        val sharedPref = context?.getSharedPreferences(getString(R.string.shared_preferences_name), Context.MODE_PRIVATE)
        val isAudioEnabled = sharedPref?.getBoolean(getString(R.string.shared_preference_audio), true)!!

        // Load & Start the audio only if the preference is enabled
        if (isAudioEnabled) {
            // Load soundtrack
            soundtrack = MediaPlayer.create(context, R.raw.soundtrack_gameplay)
            soundtrack?.setOnPreparedListener { mp ->
                // Stop main soundtrack
                (activity as MainActivity).stopMainSoundtrack()

                // Set looping gameplay soundtrack
                mp.isLooping = true

                // Check if a state has to be restored
                if (savedInstanceState != null) {
                    // Restore gameplay soundtrack position
                    val pos = savedInstanceState.getInt("gameplaySoundtrackPosition")
                    mp.seekTo(pos)
                }

                mp.start()
            }
        }

        // Remove LoadingFragment from the backstack if multiplayer mode
        if (MULTIPLAYER)
            try {
                findNavController().popBackStack(R.id.loadingFragment, true)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }

        val view: View = if (MULTIPLAYER) GameViewMultiplayer(context, this) else GameView(context, this)

        // Set view ID to override onSaveInstanceState and onRestoreInstanceState of the view
        view.id = R.id.game_view

        // Inflate the layout for this fragment
        return view
    }

    // GameView call this function when the game ends
    // (single player -> game over, multiplayer -> undefined, game winner, loser or draw)
    fun gameEnd(score: Long, winner: Int) {
        stopSoundtrack()

        // Check if winner is undefined in multiplayer mode
        if (MULTIPLAYER && winner == WINNER_UNDEFINED) {
            // Go back to the main fragment using a coroutine tied to the fragment lifecycle
            // in order to avoid crashes
            lifecycleScope.launchWhenResumed {
                findNavController().popBackStack(R.id.mainFragment, false)
            }

            return
        }

        // Otherwise go to game end fragment
        val bundle = Bundle()
        bundle.putLong("score", score)
        bundle.putInt("winner", winner)

        // Try to go to next fragment in order to avoid possible crashes
        try {
            findNavController().navigate(R.id.action_gameFragment_to_gameEndFragment, bundle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun playSoundtrack() {
        if (soundtrack != null && !soundtrack!!.isPlaying)
            soundtrack!!.start()
    }

    private fun stopSoundtrack() {
        if (soundtrack != null && soundtrack!!.isPlaying)
            soundtrack!!.pause()
    }

    override fun onStart() {
        super.onStart()

        playSoundtrack()
    }

    override fun onStop() {
        stopSoundtrack()

        super.onStop()
    }

    // Save gameplay soundtrack current position and pause it
    override fun onSaveInstanceState(outState: Bundle) {
        //Log.d("save", "save state game fragment")
        if (soundtrack != null) {
            outState.putInt("gameplaySoundtrackPosition", soundtrack!!.currentPosition)

            stopSoundtrack()
        }

        super.onSaveInstanceState(outState)
    }

}