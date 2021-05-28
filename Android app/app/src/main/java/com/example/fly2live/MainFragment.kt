package com.example.fly2live

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
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.games.Games
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
        val btnLogout       = view.findViewById<ImageView>(R.id.button_logout)

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

                Handler(Looper.myLooper()!!).postDelayed( {
                    btnSinglePlayer.visibility = View.VISIBLE
                    btnMultiplayer.visibility  = View.VISIBLE
                    btnStats.visibility        = View.VISIBLE
                    btnLogout.visibility       = View.VISIBLE
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

        btnLogout.setOnClickListener{
            signOut()
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

    private fun signOut() {
        // Configure sign-in to request the user's ID, email address, and basic profile.
        // ID and basic profile are included in DEFAULT_SIGN_IN
        /*val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()*/

        // Configure Google Games sign-in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
            .build()

        // Build a GoogleSignInClient with the options specified by gso
        val mGoogleSignInClient = GoogleSignIn.getClient(context!!, gso)

        mGoogleSignInClient.signOut().addOnCompleteListener{
            findNavController().navigate(R.id.action_mainFragment_to_loginFragment)

            Toast.makeText(context, "A presto", Toast.LENGTH_SHORT).show()
        }
    }

}