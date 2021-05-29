package com.example.fly2live

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.example.fly2live.configuration.Configuration
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.Thread.sleep


class GameEndFragment : Fragment() {
    private var game_end_sound: MediaPlayer? = null

    private var score  = 0L
    private var winner = false

    private var new_high_score = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access control
        val account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account == null) {
            // Attempt to pop the controller's back stack back to a specific destination
            findNavController().popBackStack(R.id.mainFragment, false)
            return
        }

        // Get passed arguments the first time the fragment is created
        if (savedInstanceState == null) {
            score  = arguments!!.getLong("score")
            winner = arguments!!.getBoolean("winner")


            val mLeaderboardClient = Games.getLeaderboardsClient(context!!, account)
            //mLeaderboardClient.submitScore(getString(R.string.leaderboard_best_score_id), score)
            //mLeaderboardClient.submitScore(getString(R.string.leaderboard_win_percentage_id), winner)

            // TODO: retrieve player high score

            /*if (score > high_score)
                new_high_score = true*/
        }

        val sharedPref = context?.getSharedPreferences(getString(R.string.shared_preferences_name), Context.MODE_PRIVATE)
        val isAudioEnabled = sharedPref?.getBoolean(getString(R.string.shared_preference_audio), true)!!

        // Load & Start the audio only if the preference is enabled
        if (isAudioEnabled) {
            // Load the correct sound
            if (Configuration.MULTIPLAYER && winner)
                loadSound(R.raw.soundtrack_game_win)
            else
                loadSound(R.raw.soundtrack_game_over)
        }

    }

        override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game_end, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Check if the player has won in multiplayer mode
        if (Configuration.MULTIPLAYER && winner)
            view.findViewById<TextView>(R.id.game_status_text).text = getString(R.string.you_win)

        val scoreView = view.findViewById<TextView>(R.id.score)

        // Check if the player has achieved a new high score
        if (new_high_score) {
            scoreView.text = "NEW HIGH SCORE: " + score.toString()

            // A LifecycleScope is defined for each Lifecycle object.
            // Any coroutine launched in this scope is canceled when the Lifecycle is destroyed.
            lifecycleScope.launch(Dispatchers.Default) {
                while (true) {
                    sleep(700)

                    // Blink the high score text
                    activity?.runOnUiThread(kotlinx.coroutines.Runnable {
                        if (scoreView.visibility == View.VISIBLE) scoreView.visibility =
                            View.INVISIBLE
                        else scoreView.visibility = View.VISIBLE
                    })
                }
            }
        }
        else
            scoreView.text = scoreView.text.toString() + " " + score.toString()


        view.findViewById<TextView>(R.id.restart).setOnClickListener{
            findNavController().navigate(R.id.action_gameEndFragment_to_gameFragment)
        }

        view.findViewById<TextView>(R.id.go_to_menu).setOnClickListener{
            findNavController().navigate(R.id.action_gameEndFragment_to_mainFragment)
        }

        // Check if the player has won a multiplayer game or has achieved a new high score
        if (winner || new_high_score) {
            val shareView = view.findViewById<TextView>(R.id.share)

            // Make the share button visible and add listener
            shareView.visibility = View.VISIBLE
            shareView.setOnClickListener {
                val sharingIntent = Intent(Intent.ACTION_SEND)
                sharingIntent.type = "text/plain"

                // TODO: add link to open stats page at the correct position

                val shareBody: String
                if (winner && new_high_score)
                    shareBody = "I've won with a new high score of " + score + " on Fly2Live - multiplayer"
                else if (winner)
                    shareBody = "I've won with a score of " + score + " on Fly2Live - multiplayer"
                else
                    shareBody = "I've achieved a new high score of " + score + " on Fly2Live - single player"

                sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Fly2Live winner")
                sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody)

                startActivity(Intent.createChooser(sharingIntent, "Share via"))
            }
        }

    }

    // Load the passed soundtrack
    private fun loadSound(trackID: Int) {
        game_end_sound = MediaPlayer.create(context, trackID)
        game_end_sound?.setOnPreparedListener { mp ->
            // Stop main soundtrack
            (activity as MainActivity).stopMainSoundtrack()

            // Start game over soundtrack
            mp.start()
        }
    }

    override fun onStop() {
        if (game_end_sound != null && game_end_sound!!.isPlaying)
            game_end_sound!!.pause()

        super.onStop()
    }

}