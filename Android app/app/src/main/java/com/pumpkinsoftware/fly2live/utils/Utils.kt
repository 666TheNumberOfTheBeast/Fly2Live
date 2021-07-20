package com.pumpkinsoftware.fly2live.utils

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.Window
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView
import com.pumpkinsoftware.fly2live.configuration.Configuration
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.PLAYER_JWT
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.games.Games
import com.google.android.gms.games.Player
import com.pumpkinsoftware.fly2live.R
import io.realm.mongodb.App
import io.realm.mongodb.AppConfiguration
import io.realm.mongodb.Credentials
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


// ******** FUNCTIONS FOR PLAYER LOGIN BEGIN ********
// MongoDB Realm sign in with OAuth 2.0
private fun mongoDBRealmSignIn(player: Player, token: String, context: Context, callback: (result: Boolean) -> Unit): String {
    val mongoRealmAppID = context.getString(R.string.mongo_db_realm_app_id)
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
    val isTokenValid = isJWTValid(token)
    Log.d("login","is jwt valid: $isTokenValid")

    val jwt = if (isTokenValid) token
              else generateJWT(player, context)
    //Log.d("login","jwt: $jwt")

    // Set player JWT
    PLAYER_JWT = jwt

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
            callback.invoke(true)

        } else {
            Log.e("login", "Error logging in: ${it.error}")
            callback.invoke(false)
        }
    }

    return jwt
}

private fun isJWTValid(jwt: String): Boolean {
    if (jwt.isEmpty())
        return false

    val substr = jwt.split(".")

    /*for (s in substr) {
        val decoded = Base64.decode(s.toByteArray(), Base64.NO_WRAP)
        Log.d("login", "substr decoded (byte array): $decoded")
        Log.d("login", "substr decoded: ${String(decoded)}")
    }*/

    val exp: Int
    try {
        // Get the payload from the jwt
        val jwtPayloadEncoded = substr[1]

        // Get the decoded bytes
        val decodedPayloadBytes = Base64.decode(jwtPayloadEncoded.toByteArray(), Base64.NO_WRAP)

        // Reconstruct the JSON object converting the decoded bytes into a string
        val jwtPayload = JSONObject(String(decodedPayloadBytes))

        // Get expiry date
        exp = jwtPayload.getInt("exp")
    } catch (e: Exception) {
        return false
    }

    // Get current time in seconds from epoch (NumericDate)
    val now = System.currentTimeMillis() / 1000
    // Check if jwt is expired
    return now < exp
}

private fun generateJWT(player: Player, context: Context): String {
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



    // Get current time in seconds from epoch (NumericDate)
    val now = System.currentTimeMillis() / 1000
    // Set token expiry to one hour from now
    val exp = now + 3600

    // Get user data
    val userId          = player.playerId
    val userDisplayName = player.displayName

    val userData = JSONObject()
    userData.put("user_id", userId)
    userData.put("user_name", userDisplayName)
    //userData.put("user_name", "Nome prova")

    val jwtHeader = JSONObject()
    jwtHeader.put("alg", "HS256")
    jwtHeader.put("typ", "JWT")

    val jwtPayload = JSONObject()
    jwtPayload.put("aud", context.getString(R.string.mongo_db_realm_app_id))
    jwtPayload.put("sub", userId)
    //jwtPayload.put("sub", "id prova")
    jwtPayload.put("exp", exp)
    jwtPayload.put("iat", now)
    jwtPayload.put("user_data", userData)

    val hashingAlgorithm = "HmacSHA256"
    val key              = context.getString(R.string.mongo_db_realm_custom_jwt_auth_secret)

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
private fun getPlayerInfo(account: GoogleSignInAccount, context: Context, callback: (result: Player?) -> Unit) {
    // Retrieve player information
    Games.getPlayersClient(context, account).currentPlayer.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val player = task.result

            val id          = player.playerId
            val name        = player.name
            val displayName = player.displayName

            Log.d("login", "player.id: $id")
            Log.d("login", "player.name: $name")
            Log.d("login", "player.displayName: $displayName")

            // Set player ID using the Google Games account id
            Configuration.PLAYER_ID = id

            callback.invoke(player)
        }
        else
            callback.invoke(null)
    }
}

