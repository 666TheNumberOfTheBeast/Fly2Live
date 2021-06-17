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
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.example.fly2live.configuration.Configuration.Companion.PLAYER_ID
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.games.Games
import com.google.android.gms.games.Games.getPlayersClient
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


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
        mGoogleSignInClient = GoogleSignIn.getClient(context!!, gso)

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

            if (account != null) {
                // MongoDB Realm login is necessary for access control on queries at the backend
                mongoDBRealmSignIn(account)

                // Retrieve player information
                //getPlayerInfo(account, "Benvenuto ")
            }
        }
        else {
            var msg = result.status.statusMessage

            if (msg == null || msg.isEmpty())
                msg = "login error"

            Log.d("login", msg)
        }
    }

    // MongoDB Realm sign in with OAuth 2.0
    private fun mongoDBRealmSignIn(account: GoogleSignInAccount) {
        val mongoRealmAppID = getString(R.string.mongo_db_realm_app_id)
        val app = App(AppConfiguration.Builder(mongoRealmAppID).build())

        // GOOGLE AUTHENTICATION
        // QUESTI 2 METODI DI AUTENTICAZIONE TRAMITE GOOGLE NON FUNZIONANO PER PROBLEMI DELLA LIBRERIA DI MONGODB REALM!
        /*val authorizationCode = account.serverAuthCode
        Log.d("login","authorizationCode: $authorizationCode")
        val googleCredentials = Credentials.google(authorizationCode, GoogleAuthType.AUTH_CODE)*/

        /*val idToken = account.idToken
        Log.d("login","idToken: $idToken")
        val googleCredentials = Credentials.google(idToken, GoogleAuthType.ID_TOKEN)

        try {
            app.loginAsync(googleCredentials) { task ->
                if (task.isSuccess) {
                    Log.d("login","Successfully logged in to MongoDB Realm using Google OAuth")

                    // Retrieve player information
                    //getPlayerInfo(account, "Bentornato ")
                }
                else {
                    Log.d("login", "Failed to log in to MongoDB Realm", task.error)
                }
            }
        } catch (e: ApiException) {
            Log.d("login", "Failed to authenticate using Google OAuth: " + e.message);
        }*/


        // API KEY AUTHENTICATION
        // OK MA OGNI VOLTA CREA UN DEVICE ID DIVERSO LATO SERVER
        // E SALVARE LA CLIENT KEY SUL DISPOSITIVO PUÒ ESSERE PERICOLOSO DAL MOMENTO CHE PUÒ ANDARE PERSA!
        /*val id = account.id
        Log.d("login","id: $id")

        val apiKeyCredentials = Credentials.apiKey(getString(R.string.mongo_db_realm_server_api_key))
        app.loginAsync(apiKeyCredentials) { task ->
            if (task.isSuccess) {
                Log.v("login", "Successfully authenticated using an API Key")

                // Retrieve player information
                getPlayerInfo(account, "Bentornato ")
            } else {
                Log.e("login", "Error logging in: ${task.error}")
            }
        }*/


        // CUSTOM JWT AUTHENTICATION
        val jwt = generateJWT(account)
        //Log.d("login","jwt: $jwt")

        // Debug
        /*val bytes = jwt.toByteArray()
        for ((i, b) in bytes.withIndex()) {
            Log.d("login", "byte $i: $b -> ${b.toChar()}")
        }*/

        /*val substr = jwt.split(".")
        for (s in substr) {
            val decoded = Base64.decode(s.toByteArray(), Base64.NO_WRAP)
            Log.d("login", "substr decoded (byte array): $decoded")
            Log.d("login", "substr decoded: ${String(decoded)}")
        }*/

        val customJWTCredentials = Credentials.jwt(jwt)
        app.loginAsync(customJWTCredentials) {
            if (it.isSuccess) {
                Log.d("login", "Successfully authenticated using a custom JWT")

                /*val user = app.currentUser()
                Log.d("login", "user!!.identities[0].id: " + user!!.identities[0].id)*/

                /*val user = app.currentUser()
                user?.linkCredentialsAsync(customJWTCredentials) { task ->
                    if (task.isSuccess) {
                        Log.d("login", "Credentials linked with success")
                    }
                    else
                        Log.e("login", "Error logging in: ${task.error}")
                }*/

                // Retrieve player information
                getPlayerInfo(account, "Bentornato ")
            } else {
                Log.e("login", "Error logging in: ${it.error}")
            }
        }
    }

    private fun generateJWT(account: GoogleSignInAccount): String {
        // CUSTOM JWT TOKEN
        // HEADER:
        // {
        //  "alg": "HS256",
        //  "typ": "JWT"
        // }
        // PAYLOAD:
        // {
        //  "aud": "<realm app id>"
        //  "sub": "<unique user id>",
        //  "exp": <NumericDate>,    NumericDate is the number of seconds (not milliseconds) since Epoch
        //  "iat": <NumericDate>,
        //  "nbf": <NumericDate>,
        //  ...
        // }
        // SIGNATURE:
        // HMACSHA256(
        //  base64UrlEncode(header) + "." + base64UrlEncode(payload),
        //  secret)

        // Get Google user ID
        val userId = account.id
        Log.d("login","account.id: $userId")

        // Get current time in seconds from epoch (NumericDate)
        val now = System.currentTimeMillis() / 1000
        // Set token expiry to one hour from now
        val exp = now + 3600

        // Get user data
        Log.d("login","account.displayName: ${account.displayName}")
        Log.d("login","account.email: ${account.email}")
        Log.d("login","account.givenName: ${account.givenName}")
        val userData = JSONObject()
        userData.put("user_id", userId)
        userData.put("user_name", account.displayName)
        //userData.put("user_name", "Nome prova")

        val jwtHeader = JSONObject()
        jwtHeader.put("alg", "HS256")
        jwtHeader.put("typ", "JWT")

        val jwtPayload = JSONObject()
        jwtPayload.put("aud", getString(R.string.mongo_db_realm_app_id))
        jwtPayload.put("sub", userId)
        //jwtPayload.put("sub", "id prova")
        jwtPayload.put("exp", exp)
        jwtPayload.put("iat", now)
        jwtPayload.put("user_data", userData)

        val hashingAlgorithm = "HmacSHA256"
        val key              = getString(R.string.mongo_db_realm_custom_jwt_auth_secret)

        val message: String
        val signature64: String

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Version >= O => use the URL and Filename safe Base64 Alphabet ('-' replaces '+' and '_' replaces '/')
            // WithoutPadding is not essential
            val encoder = java.util.Base64.getUrlEncoder().withoutPadding()

            val jwtHeader64  = encoder.encodeToString(jwtHeader.toString().toByteArray())
            val jwtPayload64 = encoder.encodeToString(jwtPayload.toString().toByteArray())

            message = "$jwtHeader64.$jwtPayload64"
            val hmacBytes = hmac(hashingAlgorithm, key, message)
            signature64 = encoder.encodeToString(hmacBytes)
        }
        else {
            // Version < O => the encoder uses the Base64 Alphabet, so convert to the URL and Filename safe Base64 Alphabet
            val jwtHeader64  = Base64.encodeToString(jwtHeader.toString().toByteArray(), Base64.NO_WRAP).toURLSafeBase64Encoding()
            val jwtPayload64 = Base64.encodeToString(jwtPayload.toString().toByteArray(), Base64.NO_WRAP).toURLSafeBase64Encoding()

            message = "$jwtHeader64.$jwtPayload64"
            val hmacBytes = hmac(hashingAlgorithm, key, message)
            signature64 = Base64.encodeToString(hmacBytes, Base64.NO_WRAP).toURLSafeBase64Encoding()
        }


        // Debug
        /*val encoder = java.util.Base64.getUrlEncoder().withoutPadding()

        val jwtHeader64  = encoder.encodeToString(jwtHeader.toString().toByteArray())
        val jwtPayload64 = encoder.encodeToString(jwtPayload.toString().toByteArray())

        val message = "$jwtHeader64.$jwtPayload64"
        val hmacBytes = hmac(hashingAlgorithm, key, message)
        val signature64 = encoder.encodeToString(hmacBytes)


        val jwtHeader64_  = Base64.encodeToString(jwtHeader.toString().toByteArray(), Base64.NO_WRAP).toURLSafeBase64Encoding()
        val jwtPayload64_ = Base64.encodeToString(jwtPayload.toString().toByteArray(), Base64.NO_WRAP).toURLSafeBase64Encoding()

        val message_ = "$jwtHeader64.$jwtPayload64"
        val hmacBytes_ = hmac(hashingAlgorithm, key, message)
        val signature64_ = Base64.encodeToString(hmacBytes, Base64.NO_WRAP).toURLSafeBase64Encoding()


        var decoded = Base64.decode(jwtHeader64, Base64.NO_WRAP)
        Log.d("login", "jwtHeader64: $jwtHeader64")
        Log.d("login", "jwtHeader64 decoded (byte array): $decoded")
        Log.d("login", "jwtHeader64 decoded: ${String(decoded)}")

        decoded = Base64.decode(jwtHeader64_, Base64.NO_WRAP)
        Log.d("login", "jwtHeader64_: $jwtHeader64_")
        Log.d("login", "jwtHeader64_ decoded (byte array): $decoded")
        Log.d("login", "jwtHeader64_ decoded: ${String(decoded)}")

        decoded = Base64.decode(jwtPayload64, Base64.NO_WRAP)
        Log.d("login", "jwtPayload64: $jwtPayload64")
        Log.d("login", "jwtPayload64 decoded (byte array): $decoded")
        Log.d("login", "jwtPayload64 decoded: ${String(decoded)}")

        decoded = Base64.decode(jwtPayload64_, Base64.NO_WRAP)
        Log.d("login", "jwtPayload64_: $jwtPayload64_")
        Log.d("login", "jwtPayload64_ decoded (byte array): $decoded")
        Log.d("login", "jwtPayload64_ decoded: ${String(decoded)}")

        //Log.d("login", "signature bytes: $hmacBytes")
        //Log.d("login", "signature hmacbytes: ${String(hmacBytes)}")
        Log.d("login", "signature encoded (byte array): $signature64_")
        Log.d("login", "signature encoded (byte array) with safe URL alphabet without padding: $signature64")*/
        /*decoded = Base64.decode(signature64, Base64.NO_WRAP)
        Log.d("login", "signature decoded (byte array): $decoded")
        Log.d("login", "signature decoded: ${String(decoded)}")*/


        return "$message.$signature64"
    }

    // Convert a base64-encoded string into a URL and Filename safe Base64 Alphabet encoded one
    private fun String.toURLSafeBase64Encoding(): String {
        //return base64Encoding.replace('+', '-').replace('/', '_')

        var safeEncoding = ""

        for (char in this) {
            when (char) {
                '+' ->  safeEncoding += "-"
                '/' ->  safeEncoding += "_"
                else -> safeEncoding += char
            }
        }

        return safeEncoding
    }

    // Calculate HMAC of the message using the given algorithm and key
    private fun hmac(algorithm: String, key: String, message: String): ByteArray {
        val mac = Mac.getInstance(algorithm);
        mac.init(SecretKeySpec(key.toByteArray(), algorithm))
        return mac.doFinal(message.toByteArray())
    }

    // Get player info in order to display a message and store the player ID
    private fun getPlayerInfo(account: GoogleSignInAccount, msgText: String) {
        // Retrieve player information
        getPlayersClient(context!!, account).currentPlayer.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val player = task.result

                val name        = player.name
                val displayName = player.displayName
                val id          = player.playerId

                //Toast.makeText(context, msgText + player.name, Toast.LENGTH_SHORT).show()

                Log.d("login", "player.name: $name")
                Log.d("login", "player.displayName: $displayName")
                Log.d("login", "player.id: $id")

                // Set player ID using the Google account one
                PLAYER_ID = id

                updateUI()
            }
            else
                signIn()
        }
    }

    // Update UI to move to the next fragment (for Google Games sign in)
    private fun updateUI() {
        findNavController().navigate(R.id.action_loginFragment_to_mainFragment)
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
            // Account already signed in and stored in the 'account' variable.
            // Show Google Games pop-up
            Games.getGamesClient(context!!, account).setViewForPopups(activity?.findViewById(android.R.id.content)!!)
            //getPlayerInfo(account, "Bentornato ")

            // MongoDB Realm login is necessary for access control on queries at the backend
            mongoDBRealmSignIn(account)
        }
        else {
            // Haven't been signed-in before
            Log.d("login", "User is not already signed in")
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