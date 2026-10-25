package com.example.model

import androidx.compose.ui.graphics.Color

/**
 * Difficulty levels for FunVerse: Color Match.
 */
enum class GameDifficulty(
    val title: String,
    val gridSize: Int,
    val colorCount: Int,
    val initialMoves: Int,
    val targetScore: Int,
    val description: String
) {
    EASY(
        title = "Easy",
        gridSize = 4,
        colorCount = 4,
        initialMoves = 30,
        targetScore = 500,
        description = "4×4 grid • 4 colors • 30 moves • Relaxed introductory pace"
    ),
    MEDIUM(
        title = "Medium",
        gridSize = 6,
        colorCount = 5,
        initialMoves = 25,
        targetScore = 1200,
        description = "6×6 grid • 5 colors • 25 moves • Moderate challenge"
    ),
    HARD(
        title = "Hard",
        gridSize = 8,
        colorCount = 6,
        initialMoves = 20,
        targetScore = 2000,
        description = "8×8 grid • 6 colors • 20 moves • High score target"
    )
}

/**
 * Tile colors with high contrast, distinctive tones, and associated geometric badge shapes
 * for high visual richness and colorblind accessibility.
 */
enum class TileColor(
    val id: Int,
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val glowColor: Color,
    val shapeType: TileShapeType
) {
    RUBY(
        id = 0,
        displayName = "Ruby",
        primaryColor = Color(0xFFFF2A6D),
        secondaryColor = Color(0xFFC70039),
        glowColor = Color(0x66FF2A6D),
        shapeType = TileShapeType.DIAMOND
    ),
    CYAN(
        id = 1,
        displayName = "Cyan",
        primaryColor = Color(0xFF05D9E8),
        secondaryColor = Color(0xFF009AA6),
        glowColor = Color(0x6605D9E8),
        shapeType = TileShapeType.HEXAGON
    ),
    GOLD(
        id = 2,
        displayName = "Gold",
        primaryColor = Color(0xFFFFD166),
        secondaryColor = Color(0xFFE09F00),
        glowColor = Color(0x66FFD166),
        shapeType = TileShapeType.STAR
    ),
    EMERALD(
        id = 3,
        displayName = "Emerald",
        primaryColor = Color(0xFF06D6A0),
        secondaryColor = Color(0xFF029E75),
        glowColor = Color(0x6606D6A0),
        shapeType = TileShapeType.CIRCLE
    ),
    AMETHYST(
        id = 4,
        displayName = "Amethyst",
        primaryColor = Color(0xFFA646EE),
        secondaryColor = Color(0xFF791EBE),
        glowColor = Color(0x66A646EE),
        shapeType = TileShapeType.HEART
    ),
    TANGERINE(
        id = 5,
        displayName = "Tangerine",
        primaryColor = Color(0xFFFF7A00),
        secondaryColor = Color(0xFFCC5200),
        glowColor = Color(0x66FF7A00),
        shapeType = TileShapeType.TRIANGLE
    )
}

enum class TileShapeType {
    DIAMOND,
    HEXAGON,
    STAR,
    CIRCLE,
    HEART,
    TRIANGLE
}

/**
 * Represents a position on the grid.
 */
data class Position(val row: Int, val col: Int)

/**
 * Direction for swipe gestures.
 */
enum class SwipeDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

/**
 * Represents an individual game tile.
 */
data class Tile(
    val id: Long,
    val color: TileColor,
    val row: Int,
    val col: Int,
    val isMatched: Boolean = false,
    val isSelected: Boolean = false,
    val hasIce: Boolean = false
)

/**
 * Floating score popup notification.
 */
data class FloatingScore(
    val id: Long,
    val text: String,
    val row: Int,
    val col: Int,
    val isCombo: Boolean = false
)

/**
 * Navigation screen routes.
 */
enum class GameScreen {
    MAIN_MENU,
    LEVEL_JOURNEY,
    GAMEPLAY,
    HOW_TO_PLAY
}

/**
 * Modal dialog state within the game.
 */
enum class GameDialog {
    NONE,
    PAUSE,
    LEVEL_COMPLETE,
    GAME_OVER,
    INTERSTITIAL_AD
}
