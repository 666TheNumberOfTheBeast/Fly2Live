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
        soundtrack = MediaPlayer.create(context, R.raw.soundtrack)
        soundtrack?.setOnPreparedListener { mp ->
            mp.isLooping = true
            mp.start()
        }

        // Inflate the layout for this fragment
        return GameView(context)
    }

    public fun gameOver() {
        if (soundtrack != null && soundtrack!!.isPlaying)
            soundtrack!!.pause()

        findNavController().navigate(R.id.action_gameFragment_to_gameOverFragment)
    }

    /*override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //findNavController().popBackStack( R.id.loadingFragment, true)
    }*/


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