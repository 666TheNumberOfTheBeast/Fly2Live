package com.pumpkinsoftware.fly2live

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
import com.pumpkinsoftware.fly2live.utils.logIn
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.games.Games


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
            /*.requestServerAuthCode(getString(R.string.client_id_for_mongo_db))
            .requestIdToken(getString(R.string.client_id_for_mongo_db))*/
            .requestEmail()
            .build()*/

        // Configure Google Games player sign-in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
            /*.requestServerAuthCode(getString(R.string.client_id_for_mongo_db))
            .requestIdToken(getString(R.string.client_id_for_mongo_db))*/
            .build()

        // Build a GoogleSignInClient with the options specified by gso
        mGoogleSignInClient = GoogleSignIn.getClient(requireContext(), gso)

        // Register the activity for result of sign in
        mActivityResultLauncher = registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data

                // For default Google sign in
                // The Task returned from this call is always completed, no need to attach a listener
                /*val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                handleSignInResult(task)*/

                // For Google Games sign in
                val res = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                handleSignInResult(res)
            }
            else
                Log.d("login", "Result launcher for sign in failed")
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

            logIn(account, requireContext()) { isLoggedIn ->
                if (isLoggedIn) {
                    // User is logged in.
                    // Show Google Games pop-up
                    Games.getGamesClient(requireContext(), account!!)
                         .setViewForPopups(activity?.findViewById(android.R.id.content)!!)

                    // Go to main fragment
                    findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
                }
                else
                    // User is not logged in
                    Log.d("login", "Login error")
            }
        }
        else {
            var msg = result.status.statusMessage

            if (msg == null || msg.isEmpty())
                msg = "login error"

            Log.d("login", msg)
        }
    }




    // Handle sign in result (for default Google sign in)
    /*private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Check if signed in successfully
            if (account != null) {


                // MongoDB Realm login is necessary for access control on queries at the backend
                mongoDBRealmSignIn(account)


                //Toast.makeText(context, "Benvenuto " + account.displayName, Toast.LENGTH_SHORT).show()
                //updateUI(account)
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