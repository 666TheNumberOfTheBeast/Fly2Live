package com.example.fly2live.configuration

import io.socket.client.Socket

class Configuration {
    companion object {
        // Google Games player ID
        var PLAYER_ID = "" // TEMP DISABLED TO SAVE API CALLS
        //var PLAYER_ID = (0..1000).random().toString()
        // Player JWT for MongoDB Realm
        var PLAYER_JWT = "" // MESSO QUI E NON IN SHARED PREFERENCES, VIENE ELIMINATO OGNI VOLTA CHE L'APP VIENE CHIUSA
        // Player levels and xp (retrieve the first time and then handle them locally)
        var PLAYER_LEVEL = -1
        var PLAYER_XP    = -1L
        //var PLAYER_SCORE = -1L


        // Game mode
        var MULTIPLAYER = false
        var SCENARIO    = 0

        // Constants for the scenarios codes
        const val SCENARIO_CITY_DAY   = 0
        const val SCENARIO_CITY_NIGHT = 1


        // Socket instance
        var SOCKET_INSTANCE: Socket? = null

        // Server URL
        const val URL = "http://192.168.1.110:5000/"

        // Constants for the client requests
        const val REQ_CODE_NEW_GAME = 0
        const val REQ_CODE_NEW_MOVE = 1

        // Constants for the server message codes
        const val MSG_CODE_BAD_REQ       = 0
        const val MSG_CODE_SEARCHING_ADV = 1
        const val MSG_CODE_FOUND_ADV     = 2
        const val MSG_CODE_PREPARE       = 3
        const val MSG_CODE_GAME_START    = 4
        const val MSG_CODE_GAME_READY    = 5
        const val MSG_CODE_GAME_PLAY     = 6
        const val MSG_CODE_GAME_END      = 7
        const val MSG_CODE_SERVER_BUSY   = 8
        const val MSG_CODE_RESEND        = 9

        // Constants for game end status
        const val WINNER_UNDEFINED = -1
        const val WINNER_ADVERSARY = 0
        const val WINNER_PLAYER    = 1
        const val WINNER_DRAW      = 2


        // Constants for buildings dimensions
        const val BUILDING_01_DAY_WIDTH  = 35f
        const val BUILDING_01_DAY_HEIGHT = 130f
        const val BUILDING_02_DAY_WIDTH  = 18f
        const val BUILDING_02_DAY_HEIGHT = 140f
        const val BUILDING_03_DAY_WIDTH  = 28f
        const val BUILDING_03_DAY_HEIGHT = 150f
        const val BUILDING_04_DAY_WIDTH  = 53f
        const val BUILDING_04_DAY_HEIGHT = 120f
        const val BUILDING_05_DAY_WIDTH  = 80f
        const val BUILDING_05_DAY_HEIGHT = 110f
        const val BUILDING_06_DAY_WIDTH  = 160f
        const val BUILDING_06_DAY_HEIGHT = 80f
        const val BUILDING_07_DAY_WIDTH  = 30f
        const val BUILDING_07_DAY_HEIGHT = 120f
        const val BUILDING_08_DAY_WIDTH  = 28f
        const val BUILDING_08_DAY_HEIGHT = 120f
        const val BUILDING_09_DAY_WIDTH  = 40f
        const val BUILDING_09_DAY_HEIGHT = 110f
        const val BUILDING_10_DAY_WIDTH  = 30f
        const val BUILDING_10_DAY_HEIGHT = 110f
        const val BUILDING_11_DAY_WIDTH  = 35f
        const val BUILDING_11_DAY_HEIGHT = 110f
        const val BUILDING_12_DAY_WIDTH  = 30f
        const val BUILDING_12_DAY_HEIGHT = 110f
        const val BUILDING_13_DAY_WIDTH  = 45f
        const val BUILDING_13_DAY_HEIGHT = 110f
        const val BUILDING_14_DAY_WIDTH  = 55f
        const val BUILDING_14_DAY_HEIGHT = 110f
        const val BUILDING_15_DAY_WIDTH  = 25f
        const val BUILDING_15_DAY_HEIGHT = 110f
        const val BUILDING_16_DAY_WIDTH  = 40f
        const val BUILDING_16_DAY_HEIGHT = 120f
        const val BUILDING_17_DAY_WIDTH  = 50f
        const val BUILDING_17_DAY_HEIGHT = 120f

        const val BUILDING_01_NIGHT_WIDTH  = 22f
        const val BUILDING_01_NIGHT_HEIGHT = 140f
        const val BUILDING_02_NIGHT_WIDTH  = 13f
        const val BUILDING_02_NIGHT_HEIGHT = 130f
        const val BUILDING_03_NIGHT_WIDTH  = 32f
        const val BUILDING_03_NIGHT_HEIGHT = 110f
        const val BUILDING_04_NIGHT_WIDTH  = 32f
        const val BUILDING_04_NIGHT_HEIGHT = 110f
        const val BUILDING_05_NIGHT_WIDTH  = 50f
        const val BUILDING_05_NIGHT_HEIGHT = 130f
        const val BUILDING_06_NIGHT_WIDTH  = 40f
        const val BUILDING_06_NIGHT_HEIGHT = 130f
        const val BUILDING_07_NIGHT_WIDTH  = 30f
        const val BUILDING_07_NIGHT_HEIGHT = 120f
        const val BUILDING_08_NIGHT_WIDTH  = 30f
        const val BUILDING_08_NIGHT_HEIGHT = 120f
        const val BUILDING_09_NIGHT_WIDTH  = 55f
        const val BUILDING_09_NIGHT_HEIGHT = 140f
        const val BUILDING_10_NIGHT_WIDTH  = 25f
        const val BUILDING_10_NIGHT_HEIGHT = 120f
        const val BUILDING_11_NIGHT_WIDTH  = 18f
        const val BUILDING_11_NIGHT_HEIGHT = 130f

        // Constants for vehicles dimensions
        const val PLAYER_WIDTH  = 10f
        const val PLAYER_HEIGHT = 3f
        const val VEHICLE_01_WIDTH  = 6f
        const val VEHICLE_01_HEIGHT = 2.5f
        const val VEHICLE_02_WIDTH  = 20f
        const val VEHICLE_02_HEIGHT = 8f
        const val VEHICLE_03_WIDTH  = 20f
        const val VEHICLE_03_HEIGHT = 8f
        const val VEHICLE_04_WIDTH  = 20f
        const val VEHICLE_04_HEIGHT = 8f
        const val VEHICLE_05_WIDTH  = 20f
        const val VEHICLE_05_HEIGHT = 8f
        const val VEHICLE_06_WIDTH  = 20f
        const val VEHICLE_06_HEIGHT = 8f
        const val VEHICLE_07_WIDTH  = 20f
        const val VEHICLE_07_HEIGHT = 8f
        const val VEHICLE_08_WIDTH  = 20f
        const val VEHICLE_08_HEIGHT = 8f
        const val VEHICLE_09_WIDTH  = 4.2f
        const val VEHICLE_09_HEIGHT = 2.1f
    }
}