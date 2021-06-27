package com.example.fly2live

import android.app.Activity
import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.activity.addCallback
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.findFragment
import com.example.fly2live.configuration.Configuration
import com.example.fly2live.configuration.Configuration.Companion.MSG_CODE_GAME_END
import com.example.fly2live.configuration.Configuration.Companion.PLAYER_HEIGHT
import com.example.fly2live.configuration.Configuration.Companion.PLAYER_ID
import com.example.fly2live.configuration.Configuration.Companion.PLAYER_WIDTH
import com.example.fly2live.configuration.Configuration.Companion.REQ_CODE_NEW_MOVE
import com.example.fly2live.configuration.Configuration.Companion.SOCKET_INSTANCE
import com.example.fly2live.configuration.Configuration.Companion.WINNER_ADVERSARY
import com.example.fly2live.configuration.Configuration.Companion.WINNER_PLAYER
import com.example.fly2live.configuration.Configuration.Companion.WINNER_UNDEFINED
import io.socket.client.Socket
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min


class GameViewMultiplayer(context: Context?, lifecycleScope: CoroutineScope) : View(context), View.OnTouchListener, SensorEventListener2 {
    //class GameView(context: Context?) : View(context), View.OnTouchListener, SensorEventListener2 {
    private val lifecycleScope = lifecycleScope
    /*private var lifecycleScope: CoroutineScope? = null

    constructor(context: Context?, lifecycleScope: CoroutineScope) : this(context) {
        this.lifecycleScope = lifecycleScope
    }*/

    // Socket io
    private lateinit var mSocket: Socket

    // Background bitmap
    private lateinit var bg: Bitmap

    // Players' objects
    private lateinit var player0Vehicle: ObjectPlayer
    private lateinit var player1Vehicle: ObjectPlayer
    private var playerNumber = -1   // Set to 0 or 1 based on player ID

    // CPU's game objects
    private lateinit var buildings: Array<Object>
    private lateinit var cpuBuilding: Object
    private lateinit var vehicles: Array<Object>
    private lateinit var cpuVehicle: Object


    // Sensors values
    private var lastAcceleration = FloatArray(3)
    private var gyroscopeValues = FloatArray(3)
    private var previousGyroscopeInput = 0f // meters


    // World constants
    private val worldWidth  = 50f // meters
    //private var worldHeight = 60f // meters

    // Pixel per meter
    private var ppm = 1f

    private var speed = 10f // m/s
    private var score = 0f  // meters traveled
    private var winner = -1 // -1, 0, 1, 2 -> -1 undefined, 0 if player loses, 1 if player wins, 2 if the players die in the same frame


    // Variable to stop drawing when game ends
    private var gameEnd = false

    // Variable to start drawing when game has been initialized
    private var startDrawing = false

    // Variable for resume drawing when game variables has been restored
    private var resumeDrawing = true

    // Variables for text to show when game is loading
    private val textRect    = Rect()
    private val loadingText = resources.getString(R.string.loading)


    // Painter for text
    private val painterFill = Paint().apply {
        style       = Paint.Style.FILL
        color       = Color.BLACK
        textSize    = 50f
    }

    // Painter for the debug of bitmaps' physics bounds
    private val painterStroke = Paint().apply {
        style       = Paint.Style.STROKE
        color       = Color.BLACK
        strokeWidth = 3f
    }

    init {
        setOnTouchListener(this)

        val sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        /*sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )*/

        /*sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_NORMAL
        )*/

        sensorManager.registerListener(
            this,
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            SensorManager.SENSOR_DELAY_NORMAL
        )


        Log.d("ppm", "***** INITIALIZATION *****")
        // Don't use directly getWidth() and getHeight() here because they return 0
        //Log.d("ppm", "width: $width")
        //Log.d("ppm", "height: $height")

        // Get screen width and height
        val screenWidth  = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        Log.d("ppm", "w: $screenWidth")
        Log.d("ppm", "h: $screenHeight")

        init(screenWidth, screenHeight)
    }

