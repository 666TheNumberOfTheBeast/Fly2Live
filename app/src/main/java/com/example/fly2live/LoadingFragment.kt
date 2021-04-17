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

// VERSIONE CON SOCKETS
import com.example.fly2live.configuration.Configuration.Companion.HOST
import com.example.fly2live.configuration.Configuration.Companion.PORT

// VERSIONE CON VOLLEY E POLLING
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

import com.example.fly2live.configuration.Configuration.Companion.PLAYER_ID
import com.example.fly2live.configuration.Configuration.Companion.NEW_GAME
import com.example.fly2live.configuration.Configuration.Companion.POLLING
import com.example.fly2live.configuration.Configuration.Companion.URL
import com.example.fly2live.configuration.Configuration.Companion.POLLING_PERIOD
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.fragment_loading.*
import org.json.JSONObject
import java.net.URISyntaxException
import org.json.JSONException
import android.app.Activity
import android.content.res.Resources
import android.util.DisplayMetrics







// Fragment shown only if multiplayer mode
class LoadingFragment : Fragment() {
    private lateinit var textView: TextView
    //private lateinit var queue:RequestQueue
    private var found = false

    private lateinit var mSocket: Socket

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textView = view.findViewById(R.id.textView)

        // VERSIONE CON SOCKETS
        // Connect to server and set ID
        val res = connect()
        if (!res)
            return

        Log.d("json", "connected to server? " + mSocket.connected())
        Log.d("json", "is socket active? " + mSocket.isActive())

        textView.text = "Connected to the server"

        // Get screen width and height
        val displayMetrics = Resources.getSystem().displayMetrics
        val screen_width  = displayMetrics.widthPixels
        val screen_height = displayMetrics.heightPixels

        textView.text = "Searching a game"

        searchGame(screen_width, screen_height)



        // VERSIONE CON VOLLEY E POLLING
        /*queue = Volley.newRequestQueue(context)

        // Connect to server and set ID
        getNewGame()*/
    }


    // Connect to server with socket.io
    private fun connect(): Boolean {
        try {
            mSocket = IO.socket(URL)
            mSocket.connect()
            return true
        } catch (e: URISyntaxException) {
            Toast.makeText(context, "Error in connecting to the server", Toast.LENGTH_SHORT).show()
            goBack()
            return false
        }
    }

    private fun searchGame(screen_width: Int, screen_height: Int) {
        val json = JSONObject()
        json.put("who", PLAYER_ID)
        json.put("req", NEW_GAME)
        json.put("screen_width", screen_width)
        json.put("screen_height", screen_height)
        mSocket.emit("new game request", json)
        Log.d("json", "request sent")


        mSocket.on("new game response", Emitter.Listener { args ->
            activity!!.runOnUiThread(Runnable {
                Log.d("json", "response arrived")
                val data = args[0] as JSONObject
                val error: Boolean
                val message: String

                try {
                    error   = data.getBoolean("error")
                    message = data.getString("message")
                } catch (e: JSONException) {
                    return@Runnable
                }

                if (error) {
                    Toast.makeText(context, "Invalid request to the server", Toast.LENGTH_SHORT).show()
                    goBack()
                }
                else
                    textView.text = message

                if (message.equals("OPPONENT FOUND")) {
                    findNavController().navigate(R.id.action_loadingFragment_to_gameFragment)
                }
            })
        })
    }

    private fun goBack() {
        Handler(Looper.myLooper()!!).postDelayed({ findNavController().navigateUp() }, 1000)
    }


// Connect to server with standard sockets
/*private fun connect() {
    val handler = Handler()
    val thread = Thread(Runnable {
        try {
            val s = Socket(HOST, PORT)

            val out = s.getOutputStream()
            val output = PrintWriter(out)

            output.println(msg)
            output.flush()
            val input = BufferedReader(InputStreamReader(s.getInputStream()))
            val st = input.readLine()

            handler.post {
                val s = mTextViewReplyFromServer.getText().toString()
                if (st.trim({ it <= ' ' }).length != 0)
                    mTextViewReplyFromServer.setText(s + "\nFrom Server : " + st)
            }

            output.close()
            out.close()
            s.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    })

    thread.start()
}*/





// VERSIONE CON VOLLEY E POLLING
/*private fun getNewGame() {
    // Request a JSON response from the provided URL
    val req = JsonObjectRequest(
        Request.Method.GET,
        URL + "req=" + NEW_GAME + "&who=-1",
        null,
        { response ->
            Log.d("getNewGame", response.toString())

            // Get ID from JSON
            ID = response.getInt("who")

            textView.setText(R.string.waiting)

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
}*/

}