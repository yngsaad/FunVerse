package com.example

import com.example.game.GameEngine
import com.example.game.LevelGenerator
import com.example.model.GameDifficulty
import com.example.model.Position
import com.example.model.Tile
import com.example.model.TileColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    private val engine = GameEngine()

    @Test
    fun testDifficultyGridAndMoveSpecs() {
        assertEquals(4, GameDifficulty.EASY.gridSize)
        assertEquals(30, GameDifficulty.EASY.initialMoves)
        assertEquals(4, GameDifficulty.EASY.colorCount)

        assertEquals(6, GameDifficulty.MEDIUM.gridSize)
        assertEquals(25, GameDifficulty.MEDIUM.initialMoves)
        assertEquals(5, GameDifficulty.MEDIUM.colorCount)

        assertEquals(8, GameDifficulty.HARD.gridSize)
        assertEquals(20, GameDifficulty.HARD.initialMoves)
        assertEquals(6, GameDifficulty.HARD.colorCount)
    }

    @Test
    fun testIsAdjacent() {
        assertTrue(engine.isAdjacent(Position(1, 1), Position(1, 2)))
        assertTrue(engine.isAdjacent(Position(1, 1), Position(2, 1)))
        assertTrue(engine.isAdjacent(Position(1, 1), Position(1, 0)))
        assertTrue(engine.isAdjacent(Position(1, 1), Position(0, 1)))

        assertFalse(engine.isAdjacent(Position(1, 1), Position(2, 2)))
        assertFalse(engine.isAdjacent(Position(1, 1), Position(1, 3)))
        assertFalse(engine.isAdjacent(Position(1, 1), Position(1, 1)))
    }

    @Test
    fun testInitialBoardHasNoMatches() {
        for (diff in GameDifficulty.entries) {
            val board = engine.createInitialBoard(diff)
            val matches = engine.findMatches(board, diff.gridSize)
            assertEquals("Initial board for ${diff.name} should have 0 matches", 0, matches.matchedPositions.size)
        }
    }

    @Test
    fun testHorizontalMatchDetection() {
        val size = 4
        val board: Array<Array<Tile?>> = Array(size) { r ->
            Array(size) { c ->
                Tile(id = (r * size + c).toLong(), color = TileColor.CYAN, row = r, col = c)
            }
        }
        val colors = listOf(TileColor.RUBY, TileColor.CYAN, TileColor.GOLD, TileColor.EMERALD)
        for (r in 0 until size) {
            for (c in 0 until size) {
                board[r][c] = Tile(id = (r * size + c).toLong(), color = colors[(r + c) % colors.size], row = r, col = c)
            }
        }
        // Place 3 Ruby in row 0
        board[0][0] = Tile(1, TileColor.RUBY, 0, 0)
        board[0][1] = Tile(2, TileColor.RUBY, 0, 1)
        board[0][2] = Tile(3, TileColor.RUBY, 0, 2)
        board[0][3] = Tile(4, TileColor.GOLD, 0, 3)

        val matches = engine.findMatches(board, size)
        assertTrue(matches.matchedPositions.contains(Position(0, 0)))
        assertTrue(matches.matchedPositions.contains(Position(0, 1)))
        assertTrue(matches.matchedPositions.contains(Position(0, 2)))
        assertFalse(matches.matchedPositions.contains(Position(0, 3)))
        assertEquals(30, matches.pointsAwarded) // 3 * 10
    }

    @Test
    fun testGravityAndFill() {
        val size = 4
        val availableColors = listOf(TileColor.RUBY, TileColor.CYAN)
        val board = Array(size) { r ->
            Array<Tile?>(size) { c ->
                Tile(id = (r * size + c).toLong(), color = TileColor.RUBY, row = r, col = c)
            }
        }

        // Null out bottom two tiles in column 0
        board[2][0] = null
        board[3][0] = null

        val newBoard = engine.applyGravityAndFill(board, size, availableColors)

        for (r in 0 until size) {
            for (c in 0 until size) {
                assertTrue("Board tile at ($r, $c) should not be null", newBoard[r][c] != null)
            }
        }
    }

    @Test
    fun testUnlimitedProceduralLevelsBeyond100() {
        val testLevels = listOf(1, 10, 25, 50, 75, 100, 105, 200, 500)
        for (lvl in testLevels) {
            val config = LevelGenerator.generate(lvl)
            assertEquals(lvl, config.levelNumber)
            assertTrue("Grid size must be between 4 and 8", config.gridSize in 4..8)
            assertTrue("Color count must be between 3 and 6", config.colorCount in 3..6)
            assertTrue("Moves must be fair (at least 18)", config.initialMoves >= 18)
            assertTrue("Target score must be positive", config.objective.targetScore > 0)

            // Test initial board creation for this level
            val board = engine.createInitialBoardForLevel(config)
            assertEquals(config.gridSize, board.size)
            val matches = engine.findMatches(board, config.gridSize)
            assertEquals("Initial board should start without pre-existing matches", 0, matches.matchedPositions.size)
        }
    }

    @Test
    fun testIceObstaclesAtLevels51Plus() {
        val configLvl55 = LevelGenerator.generate(55)
        assertTrue("Levels 51+ should generate ice obstacles", configLvl55.icePositions.isNotEmpty())
        assertTrue("Required ice to break should match count", configLvl55.objective.totalIceToBreak > 0)

        // Test ice breaking
        val size = 4
        val board: Array<Array<Tile?>> = Array(size) { r ->
            Array(size) { c ->
                Tile(id = (r * size + c).toLong(), color = TileColor.CYAN, row = r, col = c, hasIce = false)
            }
        }
        // Place ice at (1, 1)
        board[1][1] = Tile(99, TileColor.GOLD, 1, 1, hasIce = true)

        // Match at (1, 0) adjacent to (1, 1)
        val matched = setOf(Position(1, 0))
        val broken = engine.breakIce(board, size, matched)
        assertEquals(1, broken)
        assertFalse(board[1][1]?.hasIce ?: true)
    }
}