    // Multithreading version
    private fun init(screenWidth: Int, screenHeight: Int) {
        // Dispatchers.Default — is used by all standard builders if no dispatcher or any other ContinuationInterceptor is specified in their context.
        // It uses a common pool of shared background threads.
        // This is an appropriate choice for compute-intensive coroutines that consume CPU resources
        lifecycleScope.launch(Dispatchers.Default) {
            // Retrieve current socket
            mSocket = SOCKET_INSTANCE!!

            withContext(Dispatchers.Main) {
                // Override back behavior
                (context as FragmentActivity).onBackPressedDispatcher.addCallback(context as FragmentActivity) {
                    // Remove all listeners (for any event)
                    mSocket.off()

                    // Disconnect the socket
                    mSocket.disconnect()

                    winner =
                        if (!startDrawing)  WINNER_UNDEFINED  // Disconnection before start drawing, winner undefined
                        else                WINNER_ADVERSARY  // Disconnection during gameplay, so adversary is the winner


                    gameEnd()
                }
            }

            // Calculate PPM

            // ===========================================
            // OPTION 1
            // Use a fixed unit of measure (i.e ppm) for drawing the bitmaps when rotating the screen

            // OPTION 1.1
            // I divide both to worldWidth because I'm interested in screenWidth for obstacles respawn
            //ppm = min(screenWidth / worldWidth, screenHeight / worldWidth)

            // OPTION 1.2
            // I divide both to worldHeight because I'm interested in screenHeight for avoiding obstacles
            //ppm = min(screenWidth / worldHeight, screenHeight / worldHeight)

            // AN ISSUE:
            // If orientation is landscape, current world screenWidth is larger than portrait mode
            // because when:
            // - portrait  => min = screenWidth / worldWidth  => currentWorldWidth = worldWidth
            // - landscape => min = screenHeight / worldWidth => currentWorldWidth > worldWidth
            // ===========================================

            // ===========================================
            // OPTION 2
            // Use a variable unit of measure (i.e ppm) for drawing the bitmaps when rotating the screen

            // OPTION 2.1
            // PPM based on the screenWidth of the screen to have the same proportion horizontally
            ppm = screenWidth / worldWidth

            // OPTION 2.2
            // PPM based on the screenHeight of the screen to have the same proportion vertically
            //ppm = screenHeight / worldHeight

            // OPTION 2.3
            // PPM based on the screenHeight of the screen to have the same proportion vertically and to the previously calculated scale factor
            //ppm = screenHeight / (worldHeight * scaleFactor)
            //ppm = (screenHeight / worldHeight) * scaleFactor
            Log.d("ppm", "ppm: $ppm")

            // POSSIBLE ISSUE:
            // It's difficult to find a worldWidth or worldHeight that is ok for both portrait and landscape orientations
            // because there is no constraint on the other dimension.
            // I cannot use two PPM (one for X and one for Y) to contrain the two dimensions because
            // if I use different ppm for x and y, then images are distorted.

            // However, options 2.1 and 2.2 works well.
            // 2.1 -> fixed worldWidth, variable worldHeight results in a zoom when landscape (better!)
            // 2.2 -> variable worldWidth, fixed worldHeight results in a greater field of view when landscape
            // ===========================================

            // Calculate current world dimensions
            val currentWorldWidth  = screenWidth / ppm
            val currentWorldHeight = screenHeight / ppm

            Log.d("ppm", "worldWidth: $worldWidth")
            Log.d("ppm", "currentWorldWidth = screenWidth / ppm: $currentWorldWidth")
            //Log.d("ppm", "worldHeight: $worldHeight")
            Log.d("ppm", "currentWorldHeight = screenHeight / ppm: $currentWorldHeight")

            // Initial position of obstacles
            val initialObstaclePosX = 0f  // In meters
            val initialObstaclePosY = 0f  // In meters

            val loadBuildings = async {
                Log.d("COROUTINE", "Load buildings")

                // Convert the correct scenario images into bitmaps once
                if (Configuration.SCENARIO == Configuration.SCENARIO_CITY_DAY) {
                    bg = ResourcesCompat.getDrawable(resources, R.drawable.city_bg_day, null)?.toBitmap(screenWidth, screenHeight)!!

                    buildings = arrayOf(
                        Object(
                            "building_01",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_01, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 25f, 70f, initialObstaclePosX, initialObstaclePosY // Measures in meters except screen dimensions
                        ),
                        Object(
                            "building_02",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_02, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 10f, 80f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_03",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_03, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 25f, 110f,initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_04",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_04, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 35f, 90f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_05",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_05, null)?.toBitmap(screenWidth, screenHeight)!!),
                            //arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_05, null)?.toBitmap()!!),
                            screenWidth, screenHeight, ppm, 35f, 70f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_06",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_06, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 45f, 50f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_07",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_07, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 15f, 80f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_08",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_08, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 13f, 70f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_09",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_09, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 17f, 70f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_10",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_10, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 13f, 60f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_11",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_11, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 17f, 55f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_12",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_12, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 15f, 50f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_13",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_13, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 20f, 50f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_14",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_14, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 30f, 55f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_15",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_15, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 13f, 55f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_16",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_16, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 20f, 50f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_17",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_17, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 28f, 60f, initialObstaclePosX, initialObstaclePosY
                        )
                    )
                }
                else {
                    bg = ResourcesCompat.getDrawable(resources, R.drawable.city_bg_night, null)?.toBitmap(screenWidth, screenHeight)!!

                    buildings = arrayOf(
                        Object(
                            "building_01",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_01, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 15f, 100f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_02",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_02, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 10f, 110f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_03",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_03, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 18f, 70f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_04",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_04, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 20f, 70f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_05",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_05, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 25f, 60f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_06",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_06, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 25f, 100f, initialObstaclePosX, initialObstaclePosY // Measures in meters except pos_y
                        ),
                        Object(
                            "building_07",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_07, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 18f, 80f, initialObstaclePosX, initialObstaclePosY // Measures in meters except pos_y
                        ),
                        Object(
                            "building_08",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_08, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 17f, 70f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_09",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_09, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 25f, 70f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_10",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_10, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 13f, 65f, initialObstaclePosX, initialObstaclePosY
                        ),
                        Object(
                            "building_11",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_11, null)?.toBitmap(screenWidth, screenHeight)!!),
                            screenWidth, screenHeight, ppm, 13f, 80f, initialObstaclePosX, initialObstaclePosY
                        )
                    )
                }

                cpuBuilding = buildings[0]
            }

