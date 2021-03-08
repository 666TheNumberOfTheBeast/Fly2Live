package com.example.fly2live

import android.media.MediaPlayer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.fragment.findNavController

class GameOverFragment : Fragment() {
    private var game_over_sound: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Load game over sound
        game_over_sound = MediaPlayer.create(context, R.raw.game_over)
        game_over_sound?.setOnPreparedListener { mp ->
            mp.start()
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game_over, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.restart).setOnClickListener{
            findNavController().navigate(R.id.action_gameOverFragment_to_gameFragment)
        }

        view.findViewById<TextView>(R.id.go_to_menu).setOnClickListener{
            findNavController().navigate(R.id.action_gameOverFragment_to_mainFragment)
        }
    }

    override fun onStop() {
        if (game_over_sound != null && game_over_sound!!.isPlaying)
            game_over_sound!!.pause()

        super.onStop()
    }

}