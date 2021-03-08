package com.example.fly2live

import android.os.Bundle
import android.os.Handler
import android.os.Looper

import android.util.Log
import androidx.fragment.app.Fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController

import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

import com.example.fly2live.configuration.Configuration.Companion.ID
import com.example.fly2live.configuration.Configuration.Companion.NEW_GAME
import com.example.fly2live.configuration.Configuration.Companion.POLLING
import com.example.fly2live.configuration.Configuration.Companion.URL
import com.example.fly2live.configuration.Configuration.Companion.POLLING_PERIOD

// Fragment shown only if multiplayer mode
class LoadingFragment : Fragment() {
    private lateinit var queue:RequestQueue
    private var found = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        queue = Volley.newRequestQueue(context)

        // Connect to server and set ID
        getNewGame()
    }

    private fun getNewGame() {
        // Request a JSON response from the provided URL
        val req = JsonObjectRequest(
            Request.Method.GET,
            URL + "req=" + NEW_GAME + "&who=-1",
            null,
            { response ->
                Log.d("getNewGame", response.toString())

                // Get ID from JSON
                ID = response.getInt("who")

                view?.findViewById<TextView>(R.id.textView)?.setText(R.string.waiting)

                // Wait a player
                poll()
            },
            { error ->
                Log.d("getNewGame", error.toString())
                Toast.makeText(context, "Error in connecting to the server", Toast.LENGTH_SHORT).show()

                // Go back
                Handler(Looper.myLooper()!!).postDelayed({ findNavController().navigateUp() }, 2*POLLING_PERIOD)
            }
        )

        // Add the request to the RequestQueue
        queue.add(req)
    }

    private fun waitAdversary() {
        // Request a JSON response from the provided URL
        val req = JsonObjectRequest(
            Request.Method.GET,
            URL + "req=" + POLLING + "&who=" + ID,
            null,
            { response ->
                Log.d("getAdversary", response.toString())

                // Get value from JSON
                found = response.getBoolean("error") == false
            },
            { error ->
                Log.d("getAdversary", error.toString())

                Toast.makeText(context, "Error in connecting to the server", Toast.LENGTH_SHORT).show()

                // Go back
                Handler(Looper.myLooper()!!).postDelayed({ findNavController().navigateUp() }, 2*POLLING_PERIOD)
            }
        )

        // Add the request to the RequestQueue
        queue.add(req)
    }

    // Continuos polling
    private fun poll() {
        // Check if an adversary joined the game
        if (found) {
            findNavController().navigate(R.id.action_loadingFragment_to_gameFragment)
            return
        }

        waitAdversary()
        Handler(Looper.myLooper()!!).postDelayed({ poll() }, POLLING_PERIOD)
    }

}