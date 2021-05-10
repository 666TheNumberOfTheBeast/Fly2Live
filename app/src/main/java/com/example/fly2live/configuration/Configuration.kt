package com.example.fly2live.configuration

class Configuration {
    companion object {
        // Player ID
        var PLAYER_ID = (0..50).random().toString()

        // Game mode
        var MULTIPLAYER = false

        // Server URL
        const val URL = "http://192.168.1.110:5000/"

        // Constants for the client requests
        const val NEW_GAME = 0
        const val POLLING  = 1
        const val NEW_MOVE = 2

        // Constants for the server message codes
        const val MSG_CODE_BAD_REQ       = 0
        const val MSG_CODE_SEARCHING_ADV = 1
        const val MSG_CODE_FOUND_ADV     = 2
        const val MSG_CODE_PREPARE       = 3
        const val MSG_CODE_GAME_START    = 4
        const val MSG_CODE_GAMEPLAY      = 5
        const val MSG_CODE_GAME_END      = 6
        const val MSG_CODE_SERVER_BUSY   = 7
    }
}
