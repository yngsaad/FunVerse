package com.example.game

import com.example.model.GameDifficulty
import com.example.model.LevelConfig
import com.example.model.Position
import com.example.model.Tile
import com.example.model.TileColor
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.random.Random

/**
 * Result of checking or executing matches on the board.
 */
data class MatchDetection(
    val matchedPositions: Set<Position>,
    val horizontalMatchCount: Int,
    val verticalMatchCount: Int,
    val maxMatchLength: Int,
    val pointsAwarded: Int
)

class GameEngine {
    private val nextTileId = AtomicLong(1000)

    fun createInitialBoard(difficulty: GameDifficulty): Array<Array<Tile?>> {
        val size = difficulty.gridSize
        val availableColors = TileColor.entries.take(difficulty.colorCount)
        var board: Array<Array<Tile?>>

        var attempts = 0
        do {
            val candidate = Array(size) { arrayOfNulls<Tile>(size) }
            for (row in 0 until size) {
                for (col in 0 until size) {
                    val color = pickColorAvoidingMatches(candidate, row, col, availableColors)
                    candidate[row][col] = Tile(
                        id = nextTileId.incrementAndGet(),
                        color = color,
                        row = row,
                        col = col
                    )
                }
            }
            board = candidate
            attempts++
        } while (!hasValidMoves(board, size, availableColors) && attempts < 50)

        // If after 50 attempts still no moves (rare on 4x4), force a valid move setup
        if (!hasValidMoves(board, size, availableColors)) {
            ensureAtLeastOneValidMove(board, size, availableColors)
        }

        return board
    }

    fun createInitialBoardForLevel(config: LevelConfig): Array<Array<Tile?>> {
        val size = config.gridSize
        val availableColors = TileColor.entries.take(config.colorCount)
        var board: Array<Array<Tile?>>

        var attempts = 0
        do {
            val candidate = Array(size) { arrayOfNulls<Tile>(size) }
            for (row in 0 until size) {
                for (col in 0 until size) {
                    val color = pickColorAvoidingMatches(candidate, row, col, availableColors)
                    val hasIce = config.icePositions.contains(Position(row, col))
                    candidate[row][col] = Tile(
                        id = nextTileId.incrementAndGet(),
                        color = color,
                        row = row,
                        col = col,
                        hasIce = hasIce
                    )
                }
            }
            board = candidate
            attempts++
        } while (!hasValidMoves(board, size, availableColors) && attempts < 50)

        if (!hasValidMoves(board, size, availableColors)) {
            ensureAtLeastOneValidMove(board, size, availableColors)
        }

        return board
    }

    private fun pickColorAvoidingMatches(
        board: Array<Array<Tile?>>,
        row: Int,
        col: Int,
        availableColors: List<TileColor>
    ): TileColor {
        val disallowed = mutableSetOf<TileColor>()

        // Check horizontal preceding two tiles
        if (col >= 2) {
            val left1 = board[row][col - 1]?.color
            val left2 = board[row][col - 2]?.color
            if (left1 != null && left1 == left2) {
                disallowed.add(left1)
            }
        }

        // Check vertical preceding two tiles
        if (row >= 2) {
            val up1 = board[row - 1][col]?.color
            val up2 = board[row - 2][col]?.color
            if (up1 != null && up1 == up2) {
                disallowed.add(up1)
            }
        }

        val choices = availableColors.filter { it !in disallowed }
        return if (choices.isNotEmpty()) {
            choices.random()
        } else {
            availableColors.random()
        }
    }

    /**
     * Checks if two positions are directly adjacent (up, down, left, right).
     */
    fun isAdjacent(p1: Position, p2: Position): Boolean {
        return abs(p1.row - p2.row) + abs(p1.col - p2.col) == 1
    }

    /**
     * Checks if swapping p1 and p2 would produce a valid match.
     */
    fun canSwap(board: Array<Array<Tile?>>, p1: Position, p2: Position): Boolean {
        if (!isAdjacent(p1, p2)) return false

        val tile1 = board[p1.row][p1.col] ?: return false
        val tile2 = board[p2.row][p2.col] ?: return false

        // Temporarily swap
        board[p1.row][p1.col] = tile2.copy(row = p1.row, col = p1.col)
        board[p2.row][p2.col] = tile1.copy(row = p2.row, col = p2.col)

        val matches = findMatches(board, board.size)

        // Revert swap
        board[p1.row][p1.col] = tile1
        board[p2.row][p2.col] = tile2

        return matches.matchedPositions.isNotEmpty()
    }

