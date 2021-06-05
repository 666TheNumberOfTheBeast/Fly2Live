package com.example.fly2live

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import android.content.res.Configuration
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.example.fly2live.configuration.Configuration as myConfiguration


class MainFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access control
        val account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account == null) {
            findNavController().navigate(R.id.action_mainFragment_to_loginFragment)
            return
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cockpit = view.findViewById<ImageView>(R.id.cockpit)
        val logo = view.findViewById<ImageView>(R.id.logo)

        val btnSinglePlayer = view.findViewById<TextView>(R.id.button_single_player)
        val btnMultiplayer  = view.findViewById<TextView>(R.id.button_multiplayer)
        val btnStats        = view.findViewById<TextView>(R.id.button_stats)
        val btnSettings     = view.findViewById<TextView>(R.id.button_settings)

        // Set logo scale based on smartphone orientation
        val orientation = resources.configuration.orientation

        // Animate the logo only the first time the fragment is created
        if (savedInstanceState == null) {
            val animZoomOut =
                if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                    createScaleAnimation(1f, 0.4f, 1f, 0.4f)
                else
                    createScaleAnimation(1f, 0.5f, 1f, 0.5f)

            animZoomOut.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation) {}

                override fun onAnimationEnd(animation: Animation) {
                    cockpit.setImageResource(R.drawable.cockpit_on)

                    Handler(Looper.myLooper()!!).postDelayed({
                        btnSinglePlayer.visibility = View.VISIBLE
                        btnMultiplayer.visibility  = View.VISIBLE
                        btnStats.visibility        = View.VISIBLE
                        btnSettings.visibility     = View.VISIBLE
                    }, 300)
                }

                override fun onAnimationRepeat(animation: Animation) {}
            })

            logo.startAnimation(animZoomOut)
        }
        else {
            // Non funziona
            /*var scaleX = 0.5f
            var scaleY = 0.5f

            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                scaleX = 0.4f
                scaleY = 0.4f
            }

            logo.scaleType = ImageView.ScaleType.MATRIX
            logo.imageMatrix = Matrix().apply {
                setScale(scaleX, scaleY, 0.5f * logo.drawable.intrinsicWidth, 0.3f * logo.drawable.intrinsicHeight)
                //setScale(scaleX, scaleY, 0.5f * logo.width, 0.3f * logo.height)
            }*/


            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                logo.scaleX = 0.4f
                logo.scaleY = 0.4f
                logo.translationY = -100f
            }
            else {
                logo.scaleX = 0.5f
                logo.scaleY = 0.5f
                logo.translationY = -view.height / 5f
            }


            /*val animZoomOut =
                if (orientation == Configuration.ORIENTATION_LANDSCAPE)
                    createScaleAnimation(1f, 0.4f, 1f, 0.4f)
                else
                    createScaleAnimation(1f, 0.5f, 1f, 0.5f)

            logo.startAnimation(animZoomOut)*/


            cockpit.setImageResource(R.drawable.cockpit_on)

            btnSinglePlayer.visibility = View.VISIBLE
            btnMultiplayer.visibility  = View.VISIBLE
            btnStats.visibility        = View.VISIBLE
            btnSettings.visibility     = View.VISIBLE
        }

        // Set buttons listeners
        btnSinglePlayer.setOnClickListener{
            myConfiguration.MULTIPLAYER = false
            findNavController().navigate(R.id.action_mainFragment_to_scenariosFragment)
        }

        btnMultiplayer.setOnClickListener{
            myConfiguration.MULTIPLAYER = true
            findNavController().navigate(R.id.action_mainFragment_to_scenariosFragment)
        }

        btnStats.setOnClickListener{
            findNavController().navigate(R.id.action_mainFragment_to_statsFragment)
        }

        btnSettings.setOnClickListener{
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
        }
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

    // Start main soundtrack (if stopped coming back from game or game over fragments or renabled in settings) based on audio preference
    override fun onResume() {
        super.onResume()
        val sharedPref = context?.getSharedPreferences(getString(R.string.shared_preferences_name), Context.MODE_PRIVATE)!!
        val isAudioEnabled = sharedPref.getBoolean(getString(R.string.shared_preference_audio), true)

        if (isAudioEnabled)
            (activity as MainActivity).startMainSoundtrack()
    }

}