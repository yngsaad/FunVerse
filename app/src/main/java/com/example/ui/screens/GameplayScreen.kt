package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameDialog
import com.example.model.GameMode
import com.example.model.SwipeDirection
import com.example.ui.components.GameBoardView
import com.example.ui.components.ScoreBoard
import com.example.ui.dialogs.GameOverDialog
import com.example.ui.dialogs.InterstitialAdDialog
import com.example.ui.dialogs.LevelCompleteDialog
import com.example.ui.dialogs.PauseDialog
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.GameUiState

@Composable
fun GameplayScreen(
    uiState: GameUiState,
    onTileClick: (row: Int, col: Int) -> Unit,
    onTileSwipe: (row: Int, col: Int, direction: SwipeDirection) -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRestartClick: () -> Unit,
    onNextLevelClick: () -> Unit,
    onAdClosed: () -> Unit,
    onToggleSound: () -> Unit,
    onQuitToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept hardware back button to show pause menu
    BackHandler {
        if (uiState.activeDialog == GameDialog.NONE) {
            onPauseClick()
        } else if (uiState.activeDialog == GameDialog.PAUSE) {
            onResumeClick()
        } else {
            onQuitToMenu()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF140D36),
                        BackgroundDark,
                        Color(0xFF090618)
                    )
                )
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 500.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Pause, Title & Mode/Level Badge, Sound Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause button
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, CardBorderDark, CircleShape)
                        .testTag("game_pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = TextPrimary
                    )
                }

                // Title and Mode Pill
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "FunVerse",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33282159))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        val badgeText = if (uiState.gameMode == GameMode.JOURNEY) {
                            "JOURNEY • LEVEL ${uiState.level}"
                        } else {
                            "${uiState.difficulty.title.uppercase()} ARCADE"
                        }
                        Text(
                            text = badgeText,
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Sound Toggle button
                IconButton(
                    onClick = onToggleSound,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.dp, CardBorderDark, CircleShape)
                        .testTag("gameplay_sound_toggle")
                ) {
                    Icon(
                        imageVector = if (uiState.soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                        contentDescription = "Toggle Sound",
                        tint = if (uiState.soundEnabled) NeonPink else TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Score & Stats HUD
            ScoreBoard(
                score = uiState.score,
                highScore = uiState.highScore,
                movesLeft = uiState.remainingMoves,
                targetScore = uiState.targetScore,
                level = uiState.level,
                comboCount = uiState.comboCount,
                tierTitle = uiState.tierTitle,
                requiredColors = uiState.requiredColors,
                collectedColors = uiState.collectedColors,
                requiredIce = uiState.requiredIce,
                brokenIce = uiState.brokenIce
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Game Board
            GameBoardView(
                board = uiState.board,
                difficulty = uiState.difficulty,
                selectedPosition = uiState.selectedPosition,
                matchedPositions = uiState.matchedPositions,
                invalidSwapPair = uiState.invalidSwapPair,
                floatingScores = uiState.floatingScores,
                isReshuffling = uiState.isReshuffling,
                onTileClick = onTileClick,
                onTileSwipe = onTileSwipe,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Footer hint
            Text(
                text = "Swipe any tile towards a neighbor to match 3+",
                color = TextSecondary.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(6.dp))
        }

        // Active Dialogs Overlay
        when (uiState.activeDialog) {
            GameDialog.PAUSE -> {
                PauseDialog(
                    soundEnabled = uiState.soundEnabled,
                    onResume = onResumeClick,
                    onRestart = onRestartClick,
                    onToggleSound = onToggleSound,
                    onQuitToMenu = onQuitToMenu
                )
            }
            GameDialog.LEVEL_COMPLETE -> {
                LevelCompleteDialog(
                    level = uiState.level,
                    score = uiState.score,
                    movesRemaining = uiState.remainingMoves,
                    targetScore = uiState.targetScore,
                    onNextLevel = onNextLevelClick,
                    onPlayAgain = onRestartClick,
                    onQuitToMenu = onQuitToMenu
                )
            }
            GameDialog.GAME_OVER -> {
                GameOverDialog(
                    score = uiState.score,
                    highScore = uiState.highScore,
                    isNewRecord = uiState.isNewHighScore,
                    onPlayAgain = onRestartClick,
                    onQuitToMenu = onQuitToMenu
                )
            }
            GameDialog.INTERSTITIAL_AD -> {
                InterstitialAdDialog(
                    onAdClosed = onAdClosed
                )
            }
            GameDialog.NONE -> {
                // No dialog
            }
        }
    }
}
