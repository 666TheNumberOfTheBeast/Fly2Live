package com.pumpkinsoftware.fly2live

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.os.Build
import android.os.Parcelable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.SCENARIO
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.SCENARIO_CITY_DAY
import kotlinx.coroutines.*
import java.lang.Thread.sleep
import android.os.Bundle
import androidx.activity.addCallback
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_01_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_01_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_01_NIGHT_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_01_NIGHT_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_02_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_02_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_02_NIGHT_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_02_NIGHT_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_03_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_03_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_03_NIGHT_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_03_NIGHT_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_04_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_04_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_04_NIGHT_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_04_NIGHT_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_05_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_05_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_05_NIGHT_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_05_NIGHT_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_06_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_06_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_06_NIGHT_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_06_NIGHT_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_07_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_07_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_07_NIGHT_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_07_NIGHT_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_08_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_08_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_08_NIGHT_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_08_NIGHT_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_09_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_09_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_09_NIGHT_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_09_NIGHT_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_10_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_10_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_10_NIGHT_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_10_NIGHT_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_11_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_11_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_11_NIGHT_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_11_NIGHT_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_12_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_12_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_13_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_13_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_14_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_14_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_15_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_15_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_16_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_16_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_17_DAY_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.BUILDING_17_DAY_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.SCENARIO_CITY_NIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_01_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_01_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_02_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_02_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_03_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_03_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_04_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_04_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_05_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_05_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_06_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_06_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_07_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_07_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_08_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_08_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_09_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.VEHICLE_09_WIDTH
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.WINNER_UNDEFINED
import com.pumpkinsoftware.fly2live.game_object_cpu.GameObjectCpu
import com.pumpkinsoftware.fly2live.game_object_player.GameObjectPlayer
import kotlin.math.*
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.PLAYER_HEIGHT
import com.pumpkinsoftware.fly2live.configuration.Configuration.Companion.PLAYER_WIDTH


class GameView(context: Context?, fragment: GameFragment) : View(context), View.OnTouchListener, SensorEventListener2 {
    // Fragment associated with this view
    private val fragment = fragment

    // Background bitmap
    private lateinit var bg: Bitmap

    // Player's game object
    private lateinit var playerVehicle: GameObjectPlayer

    // CPU's game objects
    private lateinit var buildings: Array<GameObjectCpu>
    private lateinit var cpuBuilding: GameObjectCpu
    private lateinit var vehicles: Array<GameObjectCpu>
    private lateinit var cpuVehicle: GameObjectCpu

    // Explosion bitmaps for game end
    private lateinit var explosionBitmaps: Array<Bitmap>
    private var explosionAnimationIndex = 0


    // Sensors values
    private var sensorManager: SensorManager
    //private var lastAcceleration = FloatArray(3)
    private var gyroscopeValues = FloatArray(3)
    private var lastGyroscopeInput = 0f // meters


    // World constants
    private val worldWidth  = 55f // meters 50f
    //private var worldHeight = 60f // meters

    // Pixel per meter
    private var ppm = 1f

    private var speed = 10f // m/s
    private var score = 0f  // meters traveled


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

        sensorManager = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager

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

        // Override back behavior
        //fragment.requireActivity().onBackPressedDispatcher.addCallback(fragment) {
        (context as FragmentActivity).onBackPressedDispatcher.addCallback(fragment) {
            gameEnd = true

            sensorManager.unregisterListener(this@GameView)

            //val fragment = findFragment<GameFragment>()
            fragment.gameEnd(score.toLong(), WINNER_UNDEFINED)
        }


        Log.d("ppm", "***** INITIALIZATION *****")
        // Don't use directly getWidth() and getHeight() here because they return 0
        //Log.d("ppm", "width: $width")
        //Log.d("ppm", "height: $height")

        // Get screen width and height
        // METHOD 1 - Returns the screen size excluding the status and navigation bars
        /*var screenWidth  = resources.displayMetrics.widthPixels
        var screenHeight = resources.displayMetrics.heightPixels
        Log.d("ppm", "screenWidth: $screenWidth")
        Log.d("ppm", "screenHeight: $screenHeight")

        // Returns the screen size excluding the navigation bar
        if (screenWidth < screenHeight)
            screenHeight += getResourceDim("status_bar_height")
        else
            screenWidth += getResourceDim("status_bar_width") // non ok
        Log.d("ppm", "screenWidth (including status bar width): $screenWidth")
        Log.d("ppm", "screenHeight (including status bar height): $screenHeight")


        // METHOD 2 - Returns the screen size including the status and navigation bars (returns zero when device rotates)
        screenWidth  = context.window.decorView.width
        screenHeight = context.window.decorView.height
        Log.d("ppm", "screen width decorView: $screenWidth")
        Log.d("ppm", "screen height decorView: $screenHeight")

        // Returns the screen size excluding the navigation bar
        if (screenWidth < screenHeight)
            screenHeight -= getNavBarHeight()
        else
            screenWidth -= getNavBarWidth()
        Log.d("ppm", "screen width decorView: $screenWidth")
        Log.d("ppm", "screen height decorView (excluding nav bar height): $screenHeight")

        init(screenWidth, screenHeight)*/

