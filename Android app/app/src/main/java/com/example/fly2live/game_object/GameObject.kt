package com.example.fly2live.game_object

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF

abstract class GameObject(
    name: String,
    bitmaps: Array<Bitmap>,
    bounds_offsets: Array<Array<Float>>,
    width: Float, height: Float,
    screen_width: Int, screen_height: Int, ppm: Float,
    pos_x: Float, pos_y: Float, speed: Float) {

    private val name: String

    // Array of bitmaps for animating the game object
    private val bitmaps: Array<Bitmap>
    private var bitmap: Bitmap
    private var bitmapAnimationIndex: Int

    // Array of arrays for bounds offsets for game physics
    private val boundsOffsets: Array<Array<Float>>
    private val bounds = ArrayList<RectF>()

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
    private var speed: Float

    init {
        this.name = name

        this.bitmaps = bitmaps.clone()
        bitmapAnimationIndex = 0
        bitmap = bitmaps[bitmapAnimationIndex]

        this.boundsOffsets = bounds_offsets

        for (offset_array in bounds_offsets)
            bounds.add( RectF() )

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
        this.speed = speed * ppm // Convert m/s into pixels/s (for efficiency of update calls)
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

    fun getBounds(): ArrayList<RectF> {
        @Suppress("UNCHECKED_CAST")
        return bounds.clone() as ArrayList<RectF>
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

    fun getSpeed(): Float {
        return speed // In pixels/s (for efficiency of update calls)
    }


    // Setters
    fun setX(pos_x: Float) {
        this.posX = pos_x  // In pixels (both in input and output for efficiency of update calls)
    }

    fun setY(pos_y: Float) {
        this.posY = pos_y  // In pixels (both in input and output for efficiency of update calls)
    }

    fun setSpeed(speed: Float) {
        this.speed = speed * ppm // Convert m/s into pixels/s (for efficiency of update calls. Unlike setX and setY, here the input can be useful in meters)
    }

    // Update UI
    open fun update(dt: Float) {
        // Graphics transformations
        //transform()

        // Select next bitmap for the animation frame
        if (bitmaps.size > 1) {
            bitmapAnimationIndex = (bitmapAnimationIndex + 1) % bitmaps.size
            bitmap = bitmaps[bitmapAnimationIndex]
        }

        // Set physics bounds
        for (i in boundsOffsets.indices) {
            val offsetArray = boundsOffsets[i]

            if (offsetArray.size == 4)
                bounds[i].set(
                    getX() + offsetArray[0] * getBitmapScaledWidth(),
                    getY() + offsetArray[1] * getBitmapScaledHeight(),
                    getX() + offsetArray[2] * getBitmapScaledWidth(),
                    getY() + offsetArray[3] * getBitmapScaledHeight()
                )
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

        other as GameObject

        if (name != other.name) return false
        if (!bitmaps.contentDeepEquals(other.bitmaps)) return false
        if (bitmapScaleX != other.bitmapScaleX) return false
        if (bitmapScaleY != other.bitmapScaleY) return false
        if (bitmapWidthScaled != other.bitmapWidthScaled) return false
        if (bitmapHeightScaled != other.bitmapHeightScaled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + bitmaps.contentDeepHashCode()
        result = 31 * result + bitmapScaleX.hashCode()
        result = 31 * result + bitmapScaleY.hashCode()
        result = 31 * result + bitmapWidthScaled.hashCode()
        result = 31 * result + bitmapHeightScaled.hashCode()
        return result
    }

}