fun logIn(account: GoogleSignInAccount?, context: Context, callback: (result: Boolean) -> Unit) {
    // For Google Games sign in
    /*if (GoogleSignIn.hasPermissions(account, Games.SCOPE_GAMES)) {
        // Account already signed in and stored in the 'account' variable
        getPlayerInfo(account!!, "Bentornato ")
    }*/
    if (account != null) {
        // Account already signed in and stored in the 'account' variable.

        getPlayerInfo(account, context) { player ->
            if (player != null) {
                // Player info retrieved with success

                // MongoDB Realm login is necessary for access control on queries at the backend
                mongoDBRealmSignIn(player, PLAYER_JWT, context) { result ->
                    if (result)
                        callback.invoke(true)
                    else
                        callback.invoke(false)
                }
            }
            else {
                Log.d("login", "Error in retrieving player info")
                callback.invoke(false)
            }
        }
    }
    else {
        Log.d("login", "User is not already signed in")
        callback.invoke(false)
    }
}
// ******** FUNCTIONS FOR PLAYER LOGIN END ********


// ******** FUNCTIONS FOR PLAYER LEVEL BEGIN ********
// Given the player level, return the max xp of it
fun getCurrentLevelMaxXp(playerLevel: Int): Long {
    Log.d("progress", "Print xp when each level is from 0 to 1000*playerLevel starting from level 1")
    var xpCurrent = 0
    for (i in 1..10) {
       val j = i+1
       //val xpNext = xpCurrent + 1000*j
       val xpNext = xpCurrent + 1000*i
       Log.d("progress", "Level $i -> Level $j = $xpCurrent -> $xpNext")
       xpCurrent = xpNext
    }

    Log.d("playerLevel", "playerLevel: $playerLevel")

    // RAGIONANDO IN TERMINI DI XP ASSOLUTI (SOMMO TUTTE LE XP DI OGNI LIVELLO RAGGIUNTO)
    // Calculate current player level max xp
    /*var playerLevelMaxXp = 0L

    //for (i in 2..playerLevel) {
    for (i in 1..playerLevel) {
        playerLevelMaxXp += i * 1000
        Log.d("playerLevel", "xpPlayerLevelMax: $playerLevelMaxXp")
    }

    return playerLevelMaxXp*/

    // RAGIONANDO IN TERMINI DI XP RELATIVI (OGNI VOLTA CHE SALGO DI LIVELLO SOTTRAGGO MAX XP LIVELLO PRECEDENTE)
    Log.d("playerLevel", "xpPlayerLevelMax: ${playerLevel * 1000L}")
    return playerLevel * 1000L
}

// Given the player level and the new xp, return if a new level has been achieved
fun isNewLevel(playerLevel: Int, newPlayerXp: Long): Boolean {
    return newPlayerXp >= getCurrentLevelMaxXp(playerLevel)
}
// ******** FUNCTIONS FOR PLAYER LEVEL END ********



fun createScaleAnimation(fromX: Float, toX: Float, fromY: Float, toY: Float, pivotX: Float, pivotY: Float, duration: Long): ScaleAnimation {
    val anim = ScaleAnimation(
        fromX, toX,
        fromY, toY,
        Animation.RELATIVE_TO_SELF, pivotX,
        Animation.RELATIVE_TO_SELF, pivotY)

    anim.fillAfter = true
    anim.duration  = duration

    return anim
}


fun adaptBackButton2notch(btnBack: ImageView, notchRects: List<Rect>?, activity: Activity?) {
    // Get back button rect
    val btnBackRect = Rect(btnBack.left, btnBack.top, btnBack.right, btnBack.bottom)

    /*Log.d("notch", "back button left: ${btnBackRect.left}")
    Log.d("notch", "back button top: ${btnBackRect.top}")
    Log.d("notch", "back button right: ${btnBackRect.right}")
    Log.d("notch", "back button bottom: ${btnBackRect.bottom}")*/

    if (notchRects == null) {
        btnBack.visibility = View.VISIBLE
        return
    }

    // Check if back button is cutout by the notch
    for (rect in notchRects) {
        /*Log.d("notch", "notch left: ${rect.left}")
        Log.d("notch", "notch top: ${rect.top}")
        Log.d("notch", "notch right: ${rect.right}")
        Log.d("notch", "notch bottom: ${rect.bottom}")*/

        val isBackBtnCutout = rect.contains(btnBackRect)
        if (isBackBtnCutout) {
            // Move the back button to bottom
            btnBack.y = activity?.window?.decorView?.height?.minus(btnBack.height.toFloat()) ?: 0f
            btnBack.setImageResource(R.drawable.back_button_bottom)
            break
        }
    }

    btnBack.visibility = View.VISIBLE
}