    /**
     * Scans the entire board for horizontal and vertical lines of 3 or more matching colors.
     */
    fun findMatches(board: Array<Array<Tile?>>, size: Int): MatchDetection {
        val matchedPositions = mutableSetOf<Position>()
        var hMatches = 0
        var vMatches = 0
        var maxLength = 0

        // 1. Horizontal check
        for (r in 0 until size) {
            var matchStart = 0
            while (matchStart < size) {
                val currentColor = board[r][matchStart]?.color
                if (currentColor == null) {
                    matchStart++
                    continue
                }

                var matchEnd = matchStart + 1
                while (matchEnd < size && board[r][matchEnd]?.color == currentColor) {
                    matchEnd++
                }

                val runLength = matchEnd - matchStart
                if (runLength >= 3) {
                    hMatches++
                    maxLength = maxOf(maxLength, runLength)
                    for (c in matchStart until matchEnd) {
                        matchedPositions.add(Position(r, c))
                    }
                }
                matchStart = matchEnd
            }
        }

        // 2. Vertical check
        for (c in 0 until size) {
            var matchStart = 0
            while (matchStart < size) {
                val currentColor = board[matchStart][c]?.color
                if (currentColor == null) {
                    matchStart++
                    continue
                }

                var matchEnd = matchStart + 1
                while (matchEnd < size && board[matchEnd][c]?.color == currentColor) {
                    matchEnd++
                }

                val runLength = matchEnd - matchStart
                if (runLength >= 3) {
                    vMatches++
                    maxLength = maxOf(maxLength, runLength)
                    for (r in matchStart until matchEnd) {
                        matchedPositions.add(Position(r, c))
                    }
                }
                matchStart = matchEnd
            }
        }

        // Scoring: 10 points per matched tile + bonus for 4 or 5+ matches
        val basePoints = matchedPositions.size * 10
        val bonusPoints = when {
            maxLength >= 5 -> 80
            maxLength == 4 -> 30
            else -> 0
        }

        return MatchDetection(
            matchedPositions = matchedPositions,
            horizontalMatchCount = hMatches,
            verticalMatchCount = vMatches,
            maxMatchLength = maxLength,
            pointsAwarded = basePoints + bonusPoints
        )
    }

    /**
     * Breaks ice at or adjacent to matched positions and returns the number of broken ice tiles.
     */
    fun breakIce(board: Array<Array<Tile?>>, size: Int, matchedPositions: Set<Position>): Int {
        var brokenCount = 0
        val iceToBreak = mutableSetOf<Position>()

        for (pos in matchedPositions) {
            // Check the matched position itself
            if (board[pos.row][pos.col]?.hasIce == true) {
                iceToBreak.add(pos)
            }
            // Check 4 adjacent orthogonal neighbors
            val neighbors = listOf(
                Position(pos.row - 1, pos.col),
                Position(pos.row + 1, pos.col),
                Position(pos.row, pos.col - 1),
                Position(pos.row, pos.col + 1)
            )
            for (n in neighbors) {
                if (n.row in 0 until size && n.col in 0 until size) {
                    if (board[n.row][n.col]?.hasIce == true) {
                        iceToBreak.add(n)
                    }
                }
            }
        }

        for (pos in iceToBreak) {
            val tile = board[pos.row][pos.col]
            if (tile != null && tile.hasIce) {
                board[pos.row][pos.col] = tile.copy(hasIce = false)
                brokenCount++
            }
        }

        return brokenCount
    }

    /**
     * Counts how many tiles of each color were matched in the current step.
     */
    fun countMatchedColors(board: Array<Array<Tile?>>, matchedPositions: Set<Position>): Map<TileColor, Int> {
        val result = mutableMapOf<TileColor, Int>()
        for (pos in matchedPositions) {
            board[pos.row][pos.col]?.color?.let { color ->
                result[color] = (result[color] ?: 0) + 1
            }
        }
        return result
    }

