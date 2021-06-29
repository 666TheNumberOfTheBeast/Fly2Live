package com.example.fly2live.game_object_cpu

import com.example.fly2live.game_object.GameObject
import android.graphics.Bitmap

class GameObjectCpu(
        name: String,
        bitmaps: Array<Bitmap>,
        bounds_offsets: Array<Array<Float>>,
        screen_width: Int, screen_height: Int, ppm: Float,
        width: Float, height: Float,
        pos_x: Float, pos_y: Float, speed: Float) :
    GameObject(name, bitmaps, bounds_offsets, screen_width, screen_height, ppm, width, height, pos_x, pos_y, speed) {

    private var respawn: Boolean
    private val respawnPosX: Float

    init {
        respawn = false
        respawnPosX = screen_width + ppm // In pixels
    }

    // Return true if the game object has been respawn in the current UI update
    fun isRespawn(): Boolean {
        return respawn
    }

    // Update UI
    override fun update(dt: Float) {
        // Move object to left
        setX( getX() - getSpeed() * dt )

        // Check if the bitmap is outside the left margin of the screen
        if (getX() + getBitmapScaledWidth() < 0f) {
            setX(respawnPosX)
            respawn = true
        }
        else
            respawn = false

        super.update(dt)
    }

}