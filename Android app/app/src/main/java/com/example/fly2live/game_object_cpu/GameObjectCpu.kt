package com.example.fly2live.game_object_cpu

import com.example.fly2live.game_object.GameObject
import android.graphics.Bitmap

class GameObjectCpu(name: String,
              bitmaps: Array<Bitmap>,
              bounds_offsets: Array<Array<Float>>,
              width: Float, height: Float,
              screen_width: Int, screen_height: Int, ppm: Float,
              pos_x: Float, pos_y: Float, speed: Float) :
    GameObject(name, bitmaps, bounds_offsets, width, height, screen_width, screen_height, ppm, pos_x, pos_y, speed) {

    private var speed: Float

    private var respawn: Boolean
    private val respawnPosX: Float

    init {
        this.speed = speed * ppm // Convert m/s into pixels/s

        respawn = false
        respawnPosX = screen_width + ppm // In pixels
    }

    // Getters
    fun getSpeed(): Float {
        return speed // Return pixels/s
    }

    // Return true if the game object has been respawn in the current UI update
    fun isRespawn(): Boolean {
        return respawn
    }

    // Setters
    fun setSpeed(speed: Float) {
        this.speed = speed * getPPM() // Convert m/s into pixels/s
    }

    // Update UI
    override fun update(dt: Float) {
        super.update(dt)

        // CON QUESTA VERSIONE NON LAGGA MAI PERCHÈ NON USO dt
        //setX( getX() - speed )
        // CON QUESTA VERSIONE POTREBBE LAGGARE UN PO' PERCHÈ dt NON È COSTANTE
        setX( getX() - speed * dt )

        if (getX() + getBitmapScaledWidth() < 0) {
            setX(respawnPosX)
            respawn = true
        }
        else
            respawn = false
    }



}