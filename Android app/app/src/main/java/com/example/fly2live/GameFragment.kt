package com.example.fly2live

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import android.media.MediaPlayer

class GameFragment : Fragment() {
    private lateinit var gameView: View
    private var soundtrack: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        gameView = GameView(context)

        // Load soundtrack
        soundtrack = MediaPlayer.create(context, R.raw.soundtrack_gameplay)
        soundtrack?.setOnPreparedListener { mp ->
            // Stop main soundtrack
            (activity as MainActivity).stopMainSoundtrack()

            // Start gameplay soundtrack
            mp.isLooping = true
            mp.start()
        }

        // Inflate the layout for this fragment
        return GameView(context)
    }

    fun gameOver(score: Long, winner: Boolean) {
        if (soundtrack != null && soundtrack!!.isPlaying)
            soundtrack!!.pause()

        val bundle = Bundle()
        bundle.putLong("score", score)
        bundle.putBoolean("winner", winner)

        findNavController().navigate(R.id.action_gameFragment_to_gameOverFragment, bundle)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findNavController().popBackStack(R.id.loadingFragment, true)
    }


    override fun onStart() {
        super.onStart()
        if (soundtrack != null && !soundtrack!!.isPlaying)
            soundtrack!!.start()
    }

    override fun onStop() {
        if (soundtrack != null && soundtrack!!.isPlaying)
            soundtrack!!.pause()

        super.onStop()
    }

}