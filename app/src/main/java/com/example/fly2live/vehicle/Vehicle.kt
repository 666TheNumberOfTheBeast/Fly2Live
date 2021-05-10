package com.example.fly2live.vehicle

import com.example.fly2live.obstacle.Obstacle

import android.graphics.Bitmap
import android.graphics.RectF

class Vehicle(name: String,
              bitmap: Bitmap,
              bounds_offsets: Array<Array<Float>>,
              width: Float, height: Float,
              screen_width: Int, screen_height: Int, ppm: Float,
              pos_x: Float, pos_y: Float, speed: Float) :
    Obstacle(name, bitmap, width, height, screen_width, screen_height, ppm, pos_x, pos_y, speed) {

    private val bounds_offsets: Array<Array<Float>>
    private val bounds = ArrayList<RectF>()

    init {
        this.bounds_offsets = bounds_offsets

        for (offset_array in bounds_offsets)
            bounds.add( RectF() )
    }

    // Getters
    fun getBounds(): ArrayList<RectF> {
        @Suppress("UNCHECKED_CAST")
        return bounds.clone() as ArrayList<RectF>
    }

    // Update UI
    override fun update(dt: Float) {
        super.update(dt)

        for (i in bounds_offsets.indices) {
            val offset_array = bounds_offsets[i]

            bounds[i].set(
                getX() + offset_array[0] * getBitmapScaledWidth(),
                getY() + offset_array[1] * getBitmapScaledHeight(),
                getX() + offset_array[2] * getBitmapScaledWidth(),
                getY() + offset_array[3] * getBitmapScaledHeight()
            )
        }
    }

}






/*package com.example.game.vehicle

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF

class Vehicle(name: String,
              bitmap: Bitmap,
              bounds_offsets: Array<Array<Float>>,
              width: Float, height: Float,
              screen_width: Int, screen_height: Int, ppm: Int,
              pos_x: Float, pos_y: Float, speed: Float) {

    private val name: String

    private val bitmap: Bitmap
    private val matrix = Matrix()

    private val bounds_offsets: Array<Array<Float>>
    private val bounds = ArrayList<RectF>()

    private var screen_width  = 0
    private var screen_height = 0

    private var bitmap_scale_x = 0f
    private var bitmap_scale_y = 0f

    private var bitmap_width_scaled  = 0f
    private var bitmap_height_scaled = 0f

    private var pos_x = 0f
    private var pos_y = 0f
    private var speed = 0f

    private var respawn = false

    init {
        this.name = name

        this.bitmap = bitmap

        this.screen_width  = screen_width
        this.screen_height = screen_height

        bitmap_scale_x = width * ppm / screen_width
        bitmap_scale_y = height * ppm / screen_height

        bitmap_width_scaled  = screen_width * bitmap_scale_x
        bitmap_height_scaled = screen_height * bitmap_scale_y

        this.pos_x = pos_x
        this.pos_y = pos_y

        this.speed = speed

        this.bounds_offsets = bounds_offsets
    }

    // Getters
    fun getName(): String {
        return name
    }

    fun getBitmap(): Bitmap {
        return bitmap
    }

    fun getMatrix(): Matrix {
        return matrix
    }

    fun getBitmapScaleX(): Float {
        return bitmap_scale_x
    }

    fun getBitmapScaleY(): Float {
        return bitmap_scale_y
    }

    fun getBitmapScaledWidth(): Float {
        return bitmap_width_scaled
    }

    fun getBitmapScaledHeight(): Float {
        return bitmap_height_scaled
    }

    fun getX(): Float {
        return pos_x
    }

    fun getY(): Float {
        return pos_y
    }

    fun getSpeed(): Float {
        return speed
    }

    fun getBounds(): ArrayList<RectF> {
        return bounds.clone() as ArrayList<RectF>
    }

    // Setters
    fun setX(pos_x: Float) {
        this.pos_x = pos_x
    }

    fun setY(pos_y: Float) {
        this.pos_y = pos_y
    }

    fun setSpeed(speed: Float) {
        this.speed = speed
    }

    // Update UI
    fun update() {
        matrix.setScale(bitmap_scale_x, bitmap_scale_y)
        matrix.postTranslate(pos_x, pos_y)

        pos_x -= speed

        if (pos_x + bitmap_width_scaled < 0) {
            //pos_x = screen_width + bitmap_width_scaled
            pos_x = screen_width + 20f
            respawn = true
        }
        else
            respawn = false

        /*for (i in bounds_offsets.indices)
            bounds.set(i, RectF(
                pos_x + bounds_offsets[i][0] * bitmap_width_scaled,
                pos_y + bounds_offsets[i][1],
                pos_x + bounds_offsets[i][2] * bitmap_width_scaled,
                screen_height + bounds_offsets[i][3])
            )*/

        for (offset_array in bounds_offsets)
            bounds.add(RectF(
                pos_x + offset_array[0] * bitmap_width_scaled,
                pos_y + offset_array[1],
                pos_x + offset_array[2] * bitmap_width_scaled,
                screen_height + offset_array[3])
            )
    }

    // Return true if the vehicle has been respawn in the current UI update
    fun isRespawn(): Boolean {
        return respawn
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vehicle

        if (bitmap != other.bitmap) return false
        if (bitmap_scale_x != other.bitmap_scale_x) return false
        if (bitmap_scale_y != other.bitmap_scale_y) return false
        if (bitmap_width_scaled != other.bitmap_width_scaled) return false
        if (bitmap_height_scaled != other.bitmap_height_scaled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bitmap.hashCode()
        result = 31 * result + bitmap_scale_x.hashCode()
        result = 31 * result + bitmap_scale_y.hashCode()
        result = 31 * result + bitmap_width_scaled.hashCode()
        result = 31 * result + bitmap_height_scaled.hashCode()
        return result
    }

}*/