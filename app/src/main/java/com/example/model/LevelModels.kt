package com.example.model

/**
 * Game Mode:
 * JOURNEY - Unlimited progressive levels with objectives, ice obstacles, and star progression.
 * ARCADE - Classic quick game with Easy, Medium, or Hard difficulty.
 */
enum class GameMode {
    JOURNEY,
    ARCADE
}

/**
 * Specific objectives for a level.
 */
data class LevelObjective(
    val targetScore: Int,
    val requiredColors: Map<TileColor, Int> = emptyMap(),
    val totalIceToBreak: Int = 0
) {
    fun isSatisfied(score: Int, collectedColors: Map<TileColor, Int>, brokenIce: Int): Boolean {
        if (score < targetScore) return false
        if (totalIceToBreak > 0 && brokenIce < totalIceToBreak) return false
        for ((color, reqCount) in requiredColors) {
            val collected = collectedColors[color] ?: 0
            if (collected < reqCount) return false
        }
        return true
    }
}

/**
 * Full configuration for any procedural level.
 */
data class LevelConfig(
    val levelNumber: Int,
    val gridSize: Int,
    val colorCount: Int,
    val initialMoves: Int,
    val objective: LevelObjective,
    val icePositions: Set<Position> = emptySet(),
    val tierTitle: String
)
