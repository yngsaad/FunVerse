package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FloatingScore
import com.example.model.GameDifficulty
import com.example.model.Position
import com.example.model.SwipeDirection
import com.example.model.Tile
import com.example.ui.theme.BoardBackground
import com.example.ui.theme.CardBorderDark
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGold
import com.example.ui.theme.NeonPink
import com.example.ui.theme.ScoreGold
import com.example.ui.theme.TileSlotDark

@Composable
fun GameBoardView(
    board: List<List<Tile?>>,
    difficulty: GameDifficulty,
    selectedPosition: Position?,
    matchedPositions: Set<Position>,
    invalidSwapPair: Pair<Position, Position>?,
    floatingScores: List<FloatingScore>,
    isReshuffling: Boolean,
    onTileClick: (row: Int, col: Int) -> Unit,
    onTileSwipe: (row: Int, col: Int, direction: SwipeDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridSize = if (board.isNotEmpty()) board.size else difficulty.gridSize

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val boardSize = minOf(maxWidth, maxHeight)

        Box(
            modifier = Modifier
                .size(boardSize)
                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color(0x66000000), spotColor = Color(0x66A646EE))
                .clip(RoundedCornerShape(24.dp))
                .background(BoardBackground)
                .border(2.dp, CardBorderDark, RoundedCornerShape(24.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Render grid rows and columns
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                for (r in 0 until gridSize) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        for (c in 0 until gridSize) {
                            val tile = board.getOrNull(r)?.getOrNull(c)
                            val isSelected = selectedPosition?.row == r && selectedPosition.col == c
                            val isMatched = matchedPositions.contains(Position(r, c))
                            val isInvalid = invalidSwapPair?.let {
                                (it.first.row == r && it.first.col == c) || (it.second.row == r && it.second.col == c)
                            } ?: false

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TileSlotDark),
                                contentAlignment = Alignment.Center
                            ) {
                                TileView(
                                    tile = tile,
                                    isSelected = isSelected,
                                    isMatched = isMatched,
                                    isInvalid = isInvalid,
                                    onClick = { onTileClick(r, c) },
                                    onSwipe = { dir -> onTileSwipe(r, c, dir) }
                                )
                            }
                        }
                    }
                }
            }

            // Floating scores overlay
            floatingScores.forEach { floatScore ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingScorePopup(floatScore)
                }
            }

            // Reshuffling overlay
            AnimatedVisibility(
                visible = isReshuffling,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xCC0D0B1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "No moves left!\nReshuffling board...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingScorePopup(floatScore: FloatingScore) {
    val floatOffset by animateFloatAsState(
        targetValue = -40f,
        animationSpec = tween(durationMillis = 350),
        label = "float"
    )

    Box(
        modifier = Modifier
            .offset(y = floatOffset.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (floatScore.isCombo) {
                    Brush.horizontalGradient(listOf(NeonPink, NeonGold))
                } else {
                    Brush.horizontalGradient(listOf(Color(0xFF221C4E), Color(0xFF382E78)))
                }
            )
            .border(1.dp, ScoreGold, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = floatScore.text,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = if (floatScore.isCombo) 16.sp else 14.sp
        )
    }
}
