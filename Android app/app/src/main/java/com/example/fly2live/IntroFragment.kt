package com.example.fly2live

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fly2live.utils.createScaleAnimation
import com.example.fly2live.utils.logIn
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.games.Games
import kotlinx.coroutines.delay

class IntroFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_intro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bg = view.findViewById<ImageView>(R.id.bg)

        val animZoomIn = createScaleAnimation(1f, 1.5f, 1f, 1.5f, 0.5f, 0.5f)
        bg.startAnimation(animZoomIn)
    }


    override fun onStart() {
        super.onStart()

        // Check for existing Google Sign In account.
        // If the user is already signed in the GoogleSignInAccount will be non-null.
        val account = GoogleSignIn.getLastSignedInAccount(context)

        // Check if the user is already logged in
        logIn(account, context!!) { isLoggedIn ->
            // Delay transition to next fragment to perform part of the intro animation
            Handler(Looper.getMainLooper()).postDelayed({
                if (isLoggedIn) {
                    // User is already logged in.
                    // Show Google Games pop-up
                    Games.getGamesClient(context!!, account!!).setViewForPopups(activity?.findViewById(android.R.id.content)!!)

                    // Go to main fragment
                    findNavController().navigate(R.id.action_introFragment_to_mainFragment)
                }
                else
                    // User is not logged in
                    findNavController().navigate(R.id.action_introFragment_to_loginFragment)

            }, 1000)
        }
    }

}