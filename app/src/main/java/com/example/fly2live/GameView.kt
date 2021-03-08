package com.example.fly2live

import android.content.Context
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.findFragment
import com.example.fly2live.vehicle.Vehicle
import com.example.fly2live.building.Building


class GameView(context: Context?) : View(context), View.OnTouchListener, SensorEventListener2 {
    // Bitmaps
    private var converted = false

    // Helicopter animation
    private lateinit var heli_animation: Array<Bitmap>
    private lateinit var heli: Bitmap
    private var heli_animation_index = 0

    // Other bitmaps
    private lateinit var bg: Bitmap
    //private lateinit var buildings: Array<Bitmap>
    //private lateinit var building: Bitmap
    //private lateinit var dirigible: Bitmap
    //private lateinit var ufo: Bitmap


    private lateinit var buildings: Array<Building>
    private lateinit var building: Building
    private lateinit var vehicles: Array<Vehicle>
    private var vehicle: Vehicle? = null


    // Helicopter size
    private val heli_width  = 8f // meters
    private val heli_height = 3f // meters
    private var heli_scale_x = 0f
    private var heli_scale_y = 0f
    private var heli_width_scaled  = 0f
    private var heli_height_scaled = 0f

    // Dirigible size
    /*private val dirigible_width  = 20f // meters
    private val dirigible_height = 8f // meters
    private var dirigible_scale_x = 0f
    private var dirigible_scale_y = 0f
    private var dirigible_width_scaled  = 0f
    private var dirigible_height_scaled = 0f

    // Ufo size
    private val ufo_width  = 4f // meters
    private val ufo_height = 2f // meters
    private var ufo_scale_x = 0f
    private var ufo_scale_y = 0f
    private var ufo_width_scaled  = 0f
    private var ufo_height_scaled = 0f*/


    // Graphics bitmaps matrix
    private var heli_matrix      = Matrix()
    /*private var dirigible_matrix = Matrix()
    private var ufo_matrix       = Matrix()*/


    // Helicopter position
    private var user_dx = 0f
    private var user_dy = 0f

    // Dirigible position
    /*private var dirigible_dx = 0f
    private var dirigible_dy = 0f

    // Ufo position
    private var ufo_dx = 0f
    private var ufo_dy = 0f*/

    //private var building_dx = 0f
    //private var building_dy = 0f


    // Bitmap rect
    private val user_rect = RectF()


    // Sensors values
    private var lastAcceleration = FloatArray(3)
    private var lastGyroscope    = FloatArray(3)


    // TEMP
    private val painter_fill = Paint().apply {
        style       = Paint.Style.FILL
        color       = Color.BLACK
        textSize    = 50f
    }

    private val painter_stroke = Paint().apply {
        style       = Paint.Style.STROKE
        color       = Color.BLACK
        strokeWidth = 3f
    }

    private var score     = 0.0
    private var game_over = false

    private val initial_speed = 5f

    private var increment = 0f





    // Cannon ball graphics variables
    private var radius = 0.18f // meters
    private var cx = 0f
    private var cy = 0f

    private val ballPainter = Paint().apply {
        shader = RadialGradient(0f, 0f, radius, Color.WHITE, Color.BLACK, Shader.TileMode.CLAMP)
    }

    // Graphics shader matrix
    private var shaderMatrix = Matrix()

    // Variables for handling the shooting
    private var fired = false
    private var previous = 0L

    // Cannon ball velocity components
    private var vx = 0f
    private var vy = 0f

    // World constants
    private var speed = 15f   // m/s
    private var g     = 9.81f // m/s^2
    private val l     = 30    // World length in meters

    // Pixel per meter
    private var ppm = 1

    // Finger coordinates
    private var fx = 0.0
    private var fy = 0.0




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

        if (game_over)
            return

