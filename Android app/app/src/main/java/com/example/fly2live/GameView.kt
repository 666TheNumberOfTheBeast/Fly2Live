package com.example.fly2live

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.os.Parcelable
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.findFragment
import com.example.fly2live.configuration.Configuration.Companion.SCENARIO
import com.example.fly2live.configuration.Configuration.Companion.SCENARIO_CITY_DAY
import kotlinx.coroutines.*
import java.lang.Thread.sleep
import kotlin.math.max
import kotlin.math.min
import android.os.Bundle
import com.example.fly2live.game_object_cpu.GameObjectCpu
import com.example.fly2live.game_object_player.GameObjectPlayer


class GameView(context: Context?, lifecycleScope: CoroutineScope) : View(context), View.OnTouchListener, SensorEventListener2 {
//class GameView(context: Context?) : View(context), View.OnTouchListener, SensorEventListener2 {
    private val lifecycleScope = lifecycleScope
    /*private var lifecycleScope: CoroutineScope? = null

    constructor(context: Context?, lifecycleScope: CoroutineScope) : this(context) {
        this.lifecycleScope = lifecycleScope
    }*/

    // Background bitmap
    private lateinit var bg: Bitmap

    // Player's game object
    private lateinit var playerVehicle: GameObjectPlayer

    // CPU's game objects
    private lateinit var buildings: Array<GameObjectCpu>
    private lateinit var building: GameObjectCpu
    private lateinit var vehicles: Array<GameObjectCpu>
    private lateinit var vehicle: GameObjectCpu


    // Sensors values
    private var lastAcceleration = FloatArray(3)
    private var lastGyroscope    = FloatArray(3)
    private var lastGyroscopeInput = 0f // meters


    // World constants
    private val worldWidth  = 40f //30f  // meters
    private var worldHeight = 60f //40f (ok horizontal) //65f o 60f (ok vertical)  // meters

    // Pixel per meter
    private var ppm = 1f

    //private var speed = 0.2f // m/s (max 0.5) VERSIONE CON UPDATE SENZA L'USO DI dt
    private var speed = 10f // m/s
    private var score = 0f  // meters traveled


    // Variable to stop drawing when game ends
    private var gameEnd = false

    // Variable to start drawing when game has been initialized
    private var startDrawing = false

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
        //Log.d("ppm", "width: $width")
        //Log.d("ppm", "height: $height")

        // Get screen width and height (don't use directly getWidth() and getHeight() because here return 0)
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        Log.d("ppm", "w: $width")
        Log.d("ppm", "h: $height")

