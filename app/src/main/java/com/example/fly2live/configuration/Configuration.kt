package com.example.fly2live.configuration

class Configuration {
    companion object {
        // Player ID
        var PLAYER_ID = "my_player_id"

        // Game mode
        var MULTIPLAYER = false

        // Server URL
        const val URL = "http://192.168.1.110:5000/"
        //const val URL = "http://127.0.0.1:6000/"
        const val POLLING_PERIOD = 1000L

        const val HOST = "192.168.1.110"
        const val PORT = 5000

        // Constants for the client requests
        const val NEW_GAME = 0
        const val POLLING  = 1
        const val NEW_MOVE = 2
    }
}