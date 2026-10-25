package com.example.game

import com.example.model.LevelConfig
import com.example.model.LevelObjective
import com.example.model.Position
import com.example.model.TileColor
import kotlin.random.Random

/**
 * Deterministic procedural generator for unlimited levels.
 * Seamlessly supports Level 1 to 100+ with smooth, fair difficulty scaling.
 */
object LevelGenerator {

    fun generate(level: Int): LevelConfig {
        val lvl = level.coerceAtLeast(1)
        val rng = Random(lvl * 4099L + 17L)

        val tierTitle: String
        val gridSize: Int
        val colorCount: Int
        val moves: Int
        val targetScore: Int
        val reqColors = mutableMapOf<TileColor, Int>()
        var icePositions = emptySet<Position>()

        when {
            lvl <= 10 -> {
                tierTitle = "Beginner • Levels 1–10"
                gridSize = if (lvl <= 5) 4 else 5
                colorCount = if (lvl <= 4) 3 else 4
                moves = (30 - (lvl * 0.4)).toInt().coerceIn(26, 30)
                targetScore = 250 + lvl * 45
                if (lvl in 6..10) {
                    val color = TileColor.entries[lvl % colorCount]
                    reqColors[color] = 6 + (lvl - 5)
                }
            }
            lvl <= 25 -> {
                tierTitle = "Novice • Levels 11–25"
                gridSize = if (lvl <= 18) 5 else 6
                colorCount = if (lvl <= 20) 4 else 5
                moves = (26 - ((lvl - 10) * 0.25)).toInt().coerceIn(22, 26)
                targetScore = 700 + (lvl - 10) * 55
                val c1 = TileColor.entries[(lvl * 2) % colorCount]
                reqColors[c1] = 10 + (lvl - 10)
            }
            lvl <= 50 -> {
                tierTitle = "Challenger • Levels 26–50"
                gridSize = if (lvl <= 38) 6 else 7
                colorCount = 5
                moves = (24 - ((lvl - 25) * 0.16)).toInt().coerceIn(20, 24)
                targetScore = 1350 + (lvl - 25) * 45
                val c1 = TileColor.entries[(lvl * 3) % colorCount]
                val c2 = TileColor.entries[(lvl * 3 + 1) % colorCount]
                reqColors[c1] = 12 + (lvl - 25) / 2
                reqColors[c2] = 12 + (lvl - 25) / 2
            }
            lvl <= 100 -> {
                tierTitle = "Master • Levels 51–100"
                gridSize = if (lvl <= 75) 7 else 8
                colorCount = if (lvl <= 70) 5 else 6
                moves = (22 - ((lvl - 50) * 0.1)).toInt().coerceIn(18, 22)
                targetScore = 2300 + (lvl - 50) * 40

                val iceCount = (4 + (lvl - 50) / 6).coerceIn(4, 12)
                icePositions = generateIcePositions(gridSize, iceCount, rng)

                val c1 = TileColor.entries[(lvl * 2) % colorCount]
                val c2 = TileColor.entries[(lvl * 2 + 1) % colorCount]
                reqColors[c1] = 14 + (lvl - 50) / 4
                reqColors[c2] = 14 + (lvl - 50) / 4
            }
            else -> {
                val tierNum = (lvl - 1) / 25 + 1
                tierTitle = "Legend • Tier $tierNum (Level $lvl)"
                gridSize = 8
                colorCount = 6
                moves = (20 + (lvl % 5) - (lvl / 200)).coerceIn(18, 23)
                targetScore = 3800 + (lvl - 100) * 35

                val iceCount = (6 + (lvl % 7)).coerceIn(6, 12)
                icePositions = generateIcePositions(gridSize, iceCount, rng)

                val c1 = TileColor.entries[(lvl * 5) % colorCount]
                val c2 = TileColor.entries[(lvl * 5 + 2) % colorCount]
                reqColors[c1] = 16 + (lvl % 6)
                reqColors[c2] = 16 + ((lvl + 3) % 6)
            }
        }

        return LevelConfig(
            levelNumber = lvl,
            gridSize = gridSize,
            colorCount = colorCount,
            initialMoves = moves,
            objective = LevelObjective(
                targetScore = targetScore,
                requiredColors = reqColors,
                totalIceToBreak = icePositions.size
            ),
            icePositions = icePositions,
            tierTitle = tierTitle
        )
    }

    private fun generateIcePositions(gridSize: Int, count: Int, rng: Random): Set<Position> {
        val result = mutableSetOf<Position>()
        val minR = 1
        val maxR = gridSize - 2
        val candidates = mutableListOf<Position>()
        for (r in minR..maxR) {
            for (c in minR..maxR) {
                candidates.add(Position(r, c))
            }
        }
        candidates.shuffle(rng)
        for (pos in candidates) {
            if (result.size >= count) break
            result.add(pos)
        }
        return result
    }
}
