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


// VERSIONE CON VOLLEY E POLLING

import com.example.fly2live.configuration.Configuration.Companion.PLAYER_ID
import com.example.fly2live.configuration.Configuration.Companion.REQ_CODE_NEW_GAME
import com.example.fly2live.configuration.Configuration.Companion.URL
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException
import org.json.JSONException
import android.content.res.Resources
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.example.fly2live.configuration.Configuration.Companion.MSG_CODE_BAD_REQ
import com.example.fly2live.configuration.Configuration.Companion.MSG_CODE_FOUND_ADV
import com.example.fly2live.configuration.Configuration.Companion.MSG_CODE_GAME_START
import com.example.fly2live.configuration.Configuration.Companion.MSG_CODE_PREPARE
import com.example.fly2live.configuration.Configuration.Companion.MSG_CODE_SEARCHING_ADV
import com.example.fly2live.configuration.Configuration.Companion.MSG_CODE_SERVER_BUSY
import io.socket.engineio.client.transports.WebSocket
import kotlinx.coroutines.Runnable


// Fragment shown only if multiplayer mode
class LoadingFragment : Fragment() {
    private lateinit var textView: TextView
    //private lateinit var queue:RequestQueue
    private var searching = false
    private var found = false

    private lateinit var mSocket: Socket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access control
        /*account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account == null) {
            // Attempt to pop the controller's back stack back to a specific destination
            findNavController().popBackStack(R.id.mainFragment, false)
            return
        }*/ // TEMP DISABLED TO SAVE API CALLS
    }

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
        connect()



        // VERSIONE CON VOLLEY E POLLING
        /*queue = Volley.newRequestQueue(context)

        // Connect to server and set ID
        getNewGame()*/
    }


    // Connect to server with socket.io
    private fun connect() {
        // Force websocket
        val options = IO.Options.builder()
            .setTransports(arrayOf(WebSocket.NAME))
            .build()

        try {
            mSocket = IO.socket(URL, options)
        } catch (e: URISyntaxException) {
            Toast.makeText(context, "Error in connecting to the server: bad URL", Toast.LENGTH_SHORT).show()
            goBack()
            return
        }

        mSocket.connect()

        mSocket.on(Socket.EVENT_CONNECT) { args ->
            Log.d("json", "connected to the server")
            Log.d("json", "socket ID: " + mSocket.id()) // sid (e.g. x8WIv7-mJelg7on_ALbx)

            activity?.runOnUiThread(Runnable {
                // Get screen width and height
                val displayMetrics = Resources.getSystem().displayMetrics
                // val displayMetrics = resources.displayMetrics
                val screenWidth  = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels

                Log.d("ppm", "w: $screenWidth")
                Log.d("ppm", "h: $screenHeight")

                textView.text = getString(R.string.msg_searching_adv)

                searching = true
                searchGame(screenWidth, screenHeight)
            })

            requireActivity().onBackPressedDispatcher.addCallback(this) {
                goBack()
            }
        }

        mSocket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            activity?.runOnUiThread(Runnable {
                // Create Toast outside the looper to avoid crashes due to bad context
                val toast = Toast.makeText(context, "Error in connecting to the server", Toast.LENGTH_SHORT)

                // Delay action for a better UX
                Handler(Looper.myLooper()!!).postDelayed({
                    toast.show()
                    goBack()
                }, 1000)
            })
        }

        mSocket.on(Socket.EVENT_DISCONNECT) { args ->
            Log.d("json", "disconnected from the server")
        }
    }

    private fun searchGame(screen_width: Int, screen_height: Int) {
        val json = JSONObject()
        json.put("who", PLAYER_ID)
        json.put("req", REQ_CODE_NEW_GAME)
        json.put("screen_width", screen_width)
        json.put("screen_height", screen_height)

        mSocket.emit("new game request", json)
        Log.d("json", "request sent")


        mSocket.on("new game response") { args ->
            Log.d("json", "response arrived")
            val data = args[0] as JSONObject
            val error: Boolean
            val messageCode: Int

            try {
                error       = data.getBoolean("error")
                messageCode = data.getInt("msg_code")
            } catch (e: JSONException) {
                return@on
            }

            activity?.runOnUiThread(Runnable {
                if (error) {
                    Toast.makeText(context, "Invalid request to the server", Toast.LENGTH_SHORT).show()
                    goBack()
                }
                else {
                    textView.text = toMessage(messageCode)

                    if (messageCode == MSG_CODE_GAME_START) {
                        findNavController().navigate(R.id.action_loadingFragment_to_gameFragment)
                    }
                }
            })
        }
    }

    private fun goBack() {
        // Remove all listeners (for any event)
        mSocket.off()

        // Disconnect the socket
        mSocket.disconnect()

        // Go to previous fragment using a coroutine tied to the fragment lifecycle
        // in order to avoid crashes
        lifecycleScope.launchWhenResumed {
            // Attempt to pop the controller's back stack back to a specific destination
            // in order to avoid to double go back if the user goes back
            findNavController().popBackStack(R.id.mainFragment, false)
        }
    }

    private fun toMessage(msg_code: Int): String {
        when (msg_code) {
            MSG_CODE_BAD_REQ       -> return getString(R.string.msg_bad_request)
            MSG_CODE_SEARCHING_ADV -> return getString(R.string.msg_searching_adv)
            MSG_CODE_FOUND_ADV     -> return getString(R.string.msg_found_adv)
            MSG_CODE_PREPARE       -> return getString(R.string.msg_prepare)
            /*MSG_CODE_GAME_START    -> return getString(R.string.msg_game_start)
            MSG_CODE_GAMEPLAY      -> return getString(R.string.msg_gameplay)
            MSG_CODE_GAME_END      -> return getString(R.string.msg_game_end)*/
            MSG_CODE_SERVER_BUSY   -> return getString(R.string.msg_server_busy)
        }

        return ""
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
        URL + "req=" + REQ_CODE_NEW_GAME + "&who=-1",
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