    /**
     * Checks if there is at least one valid swap move remaining on the board.
     */
    fun hasValidMoves(
        board: Array<Array<Tile?>>,
        size: Int,
        availableColors: List<TileColor>
    ): Boolean {
        for (r in 0 until size) {
            for (c in 0 until size) {
                // Check right swap
                if (c + 1 < size) {
                    if (canSwap(board, Position(r, c), Position(r, c + 1))) {
                        return true
                    }
                }
                // Check down swap
                if (r + 1 < size) {
                    if (canSwap(board, Position(r, c), Position(r + 1, c))) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun ensureAtLeastOneValidMove(
        board: Array<Array<Tile?>>,
        size: Int,
        availableColors: List<TileColor>
    ) {
        val colorA = availableColors[0]
        val colorB = availableColors[1 % availableColors.size]
        // Create an explicit match-ready pattern: [A, A, B, A] in row 0
        if (size >= 4) {
            board[0][0] = Tile(nextTileId.incrementAndGet(), colorA, 0, 0)
            board[0][1] = Tile(nextTileId.incrementAndGet(), colorA, 0, 1)
            board[0][2] = Tile(nextTileId.incrementAndGet(), colorB, 0, 2)
            board[1][2] = Tile(nextTileId.incrementAndGet(), colorA, 1, 2)
        }
    }

    /**
     * Drops existing tiles down to fill gaps, and spawns new random tiles at the top.
     */
    fun applyGravityAndFill(
        board: Array<Array<Tile?>>,
        size: Int,
        availableColors: List<TileColor>
    ): Array<Array<Tile?>> {
        val newBoard = Array(size) { r ->
            Array(size) { c -> board[r][c] }
        }

        for (col in 0 until size) {
            // Collect all non-null tiles in this column from bottom to top
            val columnTiles = mutableListOf<Tile>()
            for (row in size - 1 downTo 0) {
                val t = newBoard[row][col]
                if (t != null) {
                    columnTiles.add(t)
                }
            }

            // Place existing tiles starting from the bottom
            var placeRow = size - 1
            for (tile in columnTiles) {
                newBoard[placeRow][col] = tile.copy(
                    row = placeRow,
                    col = col,
                    isMatched = false,
                    isSelected = false
                )
                placeRow--
            }

            // Fill remaining top slots with new random tiles
            while (placeRow >= 0) {
                newBoard[placeRow][col] = Tile(
                    id = nextTileId.incrementAndGet(),
                    color = availableColors.random(),
                    row = placeRow,
                    col = col,
                    isMatched = false,
                    isSelected = false
                )
                placeRow--
            }
        }

        return newBoard
    }

    /**
     * Reshuffles the board when no moves are left, ensuring a playable state.
     */
    fun reshuffleBoard(
        board: Array<Array<Tile?>>,
        difficulty: GameDifficulty
    ): Array<Array<Tile?>> {
        val size = difficulty.gridSize
        val availableColors = TileColor.entries.take(difficulty.colorCount)
        val allColors = mutableListOf<TileColor>()

        for (r in 0 until size) {
            for (c in 0 until size) {
                board[r][c]?.color?.let { allColors.add(it) }
            }
        }
        allColors.shuffle(Random)

        var newBoard: Array<Array<Tile?>>
        var attempts = 0
        do {
            var colorIdx = 0
            newBoard = Array(size) { row ->
                Array(size) { col ->
                    val color = if (colorIdx < allColors.size) {
                        allColors[colorIdx++]
                    } else {
                        availableColors.random()
                    }
                    Tile(
                        id = nextTileId.incrementAndGet(),
                        color = color,
                        row = row,
                        col = col
                    )
                }
            }
            attempts++
            // Ensure no auto matches and has valid moves
            val matches = findMatches(newBoard, size)
            if (matches.matchedPositions.isEmpty() && hasValidMoves(newBoard, size, availableColors)) {
                return newBoard
            }
        } while (attempts < 30)

        // Fallback to fresh clean initial board
        return createInitialBoard(difficulty)
    }
}