        // METHOD 3 - use post
        // Post queues the code after the view's measure
        post {
            Log.d("ppm", "width: $width")
            Log.d("ppm", "height: $height")

            init(width, height)

            // Returns the screen size including the status and navigation bars
            /*var screenWidth  = context.window.decorView.width
            var screenHeight = context.window.decorView.height
            Log.d("ppm", "screen width decorView: $screenWidth")
            Log.d("ppm", "screen height decorView: $screenHeight")

            // Returns the screen size excluding the navigation bar
            if (screenWidth < screenHeight)
                screenHeight -= getNavBarHeight()
            else
                screenWidth -= getNavBarWidth()
            Log.d("ppm", "screen width decorView (excluding nav bar width): $screenWidth")
            Log.d("ppm", "screen height decorView (excluding nav bar height): $screenHeight")

            init(screenWidth, screenHeight)*/
        }
    }

    /*private fun getResourceDim(res: String): Int {
        val resId = resources.getIdentifier(res, "dimen", "android")
        if (resId > 0)
            return resources.getDimensionPixelSize(resId)
        return 0
    }*/

    /*private fun getNavBarWidth(): Int {
        val navBar = resources.getIdentifier("navigation_bar_width", "dimen", "android")
        if (navBar > 0)
            return resources.getDimensionPixelSize(navBar)
        return 0
    }

    private fun getNavBarHeight(): Int {
        val navBar = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (navBar > 0)
            return resources.getDimensionPixelSize(navBar)
        return 0
    }*/

    // Multithreading version
    private fun init(screenWidth: Int, screenHeight: Int) {
        // Dispatchers.Default — is used by all standard builders if no dispatcher or any other ContinuationInterceptor is specified in their context.
        // It uses a common pool of shared background threads.
        // This is an appropriate choice for compute-intensive coroutines that consume CPU resources
        fragment.lifecycleScope.launch(Dispatchers.Default) {

            // Calculate scale factor

            // ===========================================
            // OPZIONE 1, NON SI OTTIENE L'EFFETTO DESIDERATO CHE
            // SI OTTERREBBE TOGLIENDO LO SCALE FACTOR ALLA WIDTH DELLE BITMAP MA VERREBBERO DISTORTE
            // PERCHÈ ANDREBBE COMUNQUE APPLICATO ALLA HEIGHT!
            // SITUAZIONE RIMANE INVARIATA, IMMAGINI RIDOTTE IN 36 m PER LANDSCAPE, COME VEDERE PIÙ CAMPO VISIVO

            // PORTRAIT
            //ppm = screenHeight / worldHeight
            //currentWorldWidth_1 = screenWidth / ppm

            // LANDSCAPE
            //ppm = screenHeight / (worldHeight * scaleFactor)
            //currentWorldWidth_2 = screenWidth / ppm

            // I want that the 2 currentWorldWidths are the same.
            // I know that height_1 = width_2 and width_1 = height_2 .
            // => width_1 / ppm_1 = width_2 / ppm_2
            // => (width_1 * worldHeight) / height_1 = (width_2 * worldHeight * scaleFactor) / height_2
            // => width_1 / height_1 = (width_2 * scaleFactor) / height_2
            // => scaleFactor = (width_1 * height_2) / (height_1 * width_2)
            // => scaleFactor = (width_1)^2 / (height_1)^2 = (height_2)^2 / (width_2)^2

            // Set the scale factor based on device orientation in order to have the same worldWidth in both modes
            //val scaleFactor = if (screenWidth < screenHeight) 1f else (screenHeight * screenHeight * 1f) / (screenWidth * screenWidth)

            // NO! SAME RELATIVE HEIGHT
            /*val scaleFactor =
                if (screenWidth < screenHeight) (screenWidth * screenWidth * 1f) / (screenHeight * screenHeight)
                else                (screenHeight * screenHeight * 1f) / (screenWidth * screenWidth)*/
            // ===========================================

            // ===========================================
            // OPZIONE 2, NO, PPM ESPLODE E SI PERDONO LE PROPORZIONI VERTICALI!

            // PORTRAIT
            //ppm = screenHeight / worldHeight
            //currentWorldWidth_1 = screenWidth / ppm

            // LANDSCAPE
            //ppm = (screenHeight / worldHeight) * scaleFactor
            //currentWorldWidth_2 = screenWidth / ppm

            // I want that the 2 currentWorldWidths are the same.
            // I know that height_1 = width_2 and width_1 = height_2 .
            // => width_1 / ppm_1 = width_2 / ppm_2
            // => (width_1 * worldHeight) / height_1 = (width_2 * worldHeight) / (height_2 * scaleFactor)
            // => width_1 / height_1 = width_2 / (height_2 * scaleFactor)
            // => scaleFactor = (width_2 * height_1) / (height_2 * width_1)
            // => scaleFactor = (height_1)^2 / (width_1)^2 = (width_2)^2 / (height_2)^2

            // Set the scale factor based on device orientation in order to have the same worldWidth in both modes
            //val scaleFactor = if (screenWidth < screenHeight) 1f else (screenWidth * screenWidth * 1f) / (screenHeight * screenHeight)
            // ===========================================

            // ===========================================
            // OPZIONE 3, LA MIGLIORE FINORA!

            // Set the scale factor based on device orientation in order to have similar worldWidth in both modes
            //val scaleFactor = if (screenWidth < screenHeight) 1f else 0.2f //0.4f
            // ===========================================

            val scaleFactor = 1f



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
            val initialObstaclePosX = currentWorldWidth + 3f  // In meters

            val loadBuildings = async {
                Log.d("COROUTINE", "Load buildings")

                val initialObstaclePosY = 40f  // In meters

                // Convert the correct scenario images into bitmaps once
                if (SCENARIO == SCENARIO_CITY_DAY) {
                    bg = ResourcesCompat.getDrawable(resources, R.drawable.city_bg_day, null)?.toBitmap(screenWidth, screenHeight)!!


                    // Consider that with a fixed width of 50 m,
                    // current device resolutions produces an height of 20 - 30 m if lanscape and 80 - 100 m if portrait.

                    // GameObjectCpu uses bounds offset based on bitmap's screenWidth and screenHeight in order to be consistent with any value of the world's width and height
                    buildings = arrayOf(
                        GameObjectCpu(
                            "building_01",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_01, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.048f, 1f, 1f),       // Bottom rect
                                arrayOf(0.07f, 0.031f, 0.38f, 1f), // Middle left rect
                                arrayOf(0.47f, 0.031f, 0.8f, 1f),  // Middle right rect
                                arrayOf(0.1f, 0f, 0.18f, 1f),      // Top left rect
                                arrayOf(0.47f, 0.023f, 0.63f, 1f)  // Top right rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_01_DAY_WIDTH * scaleFactor, BUILDING_01_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed // Measures in meters except pos_y in pixels
                        ),
                        GameObjectCpu(
                            "building_02",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_02,null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0.18f, 0.365f, 0.82f, 1f),  // Bottom rect
                                arrayOf(0.33f, 0.187f, 0.7f, 1f),   // Middle rect
                                arrayOf(0.47f, 0f, 0.56f, 1f)       // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_02_DAY_WIDTH * scaleFactor, BUILDING_02_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_03",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_03, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.16f, 1f, 1f),         // Bottom rect
                                arrayOf(0.13f, 0.105f, 0.86f, 1f),  // Middle rect 1
                                arrayOf(0.28f, 0.045f, 0.68f, 1f),  // Middle rect 2
                                arrayOf(0.45f, 0f, 0.6f, 1f)        // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_03_DAY_WIDTH * scaleFactor, BUILDING_03_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_04",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_04, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0.13f, 0.28f, 0.86f, 1f),  // Bottom rect
                                arrayOf(0.21f, 0.173f, 0.79f, 1f), // Middle rect 1
                                arrayOf(0.23f, 0.07f, 0.76f, 1f),  // Middle rect 2
                                arrayOf(0.31f, 0.003f, 0.67f, 1f)  // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_04_DAY_WIDTH * scaleFactor, BUILDING_04_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_05",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_05, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.185f, 1f, 1f),       // Bottom rect
                                arrayOf(0f, 0.1f, 0.23f, 1f),      // Middle left rect
                                arrayOf(0.27f, 0.136f, 0.75f, 1f), // Middle center rect
                                arrayOf(0.77f, 0.136f, 0.9f, 1f),  // Middle right rect
                                arrayOf(0.33f, 0f, 0.75f, 1f)      // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_05_DAY_WIDTH * scaleFactor, BUILDING_05_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_06",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_06, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.55f, 1f, 1f),       // Bottom rect
                                arrayOf(0f, 0.14f, 0.36f, 1f),    // Middle left rect
                                arrayOf(0.53f, 0.32f, 0.86f, 1f), // Middle right rect
                                arrayOf(0.07f, 0f, 0.29f, 1f)     // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_06_DAY_WIDTH * scaleFactor, BUILDING_06_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed // Measures in meters except pos_y
                        ),
                        GameObjectCpu(
                            "building_07",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_07, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.291f, 1f, 1f),      // Bottom rect
                                arrayOf(0f, 0.15f, 0.91f, 1f),    // Middle rect
                                arrayOf(0f, 0.13f, 0.22f, 1f),    // Left "triangle"
                                arrayOf(0.7f, 0.13f, 0.91f, 1f),  // Right "triangle"
                                arrayOf(0.31f, 0.06f, 0.39f, 1f), // Top rect 1
                                arrayOf(0.31f, 0f, 0.33f, 1f)     // Top rect 2
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_07_DAY_WIDTH * scaleFactor, BUILDING_07_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed // Measures in meters except pos_y
                        ),
                        GameObjectCpu(
                            "building_08",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_08, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.123f, 1f, 1f),  // Bottom rect
                                arrayOf(0.1f, 0f, 0.9f, 1f)   // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_08_DAY_WIDTH * scaleFactor, BUILDING_08_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_09",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_09, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.115f, 1f, 1f),       // Bottom rect
                                arrayOf(0.28f, 0.095f, 0.7f, 1f),  // Middle rect 1
                                arrayOf(0.4f, 0.026f, 0.5f, 1f),   // Middle rect 2
                                arrayOf(0.43f, 0.003f, 0.48f, 1f)  // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_09_DAY_WIDTH * scaleFactor, BUILDING_09_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_10",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_10, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.094f, 1f, 1f),        // Bottom rect
                                arrayOf(0.09f, 0.087f, 0.96f, 1f),  // Middle rect 1
                                arrayOf(0.13f, 0.015f, 0.29f, 1f),  // Top rect left
                                arrayOf(0.4f, 0f, 0.7f, 1f),        // Top rect middle
                                arrayOf(0.75f, 0.026f, 0.92f, 1f)   // Top rect right
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_10_DAY_WIDTH * scaleFactor, BUILDING_10_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_11",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_11, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.03f, 1f, 1f),    // Bottom rect
                                arrayOf(0.15f, 0f, 0.57f, 1f)  // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_11_DAY_WIDTH * scaleFactor, BUILDING_11_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_12",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_12, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.042f, 1f, 1f),  // Bottom rect
                                arrayOf(0.06f, 0f, 0.9f, 1f)  // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_12_DAY_WIDTH * scaleFactor, BUILDING_12_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_13",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_13, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.11f, 1f, 1f),        // Bottom rect
                                arrayOf(0.05f, 0.01f, 0.15f, 1f),  // Top rect left
                                arrayOf(0.15f, 0f, 0.82f, 1f),     // Top rect middle
                                arrayOf(0.83f, 0.04f, 1f, 1f),     // Top rect right 1
                                arrayOf(0.86f, 0f, 0.94f, 1f)      // Top rect right 2
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_13_DAY_WIDTH * scaleFactor, BUILDING_13_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_14",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_14, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.185f, 1f, 1f),        // Bottom rect
                                arrayOf(0.29f, 0.065f, 0.68f, 1f),  // Middle rect
                                arrayOf(0.53f, 0.035f, 0.59f, 1f),  // Top rect left
                                arrayOf(0.34f, 0f, 0.35f, 1f)       // Top rect right
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_14_DAY_WIDTH * scaleFactor, BUILDING_14_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_15",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_15, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.43f, 1f, 1f),         // Bottom rect
                                arrayOf(0.1f, 0.3f, 0.86f, 1f),     // Middle rect 1
                                arrayOf(0.25f, 0.256f, 0.71f, 1f),  // Middle rect 2
                                arrayOf(0.28f, 0.236f, 0.68f, 1f),  // Middle rect 2
                                arrayOf(0.4f, 0.18f, 0.57f, 1f),    // Top rect 1
                                arrayOf(0.53f, 0f, 0.57f, 1f)       // Top rect 2
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_15_DAY_WIDTH * scaleFactor, BUILDING_15_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_16",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_16, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.2f, 1f, 1f),          // Bottom rect
                                arrayOf(0.13f, 0.175f, 0.87f, 1f),  // Middle rect 1
                                arrayOf(0.24f, 0.147f, 0.76f, 1f),  // Middle rect 2
                                arrayOf(0.28f, 0.122f, 0.49f, 1f),  // Middle rect 3
                                arrayOf(0.53f, 0f, 0.55f, 1f),      // Top rect left
                                arrayOf(0.6f, 0.039f, 0.62f, 1f),   // Top rect middle
                                arrayOf(0.68f, 0.03f, 0.71f, 1f)    // Top rect right
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_16_DAY_WIDTH * scaleFactor, BUILDING_16_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_17",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_17, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.094f, 1f, 1f),       // Bottom rect
                                arrayOf(0.15f, 0.06f, 0.88f, 1f),  // Middle rect
                                arrayOf(0.41f, 0f, 0.63f, 1f)      // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_17_DAY_WIDTH * scaleFactor, BUILDING_17_DAY_HEIGHT * scaleFactor,
                            initialObstaclePosX, initialObstaclePosY, speed
                        )
                    )
                }
                else {
                    bg = ResourcesCompat.getDrawable(resources,
                        R.drawable.city_bg_night, null)?.toBitmap(screenWidth, screenHeight)!!

                    buildings = arrayOf(
                        GameObjectCpu(
                            "building_01",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_01, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.19f, 1f, 1f),        // Bottom rect
                                arrayOf(0.05f, 0.175f, 1f, 1f),    // Bottom rect
                                arrayOf(0.05f, 0.151f, 0.92f, 1f), // Middle rect 1
                                arrayOf(0.85f, 0.135f, 0.92f, 1f), // Middle rect 1
                                arrayOf(0.1f, 0.12f, 0.86f, 1f),   // Middle rect 2
                                arrayOf(0.78f, 0.105f, 0.86f, 1f), // Middle rect 2
                                arrayOf(0.2f, 0.087f, 0.78f, 1f),  // Top rect
                                arrayOf(0.25f, 0.077f, 0.72f, 1f), // Top rect
                                arrayOf(0.3f, 0.067f, 0.65f, 1f),  // Top rect
                                arrayOf(0.35f, 0.06f, 0.6f, 1f)    // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_01_NIGHT_WIDTH, BUILDING_01_NIGHT_HEIGHT,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_02",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_02, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0.26f, 0.23f, 0.72f, 1f),  // Bottom rect
                                arrayOf(0.4f, 0.1f, 0.6f, 1f),    // Middle rect
                                arrayOf(0.45f, 0f, 0.54f, 1f)     // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_02_NIGHT_WIDTH, BUILDING_02_NIGHT_HEIGHT,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_03",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_03, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0.75f, 0.072f, 1f, 1f),   // Bottom rect
                                arrayOf(0f, 0.045f, 0.75f, 1f),   // Bottom rect
                                arrayOf(0.68f, 0.02f, 0.82f, 1f), // Top rect
                                arrayOf(0.16f, 0f, 0.68f, 1f)     // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_03_NIGHT_WIDTH, BUILDING_03_NIGHT_HEIGHT,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_04",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_04, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.11f, 0.24f, 1f),   // Bottom rect
                                arrayOf(0.24f, 0.085f, 1f, 1f),   // Bottom rect
                                arrayOf(0.16f, 0.02f, 0.3f, 1f), // Top rect
                                arrayOf(0.3f, 0f, 0.82f, 1f)     // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_04_NIGHT_WIDTH, BUILDING_04_NIGHT_HEIGHT,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_05",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_05,null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.09f, 1f, 1f),      // Bottom rect
                                arrayOf(0f, 0.031f, 0.7f, 1f),   // Middle rect 1
                                arrayOf(0.45f, 0.01f, 0.6f, 1f), // Middle rect 2
                                arrayOf(0.7f, 0.05f, 0.8f, 1f),  // Middle rect 3
                                arrayOf(0.8f, 0.07f, 0.9f, 1f),  // Middle rect 4
                                arrayOf(0.13f, 0f, 0.45f, 1f)    // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_05_NIGHT_WIDTH, BUILDING_05_NIGHT_HEIGHT,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_06",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_06, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.11f, 1f, 1f),        // Bottom rect
                                arrayOf(0.05f, 0.09f, 0.95f, 1f),  // Middle rect 1
                                arrayOf(0.1f, 0.07f, 0.9f, 1f),    // Middle rect 2
                                arrayOf(0.15f, 0.057f, 0.86f, 1f), // Middle rect 3
                                arrayOf(0.18f, 0.048f, 0.82f, 1f), // Middle rect 4
                                arrayOf(0.23f, 0.04f, 0.76f, 1f),  // Middle rect 5
                                arrayOf(0.28f, 0.03f, 0.72f, 1f),  // Middle rect 6
                                arrayOf(0.33f, 0.02f, 0.66f, 1f),  // Middle rect 7
                                arrayOf(0.37f, 0.015f, 0.63f, 1f), // Middle rect 8
                                arrayOf(0.41f, 0.009f, 0.59f, 1f), // Middle rect 9
                                arrayOf(0.47f, 0f, 0.52f, 1f)      // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_06_NIGHT_WIDTH, BUILDING_06_NIGHT_HEIGHT,
                            initialObstaclePosX, initialObstaclePosY, speed // Measures in meters except pos_y
                        ),
                        GameObjectCpu(
                            "building_07",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_07, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.09f, 0.93f, 1f),     // Bottom rect
                                arrayOf(0.06f, 0.05f, 0.87f, 1f),  // Middle rect
                                arrayOf(0.19f, 0.01f, 0.74f, 1f),  // Middle rect
                                arrayOf(0.26f, 0f, 0.67f, 1f)      // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_07_NIGHT_WIDTH, BUILDING_07_NIGHT_HEIGHT,
                            initialObstaclePosX, initialObstaclePosY, speed // Measures in meters except pos_y
                        ),
                        GameObjectCpu(
                            "building_08",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_08, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.02f, 1f, 1f),       // Bottom rect
                                arrayOf(0.1f, 0.015f, 0.95f, 1f), // Middle rect 1
                                arrayOf(0.27f, 0.01f, 0.9f, 1f),  // Middle rect 2
                                arrayOf(0.47f, 0.05f, 0.8f, 1f),  // Middle rect 3
                                arrayOf(0.65f, 0f, 0.75f, 1f)     // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_08_NIGHT_WIDTH, BUILDING_08_NIGHT_HEIGHT,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_09",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_09, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.022f, 1f, 1f),        // Bottom rect
                                arrayOf(0.08f, 0.015f, 0.93f, 1f),  // Middle rect 1
                                arrayOf(0.15f, 0.008f, 0.87f, 1f),  // Middle rect 2
                                arrayOf(0.23f, 0f, 0.77f, 1f)       // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_09_NIGHT_WIDTH, BUILDING_09_NIGHT_HEIGHT,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_10",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_10, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0.25f, 0.029f, 1f, 1f),   // Bottom rect
                                arrayOf(0.33f, 0.02f, 0.87f, 1f)  // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_10_NIGHT_WIDTH, BUILDING_10_NIGHT_HEIGHT,
                            initialObstaclePosX, initialObstaclePosY, speed
                        ),
                        GameObjectCpu(
                            "building_11",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_11, null)?.toBitmap(screenWidth, screenHeight)!!),
                            arrayOf(
                                arrayOf(0f, 0.183f, 1f, 1f),     // Bottom rect
                                arrayOf(0.1f, 0.16f, 0.9f, 1f),  // Middle rect
                                arrayOf(0.2f, 0.14f, 0.8f, 1f),  // Middle rect
                                arrayOf(0.3f, 0.125f, 0.7f, 1f), // Middle rect
                                arrayOf(0.4f, 0.09f, 0.54f, 1f), // Middle rect
                                arrayOf(0.46f, 0f, 0.5f, 1f)     // Top rect
                            ),
                            screenWidth, screenHeight, ppm,
                            BUILDING_11_NIGHT_WIDTH, BUILDING_11_NIGHT_HEIGHT,
                            initialObstaclePosX, initialObstaclePosY, speed
                        )
                    )
                }

                pickBuilding(screenWidth, screenHeight)
            }

            val loadVehicles = async {
                Log.d("COROUTINE", "Load vehicles")

                val initialObstaclePosY = 5f  // In meters

                vehicles = arrayOf(
                    GameObjectCpu(
                        "vehicle_01",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_biplane, null)?.toBitmap(screenWidth, screenHeight)!!),
                        arrayOf(
                            arrayOf(0f, 0.3f, 0.2f, 1f),    // Bottom rect
                            arrayOf(0f, 0.3f, 1f, 0.77f),   // Middle rect
                            arrayOf(0.1f, 0f, 0.4f, 0.6f)   // Top rect
                        ),
                        screenWidth, screenHeight, ppm,
                        VEHICLE_01_WIDTH * scaleFactor, VEHICLE_01_HEIGHT * scaleFactor,
                        initialObstaclePosX, initialObstaclePosY, speed // Measures in meters except pos_y
                    ),

                    GameObjectCpu(
                        "vehicle_02",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_1, null)?.toBitmap(screenWidth, screenHeight)!!),
                        arrayOf(
                            arrayOf(0.27f, 0.77f, 0.57f, 1f),     // Bottom rect
                            arrayOf(0f, 0.2f, 1f, 0.6f),          // Middle rect 1
                            arrayOf(0.08f, 0.1f, 0.75f, 0.7f),    // Middle rect 2
                            arrayOf(0.15f, 0.01f, 0.65f, 0.78f),  // Middle rect 3
                            arrayOf(0.75f, 0f, 0.93f, 0.78f)      // Top rect
                        ),
                        screenWidth, screenHeight, ppm,
                        VEHICLE_02_WIDTH * scaleFactor, VEHICLE_02_HEIGHT * scaleFactor,
                        initialObstaclePosX, initialObstaclePosY, speed
                    ),
                    GameObjectCpu(
                        "vehicle_03",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_2, null)?.toBitmap(screenWidth, screenHeight)!!),
                        arrayOf(
                            arrayOf(0.24f, 0.8f, 0.4f, 1f),  // Bottom rect
                            arrayOf(0f, 0.05f, 1f, 0.85f),   // Middle rect
                            arrayOf(0.77f, 0f, 1f, 0.92f)    // Top rect
                        ),
                        screenWidth, screenHeight, ppm,
                        VEHICLE_03_WIDTH * scaleFactor, VEHICLE_03_HEIGHT * scaleFactor,
                        initialObstaclePosX, initialObstaclePosY, speed
                    ),
                    GameObjectCpu(
                        "vehicle_04",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_3, null)?.toBitmap(screenWidth, screenHeight)!!),
                        arrayOf(
                            arrayOf(0.28f, 0.8f, 0.44f, 1f),      // Bottom rect
                            arrayOf(0f, 0.3f, 1f, 0.6f),          // Middle rect 1
                            arrayOf(0.05f, 0.2f, 0.15f, 0.7f),    // Middle rect 2
                            arrayOf(0.15f, 0.06f, 0.77f, 0.85f),  // Middle rect 3
                            arrayOf(0.77f, 0f, 0.92f, 0.9f)       // Top rect
                        ),
                        screenWidth, screenHeight, ppm,
                        VEHICLE_04_WIDTH * scaleFactor, VEHICLE_04_HEIGHT * scaleFactor,
                        initialObstaclePosX, initialObstaclePosY, speed
                    ),
                    GameObjectCpu(
                        "vehicle_05",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_4, null)?.toBitmap(screenWidth, screenHeight)!!),
                        arrayOf(
                            arrayOf(0.29f, 0.8f, 0.44f, 1f),    // Bottom rect
                            arrayOf(0f, 0.3f, 1f, 0.53f),       // Middle rect 1
                            arrayOf(0.05f, 0.1f, 0.86f, 0.7f),  // Middle rect 2
                            arrayOf(0.12f, 0f, 0.77f, 0.85f),   // Middle rect 3
                            arrayOf(0.86f, 0f, 0.97f, 0.85f)    // Top rect
                        ),
                        screenWidth, screenHeight, ppm,
                        VEHICLE_05_WIDTH * scaleFactor, VEHICLE_05_HEIGHT * scaleFactor,
                        initialObstaclePosX, initialObstaclePosY, speed
                    ),
                    GameObjectCpu(
                        "vehicle_06",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_5, null)?.toBitmap(screenWidth, screenHeight)!!),
                        arrayOf(
                            arrayOf(0.29f, 0.8f, 0.51f, 1f),     // Bottom rect
                            arrayOf(0f, 0.3f, 1f, 0.58f),        // Middle rect 1
                            arrayOf(0.05f, 0.1f, 0.86f, 0.76f),  // Middle rect 2
                            arrayOf(0.12f, 0f, 0.77f, 0.85f),    // Middle rect 3
                            arrayOf(0.86f, 0.12f, 0.97f, 0.8f)   // Top rect
                        ),
                        screenWidth, screenHeight, ppm,
                        VEHICLE_06_WIDTH * scaleFactor, VEHICLE_06_HEIGHT * scaleFactor,
                        initialObstaclePosX, initialObstaclePosY, speed
                    ),
                    GameObjectCpu(
                        "vehicle_07",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_6, null)?.toBitmap(screenWidth, screenHeight)!!),
                        arrayOf(
                            arrayOf(0.26f, 0.8f, 0.49f, 1f),     // Bottom rect
                            arrayOf(0f, 0.3f, 0.82f, 0.58f),     // Middle rect 1
                            arrayOf(0.05f, 0.1f, 0.71f, 0.76f),  // Middle rect 2
                            arrayOf(0.12f, 0f, 0.6f, 0.85f),     // Middle rect 3
                            arrayOf(0.82f, 0.17f, 0.99f, 0.7f)   // Top rect
                        ),
                        screenWidth, screenHeight, ppm,
                        VEHICLE_07_WIDTH * scaleFactor, VEHICLE_07_HEIGHT * scaleFactor,
                        initialObstaclePosX, initialObstaclePosY, speed
                    ),
                    GameObjectCpu(
                        "vehicle_08",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_dirigible_7, null)?.toBitmap(screenWidth, screenHeight)!!),
                        arrayOf(
                            arrayOf(0.32f, 0.8f, 0.48f, 1f),      // Bottom rect
                            arrayOf(0f, 0.25f, 0.07f, 0.65f),     // Middle rect 1
                            arrayOf(0.1f, 0.04f, 0.82f, 0.82f),   // Middle rect 2
                            arrayOf(0.82f, 0.02f, 0.99f, 0.86f)   // Top rect
                        ),
                        screenWidth, screenHeight, ppm,
                        VEHICLE_08_WIDTH * scaleFactor, VEHICLE_08_HEIGHT * scaleFactor,
                        initialObstaclePosX, initialObstaclePosY, speed
                    ),


                    GameObjectCpu(
                        "vehicle_09",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.vehicle_ufo, null)?.toBitmap(screenWidth, screenHeight)!!),
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        screenWidth, screenHeight, ppm,
                        VEHICLE_09_WIDTH * scaleFactor, VEHICLE_09_HEIGHT * scaleFactor,
                        initialObstaclePosX, initialObstaclePosY, speed
                    )
                )

                pickVehicle(screenWidth, screenHeight)
            }






            // ===================================================================================================

            // ALTERNATIVA USANDO DIMENSIONI ORIGINALI BITMAP, NON MI SEMBRA CAMBI MOLTO,
            // RICHIEDE SOLO 2 VARIABILI IN PIÙ PER LE DIMENSIONI DELLO SCHERMO PER IL RESPAWN OLTRE A QUELLE GIÀ PRESENTI IN VEHICLE
            /*var bitmaps = arrayOf(
                ResourcesCompat.getDrawable(resources, R.drawable.biplane, null)?.toBitmap()!!,

                ResourcesCompat.getDrawable(resources, R.drawable.dirigible_1, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.dirigible_2, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.dirigible_3, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.dirigible_4, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.dirigible_5, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.dirigible_6, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.dirigible_7, null)?.toBitmap()!!,

                ResourcesCompat.getDrawable(resources, R.drawable.ufo, null)?.toBitmap()!!
            )

            vehicles = arrayOf(
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.biplane, null)?.toBitmap()!!,
                    7f, 3f, bitmaps[0].screenWidth, bitmaps[0].screenHeight, ppm, screenWidth + 20f, 20f, speed
                ),

                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_1, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[1].screenWidth, bitmaps[1].screenHeight, ppm, screenWidth + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_2, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[2].screenWidth, bitmaps[2].screenHeight, ppm, screenWidth + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_3, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[3].screenWidth, bitmaps[3].screenHeight, ppm, screenWidth + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_4, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[4].screenWidth, bitmaps[4].screenHeight, ppm, screenWidth + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_5, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[5].screenWidth, bitmaps[5].screenHeight, ppm, screenWidth + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_6, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[6].screenWidth, bitmaps[6].screenHeight, ppm, screenWidth + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_7, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[7].screenWidth, bitmaps[7].screenHeight, ppm, screenWidth + 20f + 20f, 20f, speed
                ),


                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.ufo, null)?.toBitmap()!!,
                    4f, 2f, bitmaps[8].screenWidth, bitmaps[8].screenHeight, ppm, screenWidth + 20f + 20f, 20f, speed
                )
            )*/

            // ALTERNATIVA COME SOPRA USANDO LE DIMENSIONI ORIGINALI DELLE BITMAP
            /*bitmaps = arrayOf(
                ResourcesCompat.getDrawable(resources, R.drawable.building_01, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_02, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_03, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_04, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_05, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_06, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_07, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_08, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_09, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_10, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_11, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_12, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_13, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_14, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_15, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_16, null)?.toBitmap()!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_17, null)?.toBitmap()!!
            )

            buildings = arrayOf(
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_01, null)?.toBitmap()!!,
                    15f, 50f, bitmaps[0].screenWidth, bitmaps[0].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_02, null)?.toBitmap()!!,
                    10f, 60f, bitmaps[1].screenWidth, bitmaps[1].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_03, null)?.toBitmap()!!,
                    15f, 50f, bitmaps[2].screenWidth, bitmaps[2].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_04, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[3].screenWidth, bitmaps[3].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_05, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[4].screenWidth, bitmaps[4].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_06, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[5].screenWidth, bitmaps[5].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_07, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[6].screenWidth, bitmaps[6].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_08, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[7].screenWidth, bitmaps[7].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_09, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[8].screenWidth, bitmaps[8].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_10, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[9].screenWidth, bitmaps[9].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_11, null)?.toBitmap()!!,
                    15f, 50f, bitmaps[10].screenWidth, bitmaps[10].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_12, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[11].screenWidth, bitmaps[11].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_13, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[12].screenWidth, bitmaps[12].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/2f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_14, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[13].screenWidth, bitmaps[13].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/2f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_15, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[14].screenWidth, bitmaps[14].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/2f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_16, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[15].screenWidth, bitmaps[15].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/2f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_17, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[16].screenWidth, bitmaps[16].screenHeight, ppm, screenWidth + 20f + 20f, screenHeight/2f, speed
                )
            )*/

            // ===================================================================================================








            val loadPlayer = async {
                val playerBitmaps: Array<Bitmap>

                // Convert the correct scenario images into bitmaps once
                if (SCENARIO == SCENARIO_CITY_DAY) {
                    playerBitmaps = arrayOf(
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_0, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_1, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_2, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_3, null)?.toBitmap(screenWidth, screenHeight)!!
                    )
                    /*playerBitmaps = arrayOf(
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_red_0, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_red_1, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_red_2, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_red_3, null)?.toBitmap(screenWidth, screenHeight)!!
                    )
                    playerBitmaps = arrayOf(
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_yellow_0, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_yellow_1, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_yellow_2, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_yellow_3, null)?.toBitmap(screenWidth, screenHeight)!!
                    )*/

                    /*val i = (0..2).random()

                    val playerBitmaps =
                        when (i) {
                            0 -> arrayOf(
                                    ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_0, null)?.toBitmap(screenWidth, screenHeight)!!,
                                    ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_1, null)?.toBitmap(screenWidth, screenHeight)!!,
                                    ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_2, null)?.toBitmap(screenWidth, screenHeight)!!,
                                    ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_3, null)?.toBitmap(screenWidth, screenHeight)!!
                                )
                            1 -> arrayOf(
                                    ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_red_0, null)?.toBitmap(screenWidth, screenHeight)!!,
                                    ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_red_1, null)?.toBitmap(screenWidth, screenHeight)!!,
                                    ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_red_2, null)?.toBitmap(screenWidth, screenHeight)!!,
                                    ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_red_3, null)?.toBitmap(screenWidth, screenHeight)!!
                                )
                            else -> arrayOf(
                                    ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_yellow_0, null)?.toBitmap(screenWidth, screenHeight)!!,
                                    ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_yellow_1, null)?.toBitmap(screenWidth, screenHeight)!!,
                                    ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_yellow_2, null)?.toBitmap(screenWidth, screenHeight)!!,
                                    ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_yellow_3, null)?.toBitmap(screenWidth, screenHeight)!!
                                )
                        }*/

                }
                else {
                    val b = arrayOf(
                        // Top light on
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_night_0, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_night_1, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_night_2, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_night_3, null)?.toBitmap(screenWidth, screenHeight)!!,

                        // Top light off
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_night_00, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_night_01, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_night_02, null)?.toBitmap(screenWidth, screenHeight)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.vehicle_heli_green_night_03, null)?.toBitmap(screenWidth, screenHeight)!!
                    )

                    playerBitmaps = arrayOf(
                        b[0], b[1], b[2], b[3], // All frames top light on
                        b[0], b[1], b[2], b[3], // All frames top light on
                        b[0], b[1], b[2], b[3], // All frames top light on
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7], // All frames top light off
                        b[4], b[5], b[6], b[7]  // All frames top light off
                    )
                }

                playerVehicle = GameObjectPlayer(
                    "player_vehicle",
                    playerBitmaps,
                    arrayOf(
                        arrayOf(0.33f, 0.2f, 0.64f, 0.76f),  // Bottom rect
                        arrayOf(0.07f, 0.2f, 0.33f, 0.5f),   // Middle rect
                        arrayOf(0.64f, 0.19f, 0.95f, 0.21f)  // Top rect
                    ),
                    screenWidth, screenHeight, ppm,
                    PLAYER_WIDTH * scaleFactor, PLAYER_HEIGHT * scaleFactor,
                    1f, 4f, 0f // Measures in meters
                )
            }

            val loadExplosion = async {
                explosionBitmaps = arrayOf(
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_01, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_02, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_03, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_04, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_05, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_06, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_07, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_08, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_09, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_10, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_11, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_12, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_13, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_14, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_15, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_16, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_17, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_18, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_19, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_20, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_21, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_22, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_23, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_24, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_25, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_26, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_27, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_28, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_29, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_30, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_31, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_32, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_33, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_34, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_35, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_36, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_37, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_38, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_39, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_40, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_41, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_42, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_43, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_44, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_45, null)?.toBitmap(screenWidth, screenHeight)!!,
                    ResourcesCompat.getDrawable(resources, R.drawable.explosion_46, null)?.toBitmap(screenWidth, screenHeight)!!
                )
            }

            loadBuildings.await()
            loadVehicles.await()
            loadPlayer.await()
            loadExplosion.await()

            start(screenWidth, screenHeight)
        }
    }

    private fun start(screenWidth: Int, screenHeight: Int) {
        val fps = 90
        val targetFrameTime   = 1000L / fps  // In ms
        var timePreviousFrame = 0L           // In ms


        // Dispatchers.Default — is used by all standard builders if no dispatcher or any other ContinuationInterceptor is specified in their context.
        // It uses a common pool of shared background threads.
        // This is an appropriate choice for compute-intensive coroutines that consume CPU resources
        fragment.lifecycleScope.launch(Dispatchers.Default) {
            while (true) {
                // Check if the restoring of variables is completed if any
                if (!resumeDrawing) {
                    sleep(50)
                    continue
                }

                //Log.d("COROUTINE", "==================")
                //Log.d("COROUTINE", "COROUTINE MAIN - I'm working in thread ${Thread.currentThread().name}")

                // Compute time difference
                val timeCurrentFrame = System.currentTimeMillis()  // In ms
                val dt = min(timeCurrentFrame - timePreviousFrame, targetFrameTime) / 1000f  // In s

                /*Log.d("TIME", "************")
                Log.d("TIME", "timePreviousFrame: $timePreviousFrame ms")
                Log.d("TIME", "timeCurrentFrame: $timeCurrentFrame ms")
                Log.d("TIME", "dt: $dt s")
                Log.d("TIME", "speed (m/s): $speed")
                Log.d("TIME", "speed (px/s): " + speed * ppm * dt)*/

                timePreviousFrame = timeCurrentFrame

                val updatePlayer = async {
                    //Log.d("COROUTINE", "ASYNC UPDATE USER - I'm working in thread ${Thread.currentThread().name}")

                    playerVehicle.setSpeed(lastGyroscopeInput)
                    playerVehicle.update(dt)

                    /*withContext(Dispatchers.Main) {
                        Log.d("COROUTINE", "ASYNC UPDATE USER MAIN CONTEXT - I'm working in thread ${Thread.currentThread().name}")
                        playerVehicle.transform()
                    }*/
                }

                val updateVehicle = async {
                    //Log.d("COROUTINE", "ASYNC UPDATE VEHICLE - I'm working in thread ${Thread.currentThread().name}")

                    cpuVehicle.update(dt)

                    /*withContext(Dispatchers.Main) {
                        Log.d("COROUTINE", "ASYNC UPDATE VEHICLE MAIN CONTEXT - I'm working in thread ${Thread.currentThread().name}")
                        cpuVehicle.transform()
                    }*/

                    // Check if the cpuVehicle has been respawn
                    if (cpuVehicle.isRespawn())
                        pickVehicle(screenWidth, screenHeight)
                }

                val updateBuilding = async {
                    //Log.d("COROUTINE", "ASYNC UPDATE BUILDING - I'm working in thread ${Thread.currentThread().name}")

                    cpuBuilding.update(dt)

                    /*withContext(Dispatchers.Main) {
                        Log.d("COROUTINE", "ASYNC UPDATE BUILDING MAIN CONTEXT - I'm working in thread ${Thread.currentThread().name}")
                        cpuBuilding.transform()
                    }*/

                    // Check if the cpuBuilding has been respawn
                    if (cpuBuilding.isRespawn())
                        pickBuilding(screenWidth, screenHeight)
                }

                updatePlayer.await()
                updateVehicle.await()
                updateBuilding.await()

                val gameUpdate = async {
                    //Log.d("COROUTINE", "ASYNC FINAL UPDATE - I'm working in thread ${Thread.currentThread().name}")
                    //Log.d("COROUTINE", "ASYNC FINAL UPDATE - dt $dt")

                    // Check if vehicle explosion has been fired
                    if (playerVehicle.isExploded()) {
                        // Check if this is the first frame since explosion
                        if (explosionAnimationIndex == 0)
                            // Set explosion bitmaps
                            playerVehicle.setBitmaps(explosionBitmaps)

                        //Log.d("gameEnd", "In gameUpdate - explosionAnimationIndex: $explosionAnimationIndex")

                        // Check if explosion animation is ended
                        if (explosionAnimationIndex == explosionBitmaps.size) {
                            gameEnd = true

                            sensorManager.unregisterListener(this@GameView)

                            withContext(Dispatchers.Main) {
                                //val fragment = findFragment<GameFragment>()
                                fragment.gameEnd(score.toLong(), WINNER_UNDEFINED)
                            }
                        }

                        explosionAnimationIndex++

                        return@async
                    }

                    // Check if a collision has been occurred
                    if (checkCollision())
                        // Start vehicle explosion in next frame
                        playerVehicle.explode()

                    // Update the score
                    // s = v * dt  in m
                    score += speed * dt

                    // Update the speed (max about 36-43 m/s. 50 m/s very difficult!)
                    if (speed < 40f)
                        speed += 0.001f
                }

                gameUpdate.await()

                // Compute time to wait to be consistent with the target frame time
                val timeCurrentFrameEnd = System.currentTimeMillis()
                val waitTime = targetFrameTime - (timeCurrentFrameEnd - timeCurrentFrame)

                /*Log.d("TIME", "timeCurrentFrame: $timeCurrentFrame ms")
                Log.d("TIME", "timeCurrentFrameEnd: $timeCurrentFrameEnd ms")
                Log.d("TIME", "targetFrameTime: $targetFrameTime ms")
                Log.d("TIME", "waitTime: $waitTime ms")*/

                if (waitTime > 0)
                    sleep(waitTime)

                startDrawing = true
                //Log.d("COROUTINE", "COROUTINE MAIN - draw!")

                // Draw the next frame
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

        // Check if game has been initialized & variables have been restored (if any)
        if (!startDrawing || !resumeDrawing) {
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
        playerVehicle.transform()
        cpuVehicle.transform()
        cpuBuilding.transform()

        // Draw bitmaps
        canvas?.drawBitmap(playerVehicle.getBitmap(), playerVehicle.getMatrix(), null)
        canvas?.drawBitmap(cpuVehicle.getBitmap(), cpuVehicle.getMatrix(), null)
        canvas?.drawBitmap(cpuBuilding.getBitmap(), cpuBuilding.getMatrix(), null)

        canvas?.drawText("SCORE " + score.toLong(), 20f, 60f, painterFill)


        // Debug (draw player's bounds)
        /*var bounds = playerVehicle.getBounds()
        for (rect in bounds)
            canvas?.drawRect(rect, painterStroke)

        // Debug (draw cpuVehicle's bounds)
        bounds = cpuVehicle.getBounds()
        for (rect in bounds)
            canvas?.drawRect(rect, painterStroke)

        // Debug (draw cpuBuilding's bounds)
        bounds = cpuBuilding.getBounds()
        for (rect in bounds)
            canvas?.drawRect(rect, painterStroke)*/

        // Debug (Draw min e max cpuBuilding heights)
        /*val h = max(width, height)
        canvas?.drawLine(0f, h*0.2f, width.toFloat(), h*0.2f, painterStroke)
        canvas?.drawLine(0f, h*0.58f, width.toFloat(), h*0.58f, painterStroke)*/

        // Measures from meters to pixels
        /*canvas?.drawLine(0f, 17f * ppm, width.toFloat(), 17f * ppm, painterStroke)
        canvas?.drawLine(0f, 33f * ppm, width.toFloat(), 33f * ppm, painterStroke)*/
    }

    // Convert density independent pixels into screen pixels
    /*private fun dp2px(dp: Float): Float {
        return resources.displayMetrics.density * dp
    }*/

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

    // Pick a random cpuBuilding and set a random height for it
    private fun pickBuilding(screenWidth: Int, screenHeight: Int, name: String? = null) {
        if (name == null || name.isEmpty()) {
            var b = buildings.random()

            // Check if cpuBuilding variable has been initialized
            if (startDrawing)
                // It has been init, try to pick a different cpuBuilding than the current one
                for (i in 0..2) {
                    if (b.getName() != cpuBuilding.getName())
                        break

                    b = buildings.random()
                }

            cpuBuilding = b
        }
        else {
            val i = getGameObjectIndexByName(name)

            try {
                cpuBuilding = buildings[i]
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()

                cpuBuilding = buildings.random()
            }
        }

        // Height range height*0.2 .. height*0.76 = from 20% to 76% of the screen height
        // Remember that 0% = top and 100% = bottom
        //val r = (20..76).random() / 100f

        // Building pos Y in meters
        val buildingPosY = (17..33).random()

        //=================
        // For debug
        //val r = 0.2f

        // Set building pos X in pixels
        //cpuBuilding.setX(screenWidth*0.35f)
        //cpuBuilding.setX(screenWidth*0.1f)

        // Set building pos X in pixel from meters (for efficiency of update calls)
        //cpuBuilding.setX(worldWidth*0.35f * ppm)
        //cpuBuilding.setX(worldWidth*0.1f * ppm)
        //cpuBuilding.setX(13f * ppm)

        // Set Y of the building in pixels from meters (for efficiency of update calls)
        //cpuBuilding.setY(17f * ppm)
        //=================

        // Set Y of the building relative to the height of the current screen orientation
        //cpuBuilding.setY(screenHeight*r)

        // Set Y of the building relative to the height of the screen in portrait orientation
        //cpuBuilding.setY( max(screenWidth, screenHeight) * r )

        // Set Y of the building in pixels from meters (for efficiency of update calls)
        cpuBuilding.setY(buildingPosY * ppm)

        // Increase the distance between obstacles if the height of
        // the cpuBuilding is greater than a threshold
        //if (r > 3.5f && cpuBuilding.getX() - cpuVehicle.getX() < heli_width_scaled * 2f)
        //    cpuBuilding.setX(screenWidth + cpuVehicle.getBitmapScaledWidth() + heli_width_scaled * 2f)

        // Update speed
        cpuBuilding.setSpeed(speed)
    }

    // Pick a random cpuVehicle and set a random height for it
    private fun pickVehicle(screenWidth: Int, screenHeight: Int, name: String? = null) {
        if (name == null || name.isEmpty()) {
            var v = vehicles.random()

            // Check if cpuVehicle variable has been initialized
            if (startDrawing)
            // It has been init, try to pick a different cpuVehicle than the current one
                for (i in 0..2) {
                    if (v.getName() != cpuVehicle.getName())
                        break

                    v = vehicles.random()
                }

            cpuVehicle = v
        }
        else {
            val i = getGameObjectIndexByName(name)

            try {
                cpuVehicle = vehicles[i]
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()

                cpuVehicle = vehicles.random()
            }
        }

        // Height range height*0 .. height*0.2 = from 0% to 20% of the screen height
        // Remember that 0% = top and 100% = bottom
        //val r = (0..20).random() / 100f

        // Vehicle pos Y in meters
        val vehiclePosY = (0..5).random()


        //=================
        // For debug
        //val r = 0f

        // Set vehicle pos X in pixels
        //cpuVehicle.setX(screenWidth*0.35f)
        //cpuVehicle.setX(screenWidth*0.1f)

        // Set vehicle pos X in pixels from meters (for efficiency of update calls)
        //cpuVehicle.setX(worldWidth*0.35f * ppm)
        //cpuVehicle.setX(worldWidth*0.1f * ppm)
        //cpuVehicle.setX(13f * ppm)
        //=================

        // Set Y of the vehicle relative to the height of the current screen orientation
        //cpuVehicle.setY(screenHeight*r)

        // Set Y of the vehicle relative to the height of the screen in portrait orientation
        //cpuVehicle.setY( max(screenWidth, screenHeight) * r )

        // Set Y of the vehicle in pixels from meters (for efficiency of update calls)
        cpuVehicle.setY(vehiclePosY * ppm)

        // Increase the distance between obstacles if the height of
        // the cpuBuilding is greater than a threshold
        //if (cpuBuilding.getY() > height/3.5f && cpuVehicle.getX() - cpuBuilding.getX() < heli_width_scaled * 2f)
        //cpuVehicle.setX(screenWidth + cpuBuilding.getBitmapScaledWidth() + heli_width_scaled * 2f)

        // Update speed (cpuVehicle speed different from cpuBuilding one for more realism)
        cpuVehicle.setSpeed(speed + 5f)
    }

    // Check if the user collides with an obstacle
    private fun checkCollision(): Boolean { //return false
        val obstacleRects = cpuBuilding.getBounds()
        obstacleRects.addAll(cpuVehicle.getBounds())

        val playerRects = playerVehicle.getBounds()

        for (or in obstacleRects)
            for (pr in playerRects)
                if (RectF.intersects(pr, or))
                    return true

        return false
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



    // Create a constant to convert nanoseconds to seconds
    /*private val NS2S = 1.0f / 1000000000.0f
    private val deltaRotationVector = FloatArray(4) { 0f }
    private var timestamp = 0f
    private val EPSILON = 0.1f*/


    // Detect user motion via sensors
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onFlushCompleted(sensor: Sensor?) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        //========================================
        // VERSIONE 1 (NON GIOCABILE!)

        /*when (event?.sensor?.type) {
            //Sensor.TYPE_ACCELEROMETER  -> lastAcceleration = event.values.clone()
            //Sensor.TYPE_MAGNETIC_FIELD      -> gyroscopeValues    = event.values.clone()
            Sensor.TYPE_GYROSCOPE -> gyroscopeValues = event.values.clone()
            else                  -> return
        }

        // Use X value if portrait mode, Y value otherwise
        val value =
            if (width < height)                gyroscopeValues[0]        // Portrait
            else if (display?.rotation == 1)   gyroscopeValues[1] * -1f  // Landscape
            else                               gyroscopeValues[1]        // Reverse landscape

        // Enhance gyroscope value
        /*if (value > 0f)
            lastGyroscopeInput = max(1f, min(value * 10f, 20f))
        else if (value < 0f)
            lastGyroscopeInput = min(-1f, max(value * 10f, -20f))*/
        if (value > 0.4f)
            lastGyroscopeInput = max(1f, min(value * 30f, 60f))
        else if (value < -0.4f)
            lastGyroscopeInput = min(-1f, max(value * 30f, -60f))

        Log.d("SENSOR", "****************")
        Log.d("SENSOR", "value: $value")
        Log.d("SENSOR", "lastGyroscopeInput: $lastGyroscopeInput")
        Log.d("SENSOR", "rotation: $rotation")
        Log.d("SENSOR", "display.rotation: ${display?.rotation}")*/
        //========================================



        //========================================
        // VERSIONE 2 (BUONA)

        when (event?.sensor?.type) {
            //Sensor.TYPE_ACCELEROMETER  -> lastAcceleration = event.values.clone()
            //Sensor.TYPE_MAGNETIC_FIELD      -> gyroscopeValues    = event.values.clone()
            Sensor.TYPE_GYROSCOPE -> gyroscopeValues = event.values.clone()
            else                  -> return
        }

        //if (timestamp != 0f) {
            // Calculate the delta angular speed
        //    val dt = (event.timestamp - timestamp) * NS2S

            // Axis of the rotation sample, not normalized yet
            val axisX = event.values[0]
            val axisY = event.values[1]
            val axisZ = event.values[2]

            // Calculate the angular speed of the sample
            val angularSpeed = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)


            //=========================================
            // PROVO AD USARE LA VELOCITÀ ANGOLARE 1

            // Use X value if portrait mode, Y value otherwise
            val value =
                if (width < height)                axisX        // Portrait
                else if (display?.rotation == 1)   axisY * -1f  // Landscape
                else                               axisY        // Reverse landscape

            // Filter the noise and enhance the angular speed
            if (value > 0.2f)
                lastGyroscopeInput = min(angularSpeed * 35f, 30f)
            else if (value < -0.2f)
                lastGyroscopeInput = max(angularSpeed * -35f, -30f)
            /*else {
                // Brake
                if (lastGyroscopeInput > 0f)
                    lastGyroscopeInput = max(0f, lastGyroscopeInput-1f)
                else if (lastGyroscopeInput < 0f)
                    lastGyroscopeInput = min(0f, lastGyroscopeInput+1f)
            }*/

            /*Log.d("SENSOR", "****************")
            //Log.d("SENSOR", "dt: $dt")
            Log.d("SENSOR", "value: $axisX")
            Log.d("SENSOR", "angular speed: $angularSpeed")
            //Log.d("SENSOR", "angular speed * dt: ${angularSpeed*dt}")
            Log.d("SENSOR", "lastGyroscopeInput: $lastGyroscopeInput")*/
            //=========================================
        /*}

        timestamp = event.timestamp.toFloat()*/










        // From Google tutorial
        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data
        /*if (timestamp != 0f && event != null) {
            val dt = (event.timestamp - timestamp) * NS2S

            // Axis of the rotation sample, not normalized yet
            var axisX: Float = event.values[0]
            var axisY: Float = event.values[1]
            var axisZ: Float = event.values[2]

            // Calculate the angular speed of the sample
            val omegaMagnitude: Float = sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ)

            // Normalize the rotation vector if it's big enough to get the axis
            // (that is, EPSILON should represent your maximum allowable margin of error)
            if (omegaMagnitude > EPSILON) {
                axisX /= omegaMagnitude
                axisY /= omegaMagnitude
                axisZ /= omegaMagnitude
            }

            // Integrate around this axis with the angular speed by the timestep
            // in order to get a delta rotation from this sample over the timestep.
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix
            val thetaOverTwo: Float = omegaMagnitude * dt / 2.0f
            val sinThetaOverTwo: Float = sin(thetaOverTwo)
            val cosThetaOverTwo: Float = cos(thetaOverTwo)

            deltaRotationVector[0] = sinThetaOverTwo * axisX
            deltaRotationVector[1] = sinThetaOverTwo * axisY
            deltaRotationVector[2] = sinThetaOverTwo * axisZ
            deltaRotationVector[3] = cosThetaOverTwo
        }

        timestamp = event?.timestamp?.toFloat() ?: 0f

        val deltaRotationMatrix = FloatArray(9) { 0f }
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector)*/

        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        // rotationCurrent = rotationCurrent * deltaRotationMatrix
    }

    // Save game variables
    override fun onSaveInstanceState(): Parcelable? {
        super.onSaveInstanceState()

        if (!::playerVehicle.isInitialized || !::cpuBuilding.isInitialized || !::cpuVehicle.isInitialized )
            return null

        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())

        bundle.putFloat("score", score)
        bundle.putFloat("speed", speed)


        // NON USARE PIXEL O METRI PERCHÈ UNA DELLE 2 DIMENSIONI NON È VINCOLATA E
        // DAL MOMENTO CHE STO USANDO IL FATTORE DI SCALA, NEANCHE L'ALTRA È LA STESSA MA È IN PROPORZIONE
        // USA LA PERCENTUALE DELLO SCHERMO PER ENTRAMBE LE DIMENSIONI!

        // Get screen width and height (don't use directly getWidth() and getHeight() because here return 0) ?
        /*val width  = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels

        Log.d("save", "Save state game view - player X: ${playerVehicle.getX()} px")
        Log.d("save", "Save state game view - player X: ${playerVehicle.getX() / ppm} m")
        Log.d("save", "Save state game view - player X: ${playerVehicle.getX() / width} => ${playerVehicle.getX() / width * 100} %")

        Log.d("save", "Save state game view - player Y: ${playerVehicle.getY()} px")
        Log.d("save", "Save state game view - player Y: ${playerVehicle.getY() / ppm} m")
        Log.d("save", "Save state game view - player Y: ${playerVehicle.getY() / height} => ${playerVehicle.getY() / height * 100} %")

        Log.d("save", "Save state game view - building X: ${cpuBuilding.getX()} px")
        Log.d("save", "Save state game view - building X: ${cpuBuilding.getX() / ppm} m")
        Log.d("save", "Save state game view - building X: ${cpuBuilding.getX() / width} => ${cpuBuilding.getX() / width * 100} %")

        Log.d("save", "Save state game view - building Y: ${cpuBuilding.getY()} px")
        Log.d("save", "Save state game view - building Y: ${cpuBuilding.getY() / ppm} m")
        Log.d("save", "Save state game view - building Y: ${cpuBuilding.getY() / height} => ${cpuBuilding.getY() / height * 100} %")*/


        // VERSIONE CON METRI
        bundle.putFloat("playerPosY", playerVehicle.getY() / ppm)

        bundle.putString("cpuBuildingName", cpuBuilding.getName())
        bundle.putFloat("cpuBuildingPosX", cpuBuilding.getX() / ppm)
        bundle.putFloat("cpuBuildingPosY", cpuBuilding.getY() / ppm)

        bundle.putString("cpuVehicleName", cpuVehicle.getName())
        bundle.putFloat("cpuVehiclePosX", cpuVehicle.getX() / ppm)
        bundle.putFloat("cpuVehiclePosY", cpuVehicle.getY() / ppm)//*/

        // VERSIONE CON PERCENTUALE SCHERMO
        /*bundle.putFloat("playerPosY", playerVehicle.getY() / height)

        bundle.putString("cpuVehicleName", cpuVehicle.getName())
        bundle.putFloat("cpuVehiclePosX", cpuVehicle.getX() / width)
        bundle.putFloat("cpuVehiclePosY", cpuVehicle.getY() / height)

        bundle.putString("cpuBuildingName", cpuBuilding.getName())
        bundle.putFloat("cpuBuildingPosX", cpuBuilding.getX() / width)
        bundle.putFloat("cpuBuildingPosY", cpuBuilding.getY() / height)*/

        return bundle
    }

    // Restore game variables
    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            resumeDrawing = false

            val s = state.getParcelable<Parcelable>("superState")

            score = state.getFloat("score")
            speed = state.getFloat("speed")

            // Get screen width and height (don't use directly getWidth() and getHeight() because here return 0) ?
            val width  = resources.displayMetrics.widthPixels
            val height = resources.displayMetrics.heightPixels

            fragment.lifecycleScope.launch(Dispatchers.Default) {
                val restorePlayerVehicle = async {
                    val py = state.getFloat("playerPosY")

                    // VERSIONE CON METRI
                    /*Log.d("save", "Restore state game view - player Y: $py m")
                    Log.d("save", "Restore state game view - player Y: ${py * ppm} px")
                    Log.d("save", "Save state game view - player Y: ${py * ppm / height} => ${py * ppm / height * 100} %")*/

                    // VERSIONE CON PERCENTUALE SCHERMO
                    /*Log.d("save", "Restore state game view - player Y: $py => ${py * 100} %")
                    Log.d("save", "Restore state game view - player Y: ${py * height} px")
                    Log.d("save", "Restore state game view - player Y: ${py * height / ppm} m")*/

                    while (true) {
                        try {
                            // VERSIONE CON METRI
                            // Set Y in pixels (for efficiency of update calls)
                            playerVehicle.setY(py * ppm)

                            // VERSIONE CON PERCENTUALE SCHERMO
                            //playerVehicle.setY(py * height)

                            break
                        }catch (e: UninitializedPropertyAccessException) {
                            //e.printStackTrace()
                            sleep(100)
                        }
                    }
                }

                val restoreCpuBuilding = async {
                    val bn = state.getString("cpuBuildingName")
                    val bx = state.getFloat("cpuBuildingPosX")
                    val by = state.getFloat("cpuBuildingPosY")

                    // VERSIONE CON METRI
                    /*Log.d("save", "Restore state game view - building X: $bx m")
                    Log.d("save", "Restore state game view - building X: ${bx * ppm} px")
                    Log.d("save", "Save state game view - building X: ${bx * ppm / width} => ${bx * ppm / width * 100} %")

                    Log.d("save", "Restore state game view - building Y: $by m")
                    Log.d("save", "Restore state game view - building Y: ${by * ppm} px")
                    Log.d("save", "Save state game view - building Y: ${by * ppm / height} => ${by * ppm / height * 100} %")*/

                    // VERSIONE CON PERCENTUALE SCHERMO
                    /*Log.d("save", "Restore state game view - building X: $bx => ${bx * 100} %")
                    Log.d("save", "Restore state game view - building X: ${bx * width} px")
                    Log.d("save", "Restore state game view - building X: ${bx * width / ppm} m")

                    Log.d("save", "Restore state game view - building Y: $by => ${by * 100} %")
                    Log.d("save", "Restore state game view - building Y: ${by * height} px")
                    Log.d("save", "Restore state game view - building Y: ${by * height / ppm} m")*/

                    while (true) {
                        try {
                            //Log.d("save","restore state game view - Try to pick cpuBuilding")

                            pickBuilding(width, height, bn)

                            // VERSIONE CON METRI
                            // Set X and Y in pixels (for efficiency of update calls)
                            cpuBuilding.setX(bx * ppm)
                            cpuBuilding.setY(by * ppm)

                            // VERSIONE CON PERCENTUALE SCHERMO
                            /*cpuBuilding.setX(bx * width)
                            cpuBuilding.setY(by * height)*/

                            cpuBuilding.setSpeed(speed)

                            break
                        } catch (e: UninitializedPropertyAccessException) {
                            // Buildings variable has not been initialized
                            //e.printStackTrace()
                            sleep(100)
                        }
                    }
                }

                val restoreCpuVehicle = async {
                    val vn = state.getString("cpuVehicleName")
                    val vx = state.getFloat("cpuVehiclePosX")
                    val vy = state.getFloat("cpuVehiclePosY")

                    while (true) {
                        try {
                            //Log.d("save","restore state game view - Try to pick cpuVehicle")

                            pickVehicle(width, height, vn)

                            // VERSIONE CON METRI
                            // Set X and Y in pixels (for efficiency of update calls)
                            cpuVehicle.setX(vx * ppm)
                            cpuVehicle.setY(vy * ppm)

                            // VERSIONE CON PERCENTUALE SCHERMO
                            /*cpuVehicle.setX(vx * width)
                            cpuVehicle.setY(vy * height)*/

                            cpuVehicle.setSpeed(speed + 5f)

                            break
                        } catch (e: UninitializedPropertyAccessException) {
                            // Vehicles variable has not been initialized
                            //e.printStackTrace()
                            sleep(100)
                        }
                    }
                }

                awaitAll(restorePlayerVehicle, restoreCpuBuilding, restoreCpuVehicle)
                resumeDrawing = true
            }

            super.onRestoreInstanceState(s)
        }
        else
            super.onRestoreInstanceState(state)
    }
}