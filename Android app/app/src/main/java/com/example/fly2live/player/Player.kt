package com.example.fly2live.player


class Player(name: String) {
    private val name: String
    private var wins: Long
    private var loses: Long
    private var bestScore: Long
    private var winPercentage: String
    private var position = 0

    init {
        this.name = name
        this.wins = -1
        this.loses = -1
        this.bestScore = -1
        this.winPercentage = "-1"
    }

    // Getters
    fun getName(): String {
        return name
    }

    fun getWins(): Long {
        return wins
    }

    fun getLoses(): Long {
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
    fun setWins(wins: Long) {
        this.wins = wins
    }

    fun setLoses(loses: Long) {
        this.loses = loses
    }

    fun setBestScore(bestScore: Long) {
        this.bestScore = bestScore
    }

    fun setPosition(position: Int) {
        this.position = position
    }

    // Aux methods
    fun calculateWinPercentage() {
        if (wins < 0L || loses < 0L)
            return
        if (wins == 0L && loses == 0L)
            winPercentage = "0.00"
        else
            winPercentage = String.format("%.2f", (wins / (wins + loses).toFloat()) * 100)
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
        result = 31 * result + wins.toInt()
        result = 31 * result + loses.toInt()
        result = 31 * result + bestScore.hashCode()
        return result
    }

    override fun toString(): String {
        return name + " " + winPercentage
    }
}





/*class Player(name: String, wins: Long, loses: Long, bestScore: Long) {
    private val name: String
    private val wins: Long
    private val loses: Long
    private val bestScore: Long
    private val winPercentage: String
    private var position = 0

    init {
        this.name           = name
        this.wins           = wins
        this.loses          = loses
        this.bestScore      = bestScore
        this.winPercentage  = String.format("%.3f", wins / (wins + loses).toFloat())
    }

    // Getters
    fun getName(): String {
        return name
    }

    fun getWins(): Long {
        return wins
    }

    fun getLoses(): Long {
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
        result = 31 * result + wins.toInt()
        result = 31 * result + loses.toInt()
        result = 31 * result + bestScore.hashCode()
        return result
    }

    override fun toString(): String {
        return name + " " + winPercentage
    }

}*/