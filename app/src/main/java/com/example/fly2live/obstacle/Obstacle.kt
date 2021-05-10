package com.example.fly2live.obstacle

import android.graphics.Bitmap
import android.graphics.Matrix

abstract class Obstacle(name: String,
                        bitmap: Bitmap,
                        width: Float, height: Float,
                        screen_width: Int, screen_height: Int, ppm: Float,
                        pos_x: Float, pos_y: Float, speed: Float) {

    private val name: String

    private val bitmap: Bitmap
    private val matrix: Matrix

    private val screen_width: Int
    private val screen_height: Int

    private val ppm: Float

    private var bitmap_scale_x: Float
    private var bitmap_scale_y: Float

    private var bitmap_width_scaled: Float
    private var bitmap_height_scaled: Float

    private var pos_x: Float
    private var pos_y: Float
    private var speed: Float

    private var respawn = false
    private val respawn_pos_x: Float

    init {
        this.name = name

        this.bitmap = bitmap
        this.matrix = Matrix()

        this.screen_width  = screen_width
        this.screen_height = screen_height

        this.ppm = ppm

        bitmap_scale_x = width * ppm / screen_width
        bitmap_scale_y = height * ppm / screen_height

        bitmap_width_scaled  = screen_width * bitmap_scale_x
        bitmap_height_scaled = screen_height * bitmap_scale_y

        //this.pos_x = pos_x
        this.pos_y = pos_y
        this.pos_x = pos_x * ppm
        //this.pos_y = pos_y * ppm

        //this.speed = speed
        this.speed = speed * ppm

        respawn_pos_x = screen_width + ppm
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

    fun getScreenWidth(): Int {
        return screen_width
    }

    fun getScreenHeight(): Int {
        return screen_height
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


    // Setters
    fun setX(pos_x: Float) {
        this.pos_x = pos_x
    }

    fun setY(pos_y: Float) {
        this.pos_y = pos_y
    }

    fun setSpeed(speed: Float) {
        //this.speed = speed
        this.speed = speed * ppm
    }

    // Update UI
    open fun update(dt: Float) {
        matrix.setScale(bitmap_scale_x, bitmap_scale_y)
        matrix.postTranslate(pos_x, pos_y)

        // CON QUESTA VERSIONE NON LAGGA MAI PERCHÈ NON USO dt
        //pos_x -= speed
        // CON QUESTA VERSIONE POTREBBE LAGGARE UN PO' PERCHÈ dt NON È COSTANTE
        pos_x -= speed * dt

        if (pos_x + bitmap_width_scaled < 0) {
            //pos_x = screen_width + bitmap_width_scaled
            //pos_x = screen_width + 20f
            pos_x = respawn_pos_x
            respawn = true
        }
        else
            respawn = false
    }

    // Return true if the vehicle has been respawn in the current UI update
    fun isRespawn(): Boolean {
        return respawn
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Obstacle

        if (name != other.name) return false
        if (bitmap != other.bitmap) return false
        if (bitmap_scale_x != other.bitmap_scale_x) return false
        if (bitmap_scale_y != other.bitmap_scale_y) return false
        if (bitmap_width_scaled != other.bitmap_width_scaled) return false
        if (bitmap_height_scaled != other.bitmap_height_scaled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + bitmap.hashCode()
        result = 31 * result + bitmap_scale_x.hashCode()
        result = 31 * result + bitmap_scale_y.hashCode()
        result = 31 * result + bitmap_width_scaled.hashCode()
        result = 31 * result + bitmap_height_scaled.hashCode()
        return result
    }

}