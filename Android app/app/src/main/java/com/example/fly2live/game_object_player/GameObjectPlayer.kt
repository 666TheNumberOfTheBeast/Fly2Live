package com.example.fly2live.game_object_player


import com.example.fly2live.game_object.GameObject
import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.max

class GameObjectPlayer(
        name: String,
        bitmaps: Array<Bitmap>,
        bounds_offsets: Array<Array<Float>>,
        screen_width: Int, screen_height: Int, ppm: Float,
        width: Float, height: Float,
        pos_x: Float, pos_y: Float, speed: Float) :
    GameObject(name, bitmaps, bounds_offsets, screen_width, screen_height, ppm, width, height, pos_x, pos_y, speed) {

    private var bitmapRotation = 8f // Degrees
    private val rotationMatrix = Matrix()

    private val rotationIncrement = 0.5f
    private val maxRotation = 10f
    private val minRotation = -8f

    private val screenHeightPortrait = max(screen_width, screen_height)

    // Getters
    fun getBitmapRotation(): Float {
        return bitmapRotation // Degrees
    }

    // Update UI
    override fun update(dt: Float) {
        // Set player Y based on last gyroscope input (in pixels for efficiency of update calls)
        setY( getY() + getSpeed() * dt )

        // Constrain the bitmap in the screen (based on the height of the current screen orientation)
        /*if (getY() < 0f)
            setY(0f)
        else if (getY() + getBitmapScaledHeight() > getScreenHeight())
            setY( getScreenHeight() - getBitmapScaledHeight() )*/

        // Constrain the bitmap in the screen (based on the height of the screen in portrait orientation).
        // So, in landscape orientation, the bitmap can go down the screen and
        // who plays in such a orientation has no advantages than who plays in portrait mode
        if (getY() < 0f)
            setY(0f)
        else if (getY() + getBitmapScaledHeight() > screenHeightPortrait)
            setY( screenHeightPortrait - getBitmapScaledHeight() ) // In pixels

        // Constrain the rotation of the bitmap
        if (getSpeed() > 0f && bitmapRotation <= maxRotation)
            bitmapRotation += rotationIncrement
        else if (getSpeed() < 0f && bitmapRotation >= minRotation)
            bitmapRotation -= rotationIncrement

        super.update(dt)

        for (i in bitmapBounds.indices) {
            val rect = bitmapBounds[i]

            // The output RectF is just a new non-rotated rect whose edges touch the corner points of the rotated rectangle that you are wanting
            rotationMatrix.setRotate(getBitmapRotation(), getX(), getY())
            rotationMatrix.mapRect(rect)

            // Set the rotated rect
            bitmapBounds[i].set(rect)
        }
    }

    override fun transform() {
        val m = getMatrix()
        m.setScale(getBitmapScaleX(), getBitmapScaleY())
        m.postRotate(bitmapRotation)
        m.postTranslate(getX(), getY())
    }

}