        init(width, height)
    }

    // Multithreading version
    private fun init(width: Int, height: Int) {
        // Dispatchers.Default — is used by all standard builders if no dispatcher or any other ContinuationInterceptor is specified in their context.
        // It uses a common pool of shared background threads.
        // This is an appropriate choice for compute-intensive coroutines that consume CPU resources
        lifecycleScope.launch(Dispatchers.Default) {
            // Set the scale factor based on device orientation in order to have similar worldWidth in both modes
            val scaleFactor = if (width < height) 1f else 0.4f

            // Calculate ppm

            // OPTION 1
            // keep a fixed scale factor for drawing the bitmaps when rotating the screen.

            // I divide both to worldWidth because I'm interested in width for obstacles respawn
            //ppm = min(width / worldWidth, height / worldWidth)

            // I divide both to worldHeight because I'm interested in height for avoiding obstacles
            //ppm = min(width / worldHeight, height / worldHeight)

            // OPTION 2
            // use variable scale factor for drawing the bitmaps when rotating the screen.

            // based on the width of the screen to have the same proportion horizontally
            //ppm = width / worldWidth

            // based on the height of the screen to have the same proportion vertically
            //ppm = height / worldHeight

            // based on the height of the screen to have the same proportion vertically
            ppm = height / (worldHeight * scaleFactor)
            Log.d("ppm", "ppm: $ppm")

            // If orientation is landscape, current world width is larger than portrait mode
            // because when:
            // - portrait  => min = width / worldWidth  => currentWorldWidth = worldWidth
            // - landscape => min = height / worldWidth => currentWorldWidth > worldWidth
            val currentWorldWidth = width / ppm

            Log.d("ppm", "worldWidth: $worldWidth")
            Log.d("ppm", "currentWorldWidth = width / ppm: $currentWorldWidth")


            // Otherwise, use variable ppm but fixed currentWorldWidth.
            // It's difficult to find a worldWidth that is ok for both portrait and landscape orientations
            // because no constraint on y
            /*ppm = width / worldWidth
            Log.d("ppm", "ppm variable: $ppm")

            val currentWorldWidth = width / ppm

            Log.d("ppm", "worldWidth: $worldWidth")
            Log.d("ppm", "currentWorldWidth = width / ppm: $currentWorldWidth")
            Log.d("ppm", "(worldWidth + 3f) * ppm: " + (worldWidth +3f) * ppm)*/

            // Calculate current world height and corresponding ppm_y
            val currentWorldHeight = height / ppm
            // But if I use different ppm for x and y, then images are distorted
            //val ppm_y = height / worldHeight

            Log.d("ppm", "worldHeight: $worldHeight")
            Log.d("ppm", "currentWorldHeight = height / ppm: $currentWorldHeight")
            //Log.d("ppm", "ppm_y = height / worldHeight: $ppm_y")


            // Initial position of obstacles
            val posX = currentWorldWidth + 3f

            val loadBuildings = async {
                Log.d("COROUTINE", "Load buildings")
                // Convert the correct scenario images into bitmaps once
                if (SCENARIO == SCENARIO_CITY_DAY) {
                    bg = ResourcesCompat.getDrawable(resources, R.drawable.city_bg_day, null)?.toBitmap(width, height)!!

                    // GameObjectCpu uses bounds offset based on bitmap's width and height in order to be consistent with any value of the world's width and height
                    buildings = arrayOf(
                        GameObjectCpu(
                            "building_01",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_01, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.048f, 1f, 1f),       // Bottom rect
                                arrayOf(0.07f, 0.031f, 0.38f, 1f), // Middle left rect
                                arrayOf(0.47f, 0.031f, 0.8f, 1f),  // Middle right rect
                                arrayOf(0.1f, 0f, 0.18f, 1f),      // Top left rect
                                arrayOf(0.47f, 0.023f, 0.63f, 1f)  // Top right rect
                            ),
                            25f * scaleFactor, 70f * scaleFactor, width, height, ppm, posX, height*0.2f, speed // Measures in meters except pos_y in screen %
                        ),
                        GameObjectCpu(
                            "building_02",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_02, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0.18f, 0.365f, 0.82f, 1f),  // Bottom rect
                                arrayOf(0.33f, 0.187f, 0.7f, 1f),   // Middle rect
                                arrayOf(0.47f, 0f, 0.56f, 1f)       // Top rect
                            ),
                            10f * scaleFactor, 80f * scaleFactor, width, height, ppm, posX, height*0.2f, speed
                        ),
                        GameObjectCpu(
                            "building_03",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_03, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.16f, 1f, 1f),         // Bottom rect
                                arrayOf(0.13f, 0.105f, 0.86f, 1f),  // Middle rect 1
                                arrayOf(0.28f, 0.045f, 0.68f, 1f),  // Middle rect 2
                                arrayOf(0.45f, 0f, 0.6f, 1f)        // Top rect
                            ),
                            25f * scaleFactor, 110f * scaleFactor, width, height, ppm, posX, height*0.2f, speed
                        ),
                        GameObjectCpu(
                            "building_04",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_04, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0.13f, 0.28f, 0.86f, 1f),  // Bottom rect
                                arrayOf(0.21f, 0.173f, 0.79f, 1f), // Middle rect 1
                                arrayOf(0.23f, 0.07f, 0.76f, 1f),  // Middle rect 2
                                arrayOf(0.31f, 0.003f, 0.67f, 1f)  // Top rect
                            ),
                            35f * scaleFactor, 90f * scaleFactor, width, height, ppm, posX, height*0.2f, speed
                        ),
                        GameObjectCpu(
                            "building_05",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_05, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.185f, 1f, 1f),       // Bottom rect
                                arrayOf(0f, 0.1f, 0.23f, 1f),      // Middle left rect
                                arrayOf(0.27f, 0.136f, 0.75f, 1f), // Middle center rect
                                arrayOf(0.77f, 0.136f, 0.9f, 1f),  // Middle right rect
                                arrayOf(0.33f, 0f, 0.75f, 1f)      // Top rect
                            ),
                            35f * scaleFactor, 50f * scaleFactor, width, height, ppm, posX, height*0.2f, speed
                        ),
                        GameObjectCpu(
                            "building_06",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_06, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.55f, 1f, 1f),       // Bottom rect
                                arrayOf(0f, 0.14f, 0.36f, 1f),    // Middle left rect
                                arrayOf(0.53f, 0.32f, 0.86f, 1f), // Middle right rect
                                arrayOf(0.07f, 0f, 0.29f, 1f)     // Top rect
                            ),
                            45f * scaleFactor, 50f * scaleFactor, width, height, ppm, posX, height*0.2f, speed // Measures in meters except pos_y
                        ),
                        GameObjectCpu(
                            "building_07",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_07, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.291f, 1f, 1f),      // Bottom rect
                                arrayOf(0f, 0.15f, 0.91f, 1f),    // Middle rect
                                arrayOf(0f, 0.13f, 0.22f, 1f),    // Left "triangle"
                                arrayOf(0.7f, 0.13f, 0.91f, 1f),  // Right "triangle"
                                arrayOf(0.31f, 0.06f, 0.39f, 1f), // Top rect 1
                                arrayOf(0.31f, 0f, 0.33f, 1f)     // Top rect 2
                            ),
                            15f * scaleFactor, 80f * scaleFactor, width, height, ppm, posX, height*0.2f, speed // Measures in meters except pos_y
                        ),
                        GameObjectCpu(
                            "building_08",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_08, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.123f, 1f, 1f),  // Bottom rect
                                arrayOf(0.1f, 0f, 0.9f, 1f)   // Top rect
                            ),
                            13f * scaleFactor, 70f * scaleFactor, width, height, ppm, posX, height*0.2f, speed
                        ),
                        GameObjectCpu(
                            "building_09",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_09, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.115f, 1f, 1f),       // Bottom rect
                                arrayOf(0.28f, 0.095f, 0.7f, 1f),  // Middle rect 1
                                arrayOf(0.4f, 0.026f, 0.5f, 1f),   // Middle rect 2
                                arrayOf(0.43f, 0.003f, 0.48f, 1f)  // Top rect
                            ),
                            17f * scaleFactor, 70f * scaleFactor, width, height, ppm, posX, height/5f, speed
                        ),
                        GameObjectCpu(
                            "building_10",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_10, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.094f, 1f, 1f),        // Bottom rect
                                arrayOf(0.09f, 0.087f, 0.96f, 1f),  // Middle rect 1
                                arrayOf(0.13f, 0.015f, 0.29f, 1f),  // Top rect left
                                arrayOf(0.4f, 0f, 0.7f, 1f),        // Top rect middle
                                arrayOf(0.75f, 0.026f, 0.92f, 1f)   // Top rect right
                            ),
                            13f * scaleFactor, 60f * scaleFactor, width, height, ppm, posX, height/5f, speed
                        ),
                        GameObjectCpu(
                            "building_11",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_11, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.03f, 1f, 1f),    // Bottom rect
                                arrayOf(0.15f, 0f, 0.57f, 1f)  // Top rect
                            ),
                            17f * scaleFactor, 55f * scaleFactor, width, height, ppm, posX, height/5f, speed
                        ),
                        GameObjectCpu(
                            "building_12",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_12, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.042f, 1f, 1f),  // Bottom rect
                                arrayOf(0.06f, 0f, 0.9f, 1f)  // Top rect
                            ),
                            15f * scaleFactor, 50f * scaleFactor, width, height, ppm, posX, height/5f, speed
                        ),
                        GameObjectCpu(
                            "building_13",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_13, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.11f, 1f, 1f),        // Bottom rect
                                arrayOf(0.05f, 0.01f, 0.15f, 1f),  // Top rect left
                                arrayOf(0.15f, 0f, 0.82f, 1f),     // Top rect middle
                                arrayOf(0.83f, 0.04f, 1f, 1f),     // Top rect right 1
                                arrayOf(0.86f, 0f, 0.94f, 1f)      // Top rect right 2
                            ),
                            20f * scaleFactor, 50f * scaleFactor, width, height, ppm, posX, height/5f, speed
                        ),
                        GameObjectCpu(
                            "building_14",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_14, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.185f, 1f, 1f),        // Bottom rect
                                arrayOf(0.29f, 0.065f, 0.68f, 1f),  // Middle rect
                                arrayOf(0.53f, 0.035f, 0.59f, 1f),  // Top rect left
                                arrayOf(0.34f, 0f, 0.35f, 1f)       // Top rect right
                            ),
                            30f * scaleFactor, 55f * scaleFactor, width, height, ppm, posX, height/5f, speed
                        ),
                        GameObjectCpu(
                            "building_15",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_15, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.43f, 1f, 1f),         // Bottom rect
                                arrayOf(0.1f, 0.3f, 0.86f, 1f),     // Middle rect 1
                                arrayOf(0.25f, 0.256f, 0.71f, 1f),  // Middle rect 2
                                arrayOf(0.28f, 0.236f, 0.68f, 1f),  // Middle rect 2
                                arrayOf(0.4f, 0.18f, 0.57f, 1f),    // Top rect 1
                                arrayOf(0.53f, 0f, 0.57f, 1f)       // Top rect 2
                            ),
                            13f * scaleFactor, 55f * scaleFactor, width, height, ppm, posX, height/5f, speed
                        ),
                        GameObjectCpu(
                            "building_16",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_16, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.2f, 1f, 1f),          // Bottom rect
                                arrayOf(0.13f, 0.175f, 0.87f, 1f),  // Middle rect 1
                                arrayOf(0.24f, 0.147f, 0.76f, 1f),  // Middle rect 2
                                arrayOf(0.28f, 0.122f, 0.49f, 1f),  // Middle rect 3
                                arrayOf(0.53f, 0f, 0.55f, 1f),      // Top rect left
                                arrayOf(0.6f, 0.039f, 0.62f, 1f),   // Top rect middle
                                arrayOf(0.68f, 0.03f, 0.71f, 1f)    // Top rect right
                            ),
                            20f * scaleFactor, 50f * scaleFactor, width, height, ppm, posX, height/5f, speed
                        ),
                        GameObjectCpu(
                            "building_17",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_day_17, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, 0.094f, 1f, 1f),       // Bottom rect
                                arrayOf(0.15f, 0.06f, 0.88f, 1f),  // Middle rect
                                arrayOf(0.41f, 0f, 0.63f, 1f)      // Top rect
                            ),
                            28f * scaleFactor, 60f * scaleFactor, width, height, ppm, posX, height/5f, speed
                        )
                    )
                }
                else {
                    bg = ResourcesCompat.getDrawable(resources, R.drawable.city_bg_night, null)?.toBitmap(width, height)!!

                    buildings = arrayOf(
                        GameObjectCpu(
                            "building_01",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_01, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, dp2px(260f), 1f, 0f),       // Bottom rect
                                arrayOf(0.05f, dp2px(210f), 0.92f, 0f), // Middle rect 1
                                arrayOf(0.85f, dp2px(185f), 0.92f, 0f), // Middle rect 1
                                arrayOf(0.1f, dp2px(165f), 0.86f, 0f),  // Middle rect 2
                                arrayOf(0.78f, dp2px(145f), 0.86f, 0f), // Middle rect 2
                                arrayOf(0.18f, dp2px(135f), 0.78f, 0f), // Middle rect 3
                                arrayOf(0.2f, dp2px(118f), 0.75f, 0f),  // Top rect
                                arrayOf(0.25f, dp2px(100f), 0.7f, 0f),  // Top rect
                                arrayOf(0.3f, dp2px(90f), 0.65f, 0f),   // Top rect
                                arrayOf(0.35f, dp2px(82f), 0.6f, 0f)    // Top rect
                            ),
                            15f, 100f, width, height, ppm, posX, height*0.2f, speed
                        ),
                        GameObjectCpu(
                            "building_02",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_02, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0.26f, dp2px(345f), 0.72f, 0f),  // Bottom rect
                                arrayOf(0.4f, dp2px(153f), 0.6f, 0f),    // Middle rect
                                arrayOf(0.45f, 0f, 0.54f, 0f)               // Top rect
                            ),
                            10f, 110f, width, height, ppm, posX, height*0.2f, speed
                        ),
                        GameObjectCpu(
                            "building_03",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_03, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0.75f, dp2px(75f), 1f, 0f),    // Bottom rect
                                arrayOf(0f, dp2px(43f), 0.75f, 0f),    // Bottom rect
                                arrayOf(0.68f, dp2px(19f), 0.82f, 0f), // Top rect
                                arrayOf(0.16f, 0f, 0.68f, 0f)             // Top rect
                            ),
                            18f, 70f, width, height, ppm, posX, height*0.2f, speed
                        ),
                        GameObjectCpu(
                            "building_04",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_04, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, dp2px(112f), 0.24f, 0f),  // Bottom rect
                                arrayOf(0.24f, dp2px(85f), 1f, 0f),   // Bottom rect
                                arrayOf(0.16f, dp2px(18f), 0.3f, 0f), // Top rect
                                arrayOf(0.3f, 0f, 0.82f, 0f)             // Top rect
                            ),
                            //30f, 90f, width, height, ppm, width + 20f, height*0.2f, speed
                            20f, 70f, width, height, ppm, posX, height*0.2f, speed
                        ),
                        GameObjectCpu(
                            "building_05",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_05, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, dp2px(72f), 1f, 0f),      // Bottom rect
                                arrayOf(0f, dp2px(28f), 0.7f, 0f),    // Middle rect 1
                                arrayOf(0.45f, dp2px(10f), 0.6f, 0f), // Middle rect 2
                                arrayOf(0.7f, dp2px(40f), 0.8f, 0f), // Middle rect 3
                                arrayOf(0.8f, dp2px(55f), 0.9f, 0f), // Middle rect 4
                                arrayOf(0.13f, 0f, 0.45f, 0f)           // Top rect
                            ),
                            25f, 60f, width, height, ppm, posX, height*0.2f, speed
                        ),
                        GameObjectCpu(
                            "building_06",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_06, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, dp2px(150f), 1f, 0f),        // Bottom rect
                                arrayOf(0.05f, dp2px(122f), 0.95f, 0f),  // Middle rect 1
                                arrayOf(0.1f, dp2px(98f), 0.9f, 0f),     // Middle rect 2
                                arrayOf(0.15f, dp2px(80f), 0.86f, 0f),   // Middle rect 3
                                arrayOf(0.18f, dp2px(67f), 0.82f, 0f),   // Middle rect 4
                                arrayOf(0.23f, dp2px(57f), 0.76f, 0f),   // Middle rect 5
                                arrayOf(0.28f, dp2px(46f), 0.72f, 0f),   // Middle rect 6
                                arrayOf(0.33f, dp2px(36f), 0.68f, 0f),   // Middle rect 7
                                arrayOf(0.37f, dp2px(26f), 0.65f, 0f),   // Middle rect 8
                                arrayOf(0.41f, dp2px(16f), 0.6f, 0f),    // Middle rect 9
                                arrayOf(0.47f, 0f, 0.52f, 0f)               // Top rect
                            ),
                            25f, 100f, width, height, ppm, posX, height*0.2f, speed // Measures in meters except pos_y
                        ),
                        GameObjectCpu(
                            "building_07",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_07, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, dp2px(102f), 0.93f, 0f),    // Bottom rect
                                arrayOf(0.06f, dp2px(60f), 0.87f, 0f),  // Middle rect
                                arrayOf(0.19f, dp2px(10f), 0.74f, 0f),  // Middle rect
                                arrayOf(0.26f, 0f, 0.67f, 0f)              // Top rect
                            ),
                            18f, 80f, width, height, ppm, posX, height*0.2f, speed // Measures in meters except pos_y
                        ),
                        GameObjectCpu(
                            "building_08",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_08, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, dp2px(20f), 1f, 0f),      // Bottom rect
                                arrayOf(0.1f, dp2px(15f), 0.95f, 0f), // Middle rect 1
                                arrayOf(0.27f, dp2px(10f), 0.9f, 0f), // Middle rect 2
                                arrayOf(0.47f, dp2px(5f), 0.8f, 0f),  // Middle rect 3
                                arrayOf(0.65f, 0f, 0.75f, 0f)            // Top rect
                            ),
                            17f, 70f, width, height, ppm, posX, height*0.2f, speed
                        ),
                        GameObjectCpu(
                            "building_09",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_09, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, dp2px(22f), 1f, 0f),       // Bottom rect
                                arrayOf(0.08f, dp2px(15f), 0.93f, 0f), // Middle rect 1
                                arrayOf(0.15f, dp2px(8f), 0.87f, 0f),  // Middle rect 2
                                arrayOf(0.23f, 0f, 0.77f, 0f)             // Top rect
                            ),
                            25f, 70f, width, height, ppm, posX, height/5f, speed
                        ),
                        GameObjectCpu(
                            "building_10",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_10, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0.25f, dp2px(29f), 1f, 0f),    // Bottom rect
                                arrayOf(0.33f, dp2px(20f), 0.87f, 0f)  // Top rect
                            ),
                            13f, 65f, width, height, ppm, posX, height/5f, speed
                        ),
                        GameObjectCpu(
                            "building_11",
                            arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.building_night_11, null)?.toBitmap(width, height)!!),
                            arrayOf(
                                arrayOf(0f, dp2px(203f), 1f, 0f),       // Bottom rect
                                arrayOf(0.1f, dp2px(182f), 0.9f, 0f),  // Middle rect
                                arrayOf(0.2f, dp2px(162f), 0.8f, 0f),  // Middle rect
                                arrayOf(0.3f, dp2px(140f), 0.7f, 0f),  // Middle rect
                                arrayOf(0.4f, dp2px(100f), 0.54f, 0f),  // Middle rect
                                arrayOf(0.46f, 0f, 0.5f, 0f)               // Top rect
                            ),
                            13f, 80f, width, height, ppm, posX, height/5f, speed
                        )
                    )

                    /*buildings = arrayOf(
                        Building(
                            "building_01",
                            ResourcesCompat.getDrawable(resources, R.drawable.building_night_01, null)?.toBitmap(width, height)!!,
                            arrayOf(
                                arrayOf(0f, dp2px(260f), 1f, 0f),       // Bottom rect
                                arrayOf(0.05f, dp2px(210f), 0.92f, 0f), // Middle rect 1
                                arrayOf(0.85f, dp2px(185f), 0.92f, 0f), // Middle rect 1
                                arrayOf(0.1f, dp2px(165f), 0.86f, 0f),  // Middle rect 2
                                arrayOf(0.78f, dp2px(145f), 0.86f, 0f), // Middle rect 2
                                arrayOf(0.18f, dp2px(135f), 0.78f, 0f), // Middle rect 3
                                arrayOf(0.2f, dp2px(118f), 0.75f, 0f),  // Top rect
                                arrayOf(0.25f, dp2px(100f), 0.7f, 0f),  // Top rect
                                arrayOf(0.3f, dp2px(90f), 0.65f, 0f),   // Top rect
                                arrayOf(0.35f, dp2px(82f), 0.6f, 0f)    // Top rect
                            ),
                            15f, 100f, width, height, ppm, currentWorldWidth + 3f, height*0.2f, speed
                        ),
                        Building(
                            "building_02",
                            ResourcesCompat.getDrawable(resources, R.drawable.building_night_02, null)?.toBitmap(width, height)!!,
                            arrayOf(
                                arrayOf(0.26f, dp2px(345f), 0.72f, 0f),  // Bottom rect
                                arrayOf(0.4f, dp2px(153f), 0.6f, 0f),    // Middle rect
                                arrayOf(0.45f, 0f, 0.54f, 0f)               // Top rect
                            ),
                            10f, 110f, width, height, ppm, currentWorldWidth + 3f, height*0.2f, speed
                        ),
                        Building(
                            "building_03",
                            ResourcesCompat.getDrawable(resources, R.drawable.building_night_03, null)?.toBitmap(width, height)!!,
                            arrayOf(
                                arrayOf(0.75f, dp2px(75f), 1f, 0f),    // Bottom rect
                                arrayOf(0f, dp2px(43f), 0.75f, 0f),    // Bottom rect
                                arrayOf(0.68f, dp2px(19f), 0.82f, 0f), // Top rect
                                arrayOf(0.16f, 0f, 0.68f, 0f)             // Top rect
                            ),
                            18f, 70f, width, height, ppm, currentWorldWidth + 3f, height*0.2f, speed
                        ),
                        Building(
                            "building_04",
                            ResourcesCompat.getDrawable(resources, R.drawable.building_night_04, null)?.toBitmap(width, height)!!,
                            arrayOf(
                                arrayOf(0f, dp2px(112f), 0.24f, 0f),  // Bottom rect
                                arrayOf(0.24f, dp2px(85f), 1f, 0f),   // Bottom rect
                                arrayOf(0.16f, dp2px(18f), 0.3f, 0f), // Top rect
                                arrayOf(0.3f, 0f, 0.82f, 0f)             // Top rect
                            ),
                            //30f, 90f, width, height, ppm, width + 20f, height*0.2f, speed
                            20f, 70f, width, height, ppm, currentWorldWidth + 3f, height*0.2f, speed
                        ),
                        Building(
                            "building_05",
                            ResourcesCompat.getDrawable(resources, R.drawable.building_night_05, null)?.toBitmap(width, height)!!,
                            arrayOf(
                                arrayOf(0f, dp2px(72f), 1f, 0f),      // Bottom rect
                                arrayOf(0f, dp2px(28f), 0.7f, 0f),    // Middle rect 1
                                arrayOf(0.45f, dp2px(10f), 0.6f, 0f), // Middle rect 2
                                arrayOf(0.7f, dp2px(40f), 0.8f, 0f), // Middle rect 3
                                arrayOf(0.8f, dp2px(55f), 0.9f, 0f), // Middle rect 4
                                arrayOf(0.13f, 0f, 0.45f, 0f)           // Top rect
                            ),
                            25f, 60f, width, height, ppm, currentWorldWidth + 3f, height*0.2f, speed
                        ),
                        Building(
                            "building_06",
                            ResourcesCompat.getDrawable(resources, R.drawable.building_night_06, null)?.toBitmap(width, height)!!,
                            arrayOf(
                                arrayOf(0f, dp2px(150f), 1f, 0f),        // Bottom rect
                                arrayOf(0.05f, dp2px(122f), 0.95f, 0f),  // Middle rect 1
                                arrayOf(0.1f, dp2px(98f), 0.9f, 0f),     // Middle rect 2
                                arrayOf(0.15f, dp2px(80f), 0.86f, 0f),   // Middle rect 3
                                arrayOf(0.18f, dp2px(67f), 0.82f, 0f),   // Middle rect 4
                                arrayOf(0.23f, dp2px(57f), 0.76f, 0f),   // Middle rect 5
                                arrayOf(0.28f, dp2px(46f), 0.72f, 0f),   // Middle rect 6
                                arrayOf(0.33f, dp2px(36f), 0.68f, 0f),   // Middle rect 7
                                arrayOf(0.37f, dp2px(26f), 0.65f, 0f),   // Middle rect 8
                                arrayOf(0.41f, dp2px(16f), 0.6f, 0f),    // Middle rect 9
                                arrayOf(0.47f, 0f, 0.52f, 0f)               // Top rect
                            ),
                            25f, 100f, width, height, ppm, currentWorldWidth + 3f, height*0.2f, speed // Measures in meters except pos_y
                        ),
                        Building(
                            "building_07",
                            ResourcesCompat.getDrawable(resources, R.drawable.building_night_07, null)?.toBitmap(width, height)!!,
                            arrayOf(
                                arrayOf(0f, dp2px(102f), 0.93f, 0f),    // Bottom rect
                                arrayOf(0.06f, dp2px(60f), 0.87f, 0f),  // Middle rect
                                arrayOf(0.19f, dp2px(10f), 0.74f, 0f),  // Middle rect
                                arrayOf(0.26f, 0f, 0.67f, 0f)              // Top rect
                            ),
                            18f, 80f, width, height, ppm, currentWorldWidth + 3f, height*0.2f, speed // Measures in meters except pos_y
                        ),
                        Building(
                            "building_08",
                            ResourcesCompat.getDrawable(resources, R.drawable.building_night_08, null)?.toBitmap(width, height)!!,
                            arrayOf(
                                arrayOf(0f, dp2px(20f), 1f, 0f),      // Bottom rect
                                arrayOf(0.1f, dp2px(15f), 0.95f, 0f), // Middle rect 1
                                arrayOf(0.27f, dp2px(10f), 0.9f, 0f), // Middle rect 2
                                arrayOf(0.47f, dp2px(5f), 0.8f, 0f),  // Middle rect 3
                                arrayOf(0.65f, 0f, 0.75f, 0f)            // Top rect
                            ),
                            17f, 70f, width, height, ppm, currentWorldWidth + 3f, height*0.2f, speed
                        ),
                        Building(
                            "building_09",
                            ResourcesCompat.getDrawable(resources, R.drawable.building_night_09, null)?.toBitmap(width, height)!!,
                            arrayOf(
                                arrayOf(0f, dp2px(22f), 1f, 0f),       // Bottom rect
                                arrayOf(0.08f, dp2px(15f), 0.93f, 0f), // Middle rect 1
                                arrayOf(0.15f, dp2px(8f), 0.87f, 0f),  // Middle rect 2
                                arrayOf(0.23f, 0f, 0.77f, 0f)             // Top rect
                            ),
                            25f, 70f, width, height, ppm, currentWorldWidth + 3f, height/5f, speed
                        ),
                        Building(
                            "building_10",
                            ResourcesCompat.getDrawable(resources, R.drawable.building_night_10, null)?.toBitmap(width, height)!!,
                            arrayOf(
                                arrayOf(0.25f, dp2px(29f), 1f, 0f),    // Bottom rect
                                arrayOf(0.33f, dp2px(20f), 0.87f, 0f)  // Top rect
                            ),
                            13f, 65f, width, height, ppm, currentWorldWidth + 3f, height/5f, speed
                        ),
                        Building(
                            "building_11",
                            ResourcesCompat.getDrawable(resources, R.drawable.building_night_11, null)?.toBitmap(width, height)!!,
                            arrayOf(
                                arrayOf(0f, dp2px(203f), 1f, 0f),       // Bottom rect
                                arrayOf(0.1f, dp2px(182f), 0.9f, 0f),  // Middle rect
                                arrayOf(0.2f, dp2px(162f), 0.8f, 0f),  // Middle rect
                                arrayOf(0.3f, dp2px(140f), 0.7f, 0f),  // Middle rect
                                arrayOf(0.4f, dp2px(100f), 0.54f, 0f),  // Middle rect
                                arrayOf(0.46f, 0f, 0.5f, 0f)               // Top rect
                            ),
                            13f, 80f, width, height, ppm, currentWorldWidth + 3f, height/5f, speed
                        )
                    )*/
                }

                pickBuilding(width, height)
            }

            val loadVehicles = async {
                Log.d("COROUTINE", "Load vehicles")

                vehicles = arrayOf(
                    GameObjectCpu(
                        "vehicle_01",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.biplane, null)?.toBitmap(width, height)!!),
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        6f * scaleFactor, 2.5f * scaleFactor, width, height, ppm, posX, height*0.05f, speed // Measures in meters except pos_y
                    ),

                    GameObjectCpu(
                        "vehicle_02",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.dirigible_1, null)?.toBitmap(width, height)!!),
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f * scaleFactor, 8f * scaleFactor, width, height, ppm, posX, height*0.05f, speed
                    ),
                    GameObjectCpu(
                        "vehicle_03",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.dirigible_2, null)?.toBitmap(width, height)!!),
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f * scaleFactor, 8f * scaleFactor, width, height, ppm, posX, height*0.05f, speed
                    ),
                    GameObjectCpu(
                        "vehicle_04",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.dirigible_3, null)?.toBitmap(width, height)!!),
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f * scaleFactor, 8f * scaleFactor, width, height, ppm, posX, height*0.05f, speed
                    ),
                    GameObjectCpu(
                        "vehicle_05",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.dirigible_4, null)?.toBitmap(width, height)!!),
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f * scaleFactor, 8f * scaleFactor, width, height, ppm, posX, height*0.05f, speed
                    ),
                    GameObjectCpu(
                        "vehicle_06",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.dirigible_5, null)?.toBitmap(width, height)!!),
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f * scaleFactor, 8f * scaleFactor, width, height, ppm, posX, height*0.05f, speed
                    ),
                    GameObjectCpu(
                        "vehicle_07",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.dirigible_6, null)?.toBitmap(width, height)!!),
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f * scaleFactor, 8f * scaleFactor, width, height, ppm, posX, height*0.05f, speed
                    ),
                    GameObjectCpu(
                        "vehicle_08",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.dirigible_7, null)?.toBitmap(width, height)!!),
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f * scaleFactor, 8f * scaleFactor, width, height, ppm, posX, height*0.05f, speed
                    ),


                    GameObjectCpu(
                        "vehicle_09",
                        arrayOf(ResourcesCompat.getDrawable(resources, R.drawable.ufo, null)?.toBitmap(width, height)!!),
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        4f * scaleFactor, 2f * scaleFactor, width, height, ppm, posX, height*0.05f, speed
                    )
                )

                /*vehicles = arrayOf(
                    Vehicle(
                        //"biplane",
                        "vehicle_01",
                        ResourcesCompat.getDrawable(resources, R.drawable.biplane, null)?.toBitmap(width, height)!!,
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        //7f, 3f, width, height, ppm, width + 420f, 20f, speed
                        //7f, 3f, width, height, ppm, width + 3f*ppm, ppm, speed
                        6f * scaleFactor, 2.5f * scaleFactor, width, height, ppm, posX, height*0.05f, speed // Measures in meters except pos_y
                    )/*,

                    Vehicle(
                        "dirigible_1",
                        ResourcesCompat.getDrawable(resources, R.drawable.dirigible_1, null)?.toBitmap(width, height)!!,
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                    ),
                    Vehicle(
                        "dirigible_2",
                        ResourcesCompat.getDrawable(resources, R.drawable.dirigible_2, null)?.toBitmap(width, height)!!,
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                    ),
                    Vehicle(
                        "dirigible_3",
                        ResourcesCompat.getDrawable(resources, R.drawable.dirigible_3, null)?.toBitmap(width, height)!!,
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                    ),
                    Vehicle(
                        "dirigible_4",
                        ResourcesCompat.getDrawable(resources, R.drawable.dirigible_4, null)?.toBitmap(width, height)!!,
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                    ),
                    Vehicle(
                        "dirigible_5",
                        ResourcesCompat.getDrawable(resources, R.drawable.dirigible_5, null)?.toBitmap(width, height)!!,
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                    ),
                    Vehicle(
                        "dirigible_6",
                        ResourcesCompat.getDrawable(resources, R.drawable.dirigible_6, null)?.toBitmap(width, height)!!,
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                    ),
                    Vehicle(
                        "dirigible_7",
                        ResourcesCompat.getDrawable(resources, R.drawable.dirigible_7, null)?.toBitmap(width, height)!!,
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                    ),


                    Vehicle(
                        "ufo",
                        ResourcesCompat.getDrawable(resources, R.drawable.ufo, null)?.toBitmap(width, height)!!,
                        arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                        4f, 2f, width, height, ppm, width + 420f, 20f, speed
                    )*/
                )*/

                pickVehicle(width, height)
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
                    7f, 3f, bitmaps[0].width, bitmaps[0].height, ppm, width + 20f, 20f, speed
                ),

                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_1, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[1].width, bitmaps[1].height, ppm, width + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_2, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[2].width, bitmaps[2].height, ppm, width + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_3, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[3].width, bitmaps[3].height, ppm, width + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_4, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[4].width, bitmaps[4].height, ppm, width + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_5, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[5].width, bitmaps[5].height, ppm, width + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_6, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[6].width, bitmaps[6].height, ppm, width + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_7, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[7].width, bitmaps[7].height, ppm, width + 20f + 20f, 20f, speed
                ),


                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.ufo, null)?.toBitmap()!!,
                    4f, 2f, bitmaps[8].width, bitmaps[8].height, ppm, width + 20f + 20f, 20f, speed
                )
            )*/


            /*buildings = arrayOf(
                ResourcesCompat.getDrawable(resources, R.drawable.building_01, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_02, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_03, null)?.toBitmap(width/3, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_04, null)?.toBitmap(width/2, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_05, null)?.toBitmap((width/1.5).toInt(), height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_06, null)?.toBitmap((width/1.2).toInt(), height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_07, null)?.toBitmap((width/3.5).toInt(), height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_08, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_09, null)?.toBitmap(width/3, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_10, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_11, null)?.toBitmap((width/2.5).toInt(), height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_12, null)?.toBitmap(width/3, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_13, null)?.toBitmap((width/2.5).toInt(), height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_14, null)?.toBitmap(width/2, height)!!

                /*ResourcesCompat.getDrawable(resources, R.drawable.building_15, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_16, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_17, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_18, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_19, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_20, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_21, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_22, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_23, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_24, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_25, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_26, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_27, null)?.toBitmap(width/4, height)!!*/
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
                    15f, 50f, bitmaps[0].width, bitmaps[0].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_02, null)?.toBitmap()!!,
                    10f, 60f, bitmaps[1].width, bitmaps[1].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_03, null)?.toBitmap()!!,
                    15f, 50f, bitmaps[2].width, bitmaps[2].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_04, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[3].width, bitmaps[3].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_05, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[4].width, bitmaps[4].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_06, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[5].width, bitmaps[5].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_07, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[6].width, bitmaps[6].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_08, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[7].width, bitmaps[7].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_09, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[8].width, bitmaps[8].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_10, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[9].width, bitmaps[9].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_11, null)?.toBitmap()!!,
                    15f, 50f, bitmaps[10].width, bitmaps[10].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_12, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[11].width, bitmaps[11].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_13, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[12].width, bitmaps[12].height, ppm, width + 20f + 20f, height/2f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_14, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[13].width, bitmaps[13].height, ppm, width + 20f + 20f, height/2f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_15, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[14].width, bitmaps[14].height, ppm, width + 20f + 20f, height/2f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_16, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[15].width, bitmaps[15].height, ppm, width + 20f + 20f, height/2f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_17, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[16].width, bitmaps[16].height, ppm, width + 20f + 20f, height/2f, speed
                )
            )*/

            // ===================================================================================================










            val loadPlayer = async {
                playerVehicle = GameObjectPlayer(
                    "player_vehicle",
                    arrayOf(
                        ResourcesCompat.getDrawable(resources, R.drawable.heli_green_0, null)?.toBitmap(width, height)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.heli_green_1, null)?.toBitmap(width, height)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.heli_green_2, null)?.toBitmap(width, height)!!,
                        ResourcesCompat.getDrawable(resources, R.drawable.heli_green_3, null)?.toBitmap(width, height)!!
                    ),
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    10f * scaleFactor, 3f * scaleFactor, width, height, ppm, 1f, height*0.4f, speed // Measures in meters except pos_y
                )
            }

            loadBuildings.await()
            loadVehicles.await()
            loadPlayer.await()

            start(width, height)
        }
    }

    private fun start(width: Int, height: Int) {
        val fps = 90
        val targetFrameTime = 1000L / fps
        var timePreviousFrame = 0L


        // Dispatchers.Default — is used by all standard builders if no dispatcher or any other ContinuationInterceptor is specified in their context.
        // It uses a common pool of shared background threads.
        // This is an appropriate choice for compute-intensive coroutines that consume CPU resources
        lifecycleScope.launch(Dispatchers.Default) {
            while (true) {
                //Log.d("COROUTINE", "==================")
                //Log.d("COROUTINE", "COROUTINE MAIN - I'm working in thread ${Thread.currentThread().name}")

                // Compute time difference
                val timeCurrentFrame = System.currentTimeMillis()
                val dt = min(timeCurrentFrame - timePreviousFrame, targetFrameTime) / 1000f

                /*Log.d("TIME", "************")
                Log.d("TIME", "timePreviousFrame: $timePreviousFrame ms")
                Log.d("TIME", "timeCurrentFrame: $timeCurrentFrame ms")
                Log.d("TIME", "dt: $dt s")
                Log.d("TIME", "speed (m/s): $speed")
                Log.d("TIME", "speed (px/s): " + speed * ppm * dt)*/

                timePreviousFrame = timeCurrentFrame

                val updatePlayer = async {
                    //Log.d("COROUTINE", "ASYNC UPDATE USER - I'm working in thread ${Thread.currentThread().name}")
                    //Log.d("COROUTINE", "ASYNC UPDATE USER - dt $dt")

                    playerVehicle.update(dt, lastGyroscopeInput)

                    // Scale the bitmap
                    /*heli_matrix.setScale(heli_scale_x, heli_scale_y)

                    // Rotate the bitmap
                    heli_matrix.postRotate(heli_rotation)

                    // Put the bitmap on the left-center of the screen
                    heli_matrix.postTranslate(user_dx, user_dy)*/

                    /*withContext(Dispatchers.Main) {
                        Log.d("COROUTINE", "ASYNC UPDATE USER MAIN CONTEXT - I'm working in thread ${Thread.currentThread().name}")
                        // Scale the bitmap
                        heli_matrix.setScale(heli_scale_x, heli_scale_y)

                        // Rotate the bitmap
                        heli_matrix.postRotate(heli_rotation)

                        // Put the bitmap on the left-center of the screen
                        heli_matrix.postTranslate(user_dx, user_dy)
                    }*/
                }
                val updateVehicle = async {
                    //Log.d("COROUTINE", "ASYNC UPDATE VEHICLE - I'm working in thread ${Thread.currentThread().name}")
                    //Log.d("COROUTINE", "ASYNC UPDATE VEHICLE - dt: $dt")

                    vehicle.update(dt)

                    /*withContext(Dispatchers.Main) {
                        Log.d("COROUTINE", "ASYNC UPDATE VEHICLE MAIN CONTEXT - I'm working in thread ${Thread.currentThread().name}")
                        vehicle.transform()
                    }*/

                    // Check if the vehicle has been respawn
                    if (vehicle.isRespawn())
                        pickVehicle(width, height)
                }
                val updateBuilding = async {
                    //Log.d("COROUTINE", "ASYNC UPDATE BUILDING - I'm working in thread ${Thread.currentThread().name}")
                    //Log.d("COROUTINE", "ASYNC UPDATE BUILDING - dt: $dt")

                    building.update(dt)

                    /*withContext(Dispatchers.Main) {
                        Log.d("COROUTINE", "ASYNC UPDATE BUILDING MAIN CONTEXT - I'm working in thread ${Thread.currentThread().name}")
                        building.transform()
                    }*/

                    // Check if the building has been respawn
                    if (building.isRespawn())
                        pickBuilding(width, height)
                }

                updatePlayer.await()
                updateVehicle.await()
                updateBuilding.await()

                val finalUpdate = async {
                    //Log.d("COROUTINE", "ASYNC FINAL UPDATE - I'm working in thread ${Thread.currentThread().name}")
                    //Log.d("COROUTINE", "ASYNC FINAL UPDATE - dt $dt")

                    // Check if a collision has been occurred
                    if (checkCollision()) {
                        gameEnd = true

                        val fragment = findFragment<GameFragment>()
                        fragment.gameEnd(score.toLong(), false)
                    }

                    // Update the score
                    // s = v * dt  in m/s
                    score += speed * dt

                    // Update the speed
                    //speed += 0.003f // max about 36-43 m/s. 50 m/s very difficult!
                    if (speed < 40f)
                        speed += 0.001f
                }

                finalUpdate.await()

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

        // Check if game has been initialized
        if (!startDrawing) {
            // Show loading
            painterFill.getTextBounds(loadingText, 0, loadingText.length, textRect)
            canvas?.drawText(
                loadingText,
                width/2f - textRect.width()/2f - textRect.left,
                height/2f + textRect.height()/2f - textRect.bottom,
                painterFill
            )

            return
        }

        // Draw background
        canvas?.drawBitmap(bg, 0f, 0f, null)

        // Perform matrix transformations here because
        // since onDraw is continuosly called (# calls to onDraw > # loops in game logic) even if I don't call invalidate,
        // it could happen that if I edit a global variable outside (e.g. matrices), then is not synchronized
        playerVehicle.transform()
        vehicle.transform()
        building.transform()

        // Draw bitmaps
        canvas?.drawBitmap(playerVehicle.getBitmap(), playerVehicle.getMatrix(), null)
        canvas?.drawBitmap(vehicle.getBitmap(), vehicle.getMatrix(), null)
        canvas?.drawBitmap(building.getBitmap(), building.getMatrix(), null)

        canvas?.drawText("SCORE " + score.toLong(), 20f, 60f, painterFill)

        // Debug (draw player's bounds)
        var bounds = playerVehicle.getBounds()
        for (rect in bounds)
            canvas?.drawRect(rect, painterStroke)

        // Debug (draw vehicle's bounds)
        bounds = vehicle.getBounds()
        for (rect in bounds)
            canvas?.drawRect(rect, painterStroke)

        // Debug (draw building's bounds)
        bounds = building.getBounds()
        for (rect in bounds)
            canvas?.drawRect(rect, painterStroke)
    }

    // Convert density independent pixels into screen pixels
    private fun dp2px(dp: Float): Float {
        return resources.displayMetrics.density * dp
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

    // Pick a random building and set a random height for it
    private fun pickBuilding(screenWidth: Int, screenHeight: Int, name: String? = null) {
        if (name == null || name.isEmpty()) {
            var b = buildings.random()

            // Check if building variable has been initialized
            if (startDrawing)
                // It has been init, try to pick a different building than the current one
                for (i in 0..2) {
                    if (b.getName() != building.getName())
                        break

                    b = buildings.random()
                }

            building = b
        }
        else {
            val i = getGameObjectIndexByName(name)

            try {
                building = buildings[i]
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()

                building = buildings.random()
            }
        }

        // Height range height*0.2 .. height*0.76 = from 20% to 76% of the screen height
        // Remember that 0% = top and 100% = bottom
        val r = (20..76).random() / 100f

        // For debug
        //val r = 0.2f
        //building.setX(screenWidth*0.35f)
        //building.setX(screenWidth*0.1f)

        building.setY(screenHeight*r)

        // Increase the distance between obstacles if the height of
        // the building is greater than a threshold
        //if (r > 3.5f && building.getX() - vehicle.getX() < heli_width_scaled * 2f)
        //    building.setX(screenWidth + vehicle.getBitmapScaledWidth() + heli_width_scaled * 2f)

        // Update speed
        building.setSpeed(speed)
    }

    // Pick a random vehicle and set a random height for it
    private fun pickVehicle(screenWidth: Int, screenHeight: Int, name: String? = null) {
        if (name == null || name.isEmpty()) {
            var v = vehicles.random()

            // Check if vehicle variable has been initialized
            if (startDrawing)
            // It has been init, try to pick a different vehicle than the current one
                for (i in 0..2) {
                    if (v.getName() != vehicle.getName())
                        break

                    v = vehicles.random()
                }

            vehicle = v
        }
        else {
            val i = getGameObjectIndexByName(name)

            try {
                vehicle = vehicles[i]
            } catch (e: IndexOutOfBoundsException) {
                e.printStackTrace()

                vehicle = vehicles.random()
            }
        }

        // Height range height*0 .. height*0.2 = from 0% to 20% of the screen height
        // Remember that 0% = top and 100% = bottom
        val r = (0..20).random() / 100f

        // For debug
        //val r = 0f
        //vehicle.setX(screenWidth*0.35f)

        vehicle.setY(screenHeight*r)

        // Increase the distance between obstacles if the height of
        // the building is greater than a threshold
        //if (building.getY() > height/3.5f && vehicle.getX() - building.getX() < heli_width_scaled * 2f)
        //vehicle.setX(screenWidth + building.getBitmapScaledWidth() + heli_width_scaled * 2f)

        // Update speed (vehicle speed different from building one for more realism)
        vehicle.setSpeed(speed + 5f)
    }

    // Check if the user collides with an obstacle
    private fun checkCollision(): Boolean { return false
        //user_rect.set( user_dx, user_dy, user_dx + heli_width_scaled, user_dy + heli_height_scaled )

        val rects = building.getBounds()
        rects.addAll(vehicle.getBounds())

        val playerRect = playerVehicle.getBounds()[0]

        for (r in rects)
            if (RectF.intersects(playerRect, r))
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

    // Detect user motion via sensors
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onFlushCompleted(sensor: Sensor?) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            //Sensor.TYPE_ACCELEROMETER  -> lastAcceleration = event.values.clone()
            //Sensor.TYPE_MAGNETIC_FIELD      -> lastGyroscope    = event.values.clone()
            Sensor.TYPE_GYROSCOPE      -> lastGyroscope    = event.values.clone()
        }

        // Use X value if portrait mode, Y value otherwise
        val value = if (width < height) lastGyroscope[0] else lastGyroscope[1]

        if (value > 0)
            lastGyroscopeInput = max(1f, min(value * 1, 15f))
        else if (value < 0)
            lastGyroscopeInput = min(-1f, max(value * 1, -15f))

        /*Log.d("SENSOR", "****************")
        Log.d("SENSOR", "value: " + lastGyroscope[0])
        Log.d("SENSOR", "lastGyroscopeInput: $lastGyroscopeInput")
        Log.d("SENSOR", "lastGyroscope[1]: ${lastGyroscope[1]}")*/

        invalidate()
    }

    // Save game variables
    override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())

        bundle.putFloat("score", score)
        bundle.putFloat("speed", speed)


        // NON USARE PIXEL O METRI PERCHÈ UNA DELLE 2 DIMENSIONI NON È VINCOLATA E
        // DAL MOMENTO CHE STO USANDO IL FATTORE DI SCALA, NEANCHE L'ALTRA È LA STESSA MA È IN PROPORZIONE
        Log.d("save", "Save state game view - player X: ${playerVehicle.getX()} px")
        Log.d("save", "Save state game view - player Y: ${playerVehicle.getY()} px")

        Log.d("save", "Save state game view - player X: ${playerVehicle.getX() / ppm} m")
        Log.d("save", "Save state game view - player Y: ${playerVehicle.getY() / ppm} m")

        // USA LA PERCENTUALE DELLO SCHERMO PER ENTRAMBE LE DIMENSIONI!
        // Get screen width and height (don't use directly getWidth() and getHeight() because here return 0) ?
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels

        Log.d("save", "Save state game view - player X: ${playerVehicle.getX() / width} => ${playerVehicle.getX() / width * 100} %")
        Log.d("save", "Save state game view - player Y: ${playerVehicle.getY() / height} => ${playerVehicle.getY() / height * 100} %")


        // VERSIONE CON METRI
        /*bundle.putFloat("playerPosY", playerVehicle.getY() / ppm)

        bundle.putString("vehicleName", vehicle.getName())
        bundle.putFloat("vehiclePosX", vehicle.getX() / ppm)
        bundle.putFloat("vehiclePosY", vehicle.getY() / ppm)

        bundle.putString("buildingName", building.getName())
        bundle.putFloat("buildingPosX", building.getX() / ppm)
        bundle.putFloat("buildingPosY", building.getY() / ppm)*/

        // VERSIONE CON PERCENTUALE SCHERMO
        bundle.putFloat("playerPosY", playerVehicle.getY() / height)

        bundle.putString("vehicleName", vehicle.getName())
        bundle.putFloat("vehiclePosX", vehicle.getX() / width)
        bundle.putFloat("vehiclePosY", vehicle.getY() / height)

        bundle.putString("buildingName", building.getName())
        bundle.putFloat("buildingPosX", building.getX() / width)
        bundle.putFloat("buildingPosY", building.getY() / height)

        return bundle
    }

    // Restore game variables
    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is Bundle) {
            val s = state.getParcelable<Parcelable>("superState")

            score = state.getFloat("score")
            speed = state.getFloat("speed")

            // Get screen width and height (don't use directly getWidth() and getHeight() because here return 0) ?
            val width = resources.displayMetrics.widthPixels
            val height = resources.displayMetrics.heightPixels

            // Dispatchers.Default — is used by all standard builders if no dispatcher or any other ContinuationInterceptor is specified in their context.
            // It uses a common pool of shared background threads.
            // This is an appropriate choice for compute-intensive coroutines that consume CPU resources
            lifecycleScope.launch(Dispatchers.Default) {
                val py = state.getFloat("playerPosY")

                // VERSIONE CON METRI
                /*vLog.d("save", "Restore state game view - player Y: $py m")
                Log.d("save", "Restore state game view - player Y: ${py * ppm} px")*/

                // VERSIONE CON PERCENTUALE SCHERMO
                Log.d("save", "Restore state game view - player Y: $py => ${py * 100} %")
                Log.d("save", "Restore state game view - player Y: ${py * height} px")
                Log.d("save", "Save state game view - player Y: ${py * height / ppm} m")

                while (true) {
                    try {
                        //playerVehicle.setY(py)
                        playerVehicle.setY(py * height)

                        break
                    }catch (e: UninitializedPropertyAccessException) {
                        //e.printStackTrace()
                        sleep(100)
                    }
                }
            }

            lifecycleScope.launch(Dispatchers.Default) {
                val vn = state.getString("vehicleName")
                val vx = state.getFloat("vehiclePosX")
                val vy = state.getFloat("vehiclePosY")

                while (true) {
                    try {
                        Log.d("save","restore state game view - Try to pick vehicle")

                        pickVehicle(width, height, vn)
                        //vehicle.setX(vx * ppm)
                        //vehicle.setY(vy)
                        vehicle.setX(vx * width)
                        vehicle.setY(vy * height)
                        vehicle.setSpeed(speed + 5f)

                        break
                    } catch (e: UninitializedPropertyAccessException) {
                        //e.printStackTrace()
                        sleep(100)
                    }
                }
            }

            lifecycleScope.launch(Dispatchers.Default) {
                val bn = state.getString("buildingName")
                val bx = state.getFloat("buildingPosX")
                val by = state.getFloat("buildingPosY")

                while (true) {
                    try {
                        Log.d("save","restore state game view - Try to pick building")

                        pickBuilding(width, height, bn)
                        //building.setX(bx * ppm)
                        //building.setY(by)
                        building.setX(bx * width)
                        building.setY(by * height)
                        building.setSpeed(speed)

                        break
                    } catch (e: UninitializedPropertyAccessException) {
                        //e.printStackTrace()
                        sleep(100)
                    }
                }
            }

            super.onRestoreInstanceState(s)
        }
        else
            super.onRestoreInstanceState(state)
    }
}






















