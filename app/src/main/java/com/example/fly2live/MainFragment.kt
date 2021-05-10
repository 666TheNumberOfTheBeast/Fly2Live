package com.example.fly2live

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import android.content.res.Configuration
import android.os.Handler
import android.widget.TextView
import com.example.fly2live.configuration.Configuration as myConfiguration
import androidx.core.os.HandlerCompat.postDelayed



class MainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logo = view.findViewById<ImageView>(R.id.logo)
        val btnSinglePlayer = view.findViewById<TextView>(R.id.button_single_player)
        val btnMultiplayer  = view.findViewById<TextView>(R.id.button_multiplayer)
        val btnStats        = view.findViewById<TextView>(R.id.button_stats)

        val cockpit = view.findViewById<ImageView>(R.id.cockpit)

        // Create animation based on smartphone orientation
        val orientation = resources.configuration.orientation
        val animZoomOut = if (orientation == Configuration.ORIENTATION_LANDSCAPE)
            createScaleAnimation(1f, 0.4f, 1f, 0.4f)
        else
            createScaleAnimation(1f, 0.5f, 1f, 0.5f)

        animZoomOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {}

            override fun onAnimationEnd(animation: Animation) {
                cockpit.setImageResource(R.drawable.cockpit_on)

                Handler().postDelayed(Runnable {
                    btnSinglePlayer.visibility = View.VISIBLE
                    btnMultiplayer.visibility  = View.VISIBLE
                    btnStats.visibility        = View.VISIBLE
                }, 300)
            }

            override fun onAnimationRepeat(animation: Animation) {}
        })

        logo.startAnimation(animZoomOut)

        // Set buttons listeners
        btnSinglePlayer.setOnClickListener{
            myConfiguration.MULTIPLAYER = false
            findNavController().navigate(R.id.action_mainFragment_to_gameFragment)
        }

        btnMultiplayer.setOnClickListener{
            myConfiguration.MULTIPLAYER = true
            findNavController().navigate(R.id.action_mainFragment_to_loadingFragment)
        }

        btnStats.setOnClickListener{
            findNavController().navigate(R.id.action_mainFragment_to_statsFragment)
        }

        // Start main soundtrack if stopped (coming back from game or game over fragments)
        (activity as MainActivity).startMainSoundtrack()
    }

    private fun createScaleAnimation(fromX: Float, toX: Float, fromY: Float, toY: Float): ScaleAnimation {
        val anim = ScaleAnimation(fromX, toX,
            fromY, toY,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.3f)

        anim.fillAfter = true
        anim.duration  = 2500

        return anim
    }

}