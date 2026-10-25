package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdManager
import com.example.audio.SoundManager
import com.example.data.PreferencesManager
import com.example.game.GameEngine
import com.example.game.LevelGenerator
import com.example.model.FloatingScore
import com.example.model.GameDialog
import com.example.model.GameDifficulty
import com.example.model.GameMode
import com.example.model.GameScreen
import com.example.model.LevelConfig
import com.example.model.Position
import com.example.model.SwipeDirection
import com.example.model.Tile
import com.example.model.TileColor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

data class GameUiState(
    val currentScreen: GameScreen = GameScreen.MAIN_MENU,
    val activeDialog: GameDialog = GameDialog.NONE,
    val gameMode: GameMode = GameMode.JOURNEY,
    val difficulty: GameDifficulty = GameDifficulty.EASY,
    val board: List<List<Tile?>> = emptyList(),
    val selectedPosition: Position? = null,
    val matchedPositions: Set<Position> = emptySet(),
    val invalidSwapPair: Pair<Position, Position>? = null,
    val score: Int = 0,
    val highScore: Int = 0,
    val isNewHighScore: Boolean = false,
    val remainingMoves: Int = 30,
    val targetScore: Int = 500,
    val level: Int = 1,
    val tierTitle: String = "",
    val comboCount: Int = 0,
    val isProcessingMove: Boolean = false,
    val isReshuffling: Boolean = false,
    val floatingScores: List<FloatingScore> = emptyList(),
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val bestScoresByDifficulty: Map<GameDifficulty, Int> = emptyMap(),
    // Journey Mode specific state
    val highestUnlockedLevel: Int = 1,
    val totalStars: Int = 0,
    val levelStarsMap: Map<Int, Int> = emptyMap(),
    val currentLevelConfig: LevelConfig? = null,
    val requiredColors: Map<TileColor, Int> = emptyMap(),
    val collectedColors: Map<TileColor, Int> = emptyMap(),
    val requiredIce: Int = 0,
    val brokenIce: Int = 0
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val soundManager = SoundManager(application)
    private val engine = GameEngine()
    private val adManager = AdManager(prefs)
    private val floatingScoreIdGen = AtomicLong(1)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var currentBoardArray: Array<Array<Tile?>> = emptyArray()
    private var moveProcessingJob: Job? = null

    init {
        loadPreferencesState()
    }

    private fun loadPreferencesState() {
        val savedSound = prefs.isSoundEnabled
        val savedHaptics = prefs.isHapticsEnabled
        val savedDiff = prefs.selectedDifficulty
        val highestLvl = prefs.highestUnlockedLevel
        val totalStars = prefs.getTotalStars()

        soundManager.isSoundEnabled = savedSound
        soundManager.isHapticsEnabled = savedHaptics

        val bestScores = GameDifficulty.entries.associateWith { prefs.getBestScore(it) }
        val starsMap = (1..highestLvl).associateWith { prefs.getLevelStars(it) }

        _uiState.update {
            it.copy(
                soundEnabled = savedSound,
                hapticsEnabled = savedHaptics,
                difficulty = savedDiff,
                highScore = bestScores[savedDiff] ?: 0,
                bestScoresByDifficulty = bestScores,
                highestUnlockedLevel = highestLvl,
                totalStars = totalStars,
                levelStarsMap = starsMap
            )
        }
    }

    fun startJourneyLevel(level: Int = prefs.highestUnlockedLevel) {
        moveProcessingJob?.cancel()
        val config = LevelGenerator.generate(level)
        currentBoardArray = engine.createInitialBoardForLevel(config)

        _uiState.update {
            it.copy(
                currentScreen = GameScreen.GAMEPLAY,
                activeDialog = GameDialog.NONE,
                gameMode = GameMode.JOURNEY,
                level = config.levelNumber,
                tierTitle = config.tierTitle,
                board = currentBoardArray.map { row -> row.toList() },
                selectedPosition = null,
                matchedPositions = emptySet(),
                invalidSwapPair = null,
                score = 0,
                highScore = prefs.getBestScore(_uiState.value.difficulty),
                isNewHighScore = false,
                remainingMoves = config.initialMoves,
                targetScore = config.objective.targetScore,
                requiredColors = config.objective.requiredColors,
                collectedColors = emptyMap(),
                requiredIce = config.objective.totalIceToBreak,
                brokenIce = 0,
                currentLevelConfig = config,
                comboCount = 0,
                isProcessingMove = false,
                isReshuffling = false,
                floatingScores = emptyList()
            )
        }
        prefs.incrementGamesPlayed()
    }

    fun selectDifficulty(difficulty: GameDifficulty) {
        prefs.selectedDifficulty = difficulty
        val bestScore = prefs.getBestScore(difficulty)
        _uiState.update {
            it.copy(
                difficulty = difficulty,
                highScore = bestScore
            )
        }
    }

    fun startNewGame(difficulty: GameDifficulty = _uiState.value.difficulty) {
        moveProcessingJob?.cancel()
        selectDifficulty(difficulty)

        currentBoardArray = engine.createInitialBoard(difficulty)
        val bestScore = prefs.getBestScore(difficulty)

        _uiState.update {
            it.copy(
                currentScreen = GameScreen.GAMEPLAY,
                activeDialog = GameDialog.NONE,
                gameMode = GameMode.ARCADE,
                difficulty = difficulty,
                board = currentBoardArray.map { row -> row.toList() },
                selectedPosition = null,
                matchedPositions = emptySet(),
                invalidSwapPair = null,
                score = 0,
                highScore = bestScore,
                isNewHighScore = false,
                remainingMoves = difficulty.initialMoves,
                targetScore = difficulty.targetScore,
                tierTitle = "${difficulty.title} Arcade",
                level = 1,
                requiredColors = emptyMap(),
                collectedColors = emptyMap(),
                requiredIce = 0,
                brokenIce = 0,
                currentLevelConfig = null,
                comboCount = 0,
                isProcessingMove = false,
                isReshuffling = false,
                floatingScores = emptyList()
            )
        }
        prefs.incrementGamesPlayed()
    }

    fun onTileClick(row: Int, col: Int) {
        val state = _uiState.value
        if (state.isProcessingMove || state.activeDialog != GameDialog.NONE) {
            return
        }

        val clickedPos = Position(row, col)
        val currentSelected = state.selectedPosition

        if (currentSelected == null) {
            _uiState.update { it.copy(selectedPosition = clickedPos) }
            soundManager.playTap()
        } else if (currentSelected == clickedPos) {
            _uiState.update { it.copy(selectedPosition = null) }
        } else {
            if (engine.isAdjacent(currentSelected, clickedPos)) {
                attemptSwap(currentSelected, clickedPos)
            } else {
                _uiState.update { it.copy(selectedPosition = clickedPos) }
                soundManager.playTap()
            }
        }
    }

    fun onTileSwipe(row: Int, col: Int, direction: SwipeDirection) {
        val state = _uiState.value
        if (state.isProcessingMove || state.activeDialog != GameDialog.NONE) {
            return
        }

        val targetRow = when (direction) {
            SwipeDirection.UP -> row - 1
            SwipeDirection.DOWN -> row + 1
            SwipeDirection.LEFT, SwipeDirection.RIGHT -> row
        }
        val targetCol = when (direction) {
            SwipeDirection.LEFT -> col - 1
            SwipeDirection.RIGHT -> col + 1
            SwipeDirection.UP, SwipeDirection.DOWN -> col
        }

        val size = if (state.gameMode == GameMode.JOURNEY) {
            state.currentLevelConfig?.gridSize ?: state.difficulty.gridSize
        } else {
            state.difficulty.gridSize
        }

        if (targetRow in 0 until size && targetCol in 0 until size) {
            val sourcePos = Position(row, col)
            val targetPos = Position(targetRow, targetCol)
            _uiState.update { it.copy(selectedPosition = null) }
            attemptSwap(sourcePos, targetPos)
        }
    }

    private fun attemptSwap(p1: Position, p2: Position) {
        val state = _uiState.value
        val isValid = engine.canSwap(currentBoardArray, p1, p2)

        if (!isValid) {
            soundManager.playInvalid()
            _uiState.update {
                it.copy(
                    selectedPosition = null,
                    invalidSwapPair = Pair(p1, p2),
                    isProcessingMove = true
                )
            }
            viewModelScope.launch {
                delay(300)
                _uiState.update {
                    it.copy(
                        invalidSwapPair = null,
                        isProcessingMove = false
                    )
                }
            }
            return
        }

        soundManager.playSwap()
        val tile1 = currentBoardArray[p1.row][p1.col]!!
        val tile2 = currentBoardArray[p2.row][p2.col]!!

        currentBoardArray[p1.row][p1.col] = tile2.copy(row = p1.row, col = p1.col)
        currentBoardArray[p2.row][p2.col] = tile1.copy(row = p2.row, col = p2.col)

        val newMovesLeft = (state.remainingMoves - 1).coerceAtLeast(0)

        _uiState.update {
            it.copy(
                board = currentBoardArray.map { row -> row.toList() },
                selectedPosition = null,
                remainingMoves = newMovesLeft,
                isProcessingMove = true,
                comboCount = 0
            )
        }

        moveProcessingJob = viewModelScope.launch {
            processCascadingMatches()
        }
    }

    private suspend fun processCascadingMatches() {
        val state = _uiState.value
        val size = if (state.gameMode == GameMode.JOURNEY) {
            state.currentLevelConfig?.gridSize ?: state.difficulty.gridSize
        } else {
            state.difficulty.gridSize
        }
        val availableColors = if (state.gameMode == GameMode.JOURNEY) {
            TileColor.entries.take(state.currentLevelConfig?.colorCount ?: 4)
        } else {
            TileColor.entries.take(state.difficulty.colorCount)
        }
        var cascadeStep = 0

        while (true) {
            val matches = engine.findMatches(currentBoardArray, size)
            if (matches.matchedPositions.isEmpty()) {
                break
            }

            cascadeStep++
            val comboMultiplier = if (cascadeStep > 1) 1.0 + (cascadeStep - 1) * 0.5 else 1.0
            val stepPoints = (matches.pointsAwarded * comboMultiplier).toInt()

            soundManager.playMatch(cascadeStep, matches.maxMatchLength)

            // Break ice and count matched colors
            val newlyBrokenIce = engine.breakIce(currentBoardArray, size, matches.matchedPositions)
            val stepColors = engine.countMatchedColors(currentBoardArray, matches.matchedPositions)

            _uiState.update { curState ->
                val newScore = curState.score + stepPoints
                val isNewBest = prefs.recordScore(curState.difficulty, newScore)
                val currentBest = if (isNewBest) newScore else curState.highScore

                val updatedCollectedColors = curState.collectedColors.toMutableMap()
                for ((color, count) in stepColors) {
                    updatedCollectedColors[color] = (updatedCollectedColors[color] ?: 0) + count
                }
                val updatedBrokenIce = curState.brokenIce + newlyBrokenIce

                val anchor = matches.matchedPositions.firstOrNull() ?: Position(0, 0)
                val floatText = if (cascadeStep > 1) "COMBO x$cascadeStep! +$stepPoints" else "+$stepPoints"
                val newFloat = FloatingScore(
                    id = floatingScoreIdGen.incrementAndGet(),
                    text = floatText,
                    row = anchor.row,
                    col = anchor.col,
                    isCombo = cascadeStep > 1
                )

                curState.copy(
                    matchedPositions = matches.matchedPositions,
                    score = newScore,
                    highScore = currentBest,
                    isNewHighScore = curState.isNewHighScore || isNewBest,
                    comboCount = cascadeStep,
                    floatingScores = curState.floatingScores + newFloat,
                    collectedColors = updatedCollectedColors,
                    brokenIce = updatedBrokenIce
                )
            }

            delay(280)

            // Remove matched tiles
            for (pos in matches.matchedPositions) {
                currentBoardArray[pos.row][pos.col] = null
            }

            // Drop tiles & fill new
            currentBoardArray = engine.applyGravityAndFill(currentBoardArray, size, availableColors)

            _uiState.update {
                it.copy(
                    board = currentBoardArray.map { row -> row.toList() },
                    matchedPositions = emptySet()
                )
            }

            delay(260)
        }

        delay(300)
        _uiState.update { it.copy(floatingScores = emptyList()) }

        checkGameEndConditions()
    }

    private suspend fun checkGameEndConditions() {
        val state = _uiState.value

        // Check if objective satisfied
        val isLevelCleared = if (state.gameMode == GameMode.JOURNEY && state.currentLevelConfig != null) {
            state.currentLevelConfig.objective.isSatisfied(
                score = state.score,
                collectedColors = state.collectedColors,
                brokenIce = state.brokenIce
            )
        } else {
            state.score >= state.targetScore
        }

        if (isLevelCleared) {
            soundManager.playVictory()
            // Save level completion and stars in Journey mode
            if (state.gameMode == GameMode.JOURNEY) {
                val stars = when {
                    state.remainingMoves >= 8 -> 3
                    state.remainingMoves >= 3 -> 2
                    else -> 1
                }
                prefs.saveLevelCompleted(state.level, stars)
                loadPreferencesState()
            }

            _uiState.update {
                it.copy(
                    activeDialog = GameDialog.LEVEL_COMPLETE,
                    isProcessingMove = false
                )
            }
            return
        }

        // Check if out of moves
        if (state.remainingMoves <= 0) {
            soundManager.playGameOver()
            _uiState.update {
                it.copy(
                    activeDialog = GameDialog.GAME_OVER,
                    isProcessingMove = false
                )
            }
            return
        }

        // Check for board deadlock
        val availableColors = if (state.gameMode == GameMode.JOURNEY) {
            TileColor.entries.take(state.currentLevelConfig?.colorCount ?: 4)
        } else {
            TileColor.entries.take(state.difficulty.colorCount)
        }
        val size = if (state.gameMode == GameMode.JOURNEY) {
            state.currentLevelConfig?.gridSize ?: state.difficulty.gridSize
        } else {
            state.difficulty.gridSize
        }

        if (!engine.hasValidMoves(currentBoardArray, size, availableColors)) {
            _uiState.update { it.copy(isReshuffling = true) }
            soundManager.playReshuffle()
            delay(600)
            currentBoardArray = engine.reshuffleBoard(currentBoardArray, state.difficulty)
            _uiState.update {
                it.copy(
                    board = currentBoardArray.map { row -> row.toList() },
                    isReshuffling = false
                )
            }
        }

        _uiState.update { it.copy(isProcessingMove = false) }
    }

    fun onNextLevelRequested() {
        val currentLvl = _uiState.value.level
        val shouldShowAd = adManager.shouldShowAdAfterLevel(currentLvl)

        if (shouldShowAd) {
            _uiState.update { it.copy(activeDialog = GameDialog.INTERSTITIAL_AD) }
        } else {
            advanceToNextLevel()
        }
    }

    fun onAdClosed() {
        advanceToNextLevel()
    }

    private fun advanceToNextLevel() {
        val state = _uiState.value
        if (state.gameMode == GameMode.JOURNEY) {
            startJourneyLevel(state.level + 1)
        } else {
            val nextLvl = state.level + 1
            val newTarget = state.targetScore + (state.difficulty.targetScore * 1.2).toInt()
            val bonusMoves = when (state.difficulty) {
                GameDifficulty.EASY -> 15
                GameDifficulty.MEDIUM -> 12
                GameDifficulty.HARD -> 10
            }
            val newMoves = state.remainingMoves + bonusMoves
            currentBoardArray = engine.createInitialBoard(state.difficulty)

            _uiState.update {
                it.copy(
                    activeDialog = GameDialog.NONE,
                    level = nextLvl,
                    targetScore = newTarget,
                    remainingMoves = newMoves,
                    board = currentBoardArray.map { row -> row.toList() },
                    selectedPosition = null,
                    matchedPositions = emptySet(),
                    floatingScores = emptyList(),
                    isProcessingMove = false
                )
            }
        }
    }

    fun pauseGame() {
        if (_uiState.value.activeDialog == GameDialog.NONE) {
            _uiState.update { it.copy(activeDialog = GameDialog.PAUSE) }
        }
    }

    fun resumeGame() {
        _uiState.update { it.copy(activeDialog = GameDialog.NONE) }
    }

    fun restartCurrentGame() {
        if (_uiState.value.gameMode == GameMode.JOURNEY) {
            startJourneyLevel(_uiState.value.level)
        } else {
            startNewGame(_uiState.value.difficulty)
        }
    }

    fun navigateTo(screen: GameScreen) {
        loadPreferencesState()
        _uiState.update {
            it.copy(
                currentScreen = screen,
                activeDialog = GameDialog.NONE
            )
        }
    }

    fun toggleSound() {
        val newVal = !_uiState.value.soundEnabled
        prefs.isSoundEnabled = newVal
        soundManager.isSoundEnabled = newVal
        _uiState.update { it.copy(soundEnabled = newVal) }
        if (newVal) {
            soundManager.playTap()
        }
    }

    fun toggleHaptics() {
        val newVal = !_uiState.value.hapticsEnabled
        prefs.isHapticsEnabled = newVal
        soundManager.isHapticsEnabled = newVal
        _uiState.update { it.copy(hapticsEnabled = newVal) }
    }
}