// MONO THREAD VERSION
/*import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.findFragment
import com.example.fly2live.vehicle.Vehicle
import com.example.fly2live.building.Building
import com.example.fly2live.configuration.Configuration.Companion.SCENARIO
import com.example.fly2live.configuration.Configuration.Companion.SCENARIO_CITY_DAY
import kotlinx.coroutines.*
import java.lang.Thread.sleep
import kotlin.math.max
import kotlin.math.min


class GameView(context: Context?) : View(context), View.OnTouchListener, SensorEventListener2 {
    // Convert images into bitmaps once
    private var areImagesConverted = false

    // Bitmaps for user's helicopter animation
    private lateinit var heli_animation: Array<Bitmap>
    private lateinit var heli: Bitmap
    private var heli_animation_index = 0

    // Other bitmaps
    private lateinit var bg: Bitmap
    private lateinit var buildings: Array<Building>
    private lateinit var building: Building
    private lateinit var vehicles: Array<Vehicle>
    private lateinit var vehicle: Vehicle


    // Helicopter size
    private val heli_width  = 8f // meters
    private val heli_height = 3f // meters
    private var heli_scale_x = 0f
    private var heli_scale_y = 0f
    private var heli_width_scaled  = 0f
    private var heli_height_scaled = 0f


    // Graphics helicopter bitmap matrix
    private var heli_matrix      = Matrix()


    // Helicopter position
    private var user_dx = 1f  // meters
    private var user_dy = 25f // meters


    // Helicopter rotation (inclination)
    private var heli_rotation = 8f // Degrees


    // Bitmap rect
    private val user_rect = RectF()
    private val user_rect_matrix = Matrix()


    // Sensors values
    private var lastAcceleration = FloatArray(3)
    private var lastGyroscope    = FloatArray(3)


    // TEMP
    private val painter_fill = Paint().apply {
        style       = Paint.Style.FILL
        color       = Color.BLACK
        textSize    = 50f
    }

    // Painter for the debug of bitmap rects
    private val painter_stroke = Paint().apply {
        style       = Paint.Style.STROKE
        color       = Color.BLACK
        strokeWidth = 3f
    }

    private var score   = 0.0 // meters
    private var gameEnd = false

    //private var speed = 0.2f // m/s (max 0.5) VERSIONE CON UPDATE SENZA L'USO DI dt
    private var speed = 10f // m/s

    private var last_gyroscope_input = 0f // meters


    // Cannon ball graphics variables
    /*private var radius = 0.18f // meters
    private var cx = 0f
    private var cy = 0f

    private val ballPainter = Paint().apply {
        shader = RadialGradient(0f, 0f, radius, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP)
    }

    // Graphics shader matrix
    private var shaderMatrix = Matrix()

    // Variables for handling the shooting
    private var fired = false*/
    private var previous = 0L

    // Cannon ball velocity components
    /*private var vx = 0f
    private var vy = 0f*/

    // World constants
    //private var speed = 15f   // m/s
    private var g     = 9.81f // m/s^2
    private val world_width  = 30f  // meters
    private val world_height = 60f  // meters

    // Pixel per meter
    private var ppm = 1f

    // Finger coordinates
    /*private var fx = 0.0
    private var fy = 0.0*/


    private val fps = 90
    private val targetFrameTime = 1000L / fps


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
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // Check if game ended
        if (gameEnd)
            // Stop drawing
            return

        // Convert the images into bitmaps once
        if (!areImagesConverted) {
            areImagesConverted = true

            // I use min to keep almost a fixed ratio for drawing the bitmaps when rotating the screen.
            // I divide both to world_width because I'm interested in width for obstacles respawn
            ppm = min(width / world_width, height / world_width)

            Log.d("ppm", width.toString())
            Log.d("ppm", height.toString())
            Log.d("ppm", ppm.toString())

            // Convert the correct scenario images into bitmaps
            if (SCENARIO == SCENARIO_CITY_DAY) {
                bg = ResourcesCompat.getDrawable(resources, R.drawable.city_bg_day, null)?.toBitmap(width, height)!!

                buildings = arrayOf(
                    Building(
                        "building_01",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_01, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(64f), 1f, 0f),       // Bottom rect
                            arrayOf(0.07f, dp2px(43f), 0.38f, 0f), // Middle left rect
                            arrayOf(0.47f, dp2px(44f), 0.8f, 0f),  // Middle right rect
                            arrayOf(0.1f, 0f, 0.18f, 0f),          // Top left rect
                            arrayOf(0.47f, dp2px(32f), 0.63f, 0f)  // Top right rect
                        ),
                        //15f, 100f, width, height, ppm, width + 20f, height*0.2f, speed
                        15f, 100f, width, height, ppm, world_width + 3f, height*0.2f, speed
                    ),
                    Building(
                        "building_02",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_02, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0.22f, dp2px(450f), 0.81f, 0f),  // Bottom rect
                            arrayOf(0.33f, dp2px(232f), 0.7f, 0f),   // Middle rect
                            arrayOf(0.47f, 0f, 0.56f, 0f)            // Top rect
                        ),
                        //10f, 90f, width, height, ppm, width + 20f, height*0.2f, speed
                        10f, 90f, width, height, ppm, world_width + 3f, height*0.2f, speed
                    ),
                    Building(
                        "building_03",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_03, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(230f), 1f, 0f),       // Bottom rect
                            arrayOf(0.14f, dp2px(160f), 0.87f, 0f), // Middle rect
                            arrayOf(0.29f, dp2px(70f), 0.68f, 0f),  // Middle rect
                            arrayOf(0.45f, 0f, 0.6f, 0f)            // Top rect
                        ),
                        //25f, 110f, width, height, ppm, width + 20f, height*0.2f, speed
                        25f, 110f, width, height, ppm, world_width + 3f, height*0.2f, speed
                    ),
                    Building(
                        "building_04",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_04, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0.14f, dp2px(348f), 0.87f, 0f), // Bottom rect
                            arrayOf(0.21f, dp2px(212f), 0.79f, 0f), // Middle rect
                            arrayOf(0.24f, dp2px(88f), 0.76f, 0f),  // Middle rect
                            arrayOf(0.32f, 0f, 0.68f, 0f)           // Top rect
                        ),
                        //30f, 90f, width, height, ppm, width + 20f, height*0.2f, speed
                        30f, 90f, width, height, ppm, world_width + 3f, height*0.2f, speed
                    ),
                    Building(
                        "building_05",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_05, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(154f), 1f, 0f),       // Bottom rect
                            arrayOf(0f, dp2px(74f), 0.24f, 0f),     // Middle left rect
                            arrayOf(0.27f, dp2px(112f), 0.75f, 0f), // Middle center rect
                            arrayOf(0.78f, dp2px(112f), 0.9f, 0f),  // Middle right rect
                            arrayOf(0.34f, 0f, 0.75f, 0f)           // Top rect
                        ),
                        //35f, 60f, width, height, ppm, width + 20f, height*0.2f, speed
                        35f, 60f, width, height, ppm, world_width + 3f, height*0.2f, speed
                    ),
                    Building(
                        "building_06",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_06, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(301f), 1f, 0f),       // Bottom rect
                            arrayOf(0f, dp2px(75f), 0.37f, 0f),     // Middle left rect
                            arrayOf(0.53f, dp2px(175f), 0.87f, 0f), // Middle right rect
                            arrayOf(0.07f, 0f, 0.29f, 0f)           // Top rect
                        ),
                        //35f, 40f, width, height, ppm, width + 20f, height*0.2f, speed
                        35f, 40f, width, height, ppm, world_width + 3f, height*0.2f, speed // Measures in meters except pos_y
                    ),
                    Building(
                        "building_07",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_07, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(319f), 1f, 0f),       // Bottom rect
                            arrayOf(0f, dp2px(164f), 0.91f, 0f),    // Middle rect
                            arrayOf(0f, dp2px(140f), 0.22f, 0f),    // Left "triangle"
                            arrayOf(0.7f, dp2px(140f), 0.91f, 0f),  // Right "triangle"
                            arrayOf(0.31f, 0f, 0.4f, 0f)            // Top rect
                        ),
                        //15f, 80f, width, height, ppm, width + 20f, height/5f, speed
                        //15f, 80f, width, height, ppm, width + 3f*ppm, height/5f, speed
                        15f, 80f, width, height, ppm, world_width + 3f, height*0.2f, speed // Measures in meters except pos_y
                    ),
                    Building(
                        "building_08",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_08, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(120f), 1f, 0f),  // Bottom rect
                            arrayOf(0.1f, 0f, 0.9f, 0f)        // Top rect
                        ),
                        //10f, 70f, width, height, ppm, width + 20f, height*0.2f, speed
                        10f, 70f, width, height, ppm, world_width + 3f, height*0.2f, speed
                    ),
                    Building(
                        "building_09",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_09, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(110f), 1f, 0f),     // Bottom rect
                            arrayOf(0.28f, dp2px(90f), 0.7f, 0f), // Middle rect 1
                            arrayOf(0.4f, dp2px(23f), 0.5f, 0f),  // Middle rect 2
                            arrayOf(0.43f, 0f, 0.48f, 0f)         // Top rect
                        ),
                        //17f, 70f, width, height, ppm, width + 20f, height/5f, speed
                        17f, 70f, width, height, ppm, world_width + 3f, height/5f, speed
                    ),
                    Building(
                        "building_10",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_10, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(85f), 1f, 0f),        // Bottom rect
                            arrayOf(0.09f, dp2px(75f), 0.96f, 0f),  // Middle rect 1
                            arrayOf(0.13f, dp2px(15f), 0.29f, 0f),  // Top rect left
                            arrayOf(0.4f, 0f, 0.7f, 0f),               // Top rect middle
                            arrayOf(0.75f, dp2px(24f), 0.92f, 0f)  // Top rect right
                        ),
                        //10f, 65f, width, height, ppm, width + 20f, height/5f, speed
                        10f, 65f, width, height, ppm, world_width + 3f, height/5f, speed
                    ),
                    Building(
                        "building_11",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_11, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(24f), 1f, 0f), // Bottom rect
                            arrayOf(0.15f, 0f, 0.57f, 0f)    // Top rect
                        ),
                        //17f, 55f, width, height, ppm, width + 20f, height/5f, speed
                        17f, 55f, width, height, ppm, world_width + 3f, height/5f, speed
                    ),
                    Building(
                        "building_12",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_12, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(24f), 1f, 0f),       // Bottom rect
                            arrayOf(0.06f, 0f, 0.9f, 0f)  // Top rect
                        ),
                        //15f, 40f, width, height, ppm, width + 20f, height/5f, speed
                        15f, 40f, width, height, ppm, world_width + 3f, height/5f, speed
                    ),
                    Building(
                        "building_13",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_13, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(74f), 1f, 0f),       // Bottom rect
                            arrayOf(0.05f, dp2px(9f), 0.15f, 0f),  // Top rect left
                            arrayOf(0.15f, 0f, 0.82f, 0f),            // Top rect middle
                            arrayOf(0.83f, dp2px(40f), 1f, 0f),   // Top rect right 1
                            arrayOf(0.86f, 0f, 0.94f, 0f)            // Top rect right 2
                        ),
                        //15f, 50f, width, height, ppm, width + 20f, height/5f, speed
                        15f, 50f, width, height, ppm, world_width + 3f, height/5f, speed
                    ),
                    Building(
                        "building_14",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_14, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(180f), 1f, 0f),      // Bottom rect
                            arrayOf(0.29f, dp2px(63f), 0.68f, 0f), // Top rect
                            arrayOf(0.53f, dp2px(32f), 0.59f, 0f), // Top rect
                            arrayOf(0.34f, 0f, 0.35f, 0f)             // Top rect
                        ),
                        //23f, 70f, width, height, ppm, width + 20f, height/5f, speed
                        23f, 70f, width, height, ppm, world_width + 3f, height/5f, speed
                    ),
                    Building(
                        "building_15",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_15, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(355f), 1f, 0f),       // Bottom rect
                            arrayOf(0.1f, dp2px(246f), 0.86f, 0f),  // Middle rect 1
                            arrayOf(0.25f, dp2px(207f), 0.71f, 0f), // Middle rect 2
                            arrayOf(0.4f, dp2px(148f), 0.57f, 0f),  // Top rect 1
                            arrayOf(0.53f, 0f, 0.57f, 0f)           // Top rect 2
                        ),
                        //10f, 60f, width, height, ppm, width + 20f, height/5f, speed
                        10f, 60f, width, height, ppm, world_width + 3f, height/5f, speed
                    ),
                    Building(
                        "building_16",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_16, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(137f), 1f, 0f),        // Bottom rect
                            arrayOf(0.13f, dp2px(120f), 0.87f, 0f),  // Middle rect 1
                            arrayOf(0.24f, dp2px(103f), 0.76f, 0f),  // Middle rect 2
                            arrayOf(0.28f, dp2px(84f), 0.49f, 0f),   // Middle rect 3
                            arrayOf(0.6f, dp2px(26f), 0.62f, 0f),    // Top rect 1
                            arrayOf(0.68f, dp2px(21f), 0.71f, 0f),   // Top rect 2
                            arrayOf(0.53f, 0f, 0.55f, 0f)               // Top rect 3
                        ),
                        //18f, 50f, width, height, ppm, width + 20f, height/5f, speed
                        18f, 50f, width, height, ppm, world_width + 3f, height/5f, speed
                    ),
                    Building(
                        "building_17",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_day_17, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(78f), 1f, 0f),        // Bottom rect
                            arrayOf(0.15f, dp2px(50f), 0.88f, 0f),  // Middle rect
                            arrayOf(0.41f, 0f, 0.63f, 0f)              // Top rect
                        ),
                        //25f, 60f, width, height, ppm, width + 20f, height/5f, speed
                        25f, 60f, width, height, ppm, world_width + 3f, height/5f, speed
                    )
                )
            }
            else {
                bg = ResourcesCompat.getDrawable(resources, R.drawable.city_bg_night, null)?.toBitmap(width, height)!!

                buildings = arrayOf(
                    Building(
                        "building_01",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_night_01, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(260f), 1f, 0f),       // Bottom rect
                            arrayOf(0.05f, dp2px(210f), 0.92f, 0f), // Middle rect 1
                            arrayOf(0.85f, dp2px(185f), 0.92f, 0f), // Middle rect 1
                            arrayOf(0.1f, dp2px(165f), 0.86f, 0f),  // Middle rect 2
                            arrayOf(0.78f, dp2px(145f), 0.86f, 0f), // Middle rect 2
                            arrayOf(0.18f, dp2px(135f), 0.78f, 0f), // Middle rect 3
                            arrayOf(0.2f, dp2px(118f), 0.75f, 0f),  // Top rect
                            arrayOf(0.25f, dp2px(100f), 0.7f, 0f),  // Top rect
                            arrayOf(0.3f, dp2px(90f), 0.65f, 0f),   // Top rect
                            arrayOf(0.35f, dp2px(82f), 0.6f, 0f)    // Top rect
                        ),
                        //15f, 100f, width, height, ppm, width + 20f, height*0.2f, speed
                        15f, 100f, width, height, ppm, world_width + 3f, height*0.2f, speed
                    ),
                    Building(
                        "building_02",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_night_02, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0.26f, dp2px(345f), 0.72f, 0f),  // Bottom rect
                            arrayOf(0.4f, dp2px(153f), 0.6f, 0f),    // Middle rect
                            arrayOf(0.45f, 0f, 0.54f, 0f)               // Top rect
                        ),
                        //10f, 90f, width, height, ppm, width + 20f, height*0.2f, speed
                        10f, 110f, width, height, ppm, world_width + 3f, height*0.2f, speed
                    ),
                    Building(
                        "building_03",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_night_03, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0.75f, dp2px(75f), 1f, 0f),    // Bottom rect
                            arrayOf(0f, dp2px(43f), 0.75f, 0f),    // Bottom rect
                            arrayOf(0.68f, dp2px(19f), 0.82f, 0f), // Top rect
                            arrayOf(0.16f, 0f, 0.68f, 0f)             // Top rect
                        ),
                        //25f, 110f, width, height, ppm, width + 20f, height*0.2f, speed
                        18f, 70f, width, height, ppm, world_width + 3f, height*0.2f, speed
                    ),
                    Building(
                        "building_04",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_night_04, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(112f), 0.24f, 0f),  // Bottom rect
                            arrayOf(0.24f, dp2px(85f), 1f, 0f),   // Bottom rect
                            arrayOf(0.16f, dp2px(18f), 0.3f, 0f), // Top rect
                            arrayOf(0.3f, 0f, 0.82f, 0f)             // Top rect
                        ),
                        //30f, 90f, width, height, ppm, width + 20f, height*0.2f, speed
                        20f, 70f, width, height, ppm, world_width + 3f, height*0.2f, speed
                    ),
                    Building(
                        "building_05",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_night_05, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(72f), 1f, 0f),      // Bottom rect
                            arrayOf(0f, dp2px(28f), 0.7f, 0f),    // Middle rect 1
                            arrayOf(0.45f, dp2px(10f), 0.6f, 0f), // Middle rect 2
                            arrayOf(0.7f, dp2px(40f), 0.8f, 0f), // Middle rect 3
                            arrayOf(0.8f, dp2px(55f), 0.9f, 0f), // Middle rect 4
                            arrayOf(0.13f, 0f, 0.45f, 0f)           // Top rect
                        ),
                        //35f, 60f, width, height, ppm, width + 20f, height*0.2f, speed
                        25f, 60f, width, height, ppm, world_width + 3f, height*0.2f, speed
                    ),
                    Building(
                        "building_06",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_night_06, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(150f), 1f, 0f),        // Bottom rect
                            arrayOf(0.05f, dp2px(122f), 0.95f, 0f),  // Middle rect 1
                            arrayOf(0.1f, dp2px(98f), 0.9f, 0f),     // Middle rect 2
                            arrayOf(0.15f, dp2px(80f), 0.86f, 0f),   // Middle rect 3
                            arrayOf(0.18f, dp2px(67f), 0.82f, 0f),   // Middle rect 4
                            arrayOf(0.23f, dp2px(57f), 0.76f, 0f),   // Middle rect 5
                            arrayOf(0.28f, dp2px(46f), 0.72f, 0f),   // Middle rect 6
                            arrayOf(0.33f, dp2px(36f), 0.68f, 0f),   // Middle rect 7
                            arrayOf(0.37f, dp2px(26f), 0.65f, 0f),   // Middle rect 8
                            arrayOf(0.41f, dp2px(16f), 0.6f, 0f),    // Middle rect 9
                            arrayOf(0.47f, 0f, 0.52f, 0f)               // Top rect
                        ),
                        //35f, 40f, width, height, ppm, width + 20f, height*0.2f, speed
                        25f, 100f, width, height, ppm, world_width + 3f, height*0.2f, speed // Measures in meters except pos_y
                    ),
                    Building(
                        "building_07",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_night_07, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(102f), 0.93f, 0f),    // Bottom rect
                            arrayOf(0.06f, dp2px(60f), 0.87f, 0f),  // Middle rect
                            arrayOf(0.19f, dp2px(10f), 0.74f, 0f),  // Middle rect
                            arrayOf(0.26f, 0f, 0.67f, 0f)              // Top rect
                        ),
                        //15f, 80f, width, height, ppm, width + 20f, height/5f, speed
                        //15f, 80f, width, height, ppm, width + 3f*ppm, height/5f, speed
                        18f, 80f, width, height, ppm, world_width + 3f, height*0.2f, speed // Measures in meters except pos_y
                    ),
                    Building(
                        "building_08",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_night_08, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(20f), 1f, 0f),      // Bottom rect
                            arrayOf(0.1f, dp2px(15f), 0.95f, 0f), // Middle rect 1
                            arrayOf(0.27f, dp2px(10f), 0.9f, 0f), // Middle rect 2
                            arrayOf(0.47f, dp2px(5f), 0.8f, 0f),  // Middle rect 3
                            arrayOf(0.65f, 0f, 0.75f, 0f)            // Top rect
                        ),
                        //10f, 70f, width, height, ppm, width + 20f, height*0.2f, speed
                        17f, 70f, width, height, ppm, world_width + 3f, height*0.2f, speed
                    ),
                    Building(
                        "building_09",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_night_09, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(22f), 1f, 0f),       // Bottom rect
                            arrayOf(0.08f, dp2px(15f), 0.93f, 0f), // Middle rect 1
                            arrayOf(0.15f, dp2px(8f), 0.87f, 0f),  // Middle rect 2
                            arrayOf(0.23f, 0f, 0.77f, 0f)             // Top rect
                        ),
                        //17f, 70f, width, height, ppm, width + 20f, height/5f, speed
                        25f, 70f, width, height, ppm, world_width + 3f, height/5f, speed
                    ),
                    Building(
                        "building_10",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_night_10, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0.25f, dp2px(29f), 1f, 0f),    // Bottom rect
                            arrayOf(0.33f, dp2px(20f), 0.87f, 0f)  // Top rect
                        ),
                        //10f, 65f, width, height, ppm, width + 20f, height/5f, speed
                        13f, 65f, width, height, ppm, world_width + 3f, height/5f, speed
                    ),
                    Building(
                        "building_11",
                        ResourcesCompat.getDrawable(resources, R.drawable.building_night_11, null)?.toBitmap(width, height)!!,
                        arrayOf(
                            arrayOf(0f, dp2px(203f), 1f, 0f),       // Bottom rect
                            arrayOf(0.1f, dp2px(182f), 0.9f, 0f),  // Middle rect
                            arrayOf(0.2f, dp2px(162f), 0.8f, 0f),  // Middle rect
                            arrayOf(0.3f, dp2px(140f), 0.7f, 0f),  // Middle rect
                            arrayOf(0.4f, dp2px(100f), 0.54f, 0f),  // Middle rect
                            arrayOf(0.46f, 0f, 0.5f, 0f)               // Top rect
                        ),
                        //17f, 55f, width, height, ppm, width + 20f, height/5f, speed
                        13f, 80f, width, height, ppm, world_width + 3f, height/5f, speed
                    )
                )
            }

            vehicles = arrayOf(
                Vehicle(
                    "biplane",
                    ResourcesCompat.getDrawable(resources, R.drawable.biplane, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    //7f, 3f, width, height, ppm, width + 420f, 20f, speed
                    //7f, 3f, width, height, ppm, width + 3f*ppm, ppm, speed
                    7f, 3f, width, height, ppm, world_width + 3f, height*0.05f, speed // Measures in meters except pos_y
                )/*,

                Vehicle(
                    "dirigible_1",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_1, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                ),
                Vehicle(
                    "dirigible_2",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_2, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                ),
                Vehicle(
                    "dirigible_3",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_3, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                ),
                Vehicle(
                    "dirigible_4",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_4, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                ),
                Vehicle(
                    "dirigible_5",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_5, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                ),
                Vehicle(
                    "dirigible_6",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_6, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                ),
                Vehicle(
                    "dirigible_7",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_7, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, height*0.05f, speed
                ),


                Vehicle(
                    "ufo",
                    ResourcesCompat.getDrawable(resources, R.drawable.ufo, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    4f, 2f, width, height, ppm, width + 420f, 20f, speed
                )*/
            )

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
                    7f, 3f, bitmaps[0].width, bitmaps[0].height, ppm, width + 20f, 20f, speed
                ),

                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_1, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[1].width, bitmaps[1].height, ppm, width + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_2, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[2].width, bitmaps[2].height, ppm, width + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_3, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[3].width, bitmaps[3].height, ppm, width + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_4, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[4].width, bitmaps[4].height, ppm, width + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_5, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[5].width, bitmaps[5].height, ppm, width + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_6, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[6].width, bitmaps[6].height, ppm, width + 20f + 20f, 20f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_7, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[7].width, bitmaps[7].height, ppm, width + 20f + 20f, 20f, speed
                ),


                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.ufo, null)?.toBitmap()!!,
                    4f, 2f, bitmaps[8].width, bitmaps[8].height, ppm, width + 20f + 20f, 20f, speed
                )
            )*/


            /*buildings = arrayOf(
                ResourcesCompat.getDrawable(resources, R.drawable.building_01, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_02, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_03, null)?.toBitmap(width/3, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_04, null)?.toBitmap(width/2, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_05, null)?.toBitmap((width/1.5).toInt(), height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_06, null)?.toBitmap((width/1.2).toInt(), height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_07, null)?.toBitmap((width/3.5).toInt(), height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_08, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_09, null)?.toBitmap(width/3, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_10, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_11, null)?.toBitmap((width/2.5).toInt(), height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_12, null)?.toBitmap(width/3, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_13, null)?.toBitmap((width/2.5).toInt(), height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_14, null)?.toBitmap(width/2, height)!!

                /*ResourcesCompat.getDrawable(resources, R.drawable.building_15, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_16, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_17, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_18, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_19, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_20, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_21, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_22, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_23, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_24, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_25, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_26, null)?.toBitmap(width/4, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.building_27, null)?.toBitmap(width/4, height)!!*/
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
                    15f, 50f, bitmaps[0].width, bitmaps[0].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_02, null)?.toBitmap()!!,
                    10f, 60f, bitmaps[1].width, bitmaps[1].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_03, null)?.toBitmap()!!,
                    15f, 50f, bitmaps[2].width, bitmaps[2].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_04, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[3].width, bitmaps[3].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_05, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[4].width, bitmaps[4].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_06, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[5].width, bitmaps[5].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_07, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[6].width, bitmaps[6].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_08, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[7].width, bitmaps[7].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_09, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[8].width, bitmaps[8].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_10, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[9].width, bitmaps[9].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_11, null)?.toBitmap()!!,
                    15f, 50f, bitmaps[10].width, bitmaps[10].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_12, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[11].width, bitmaps[11].height, ppm, width + 20f + 20f, height/5f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_13, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[12].width, bitmaps[12].height, ppm, width + 20f + 20f, height/2f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_14, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[13].width, bitmaps[13].height, ppm, width + 20f + 20f, height/2f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_15, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[14].width, bitmaps[14].height, ppm, width + 20f + 20f, height/2f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_16, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[15].width, bitmaps[15].height, ppm, width + 20f + 20f, height/2f, speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_17, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[16].width, bitmaps[16].height, ppm, width + 20f + 20f, height/2f, speed
                )
            )*/



            // Convert the user's helicopter images for the animation and store them in an array
            /*heli_animation = arrayOf(
                ResourcesCompat.getDrawable(resources, R.drawable.heli_1_1, null)?.toBitmap(width, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.heli_1_2, null)?.toBitmap(width, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.heli_1_3, null)?.toBitmap(width, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.heli_1_4, null)?.toBitmap(width, height)!!
            )*/

            heli_animation = arrayOf(
                ResourcesCompat.getDrawable(resources, R.drawable.heli_green_0, null)?.toBitmap(width, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.heli_green_1, null)?.toBitmap(width, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.heli_green_2, null)?.toBitmap(width, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.heli_green_3, null)?.toBitmap(width, height)!!
            )

            // Init the helicopter bitmap with the first bitmap in the animation array
            heli = heli_animation[0]

            // USO SOLO ppm E NON ANCHE ppm_y PERCHÈ ALTRIMENTI DISTORCE L'IMMAGINE
            // From heli.width * heli_scale = heli_width * ppm
            heli_scale_x = heli_width * ppm / heli.width
            heli_scale_y = heli_height * ppm / heli.height
            heli_width_scaled  = heli.width * heli_scale_x
            heli_height_scaled = heli.height * heli_scale_y


            // PER L'UTENTE SERVIREBBE UNA CLASSE A PARTE PERCHÈ IN QUESTO MOMENTO VEHICLE È UNA SOTTOCLASSE DI OBSTACLE.
            // POTREI NON FARE 2 CLASSI SE IMPOSTO LA VELOCITÀ A ZERO.
            // SOLO IL VEICOLO DELL'UTENTE HA UN'ANIMAZIONE IN QUESTO MOMENTO E QUINDI NELLA CLASSE NON È PREVISTA
            /*Vehicle(
                "user",
                ResourcesCompat.getDrawable(resources, R.drawable.heli_green_0, null)?.toBitmap(width, height)!!,
                arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                8f, 3f, width, height, ppm, 1f, height*0.4f, 0f // Measures in meters except pos_y
            )*/


            /*radius *= ppm
            speed  *= ppm
            g      *= ppm*/


            /*Log.d("ppm", "ppm: " + ppm)
            Log.d("ppm", "heli_scale: " + heli_scale)
            Log.d("ppm", "cannon width in px: " + cannon.width*heli_scale)
            Log.d("ppm", "cannon height in px: " + cannon.height*heli_scale)
            Log.d("ppm", "radius: " + radius)
            Log.d("ppm", "speed: " + speed)
            Log.d("ppm", "gravity: " + g)*/


            // Set translation values
            //user_dx = 30f
            user_dy = height * 0.4f

            // Set translation values converting from meters
            user_dx *= ppm
            //user_dy *= ppm_y

            pickBuilding()
            pickVehicle()
        }


        // Compute time difference
        val now = System.currentTimeMillis()
        val dt: Float

        // Check if this is the first call to onDraw (i.e. first frame)
        //if (previous == 0L)  dt = 0f
        //else                 dt = (now - previous) / 1000f
        if (previous == 0L)  dt = 1f / fps
        else                 dt = min( (now - previous) / 1000f, 1f / fps )//*/

        //dt = 1f / fps

        Log.d("TIME", "************")
        Log.d("TIME", "previous: " + previous)
        Log.d("TIME", "now: " + now)
        Log.d("TIME", "dt: " + dt)
        Log.d("TIME", "speed (m/s): " + speed)
        Log.d("TIME", "speed (px/s): " + speed*ppm*dt)

        previous = now//*/


        val t1 = System.currentTimeMillis()


        // Draw background
        canvas?.drawBitmap(bg, 0f, 0f, null)


        // Helicopter animation frame
        heli = heli_animation[heli_animation_index]
        heli_animation_index = (heli_animation_index + 1) % heli_animation.size

        /*Log.d("COORD", "user_dy: " + user_dy)
        Log.d("COORD", "user_dx: " + user_dx)
        Log.d("COORD", "heli.width: " + heli.width)
        Log.d("COORD", "heli.height: " + heli.height)
        Log.d("COORD", "width: " + width)
        Log.d("COORD", "height: " + height)*/


        /*Log.d("COORD", "heli.height: " + heli.height)
        Log.d("COORD", "heli.height * heli_scale_y: " + heli.height * heli_scale_y)
        Log.d("COORD", "heli_height: " + heli_height)*/

        //user_dy += last_gyroscope_input

        // It's correct use ppm_y instead of ppm because if width is much larger than height
        // 1 meter conversion in the width can be out of scale for the height
        //user_dy += last_gyroscope_input * ppm_y

        // É CORRETTO COSÌ DAL MOMENTO CHE PER CALCOLARE ppm USO IL MIN TRA LE 2 DIMENSIONI
        user_dy += last_gyroscope_input * ppm

        // Constrain the bitmap in the screen
        if (user_dy < 0)
            user_dy = 0f
        else if (user_dy + heli_height_scaled > height)
            user_dy = height.toFloat() - heli_height_scaled

        // Constrain the rotation of the bitmap
        if (last_gyroscope_input > 0 && heli_rotation <= 10f)
            heli_rotation += 0.5f
        else if (last_gyroscope_input < 0 && heli_rotation >= -8f)
            heli_rotation -= 0.5f

        // Scale the bitmap
        heli_matrix.setScale(heli_scale_x, heli_scale_y)

        // Rotate the bitmap
        heli_matrix.postRotate(heli_rotation)

        // Put the bitmap on the left-center of the screen
        heli_matrix.postTranslate(user_dx, user_dy)

        canvas?.drawBitmap(heli, heli_matrix, null)

        // Draw rotated rectangle
        /*canvas?.save()
        canvas?.rotate(heli_rotation, user_dx, user_dy)
        canvas?.drawRect(user_dx, user_dy, user_dx + heli_width_scaled, user_dy + heli_height_scaled, painter_stroke)
        canvas?.restore()*/


        user_rect.set( user_dx, user_dy, user_dx + heli_width_scaled, user_dy + heli_height_scaled )

        // VARI TENTATIVI PER RUOTARE IL RECT DELLA BITMAP DELL'UTENTE SECONDO L'INCLINAZIONE DELLA BITMAP
        // FINORA NESSUNA SOLUZIONE
        //user_rect_matrix.postRotate(heli_rotation, user_dx, user_dy)
        //user_rect_matrix.mapRect(user_rect)

        //original image coords
        /*val points = floatArrayOf(
            user_dx, user_dy,                                          // left, top
            user_dx + heli_width_scaled, user_dy,                      // right, top
            user_dx + heli_width_scaled, user_dy + heli_height_scaled, // right, bottom
            user_dx, user_dy + heli_height_scaled                      // left, bottom
        )

        val matrix = Matrix()
        matrix.postRotate(heli_rotation, user_dx, user_dy)

        //get the new edges coords
        matrix.mapPoints(points)

        //user_rect.set(points[0], points[1], points[4], points[5])
        user_rect.set(points[0], points[3], points[4], points[5])*/


        //user_rect.set( user_dx, user_dy, user_dx + heli_width_scaled, user_dy + heli_height_scaled )
        //user_rect.set( user_dx, user_dy + heli_rotation*2.8f, user_dx + heli_width_scaled, user_dy + heli_height_scaled + heli_rotation*2.8f )

        /*if (heli_rotation >= 0f)
            user_rect.set( user_dx, user_dy, user_dx + heli_width_scaled, user_dy + heli_height_scaled )
        else
            user_rect.set( user_dx, user_dy + heli_rotation*2.8f, user_dx + heli_width_scaled, user_dy + heli_height_scaled + heli_rotation*2.8f )*/

        // NON FUNZIONANO BENE QUESTE SOTTO
        // With rotation matrix only for y
        /*user_rect.set(
            user_dx,
            user_dx * sin(heli_rotation) + user_dy * cos(heli_rotation),
            user_dx + heli_width_scaled,
            (user_dx + heli_width_scaled) * sin(heli_rotation) + (user_dy + heli_height_scaled) * cos(heli_rotation)
        )*/

        //val rad = ((heli_rotation * Math.PI) / 180f).toFloat()

        // With rotation matrix only for y
        /*user_rect.set(
            user_dx,
            user_dx * sin(rad) + user_dy * cos(rad),
            user_dx + heli_width_scaled,
            (user_dx + heli_width_scaled) * sin(rad) + (user_dy + heli_height_scaled) * cos(rad)
        )*/

        // With rotation matrix
        /*user_rect.set(
            user_dx * cos(rad) - user_dy * sin(rad),
            user_dx * sin(rad) + user_dy * cos(rad),
            (user_dx + heli_width_scaled) * cos(rad) - (user_dy + heli_height_scaled) * sin(rad),
            (user_dx + heli_width_scaled) * sin(rad) + (user_dy + heli_height_scaled) * cos(rad)
        )*/

        canvas?.drawRect(user_rect, painter_stroke)


        // Scale the bitmap
        //dirigible_matrix.setScale(dirigible_scale, dirigible_scale)
        /*dirigible_matrix.setScale(dirigible_scale_x, dirigible_scale_y)

        // Put the bitmap on the left-center of the screen
        dirigible_matrix.postTranslate(dirigible_dx, dirigible_dy)

        canvas?.drawBitmap(dirigible, dirigible_matrix, null)
        canvas?.drawRect(dirigible_dx, dirigible_dy, dirigible_dx + dirigible_width_scaled, dirigible_dy + dirigible_height_scaled, painter_stroke)

        dirigible_dx -= 5f*/


        /*Log.d("COORD", "dirigible.width: " + dirigible.width)
        Log.d("COORD", "dirigible_width_scaled: " + dirigible_width_scaled)
        Log.d("COORD", "dirigible_width: " + dirigible_width)*/



        /*if (dirigible_dx + dirigible_width_scaled < 0)
            dirigible_dx = width + dirigible_width_scaled*/


        // Scale the bitmap
        /*ufo_matrix.setScale(ufo_scale_x, ufo_scale_y)

        // Put the bitmap on the left-center of the screen
        ufo_matrix.postTranslate(ufo_dx, ufo_dy)

        canvas?.drawBitmap(ufo, ufo_matrix, null)
        canvas?.drawRect(ufo_dx, ufo_dy, ufo_dx + ufo_width_scaled, ufo_dy + ufo_height_scaled, painter_stroke)

        ufo_dx -= 10f

        if (ufo_dx + ufo_width_scaled < 0)
            ufo_dx = width + ufo_width_scaled*/





        // AGGIORNAMENTO CON VEHICLE UPDATE
        vehicle.update(dt)
        canvas?.drawBitmap(vehicle.getBitmap(), vehicle.getMatrix(), null)
        /*canvas?.drawRect(
            vehicle!!.getX(),
            vehicle!!.getY(),
            vehicle!!.getX() + vehicle!!.getBitmapScaledWidth(),
            vehicle!!.getY() + vehicle!!.getBitmapScaledHeight(),
            painter_stroke)*/

        val bound = vehicle.getBounds()
        for (rect in bound)
            canvas?.drawRect(rect, painter_stroke)

        // Check if the vehicle has been respawn
        if (vehicle.isRespawn())
            pickVehicle()






        /*canvas?.drawBitmap(building, building_dx, building_dy, null)
        canvas?.drawRect(building_dx, building_dy, building_dx + building.width, building.height.toFloat(), painter_stroke)

        //building_dx -= 5f
        building_dx = 200f

        if (building_dx + building.width < 0)
            pickBuilding()*/


        // AGGIORNAMENTO CON VEHICLE UPDATE
        building.update(dt)
        //building.setX(10f)
        canvas?.drawBitmap(building.getBitmap(), building.getMatrix(), null)

        // Debug (draw building bounds)
        val bounds = building.getBounds()
        for (rect in bounds)
            canvas?.drawRect(rect, painter_stroke)

        // Check if the building has been respawn
        if (building.isRespawn())
            pickBuilding()

        // Check if a collision has been occurred
        if (checkCollision()) {
            gameEnd = true

            val fragment = findFragment<GameFragment>()
            fragment.gameEnd(score.toLong(), false)
        }


        // Update the score
        // s = v * dt  in m/s
        score += speed * dt

        // Since the score is updated to slowly using m/s,
        // I speeded it up
        //score += speed * 6 * dt

        canvas?.drawText("SCORE " + score.toLong(), 20f, 60f, painter_fill)

        // Update the speed
        speed += 0.003f // max about 36-43 m/s. 50 m/s very difficult!


        // Compute time to wait to be consistent with the target frame time
        //val waitTime = targetFrameTime - (dt * 1000).toLong()
        val t2 = System.currentTimeMillis()
        val waitTime = targetFrameTime - (t2 - t1)

        /*Log.d("TIME", "************")
        Log.d("TIME", "t1: " + t1)
        Log.d("TIME", "t2: " + t2)
        Log.d("TIME", "dt: " + dt2)
        Log.d("TIME", "speed (m/s): " + speed)
        Log.d("TIME", "speed (px/s): " + speed*ppm*dt)*/
        Log.d("TIME", "waitTime: " + waitTime)

        try {
            sleep(waitTime)
        } catch (e: Exception) {
            e.printStackTrace()
        }//*/

        // Draw the next frame
        invalidate()
        //*/







        // VERSIONE USANDO COROUTINES

        // The issue is that the event loop that calls onDraw all runs on the same thread so this wouldn't really make any significant difference in performance.
        // Quindi dovrei lasciare qui solo le draw sulla canvas e spostare la logica in GameFragment

        // Dispatchers.Default — is used by all standard builders if no dispatcher or any other ContinuationInterceptor is specified in their context.
        // It uses a common pool of shared background threads.
        // This is an appropriate choice for compute-intensive coroutines that consume CPU resources
        /*GlobalScope.launch(Dispatchers.Default) {
            // Compute time difference
            val now = System.currentTimeMillis()
            val dt: Float

            // Check if this is the first call to onDraw (i.e. first frame)
            if (previous == 0L)  dt = 0f
            else                 dt = (now - previous) / 1000f

            Log.d("TIME", "************")
            Log.d("TIME", "previous: $previous")
            Log.d("TIME", "now: $now")
            Log.d("TIME", "dt: $dt")
            Log.d("TIME", "speed (m/s): $speed")
            Log.d("TIME", "speed (px/s): " + speed*ppm*dt)

            previous = now

            val updateHeli = async {
                // Helicopter animation frame
                heli = heli_animation[heli_animation_index]
                heli_animation_index = (heli_animation_index + 1) % heli_animation.size

                user_dy += last_gyroscope_input * ppm

                // Constrain the bitmap in the screen
                if (user_dy < 0)
                    user_dy = 0f
                else if (user_dy + heli_height_scaled > height)
                    user_dy = height.toFloat() - heli_height_scaled

                // Constrain the rotation of the bitmap
                if (last_gyroscope_input > 0 && heli_rotation <= 10f)
                    heli_rotation += 0.5f
                else if (last_gyroscope_input < 0 && heli_rotation >= -8f)
                    heli_rotation -= 0.5f

                // Scale the bitmap
                heli_matrix.setScale(heli_scale_x, heli_scale_y)

                // Rotate the bitmap
                heli_matrix.postRotate(heli_rotation)

                // Put the bitmap on the left-center of the screen
                heli_matrix.postTranslate(user_dx, user_dy)

                user_rect.set( user_dx, user_dy, user_dx + heli_width_scaled, user_dy + heli_height_scaled )
            }
            val updateVehicle  = async {
                vehicle.update(dt)

                // Check if the vehicle has been respawn
                if (vehicle.isRespawn())
                    pickVehicle()
            }
            val updateBuilding = async {
                building.update(dt)

                // Check if the building has been respawn
                if (building.isRespawn())
                    pickBuilding()
            }

            updateHeli.await()
            updateVehicle.await()
            updateBuilding.await()

            val finalUpdate = async {
                // Check if a collision has been occurred
                if (checkCollision()) {
                    gameEnd = true

                    val fragment = findFragment<GameFragment>()
                    fragment.gameEnd(score.toLong(), false)
                }

                // Update the score
                // s = v * dt  in m/s
                score += speed * dt

                // Update the speed
                speed += 0.003f // max about 36-43 m/s. 50 m/s very difficult!
            }

            // Rendering
            // crasha
            /*withContext(Dispatchers.Main) {
                // Draw background
                canvas?.drawBitmap(bg, 0f, 0f, null)

                canvas?.drawBitmap(heli, heli_matrix, null)
                canvas?.drawBitmap(vehicle.getBitmap(), vehicle.getMatrix(), null)
                canvas?.drawBitmap(building.getBitmap(), building.getMatrix(), null)
            }*/

            val fragment = findFragment<GameFragment>()
            fragment.activity?.runOnUiThread(Runnable {
                // Draw background
                canvas?.drawBitmap(bg, 0f, 0f, null)

                canvas?.drawBitmap(heli, heli_matrix, null)
                canvas?.drawBitmap(vehicle.getBitmap(), vehicle.getMatrix(), null)
                canvas?.drawBitmap(building.getBitmap(), building.getMatrix(), null)

                canvas?.drawText("SCORE " + score.toLong(), 20f, 60f, painter_fill)

                invalidate()
            })

            finalUpdate.await()

            // Draw the next frame
            invalidate()
        }*/









        // Check if the cannon has fired
        /*if (!fired)     return

        // Check if the cannon ball is outside the screen
        if (cx - radius > width || cy - radius > height) {
            fired = false
            return
        }

        // Compute time difference
        val now = System.currentTimeMillis()
        val dt = now - previous
        previous = now

        // Calculate new ball center
        // Greater values at right
        cx += vx * dt / 1000f

        // Greater values at bottom
        //cy += 0.5f * g * dt/1000f * dt/1000f - vy * dt / 1000f
        cy -= vy * dt / 1000f

        // Update Y velocity
        vy -= g * dt / 1000f

        //Log.d("ball", "cx: " + cx + "\tcy: " + cy)

        // Set translation to the matrix for the shader
        shaderMatrix.setTranslate(cx - 0.3f*radius, cy - 0.3f*radius)
        ballPainter.shader.setLocalMatrix(shaderMatrix)

        canvas?.drawCircle(cx, cy, radius, ballPainter)
        invalidate()*/
    }

    // Convert density independent pixels into screen pixels
    private fun dp2px(dp: Float): Float {
        return resources.displayMetrics.density * dp
    }

    // Pick a random building and set a random height for it
    private fun pickBuilding() {
        building = buildings.random()

        // Height range height*0.2 .. height*0.76 = from 20% to 76% of the screen height
        // Remember that 0% = top and 100% = bottom
        val r = (20..76).random() / 100f

        // For debug
        //val r = 0.2f
        //building.setX(width*0.35f)
        //building.setX(width*0.1f)


        building.setY(height*r)

        // Increase the distance between obstacles if the height of
        // the building is greater than a threshold
        //if (r > 3.5f && building.getX() - vehicle.getX() < heli_width_scaled * 2f)
        //    building.setX(width + vehicle.getBitmapScaledWidth() + heli_width_scaled * 2f)

        // Update speed
        building.setSpeed(speed)
    }

    // Pick a random vehicle and set a random height for it
    private fun pickVehicle() {
        vehicle = vehicles.random()

        // Height range height*0 .. height*0.2 = from 0% to 20% of the screen height
        // Remember that 0% = top and 100% = bottom
        val r = (0..20).random() / 100f
        vehicle.setY(height*r)

        // Increase the distance between obstacles if the height of
        // the building is greater than a threshold
        if (building.getY() > height/3.5f && vehicle.getX() - building.getX() < heli_width_scaled * 2f)
            vehicle.setX(width + building.getBitmapScaledWidth() + heli_width_scaled * 2f)

        // Update speed
        vehicle.setSpeed(speed)
    }

    // Check if the user collides with an obstacle
    private fun checkCollision(): Boolean { return false
        //user_rect.set( user_dx, user_dy, user_dx + heli_width_scaled, user_dy + heli_height_scaled )

        val rects = building.getBounds()
        rects.addAll(vehicle.getBounds())

        for (r in rects)
            if (RectF.intersects(user_rect, r))
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

    // Detect user motion via sensors
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onFlushCompleted(sensor: Sensor?) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            //Sensor.TYPE_ACCELEROMETER  -> lastAcceleration = event.values.clone()
            //Sensor.TYPE_MAGNETIC_FIELD      -> lastGyroscope    = event.values.clone()
            Sensor.TYPE_GYROSCOPE      -> lastGyroscope    = event.values.clone()
        }


        if (lastGyroscope[0] > 0)
        //last_gyroscope_input = Math.max(1f, Math.min(lastGyroscope[0] * 15, 15f))
            last_gyroscope_input = max(1f, min(lastGyroscope[0] * 1, 15f))
        else if (lastGyroscope[0] < 0)
        //last_gyroscope_input = Math.min(-1f, Math.max(lastGyroscope[0] * 15, -15f))
            last_gyroscope_input = min(-1f, max(lastGyroscope[0] * 1, -15f))

        /*Log.d("SENSOR", "****************")
        Log.d("SENSOR", "value: " + lastGyroscope[0])
        Log.d("SENSOR", "last_gyroscope_input: " + last_gyroscope_input)*/

        invalidate()
    }
}*/