            val loadVehicles = async {
                Log.d("COROUTINE", "Load vehicles")

                vehicles = arrayOf(
                    Object(
                        "vehicle_01",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_biplane, null)?.toBitmap(screenWidth, screenHeight)!!),
                        screenWidth, screenHeight, ppm, 6f, 2.5f, initialObstaclePosX, initialObstaclePosY // Measures in meters except screen dimensions
                    ),

                    Object(
                        "vehicle_02",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_1, null)?.toBitmap(screenWidth, screenHeight)!!),
                        screenWidth, screenHeight, ppm, 20f, 8f, initialObstaclePosX, initialObstaclePosY
                    ),
                    Object(
                        "vehicle_03",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_2, null)?.toBitmap(screenWidth, screenHeight)!!),
                        screenWidth, screenHeight, ppm, 20f, 8f, initialObstaclePosX, initialObstaclePosY
                    ),
                    Object(
                        "vehicle_04",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_3, null)?.toBitmap(screenWidth, screenHeight)!!),
                        screenWidth, screenHeight, ppm, 20f, 8f, initialObstaclePosX, initialObstaclePosY
                    ),
                    Object(
                        "vehicle_05",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_4, null)?.toBitmap(screenWidth, screenHeight)!!),
                        screenWidth, screenHeight, ppm, 20f, 8f, initialObstaclePosX, initialObstaclePosY
                    ),
                    Object(
                        "vehicle_06",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_5, null)?.toBitmap(screenWidth, screenHeight)!!),
                        screenWidth, screenHeight, ppm, 20f, 8f, initialObstaclePosX, initialObstaclePosY
                    ),
                    Object(
                        "vehicle_07",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_6, null)?.toBitmap(screenWidth, screenHeight)!!),
                        screenWidth, screenHeight, ppm, 20f, 8f, initialObstaclePosX, initialObstaclePosY
                    ),
                    Object(
                        "vehicle_08",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_7, null)?.toBitmap(screenWidth, screenHeight)!!),
                        screenWidth, screenHeight, ppm, 20f, 8f, initialObstaclePosX, initialObstaclePosY
                    ),


                    Object(
                        "vehicle_09",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_ufo, null)?.toBitmap(screenWidth, screenHeight)!!),
                        screenWidth, screenHeight, ppm, 4.2f, 2.1f, initialObstaclePosX, initialObstaclePosY
                    )
                )

                cpuVehicle = vehicles[0]
            }

            val loadPlayers = async {
                Log.d("COROUTINE", "Load players vehicles")
                val playerVehicles = arrayOf(
                    ObjectPlayer(
                        "player_0",
                        arrayOf(
                            ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_red_0, null)?.toBitmap(screenWidth, screenHeight)!!,
                            ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_red_1, null)?.toBitmap(screenWidth, screenHeight)!!,
                            ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_red_2, null)?.toBitmap(screenWidth, screenHeight)!!,
                            ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_red_3, null)?.toBitmap(screenWidth, screenHeight)!!
                        ),
                        screenWidth, screenHeight, ppm, PLAYER_WIDTH, PLAYER_HEIGHT, 0f, 0f // Measures in meters except screen dimensions
                    ),
                    ObjectPlayer(
                        "player_1",
                        arrayOf(
                            ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_yellow_0, null)?.toBitmap(screenWidth, screenHeight)!!,
                            ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_yellow_1, null)?.toBitmap(screenWidth, screenHeight)!!,
                            ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_yellow_2, null)?.toBitmap(screenWidth, screenHeight)!!,
                            ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_yellow_3, null)?.toBitmap(screenWidth, screenHeight)!!
                        ),
                        screenWidth, screenHeight, ppm, PLAYER_WIDTH, PLAYER_HEIGHT, 0f, 0f // Measures in meters except screen dimensions
                    )
                )

                player0Vehicle = playerVehicles[0]
                player1Vehicle = playerVehicles[1]
            }

            loadBuildings.await()
            loadVehicles.await()
            loadPlayers.await()


            // Send client ready message to the server
            val json = JSONObject()
            json.put("who", PLAYER_ID)
            //json.put("req", REQ_CODE_CLIENT_READY)

            mSocket.emit("client ready", json)
            Log.d("json", "Client ready message sent to the server")


            mSocket.on(Socket.EVENT_DISCONNECT) { args ->
                Log.d("json", "disconnected from the server in game view")

                // Assume no temporary disconnection,
                // so if player disconnects & started drawing, he loses

                // If the server disconnects, the client is disconnected too but it's a server fault.
                // However, the server should never disconnect

                winner =
                    if (!startDrawing)  WINNER_UNDEFINED  // Disconnection before start drawing, winner undefined
                    else                WINNER_ADVERSARY  // Disconnection during gameplay, so adversary is the winner


                // Fragment navigation requires the main thread
                lifecycleScope.launch(Dispatchers.Main) {
                    gameEnd()
                }
            }

            // Add a listener for the game update event
            mSocket.on("game update") { args ->
                Log.d("json", "Gameplay message from server arrived in game view")

                // Get data
                val data = args[0] as JSONObject
                val error: Boolean
                val messageCode: Int
                val cpuBuildingJson: JSONObject
                val cpuVehicleJson: JSONObject
                val player0: JSONObject
                val player1: JSONObject

                try {
                    error           = data.getBoolean("error")
                    messageCode     = data.getInt("msg_code")
                    cpuBuildingJson = data.getJSONObject("cpu_building")
                    cpuVehicleJson  = data.getJSONObject("cpu_vehicle")
                    player0         = data.getJSONObject("player_0")
                    player1         = data.getJSONObject("player_1")
                    score           = data.getDouble("score").toFloat()
                    winner          = data.getInt("winner")
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Log.d("json", "Error in retrieving data from server")
                    return@on
                }

                // Check if a message occur, (only at server side can happen as the client just receives data)
                if (error) {
                    Log.d("json", "Server error")
                    return@on
                }

                Log.d("json", "Update CPU objects")
                cpuBuilding = updateObjectCpu(cpuBuildingJson, buildings, cpuBuilding)
                cpuVehicle  = updateObjectCpu(cpuVehicleJson, vehicles, cpuVehicle)

                Log.d("json", "Update players objects")
                updateObjectPlayer(player0, player0Vehicle, 0)
                updateObjectPlayer(player1, player1Vehicle, 1)


                // Check if game ended (after update object player to get the playerNumber)
                if (messageCode == MSG_CODE_GAME_END) {
                    if (winner == playerNumber)             winner = WINNER_PLAYER     // The player wins
                    else if (winner == 0 || winner == 1)    winner = WINNER_ADVERSARY  // The player loses

                    // Otherwise keep the received winner value

                    // Remove all listeners (for any event)
                    mSocket.off()

                    // Disconnect the socket
                    mSocket.disconnect()

                    // Fragment navigation requires the main thread
                    lifecycleScope.launch(Dispatchers.Main) {
                        gameEnd()
                    }
                }

                Log.d("json", "Draw bitmaps!")
                startDrawing = true

                invalidate()
            }
        }
    }

    // Use onDraw to just draw on canvas. Game logic in multithreading
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        //Log.d("COROUTINE", "onDraw - drawing")
        //Log.d("COROUTINE", "onDraw - width: $width")
        //Log.d("COROUTINE", "onDraw - height: $height")

        // If game ended, then stop drawing
        if (gameEnd)
            return

        // Check if game has been initialized
        if (!startDrawing) {
            // Get the rect of the text to center this in the screen
            painterFill.textSize = 80f
            painterFill.getTextBounds(loadingText, 0, loadingText.length, textRect)

            // Show loading text
            canvas?.drawText(
                loadingText,
                width/2f - textRect.width()/2f - textRect.left,
                height/2f + textRect.height()/2f - textRect.bottom,
                painterFill
            )

            return
        }

        painterFill.textSize = 50f

        // Draw background
        canvas?.drawBitmap(bg, 0f, 0f, null)

        // Perform matrix transformations here because,
        // since onDraw is continuosly called (# calls to onDraw > # loops in game logic) even if I don't call invalidate,
        // it could happen that if I edit a global variable outside (e.g. matrices), then is not synchronized
        player0Vehicle.transform()
        player1Vehicle.transform()
        cpuVehicle.transform()
        cpuBuilding.transform()

        // Draw bitmaps (player bitmap in foreground with respect to adversary one)
        if (playerNumber == 0) {
            canvas?.drawBitmap(player1Vehicle.getBitmap(), player1Vehicle.getMatrix(), null)
            canvas?.drawBitmap(player0Vehicle.getBitmap(), player0Vehicle.getMatrix(), null)
        }
        else {
            canvas?.drawBitmap(player0Vehicle.getBitmap(), player0Vehicle.getMatrix(), null)
            canvas?.drawBitmap(player1Vehicle.getBitmap(), player1Vehicle.getMatrix(), null)
        }

        canvas?.drawBitmap(cpuVehicle.getBitmap(), cpuVehicle.getMatrix(), null)
        canvas?.drawBitmap(cpuBuilding.getBitmap(), cpuBuilding.getMatrix(), null)

        canvas?.drawText("SCORE " + score.toLong(), 20f, 60f, painterFill)
    }


    // Extract the game object id by object name
    private fun getGameObjectIndexByName(gameObjectName: String): Int {
        var i = -1

        try {
            i = gameObjectName.substring(gameObjectName.length - 2).toInt() - 1
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return i
    }

    private fun updateObjectCpu(jsonObject: JSONObject, cpuObjects: Array<Object>, cpuObject: Object): Object {
        //val id: Int
        val id: String
        val posX: Float
        val posY: Float
        //val width: Float
        //val height: Float

        try {
            //id     = jsonObject.getInt("id")
            id     = jsonObject.getString("id")
            posX   = jsonObject.getDouble("pos_x").toFloat()
            posY   = jsonObject.getDouble("pos_y").toFloat()
            //width  = jsonObject.getDouble("width").toFloat()
            //height = jsonObject.getDouble("height").toFloat()
        } catch (e: JSONException) {
            e.printStackTrace()
            Log.d("json", "Error in retrieving data about cpu object")
            return cpuObject
        }

        val o: Object
        val i = getGameObjectIndexByName(id) // Otherwise provide directly the index as id

        // Pick building object
        try {
            o = cpuObjects[i]
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
            return cpuObject
        }

        o.setX(posX * ppm)
        o.setY(posY * ppm)

        return o
    }

    private fun updateObjectPlayer(jsonObject: JSONObject, playerObject: ObjectPlayer, playerNumber: Int) {
        //val id: Int
        val id: String
        val posX: Float
        val posY: Float
        val rotation: Float
        //val width: Float
        //val height: Float

        try {
            //id     = jsonObject.getInt("id")
            id       = jsonObject.getString("id")
            posX     = jsonObject.getDouble("pos_x").toFloat()
            posY     = jsonObject.getDouble("pos_y").toFloat()
            rotation = jsonObject.getDouble("rotation").toFloat()
            //width  = jsonObject.getDouble("width").toFloat()
            //height = jsonObject.getDouble("height").toFloat()
        } catch (e: JSONException) {
            e.printStackTrace()
            Log.d("json", "Error in retrieving data about player")
            return
        }

        // Version where player always has the same bitmap and the adversary the other.
        // Each one sees he controls 0 and the other 1 but if the two users play one beside the other,
        // it can look "strange" because apparently both control 0
        /*if (id == PLAYER_ID) {
            playerObject.animate()
            player0Vehicle.setX(posX)
            player0Vehicle.setY(posY)
            playerObject.setRotation(rotation)
        }
        else {
            playerObject.animate()
            player1Vehicle.setX(posX)
            player1Vehicle.setY(posY)
            playerObject.setRotation(rotation)
        }*/


        // Version where the player can be player0Vehicle or player1Vehicle
        // and the adversary the other one
        playerObject.animate()
        playerObject.setX(posX * ppm)
        playerObject.setY(posY * ppm)
        playerObject.setRotation(rotation)

        // Set player number for drawing the corresponding bitmap in foreground
        if (id == PLAYER_ID)
            this@GameViewMultiplayer.playerNumber = playerNumber
    }

    private fun gameEnd() {
        gameEnd = true

        val fragment = findFragment<GameFragment>()
        fragment.gameEnd(score.toLong(), winner)
    }


    // Detect user touch
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        /*when (event?.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                // Get user click coordinates
                fx = event.x.toDouble()
                fy = height.toDouble() - event.y.toDouble()

                // Get the angle in radiant
                val rad = Math.atan2(fy, fx)

                // Convert radiant to degrees and clamp max value for graphics reason
                ang = Math.toDegrees(rad).toFloat() / 2
                Log.d("angle", "ang: " + ang)

                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                fired = true
                previous = System.currentTimeMillis()

                // Calculate sin and cosine of the non-clamped angle
                val sinAng = sin( Math.toRadians(ang.toDouble()) + Math.PI/2 ).toFloat()
                val cosAng = cos( Math.toRadians(ang.toDouble()) ).toFloat()

                // Calculate cannon ball center
                //cx = cosAng*cannonX - sinAng*cannonY
                //cy = sinAng*cannonX + cosAng*cannonY

                cx = (cannon.width*heli_scale + radius) * cosAng
                cy = (height.toFloat() - cannon.height*heli_scale + radius) * sinAng

                Log.d("ball", "cx: " + cx + "\tcy: " + cy)

                // Set velocity components of the cannon ball
                vx = speed * cos( Math.toRadians(ang.toDouble() * 2) ).toFloat()
                vy = speed * sin( Math.toRadians(ang.toDouble() * 2) ).toFloat()

                invalidate()
            }
        }*/

        return true
    }

    // Detect user motion via sensors
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onFlushCompleted(sensor: Sensor?) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!startDrawing)
            return

        when (event?.sensor?.type) {
            //Sensor.TYPE_ACCELEROMETER  -> lastAcceleration = event.values.clone()
            //Sensor.TYPE_MAGNETIC_FIELD      -> gyroscopeValues    = event.values.clone()
            Sensor.TYPE_GYROSCOPE -> gyroscopeValues = event.values.clone()
        }

        // Use X value if portrait mode, Y value otherwise
        val value =
            if (width < height)                gyroscopeValues[0]        // Portrait
            else if (display?.rotation == 1)   gyroscopeValues[1] * -1f  // Landscape
            else                               gyroscopeValues[1]        // Reverse landscape

        // Enhance gyroscopeValues value
        val currentGyroscopeInput =
            if (value > 0f)         max(1f, min(value * 10f, 20f))
            else if (value < 0f)    min(-1f, max(value * 10f, -20f))
            else                    0f

        // Avoid to send useless data to the server
        if (currentGyroscopeInput == 0f || currentGyroscopeInput == previousGyroscopeInput) {
            previousGyroscopeInput = currentGyroscopeInput
            return
        }

        previousGyroscopeInput = currentGyroscopeInput

        /*Log.d("SENSOR", "****************")
        Log.d("SENSOR", "value: $value")
        Log.d("SENSOR", "rotation: $rotation")
        Log.d("SENSOR", "display.rotation: ${display?.rotation}")*/

        // Send to server
        val json = JSONObject()
        json.put("who", PLAYER_ID)
        json.put("req", REQ_CODE_NEW_MOVE)
        json.put("value", currentGyroscopeInput)

        mSocket.emit("new move request", json)
        Log.d("json", "new move request sent")
    }









    // TEMP HERE (SIMILAR TO GAME OBJECT BUT AT A HIGH LEVEL, LESS PARAMETERS (i.e. bounds offsets and speed))
    open class Object(
            name: String,
            bitmaps: Array<Bitmap>,
            screen_width: Int, screen_height: Int, ppm: Float,
            width: Float, height: Float,
            pos_x: Float, pos_y: Float) {

        private val name: String

        // Array of bitmaps for animating the game object
        private val bitmaps: Array<Bitmap>
        private var bitmap: Bitmap
        private var bitmapAnimationIndex: Int

        // Matrix for graphics transformations
        private val matrix: Matrix

        private val screenWidth: Int
        private val screenHeight: Int

        // Pixels per meter
        private val ppm: Float

        private var bitmapScaleX: Float
        private var bitmapScaleY: Float

        private var bitmapWidthScaled: Float
        private var bitmapHeightScaled: Float

        private var posX: Float
        private var posY: Float


        init {
            this.name = name

            this.bitmaps = bitmaps.clone()
            bitmapAnimationIndex = 0
            bitmap = bitmaps[bitmapAnimationIndex]

            this.matrix  = Matrix()

            this.screenWidth  = screen_width
            this.screenHeight = screen_height

            this.ppm = ppm

            // Convert meters into pixels
            bitmapWidthScaled  = width * ppm
            bitmapHeightScaled = height * ppm

            // Get bitmap scale
            bitmapScaleX = bitmapWidthScaled / screen_width
            bitmapScaleY = bitmapHeightScaled / screen_height

            // Convert meters into pixels (once and not continuosly in update() because matrix translation works with pixels)
            this.posX = pos_x * ppm
            this.posY = pos_y * ppm
        }


        // Getters
        fun getName(): String {
            return name
        }

        fun getBitmaps(): Array<Bitmap> {
            return bitmaps.clone()
        }

        fun getBitmap(): Bitmap {
            return bitmap
        }

        fun getMatrix(): Matrix {
            return matrix
        }

        fun getScreenWidth(): Int {
            return screenWidth
        }

        fun getScreenHeight(): Int {
            return screenHeight
        }

        fun getPPM(): Float {
            return ppm
        }

        fun getBitmapScaleX(): Float {
            return bitmapScaleX
        }

        fun getBitmapScaleY(): Float {
            return bitmapScaleY
        }

        fun getBitmapScaledWidth(): Float {
            return bitmapWidthScaled
        }

        fun getBitmapScaledHeight(): Float {
            return bitmapHeightScaled
        }

        fun getX(): Float {
            return posX  // In pixels (for efficiency of update calls)
        }

        fun getY(): Float {
            return posY  // In pixels (for efficiency of update calls)
        }


        // Setters
        fun setX(pos_x: Float) {
            this.posX = pos_x  // In pixels (both in input and output for efficiency of update calls)
        }

        fun setY(pos_y: Float) {
            this.posY = pos_y  // In pixels (both in input and output for efficiency of update calls)
        }


        fun animate () {
            // Select next bitmap for the animation frame
            if (bitmaps.size > 1) {
                bitmapAnimationIndex = (bitmapAnimationIndex + 1) % bitmaps.size
                bitmap = bitmaps[bitmapAnimationIndex]
            }
        }

        // Perform graphics transformations
        open fun transform() {
            matrix.setScale(bitmapScaleX, bitmapScaleY)
            matrix.postTranslate(posX, posY)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Object

            if (name != other.name) return false
            //if (!bitmaps.contentDeepEquals(other.bitmaps)) return false
            if (bitmap != other.bitmap) return false
            if (bitmapScaleX != other.bitmapScaleX) return false
            if (bitmapScaleY != other.bitmapScaleY) return false
            if (bitmapWidthScaled != other.bitmapWidthScaled) return false
            if (bitmapHeightScaled != other.bitmapHeightScaled) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            //result = 31 * result + bitmaps.contentDeepHashCode()
            result = 31 * result + bitmap.hashCode()
            result = 31 * result + bitmapScaleX.hashCode()
            result = 31 * result + bitmapScaleY.hashCode()
            result = 31 * result + bitmapWidthScaled.hashCode()
            result = 31 * result + bitmapHeightScaled.hashCode()
            return result
        }

    }

    class ObjectPlayer(
            name: String,
            bitmaps: Array<Bitmap>,
            screen_width: Int, screen_height: Int, ppm: Float,
            width: Float, height: Float,
            pos_x: Float, pos_y: Float) :
        Object(name, bitmaps, screen_width, screen_height, ppm, width, height, pos_x, pos_y) {

        private var bitmapRotation = 8f

        // Getters
        fun getRotation(): Float {
            return bitmapRotation  // In degrees (for efficiency of update calls)
        }


        // Setters
        fun setRotation(rotation: Float) {
            bitmapRotation = rotation  // In degrees (for efficiency of update calls)
        }


        // Perform graphics transformations
        override fun transform() {
            val m = getMatrix()
            m.setScale(getBitmapScaleX(), getBitmapScaleY())
            m.postRotate(bitmapRotation)
            m.postTranslate(getX(), getY())
        }

    }

}


