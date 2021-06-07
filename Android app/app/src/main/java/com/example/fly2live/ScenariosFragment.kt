package com.example.fly2live

import android.animation.ObjectAnimator
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fly2live.configuration.Configuration.Companion.MULTIPLAYER
import com.example.fly2live.configuration.Configuration.Companion.SCENARIO
import com.example.fly2live.configuration.Configuration.Companion.SCENARIO_CITY_DAY
import com.example.fly2live.configuration.Configuration.Companion.SCENARIO_CITY_NIGHT
import com.google.android.gms.auth.api.signin.GoogleSignIn


class ScenariosFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access control
        val account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account == null) {
            // Attempt to pop the controller's back stack back to a specific destination
            findNavController().popBackStack(R.id.mainFragment, false)
            return
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scenarios, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnScenario1 = view.findViewById<TextView>(R.id.button_scenario_1)
        val btnScenario2 = view.findViewById<TextView>(R.id.button_scenario_2)

        val playerLevelCurrentView  = view.findViewById<TextView>(R.id.player_level_current)
        val playerLevelNextView     = view.findViewById<TextView>(R.id.player_level_next)
        val progressPlayerLevel = view.findViewById<ProgressBar>(R.id.progress_player_level)

        // TODO: get player level and experience by querying the DB (filter with google games ID)
        //val xp          = 5235 // when each level is from 0 to 1000
        val xp          = 19342 // when each level is from 0 to 1000*playerLevel
        val playerLevel = 5
        //val xp          = 3500
        //val playerLevel = 2

        // Set buttons listeners
        btnScenario1.setOnClickListener {
            SCENARIO = SCENARIO_CITY_DAY
            navigate()
        }

        // Check if the player has unlocked the 2nd scenario
        if (playerLevel >= 5) {
            btnScenario2.setTextColor(ContextCompat.getColor(context!!, android.R.color.white))

            btnScenario2.setOnClickListener {
                SCENARIO = SCENARIO_CITY_NIGHT
                navigate()
            }
        }
        else {
            btnScenario2.setOnClickListener {
                Toast.makeText(context, "You have not unlocked this scenario yet", Toast.LENGTH_SHORT).show()
            }
        }

        // Set progress bar levels
        playerLevelCurrentView.text = playerLevel.toString()
        playerLevelNextView.text    = (playerLevel+1).toString()


        // ======================================================
        // Calculate current level progress where each level is from 0 to 1000
        // Level 0 -> Level 1 = 0    -> 1000 => 1000 - 0    = 1000
        // Level 1 -> Level 2 = 1000 -> 2000 => 2000 - 1000 = 1000
        // Level 2 -> Level 3 = 2000 -> 3000 => 3000 - 2000 = 1000
        // ...
        /*Log.d("progress", "Print xp when each level is from 0 to 1000")
        for (i in 0..10) {
            val j = i+1
            Log.d("progress", "Level $i -> Level $j = " + (1000*i) + " -> " + (1000*j))
        }*/
        // progress = ((xp - playerLevel*1000) / 1000) * 100
        //val progress = ((xp - playerLevel*1000f) / 10f).toInt()
        // ======================================================

        // ======================================================
        // Calculate current level progress where each level is from 0 to 1000*playerLevel
        // Level 0 -> Level 1 = 0    -> 1000 => 1000 - 0    = 1000
        // Level 1 -> Level 2 = 1000 -> 3000 => 3000 - 1000 = 2000 = 1000*2
        // Level 2 -> Level 3 = 3000 -> 6000 => 6000 - 3000 = 3000 = 1000*3
        // ...
        /*Log.d("progress", "Print xp when each level is from 0 to 1000*playerLevel starting from level 0")
        var xpCurrent = 0
        for (i in 0..10) {
            val j = i+1
            val xpNext = xpCurrent + 1000*j
            Log.d("progress", "Level $i -> Level $j = $xpCurrent -> $xpNext")
            xpCurrent = xpNext
        }*/
        /*var xpPlayerLevelMax = 0
        for (i in 1..playerLevel) {
            xpPlayerLevelMax += i * 1000
            Log.d("progress", "xpPlayerLevelMax: $xpPlayerLevelMax")
        }*/
        // ======================================================

        // ======================================================
        Log.d("progress", "Print xp when each level is from 0 to 1000*playerLevel starting from level 1")
        var xpCurrent = 0
        for (i in 1..10) {
            val j = i+1
            val xpNext = xpCurrent + 1000*j
            Log.d("progress", "Level $i -> Level $j = $xpCurrent -> $xpNext")
            xpCurrent = xpNext
        }

        // Calculate current player level max xp
        var xpPlayerLevelMax = 0
        for (i in 2..playerLevel) {
            xpPlayerLevelMax += i * 1000
            Log.d("progress", "xpPlayerLevelMax: $xpPlayerLevelMax")
        }

        //val progress = (((xp - xpPlayerLevelMax) / (1000f * (playerLevel+1))) * 100).toInt()
        val progress = ((xp - xpPlayerLevelMax) / (10f * (playerLevel+1))).toInt()
        // ======================================================

        Log.d("progress", "xp: $xp")
        Log.d("progress", "playerLevel: $playerLevel")
        Log.d("progress", "progress: $progress")

        // Set progress bar value smoothly
        progressPlayerLevel.smoothProgress(progress)

    }

    private fun ProgressBar.smoothProgress(percent: Int){
        val animation = ObjectAnimator.ofInt(this, "progress", percent)
        animation.duration     = 700
        animation.interpolator = DecelerateInterpolator()
        animation.start()
    }

    // Navigate to the correct fragment based on MULTIPLAYER value
    private fun navigate() {
        if (MULTIPLAYER)
            findNavController().navigate(R.id.action_scenariosFragment_to_loadingFragment)
        else
            findNavController().navigate(R.id.action_scenariosFragment_to_gameFragment)
    }

}