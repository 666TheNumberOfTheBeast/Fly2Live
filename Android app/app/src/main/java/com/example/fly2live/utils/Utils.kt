package com.example.fly2live.utils

import android.util.Log

// Given the player level, return the max xp of it
fun getCurrentLevelMaxXp(playerLevel: Int): Long {
    Log.d("progress", "Print xp when each level is from 0 to 1000*playerLevel starting from level 1")
    var xpCurrent = 0
    for (i in 1..10) {
       val j = i+1
       //val xpNext = xpCurrent + 1000*j
       val xpNext = xpCurrent + 1000*i
       Log.d("progress", "Level $i -> Level $j = $xpCurrent -> $xpNext")
       xpCurrent = xpNext
    }

    Log.d("playerLevel", "playerLevel: $playerLevel")

    // RAGIONANDO IN TERMINI DI XP ASSOLUTI (SOMMO TUTTE LE XP DI OGNI LIVELLO RAGGIUNTO)
    // Calculate current player level max xp
    /*var playerLevelMaxXp = 0L

    //for (i in 2..playerLevel) {
    for (i in 1..playerLevel) {
        playerLevelMaxXp += i * 1000
        Log.d("playerLevel", "xpPlayerLevelMax: $playerLevelMaxXp")
    }

    return playerLevelMaxXp*/

    // RAGIONANDO IN TERMINI DI XP RELATIVI (OGNI VOLTA CHE SALGO DI LIVELLO SOTTRAGGO MAX XP LIVELLO PRECEDENTE)
    Log.d("playerLevel", "xpPlayerLevelMax: ${playerLevel * 1000L}")
    return playerLevel * 1000L
}

// Given the player level and the new xp, return if a new level has been achieved
fun isNewLevel(playerLevel: Int, newPlayerXp: Long): Boolean {
    return newPlayerXp >= getCurrentLevelMaxXp(playerLevel)
}