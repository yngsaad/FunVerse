package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.GameScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.HowToPlayScreen
import com.example.ui.screens.LevelJourneyScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = BackgroundDark,
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    FunVerseApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun FunVerseApp(
    modifier: Modifier = Modifier,
    viewModel: GameViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (uiState.currentScreen) {
        GameScreen.MAIN_MENU -> {
            MainMenuScreen(
                highestUnlockedLevel = uiState.highestUnlockedLevel,
                totalStars = uiState.totalStars,
                selectedDifficulty = uiState.difficulty,
                bestScores = uiState.bestScoresByDifficulty,
                soundEnabled = uiState.soundEnabled,
                hapticsEnabled = uiState.hapticsEnabled,
                onContinueJourneyClick = { viewModel.startJourneyLevel() },
                onOpenJourneyMapClick = { viewModel.navigateTo(GameScreen.LEVEL_JOURNEY) },
                onSelectDifficulty = { diff -> viewModel.selectDifficulty(diff) },
                onPlayArcadeClick = { viewModel.startNewGame() },
                onHowToPlayClick = { viewModel.navigateTo(GameScreen.HOW_TO_PLAY) },
                onToggleSound = { viewModel.toggleSound() },
                onToggleHaptics = { viewModel.toggleHaptics() },
                modifier = modifier
            )
        }
        GameScreen.LEVEL_JOURNEY -> {
            LevelJourneyScreen(
                highestUnlockedLevel = uiState.highestUnlockedLevel,
                levelStarsMap = uiState.levelStarsMap,
                totalStars = uiState.totalStars,
                onLevelSelected = { lvl -> viewModel.startJourneyLevel(lvl) },
                onBackClick = { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                modifier = modifier
            )
        }
        GameScreen.GAMEPLAY -> {
            GameplayScreen(
                uiState = uiState,
                onTileClick = { row, col -> viewModel.onTileClick(row, col) },
                onTileSwipe = { row, col, dir -> viewModel.onTileSwipe(row, col, dir) },
                onPauseClick = { viewModel.pauseGame() },
                onResumeClick = { viewModel.resumeGame() },
                onRestartClick = { viewModel.restartCurrentGame() },
                onNextLevelClick = { viewModel.onNextLevelRequested() },
                onAdClosed = { viewModel.onAdClosed() },
                onToggleSound = { viewModel.toggleSound() },
                onQuitToMenu = { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                modifier = modifier
            )
        }
        GameScreen.HOW_TO_PLAY -> {
            HowToPlayScreen(
                onBackClick = { viewModel.navigateTo(GameScreen.MAIN_MENU) },
                modifier = modifier
            )
        }
    }
}