        // Convert the image once
        if (!converted) {
            converted = true

            ppm = Math.min(width / l, height / l)

            bg        = ResourcesCompat.getDrawable(resources, R.drawable.city_bg_1, null)?.toBitmap(width, height)!!

            //dirigible = ResourcesCompat.getDrawable(resources, R.drawable.dirigible_1, null)?.toBitmap(width, height)!!
            //ufo       = ResourcesCompat.getDrawable(resources, R.drawable.ufo, null)?.toBitmap(width, height)!!


            vehicles = arrayOf(
                Vehicle(
                    "biplane",
                    ResourcesCompat.getDrawable(resources, R.drawable.biplane, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    7f, 3f, width, height, ppm, width + 420f, 20f, initial_speed
                ),

                Vehicle(
                    "dirigible_1",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_1, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, 20f, initial_speed
                ),
                Vehicle(
                    "dirigible_2",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_2, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, 20f, initial_speed
                ),
                Vehicle(
                    "dirigible_3",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_3, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, 20f, initial_speed
                ),
                Vehicle(
                    "dirigible_4",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_4, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, 20f, initial_speed
                ),
                Vehicle(
                    "dirigible_5",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_5, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, 20f, initial_speed
                ),
                Vehicle(
                    "dirigible_6",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_6, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, 20f, initial_speed
                ),
                Vehicle(
                    "dirigible_7",
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_7, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    20f, 8f, width, height, ppm, width + 420f, 20f, initial_speed
                ),


                Vehicle(
                    "ufo",
                    ResourcesCompat.getDrawable(resources, R.drawable.ufo, null)?.toBitmap(width, height)!!,
                    arrayOf(arrayOf(0f, 0f, 1f, 1f)),
                    4f, 2f, width, height, ppm, width + 420f, 20f, initial_speed
                )
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
                    7f, 3f, bitmaps[0].width, bitmaps[0].height, ppm, width + 20f, 20f, initial_speed
                ),

                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_1, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[1].width, bitmaps[1].height, ppm, width + 20f + 20f, 20f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_2, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[2].width, bitmaps[2].height, ppm, width + 20f + 20f, 20f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_3, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[3].width, bitmaps[3].height, ppm, width + 20f + 20f, 20f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_4, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[4].width, bitmaps[4].height, ppm, width + 20f + 20f, 20f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_5, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[5].width, bitmaps[5].height, ppm, width + 20f + 20f, 20f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_6, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[6].width, bitmaps[6].height, ppm, width + 20f + 20f, 20f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.dirigible_7, null)?.toBitmap()!!,
                    20f, 8f, bitmaps[7].width, bitmaps[7].height, ppm, width + 20f + 20f, 20f, initial_speed
                ),


                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.ufo, null)?.toBitmap()!!,
                    4f, 2f, bitmaps[8].width, bitmaps[8].height, ppm, width + 20f + 20f, 20f, initial_speed
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


            buildings = arrayOf(
                /*Building(
                    "building_01",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_01, null)?.toBitmap(width, height)!!,
                    arrayOf(
                        arrayOf(0f, dp2px(64f), 1f, 0f),       // Bottom rect
                        arrayOf(0.07f, dp2px(43f), 0.38f, 0f), // Middle left rect
                        arrayOf(0.47f, dp2px(44f), 0.8f, 0f),  // Middle right rect
                        arrayOf(0.1f, 0f, 0.18f, 0f),          // Top left rect
                        arrayOf(0.47f, dp2px(32f), 0.63f, 0f)  // Top right rect
                    ),
                    15f, 100f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_02",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_02, null)?.toBitmap(width, height)!!,
                    arrayOf(
                        arrayOf(0.22f, dp2px(450f), 0.81f, 0f),  // Bottom rect
                        arrayOf(0.33f, dp2px(232f), 0.7f, 0f),   // Middle rect
                        arrayOf(0.47f, 0f, 0.56f, 0f)            // Top rect
                    ),
                    10f, 90f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_03",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_03, null)?.toBitmap(width, height)!!,
                    arrayOf(
                        arrayOf(0f, dp2px(230f), 1f, 0f),       // Bottom rect
                        arrayOf(0.14f, dp2px(160f), 0.87f, 0f), // Middle rect
                        arrayOf(0.29f, dp2px(70f), 0.68f, 0f),  // Middle rect
                        arrayOf(0.45f, 0f, 0.6f, 0f)            // Top rect
                    ),
                    25f, 110f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_04",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_04, null)?.toBitmap(width, height)!!,
                    arrayOf(
                        arrayOf(0.14f, dp2px(348f), 0.87f, 0f), // Bottom rect
                        arrayOf(0.21f, dp2px(212f), 0.79f, 0f), // Middle rect
                        arrayOf(0.24f, dp2px(88f), 0.76f, 0f),  // Middle rect
                        arrayOf(0.32f, 0f, 0.68f, 0f)           // Top rect
                    ),
                    30f, 90f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_05",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_05, null)?.toBitmap(width, height)!!,
                    arrayOf(
                        arrayOf(0f, dp2px(154f), 1f, 0f),       // Bottom rect
                        arrayOf(0f, dp2px(74f), 0.24f, 0f),     // Middle left rect
                        arrayOf(0.27f, dp2px(112f), 0.75f, 0f), // Middle center rect
                        arrayOf(0.78f, dp2px(112f), 0.9f, 0f),  // Middle right rect
                        arrayOf(0.34f, 0f, 0.75f, 0f)           // Top rect
                    ),
                    35f, 60f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_06",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_06, null)?.toBitmap(width, height)!!,
                    arrayOf(
                        arrayOf(0f, dp2px(301f), 1f, 0f),       // Bottom rect
                        arrayOf(0f, dp2px(75f), 0.37f, 0f),     // Middle left rect
                        arrayOf(0.53f, dp2px(175f), 0.87f, 0f), // Middle right rect
                        arrayOf(0.07f, 0f, 0.29f, 0f)           // Top rect
                    ),
                    35f, 40f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),*/
                Building(
                    "building_07",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_07, null)?.toBitmap(width, height)!!,
                    arrayOf(
                        arrayOf(0f, dp2px(319f), 1f, 0f),       // Bottom rect
                        arrayOf(0f, dp2px(164f), 0.91f, 0f),    // Middle rect
                        arrayOf(0f, dp2px(140f), 0.22f, 0f),    // Left "triangle"
                        arrayOf(0.7f, dp2px(140f), 0.91f, 0f),    // Right "triangle"
                        arrayOf(0.31f, 0f, 0.4f, 0f)              // Top rect
                    ),
                    15f, 80f, width, height, ppm, width + 20f, height/5f, initial_speed
                )/*,
                Building(
                    "building_08",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_08, null)?.toBitmap(width, height)!!,
                    10f, 70f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_09",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_09, null)?.toBitmap(width, height)!!,
                    17f, 70f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_10",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_10, null)?.toBitmap(width, height)!!,
                    10f, 65f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_11",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_11, null)?.toBitmap(width, height)!!,
                    17f, 55f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_12",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_12, null)?.toBitmap(width, height)!!,
                    15f, 40f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_13",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_13, null)?.toBitmap(width, height)!!,
                    15f, 50f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_14",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_14, null)?.toBitmap(width, height)!!,
                    23f, 70f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_15",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_15, null)?.toBitmap(width, height)!!,
                    10f, 60f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_16",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_16, null)?.toBitmap(width, height)!!,
                    18f, 50f, width, height, ppm, width + 20f, height/5f, initial_speed
                ),
                Building(
                    "building_17",
                    ResourcesCompat.getDrawable(resources, R.drawable.building_17, null)?.toBitmap(width, height)!!,
                    25f, 60f, width, height, ppm, width + 20f, height/5f, initial_speed
                )*/
            )



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
                    15f, 50f, bitmaps[0].width, bitmaps[0].height, ppm, width + 20f + 20f, height/5f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_02, null)?.toBitmap()!!,
                    10f, 60f, bitmaps[1].width, bitmaps[1].height, ppm, width + 20f + 20f, height/5f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_03, null)?.toBitmap()!!,
                    15f, 50f, bitmaps[2].width, bitmaps[2].height, ppm, width + 20f + 20f, height/5f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_04, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[3].width, bitmaps[3].height, ppm, width + 20f + 20f, height/5f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_05, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[4].width, bitmaps[4].height, ppm, width + 20f + 20f, height/5f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_06, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[5].width, bitmaps[5].height, ppm, width + 20f + 20f, height/5f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_07, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[6].width, bitmaps[6].height, ppm, width + 20f + 20f, height/5f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_08, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[7].width, bitmaps[7].height, ppm, width + 20f + 20f, height/5f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_09, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[8].width, bitmaps[8].height, ppm, width + 20f + 20f, height/5f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_10, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[9].width, bitmaps[9].height, ppm, width + 20f + 20f, height/5f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_11, null)?.toBitmap()!!,
                    15f, 50f, bitmaps[10].width, bitmaps[10].height, ppm, width + 20f + 20f, height/5f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_12, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[11].width, bitmaps[11].height, ppm, width + 20f + 20f, height/5f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_13, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[12].width, bitmaps[12].height, ppm, width + 20f + 20f, height/2f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_14, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[13].width, bitmaps[13].height, ppm, width + 20f + 20f, height/2f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_15, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[14].width, bitmaps[14].height, ppm, width + 20f + 20f, height/2f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_16, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[15].width, bitmaps[15].height, ppm, width + 20f + 20f, height/2f, initial_speed
                ),
                Vehicle(
                    ResourcesCompat.getDrawable(resources, R.drawable.building_17, null)?.toBitmap()!!,
                    15f, 30f, bitmaps[16].width, bitmaps[16].height, ppm, width + 20f + 20f, height/2f, initial_speed
                )
            )*/




            heli_animation = arrayOf(
                ResourcesCompat.getDrawable(resources, R.drawable.heli_1_1, null)?.toBitmap(width, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.heli_1_2, null)?.toBitmap(width, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.heli_1_3, null)?.toBitmap(width, height)!!,
                ResourcesCompat.getDrawable(resources, R.drawable.heli_1_4, null)?.toBitmap(width, height)!!
            )


            heli = heli_animation[0]

            // From heli.width * heli_scale = heli_width * ppm
            heli_scale_x      = heli_width * ppm / heli.width
            heli_scale_y      = heli_height * ppm / heli.height
            heli_width_scaled  = heli.width * heli_scale_x
            heli_height_scaled = heli.height * heli_scale_y


            /*dirigible_scale_x      = dirigible_width * ppm / dirigible.width
            dirigible_scale_y      = dirigible_height * ppm / dirigible.height
            dirigible_width_scaled  = dirigible.width * dirigible_scale_x
            dirigible_height_scaled = dirigible.height * dirigible_scale_y*/

            /*ufo_scale_x       = ufo_width * ppm / ufo.width
            ufo_scale_y       = ufo_height * ppm / ufo.height
            ufo_width_scaled  = ufo.width * ufo_scale_x
            ufo_height_scaled = ufo.height * ufo_scale_y*/

            radius *= ppm
            speed  *= ppm
            g      *= ppm


            /*Log.d("ppm", "ppm: " + ppm)
            Log.d("ppm", "heli_scale: " + heli_scale)
            Log.d("ppm", "cannon width in px: " + cannon.width*heli_scale)
            Log.d("ppm", "cannon height in px: " + cannon.height*heli_scale)
            Log.d("ppm", "radius: " + radius)
            Log.d("ppm", "speed: " + speed)
            Log.d("ppm", "gravity: " + g)*/


            // Set translation values
            user_dx = 30f
            user_dy = height / 2.5f

            /*dirigible_dx = width + dirigible_width + 20f
            dirigible_dy = 20f

            ufo_dx = width + ufo_width + 20f
            ufo_dy = width - 20f*/

            pickBuilding()
            pickVehicle()
        }

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

        user_dy += increment

        if (user_dy < 0)
            user_dy = 0f
        else if (user_dy + heli_height_scaled > height)
            user_dy = height.toFloat() - heli_height_scaled

        // Scale the bitmap
        heli_matrix.setScale(heli_scale_x, heli_scale_y)

        // Put the bitmap on the left-center of the screen
        heli_matrix.postTranslate(user_dx, user_dy)

        canvas?.drawBitmap(heli, heli_matrix, null)
        canvas?.drawRect(user_dx, user_dy, user_dx + heli_width_scaled, user_dy + heli_height_scaled, painter_stroke)


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
        vehicle?.update()
        canvas?.drawBitmap(vehicle!!.getBitmap(), vehicle!!.getMatrix(), null)
        /*canvas?.drawRect(
            vehicle!!.getX(),
            vehicle!!.getY(),
            vehicle!!.getX() + vehicle!!.getBitmapScaledWidth(),
            vehicle!!.getY() + vehicle!!.getBitmapScaledHeight(),
            painter_stroke)*/

        val bound = vehicle!!.getBounds()
        for (rect in bound)
            canvas?.drawRect(rect, painter_stroke)

        // Check if the vehicle has been respawn
        if (vehicle!!.isRespawn())
            pickVehicle()






        /*canvas?.drawBitmap(building, building_dx, building_dy, null)
        canvas?.drawRect(building_dx, building_dy, building_dx + building.width, building.height.toFloat(), painter_stroke)

        //building_dx -= 5f
        building_dx = 200f

        if (building_dx + building.width < 0)
            pickBuilding()*/


        // AGGIORNAMENTO CON VEHICLE UPDATE
        building.update()
        building.setX(10f)
        canvas?.drawBitmap(building.getBitmap(), building.getMatrix(), null)

        //canvas?.drawRect(building.getX(), building.getY(),
        //    building.getX() + building.getBitmapScaledWidth(), height.toFloat(), painter_stroke)

        val bounds = building.getBounds()
        for (rect in bounds)
            canvas?.drawRect(rect, painter_stroke)

        // Check if the building has been respawn
        if (building.isRespawn())
            pickBuilding()

        if (checkCollision()) {
            game_over = true

            //val activity = context as MainActivity
            //activity.gameOver()

            val fragment = findFragment<GameFragment>()
            fragment.gameOver()
        }


        // Compute time difference
        val now = System.currentTimeMillis()
        val dt: Long

        // Check if this is the first call to onDraw (i.e. first frame)
        if (previous == 0L)  dt = 0L
        else                 dt = now - previous

        /*Log.d("TIME", "************")
        Log.d("TIME", "previous: " + previous)
        Log.d("TIME", "now: " + now)
        Log.d("TIME", "dt: " + dt)*/

        previous = now

        //Log.d("TIME", "initial_speed * dt / 1000f: " + initial_speed * dt / 1000f)

        // s = v * dt  tutto in m/s
        score += initial_speed * dt / 1000

        canvas?.drawText("SCORE " + score.toLong(), 20f, 60f, painter_fill)

        invalidate()







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

    // Convert density indipendent pixels into screen pixels
    fun dp2px(dp: Float): Float {
        return resources.displayMetrics.density * dp
    }

    // Pick a random building and set a random height for it
    private fun pickBuilding() {
        building = buildings.random()

        // Height range height/1.3f .. height/5f
        //val r = ((13..50).random() / 10).toFloat()
        val r = 5f

        building.setY(height/r)

        // Increase the distance between obstacles if the height of
        // the building is greater than a threshold
        if (r > 3.5f && vehicle != null && building.getX() - vehicle!!.getX() < heli_width_scaled * 2f)
            building.setX(width + vehicle!!.getBitmapScaledWidth() + heli_width_scaled * 2f)
    }

    // Pick a random vehicle and set a random height for it
    private fun pickVehicle() {
        vehicle = vehicles.random()

        // Increase the distance between obstacles if the height of
        // the building is greater than a threshold
        if (building.getY() > height/3.5f && vehicle!!.getX() - building.getX() < heli_width_scaled * 2f)
            vehicle!!.setX(width + building.getBitmapScaledWidth() + heli_width_scaled * 2f)
    }

    // Check if the user collides with an obstacle
    private fun checkCollision(): Boolean { return false
        user_rect.set( user_dx, user_dy, user_dx + heli_width_scaled, user_dy + heli_height_scaled )

        val rects = building.getBounds()
        rects.addAll(vehicle!!.getBounds())

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


        if (lastGyroscope[0] > 0) {
            increment = Math.max(1f, Math.min(lastGyroscope[0] * 15, 15f))
        }
        else if (lastGyroscope[0] < 0) {
            increment = Math.min(-1f, Math.max(lastGyroscope[0] * 15, -15f))
        }

        /*Log.d("SENSOR", "****************")
        Log.d("SENSOR", "value: " + lastGyroscope[0])
        Log.d("SENSOR", "increment: " + increment)*/

        invalidate()
    }
}