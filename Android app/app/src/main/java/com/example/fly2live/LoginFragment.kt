package com.example.fly2live

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.SignInButton
import android.content.res.Configuration
import android.widget.ImageView
import android.widget.Toast
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.example.fly2live.configuration.Configuration.Companion.PLAYER_ID
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.games.Games.getPlayersClient


class LoginFragment : Fragment() {
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mActivityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Configure sign-in to request the user's ID, email address, and basic profile.
        // ID and basic profile are included in DEFAULT_SIGN_IN
        /*val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()*/

        // Configure Google Games player sign-in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
            .build()

        // Build a GoogleSignInClient with the options specified by gso
        mGoogleSignInClient = GoogleSignIn.getClient(context!!, gso)

        // Register the activity for result of sign in
        mActivityResultLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data

                // For default Google sign in
                // The Task returned from this call is always completed, no need to attach a listener
                /* val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResult(task)*/

                // For Google Games sign in
                val res = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                handleSignInResult(res)
            }
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logo = view.findViewById<ImageView>(R.id.logo)
        val orientation = resources.configuration.orientation

        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            logo.scaleX = 0.6f
            logo.scaleY = 0.6f
        }

        val signInButton = view.findViewById<SignInButton>(R.id.button_sign_in)
        signInButton.setSize(SignInButton.SIZE_STANDARD)

        signInButton.setOnClickListener{
            signIn()
        }
    }

    override fun onStart() {
        super.onStart()

        // Check for existing Google Sign In account.
        // If the user is already signed in the GoogleSignInAccount will be non-null.
        val account = GoogleSignIn.getLastSignedInAccount(context)

        // For default Google sign in
        /*if (account != null) {
            Toast.makeText(context, "Bentornato " + account.displayName, Toast.LENGTH_SHORT).show()
            updateUI(account)
        }*/

        // For Google Games sign in
        /*if (GoogleSignIn.hasPermissions(account, Games.SCOPE_GAMES)) {
            // Account already signed in and stored in the 'account' variable
            getPlayerInfo(account!!, "Bentornato ")
        }*/
        if (account != null) {
            // Account already signed in and stored in the 'account' variable
            getPlayerInfo(account, "Bentornato ")
        }
        else {
            // Haven't been signed-in before
            Log.d("login", "User is not already signed in")
        }
    }

    // Start the intent prompts the user to select a Google account to sign in with
    private fun signIn() {
        val signInIntent = mGoogleSignInClient.signInIntent
        mActivityResultLauncher.launch(signInIntent)
    }

    // Handle sign in result (for Google Games sign in)
    private fun handleSignInResult(result: GoogleSignInResult?) {
        if (result == null)
            Toast.makeText(context, "login error", Toast.LENGTH_SHORT).show()

        else if (result.isSuccess) {
            // The signed in account is stored in the result
            val account = result.signInAccount
            if (account != null) {
                // Retrieve player information
                getPlayerInfo(account, "Benvenuto ")
            }
        }
        else {
            var msg = result.status.statusMessage

            if (msg == null || msg.isEmpty())
                msg = "login error"

            Log.d("login", msg)
        }
    }

    // Get player info in order to display a message and store the player ID
    private fun getPlayerInfo(account: GoogleSignInAccount, msgText: String) {
        // Retrieve player information
        getPlayersClient(context!!, account).currentPlayer.addOnCompleteListener { task ->
            val player = task.result

            val name        = player.name
            val displayName = player.displayName
            val id          = player.playerId

            Toast.makeText(context, msgText + player.name, Toast.LENGTH_SHORT).show()

            Log.d("login", "name: $name")
            Log.d("login", "displayName: $displayName")
            Log.d("login", "id: $id")

            // Set player ID using the Google account one
            PLAYER_ID = id

            updateUI()
        }
    }

    // Update UI to move to the next fragment (for Google Games sign in)
    private fun updateUI() {
        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
    }




    // Handle sign in result (for default Google sign in)
    /*private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Check if signed in successfully
            if (account != null) {
                Toast.makeText(context, "Benvenuto " + account.displayName, Toast.LENGTH_SHORT).show()
                updateUI(account)
            }
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            //Log.w(TAG, "signInResult:failed code=" + e.statusCode)
            Toast.makeText(context, e.statusCode, Toast.LENGTH_SHORT).show()
        }

    }

    // Update UI to move to the next fragment (for default Google sign in)
    private fun updateUI(account: GoogleSignInAccount) {
        // Set player ID using the Google account one
        PLAYER_ID = account.id!!
        Log.d("login", "PLAYER_ID: $PLAYER_ID")
        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
    }*/

}