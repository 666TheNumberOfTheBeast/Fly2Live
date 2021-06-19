package com.example.fly2live.configuration

class Configuration {
    companion object {
        // Google Games player ID
        var PLAYER_ID = ""
        // Player JWT for MongoDB Realm
        var PLAYER_JWT = "" // MESSO QUI E NON IN SHARED PREFERENCES, VIENE ELIMINATO OGNI VOLTA CHE L'APP VIENE CHIUSA
        // Player levels and xp (retrieve the first time and then handle them locally)
        var PLAYER_LEVEL = -1
        var PLAYER_XP    = -1L
        var PLAYER_SCORE = -1L


        // Game mode
        var MULTIPLAYER = false
        var SCENARIO    = 0

        // Constants for the scenarios codes
        const val SCENARIO_CITY_DAY   = 0
        const val SCENARIO_CITY_NIGHT = 1

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