package com.example.fly2live

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fly2live.configuration.Configuration.Companion.MULTIPLAYER
import com.example.fly2live.configuration.Configuration.Companion.SCENARIO
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

        // TODO: get player level by querying the DB
        val playerLevel = 1

        // Set buttons listeners
        btnScenario1.setOnClickListener {
            SCENARIO = 0
            navigate()
        }

        if (playerLevel >= 5) {
            btnScenario2.setTextColor(ContextCompat.getColor(context!!, android.R.color.white))
            btnScenario2.isClickable = true

            btnScenario2.setOnClickListener {
                SCENARIO = 1
                navigate()
            }
        }
    }

    // Navigate to the correct fragment based on MULTIPLAYER value
    private fun navigate() {
        if (MULTIPLAYER)
            findNavController().navigate(R.id.action_scenariosFragment_to_loadingFragment)
        else
            findNavController().navigate(R.id.action_scenariosFragment_to_gameFragment)
    }

}