package com.pumpkinsoftware.fly2live

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

import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.PLAYER_ID
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.REQ_CODE_NEW_GAME
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.URL
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException
import org.json.JSONException
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.MSG_CODE_BAD_REQ
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.MSG_CODE_FOUND_ADV
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.MSG_CODE_GAME_START
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.MSG_CODE_PREPARE
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.MSG_CODE_RESEND
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.MSG_CODE_SEARCHING_ADV
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.MSG_CODE_SERVER_BUSY
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.SCENARIO
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.SOCKET_INSTANCE
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.Runnable


// Fragment shown only if multiplayer mode
class LoadingFragment : Fragment() {
    private var account: GoogleSignInAccount? = null

    private lateinit var textView: TextView

    private lateinit var mSocket: Socket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Access control
        account = GoogleSignIn.getLastSignedInAccount(activity)
        if (account == null) {
            // Attempt to pop the controller's back stack back to a specific destination
            findNavController().popBackStack(R.id.mainFragment, false)
            return
        } // DISABLE DURING DEVELOPING TO SAVE API CALLS
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

        // Access control (continue)
        if (account == null)
            return

        textView = view.findViewById(R.id.textView)

        // VERSIONE CON SOCKETS
        // Connect to server and set ID
        connect()
    }


    // Connect to server with socket.io
    private fun connect() {
        // Force websocket
        /*val options = IO.Options.builder()
            .setTransports(arrayOf(WebSocket.NAME))
            .build()*/

        try {
            //mSocket = IO.socket(URL, options)
            mSocket = IO.socket(URL)
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            Toast.makeText(context, "Error in connecting to the server: bad URL", Toast.LENGTH_SHORT).show()
            goBack()
            return
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            goBack()
        }

        // Store the socket instance in order to retrieve it outside this fragment
        SOCKET_INSTANCE = mSocket

        mSocket.connect()

        // Add a one-time listener for the connection event
        mSocket.once(Socket.EVENT_CONNECT) { args ->
            Log.d("json", "connected to the server")
            Log.d("json", "socket ID: " + mSocket.id()) // sid (e.g. x8WIv7-mJelg7on_ALbx)

            // Remove listener for the event of connection error
            mSocket.off(Socket.EVENT_CONNECT_ERROR)

            activity?.runOnUiThread(Runnable {
                // Get screen width and height
                //val displayMetrics = Resources.getSystem().displayMetrics
                val displayMetrics = resources.displayMetrics
                val screenWidth  = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels

                Log.d("ppm", "w: $screenWidth")
                Log.d("ppm", "h: $screenHeight")

                textView.text = getString(R.string.msg_searching_adv)

                searchGame(screenWidth, screenHeight)
            })
        }

        mSocket.once(Socket.EVENT_CONNECT_ERROR) { args ->
            activity?.runOnUiThread(Runnable {
                // Create Toast outside the looper to avoid crashes due to bad context
                val toast = Toast.makeText(context, "Error in connecting to server", Toast.LENGTH_SHORT)

                // Delay action for a better UX
                Handler(Looper.myLooper()!!).postDelayed({
                    toast.show()
                    goBack()
                }, 1000)
            })
        }

        mSocket.once(Socket.EVENT_DISCONNECT) { args ->
            Log.d("json", "disconnected from server")

            /*Log.d("json", "args: $args")
            for (arg in args)
                Log.d("json", "arg: $arg")*/

            activity?.runOnUiThread(Runnable {
                // Create Toast outside the looper to avoid crashes due to bad context
                val toast = Toast.makeText(context, "Disconnected from server", Toast.LENGTH_SHORT)

                // Delay action for a better UX
                Handler(Looper.myLooper()!!).postDelayed({
                    toast.show()
                    goBack()
                }, 1000)
            })
        }
    }

    private fun searchGame(screen_width: Int, screen_height: Int) {
        sendNewGameRequest(screen_width, screen_height)

        // Add a listener for the new game response event
        mSocket.on("new game response") { args ->
            Log.d("json", "response arrived")
            val data = args[0] as JSONObject
            val error: Boolean
            val messageCode: Int

            try {
                error       = data.getBoolean("error")
                messageCode = data.getInt("msg_code")
            } catch (e: JSONException) {
                e.printStackTrace()
                Log.d("json", "Error in retrieving data from server")
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
                        // Remove all listeners (for any event)
                        mSocket.off()

                        // Go to game fragment
                        findNavController().navigate(R.id.action_loadingFragment_to_gameFragment)
                    }
                }
            })

            if (messageCode == MSG_CODE_RESEND)
                sendNewGameRequest(screen_width, screen_height)
        }
    }

    private fun sendNewGameRequest(screen_width: Int, screen_height: Int) {
        if (!::mSocket.isInitialized)
            return

        val json = JSONObject()
        json.put("who", PLAYER_ID)
        json.put("req", REQ_CODE_NEW_GAME)
        json.put("screen_width", screen_width)
        json.put("screen_height", screen_height)
        json.put("scenario", SCENARIO)

        mSocket.emit("new game request", json)
        Log.d("json", "new game request sent")
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
            MSG_CODE_GAME_START    -> return getString(R.string.msg_game_start)
            /*MSG_CODE_GAMEPLAY      -> return getString(R.string.msg_gameplay)
            MSG_CODE_GAME_END      -> return getString(R.string.msg_game_end)*/
            MSG_CODE_SERVER_BUSY   -> return getString(R.string.msg_server_busy)
        }

        return ""
    }

}