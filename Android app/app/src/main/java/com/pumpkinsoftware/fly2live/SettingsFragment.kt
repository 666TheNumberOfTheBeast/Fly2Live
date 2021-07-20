package com.pumpkinsoftware.fly2live

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.pumpkinsoftware.fly2live.utils.adaptBackButton2notch
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration


class SettingsFragment : Fragment() {
    private var account: GoogleSignInAccount? = null

    private lateinit var sharedPref: SharedPreferences

    private lateinit var btnAudio: TextView
    private var isAudioEnabled = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access control
        account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account == null) {
            // Attempt to pop the controller's back stack back to a specific destination
            findNavController().popBackStack(R.id.mainFragment, false)
            return
        }

        sharedPref = context?.getSharedPreferences(getString(R.string.shared_preferences_name), MODE_PRIVATE)!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Access control (continue)
        if (account == null)
            return

        var notchRects: List<Rect>? = null

        // Adapt top margin to notch
        view.setOnApplyWindowInsetsListener { _, windowInsets ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Get notch margin top value (0 if notch is not present)
                val notchMarginTop = windowInsets.displayCutout?.safeInsetTop ?: 0

                // Check if notch is present and device is in portrait orientation
                if (notchMarginTop > 0 && resources.configuration.orientation == ORIENTATION_PORTRAIT) {
                    // Display the text below the notch
                    val title = view.findViewById<TextView>(R.id.menu_title)

                    // Update top margin parameter once
                    val params = title.layoutParams as ViewGroup.MarginLayoutParams
                    if (params.topMargin < notchMarginTop)
                        params.topMargin += notchMarginTop
                }

                // Get notch rects
                notchRects = windowInsets.displayCutout?.boundingRects
            }

            return@setOnApplyWindowInsetsListener windowInsets
        }

        val btnBack    = view.findViewById<ImageView>(R.id.back_button)
        btnAudio       = view.findViewById(R.id.button_audio)
        val btnLogout  = view.findViewById<TextView>(R.id.button_logout)
        val btnCredits = view.findViewById<TextView>(R.id.button_credits)

        isAudioEnabled = sharedPref.getBoolean(getString(R.string.shared_preference_audio), true)
        handleAudio()

        // Set buttons listeners
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        btnAudio.setOnClickListener {
            isAudioEnabled = !isAudioEnabled
            handleAudio()
        }

        btnLogout.setOnClickListener {
            signOut()
        }

        btnCredits.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_creditsFragment)
        }

        // Use post to wait btnBack measures
        btnBack.post {
            adaptBackButton2notch(btnBack, notchRects, activity)
        }
    }


    // Handle audio preference
    private fun handleAudio() {
        if (isAudioEnabled) {
            // Start main soundtrack
            (activity as MainActivity).startMainSoundtrack()

            // Set audio button text to ON
            btnAudio.text = getString(R.string.audio_on)
        }
        else {
            // Stop main soundtrack
            (activity as MainActivity).stopMainSoundtrack()

            // Set audio button text to OFF
            btnAudio.text = getString(R.string.audio_off)
        }

        savePreference(getString(R.string.shared_preference_audio), isAudioEnabled)
    }

    private fun savePreference(key: String, value: Boolean) {
        sharedPref.edit().putBoolean(key, value).apply()
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
        val mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        mGoogleSignInClient.signOut().addOnCompleteListener{
            findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)

            //Toast.makeText(context, "A presto", Toast.LENGTH_SHORT).show()
        }

        // Log out from MongoDB Realm
        val mongoRealmAppID = getString(R.string.mongo_db_realm_app_id)
        val app = App(AppConfiguration.Builder(mongoRealmAppID).build())
        val user = app.currentUser()

        user?.logOutAsync {
            if (it.isSuccess) {
                Log.d("login", "Successfully logged out from MongoDB Realm")
            } else {
                Log.e("login", it.error.toString())
            }
        }
    }
}