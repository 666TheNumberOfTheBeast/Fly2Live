package com.example.fly2live.game_object_player


import com.example.fly2live.game_object.GameObject
import android.graphics.Bitmap
import kotlin.math.max

class GameObjectPlayer(name: String,
                    bitmaps: Array<Bitmap>,
                    bounds_offsets: Array<Array<Float>>,
                    width: Float, height: Float,
                    screen_width: Int, screen_height: Int, ppm: Float,
                    pos_x: Float, pos_y: Float/*, speed: Float*/) :
    GameObject(name, bitmaps, bounds_offsets, width, height, screen_width, screen_height, ppm, pos_x, pos_y/*, speed*/) {

    private var bitmapRotation = 8f // Degrees

    private val rotationIncrement = 0.5f
    private val maxRotation = 10f
    private val minRotation = -8f

    private val screenHeightPortrait = max(screen_width, screen_height)

    // Update UI
    fun update(dt: Float, last_gyroscope_input: Float) {
        super.update(dt)

        // Set player Y based on last gyroscope input
        setY( getY() + last_gyroscope_input * getPPM() )

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
            setY( screenHeightPortrait - getBitmapScaledHeight() )

        // Constrain the rotation of the bitmap
        if (last_gyroscope_input > 0f && bitmapRotation <= maxRotation)
            bitmapRotation += rotationIncrement
        else if (last_gyroscope_input < 0f && bitmapRotation >= minRotation)
            bitmapRotation -= rotationIncrement
    }

    override fun transform() {
        val m = getMatrix()
        m.setScale(getBitmapScaleX(), getBitmapScaleY())
        m.postRotate(bitmapRotation)
        m.postTranslate(getX(), getY())
    }

}