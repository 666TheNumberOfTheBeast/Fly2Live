package com.example.fly2live.player

class Player(name: String, wins: Int, loses: Int, bestScore: Long) {
    private val name: String
    private val wins: Int
    private val loses: Int
    private val bestScore: Long
    private val winPercentage: String
    private var position = 0

    init {
        this.name           = name
        this.wins           = wins
        this.loses          = loses
        this.bestScore      = bestScore
        this.winPercentage = String.format("%.3f", wins / (wins + loses).toFloat())
    }

    // Getters
    fun getName(): String {
        return name
    }

    fun getWins(): Int {
        return wins
    }

    fun getLoses(): Int {
        return loses
    }

    fun getBestScore(): Long {
        return bestScore
    }

    fun getWinPercentage(): String {
        return winPercentage
    }

    fun getPosition(): Int {
        return position
    }

    // Setters
    fun setPosition(position: Int) {
        this.position = position
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Player

        if (name != other.name) return false
        if (wins != other.wins) return false
        if (loses != other.loses) return false
        if (bestScore != other.bestScore) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + wins
        result = 31 * result + loses
        result = 31 * result + bestScore.hashCode()
        return result
    }

    override fun toString(): String {
        return name + " " + winPercentage
    }

}