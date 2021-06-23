package com.example.fly2live.game_object_player


import com.example.fly2live.game_object.GameObject
import android.graphics.Bitmap

class GameObjectPlayer(name: String,
                    bitmaps: Array<Bitmap>,
                    bounds_offsets: Array<Array<Float>>,
                    width: Float, height: Float,
                    screen_width: Int, screen_height: Int, ppm: Float,
                    pos_x: Float, pos_y: Float, speed: Float) :
    GameObject(name, bitmaps, bounds_offsets, width, height, screen_width, screen_height, ppm, pos_x, pos_y, speed) {

    private var bitmapRotation = 8f // Degrees

    private val rotationIncrement = 0.5f
    private val maxRotation = 10f
    private val minRotation = -8f

    // Update UI
    fun update(dt: Float, last_gyroscope_input: Float) {
        super.update(dt)

        setY( getY() + last_gyroscope_input * getPPM() )

        // Constrain the bitmap in the screen
        if (getY() < 0)
            setY(0f)
        else if (getY() + getBitmapScaledHeight() > getScreenHeight())
            setY( getScreenHeight() - getBitmapScaledHeight() )

        // Constrain the rotation of the bitmap
        if (last_gyroscope_input > 0 && bitmapRotation <= maxRotation)
            bitmapRotation += rotationIncrement
        else if (last_gyroscope_input < 0 && bitmapRotation >= minRotation)
            bitmapRotation -= rotationIncrement
    }

    override fun transform() {
        val m = getMatrix()
        m.setScale(getBitmapScaleX(), getBitmapScaleY())
        m.postRotate(bitmapRotation)
        m.postTranslate(getX(), getY())
    }

}