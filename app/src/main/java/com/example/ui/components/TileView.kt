package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.SwipeDirection
import com.example.model.Tile
import com.example.model.TileShapeType
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TileView(
    tile: Tile?,
    isSelected: Boolean,
    isMatched: Boolean,
    isInvalid: Boolean,
    onClick: () -> Unit,
    onSwipe: (SwipeDirection) -> Unit,
    modifier: Modifier = Modifier
) {
    if (tile == null) {
        // Empty slot
        Box(
            modifier = modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x1F1F1948))
        )
        return
    }

    // Selection pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.05f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Matched pop animation
    val matchScale by animateFloatAsState(
        targetValue = if (isMatched) 1.25f else 1.0f,
        animationSpec = tween(durationMillis = 260),
        label = "matchScale"
    )
    val matchAlpha by animateFloatAsState(
        targetValue = if (isMatched) 0.0f else 1.0f,
        animationSpec = tween(durationMillis = 260),
        label = "matchAlpha"
    )

    // Invalid shake animation
    val shakeOffset by animateFloatAsState(
        targetValue = if (isInvalid) 8f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "shakeOffset"
    )

    val currentScale = when {
        isMatched -> matchScale
        isSelected -> pulseScale
        else -> 1.0f
    }

    val cornerRadius = 10.dp
    val shape = RoundedCornerShape(cornerRadius)

    val tileColor = tile.color
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            tileColor.primaryColor,
            tileColor.secondaryColor
        )
    )

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 18.dp.toPx() }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .offset(x = if (isInvalid) shakeOffset.dp else 0.dp)
            .scale(currentScale)
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier
                        .shadow(12.dp, shape, ambientColor = Color.White, spotColor = tileColor.primaryColor)
                        .border(2.5.dp, Color.White, shape)
                } else {
                    Modifier.border(1.dp, Color(0x33FFFFFF), shape)
                }
            )
            .background(gradientBrush)
            .pointerInput(tile.id) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalDragX = 0f
                    var totalDragY = 0f
                    var swiped = false

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            // Touch released without reaching swipe threshold -> process as tap
                            if (!swiped && abs(totalDragX) < swipeThresholdPx && abs(totalDragY) < swipeThresholdPx) {
                                onClick()
                            }
                            break
                        }

                        val delta = change.positionChange()
                        totalDragX += delta.x
                        totalDragY += delta.y

                        val absX = abs(totalDragX)
                        val absY = abs(totalDragY)
                        if (!swiped && (absX >= swipeThresholdPx || absY >= swipeThresholdPx)) {
                            swiped = true
                            change.consume()
                            val dir = if (absX > absY) {
                                if (totalDragX > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
                            } else {
                                if (totalDragY > 0) SwipeDirection.DOWN else SwipeDirection.UP
                            }
                            onSwipe(dir)
                        }
                    }
                }
            }
            .testTag("tile_${tile.row}_${tile.col}"),
        contentAlignment = Alignment.Center
    ) {
        // Tile shine & geometric badge canvas
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)

            // Top glossy curved highlight
            val highlightPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(width, 0f)
                cubicTo(
                    width * 0.8f, height * 0.45f,
                    width * 0.2f, height * 0.45f,
                    0f, 0f
                )
                close()
            }
            drawPath(
                path = highlightPath,
                color = Color.White.copy(alpha = 0.28f * matchAlpha)
            )

            // Geometric Shape Badge in center (for rich arcade visual and accessibility)
            val badgeSize = (minOf(width, height) * 0.42f)
            drawTileShape(
                shapeType = tileColor.shapeType,
                center = center,
                radius = badgeSize,
                color = Color.White.copy(alpha = 0.88f * matchAlpha)
            )

            // Frosted Ice Layer if frozen
            if (tile.hasIce) {
                drawRect(
                    color = Color(0x70B0EEFF),
                    size = size
                )
                val crackPath = Path().apply {
                    moveTo(width * 0.15f, height * 0.2f)
                    lineTo(width * 0.45f, height * 0.5f)
                    lineTo(width * 0.35f, height * 0.85f)
                    moveTo(width * 0.45f, height * 0.5f)
                    lineTo(width * 0.85f, height * 0.35f)
                }
                drawPath(
                    path = crackPath,
                    color = Color.White.copy(alpha = 0.85f),
                    style = Stroke(width = 2.5f)
                )
                drawRoundRect(
                    color = Color(0xD9FFFFFF),
                    size = size,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
                    style = Stroke(width = 2.5f)
                )
            }
        }
    }
}

/**
 * Draws distinct geometric icons inside each tile.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTileShape(
    shapeType: TileShapeType,
    center: Offset,
    radius: Float,
    color: Color
) {
    val shadowColor = Color(0x44000000)
    // Draw subtle drop shadow first
    drawShapePath(shapeType, center + Offset(1.5f, 2f), radius, shadowColor)
    // Draw primary white glyph
    drawShapePath(shapeType, center, radius, color)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawShapePath(
    shapeType: TileShapeType,
    center: Offset,
    radius: Float,
    color: Color
) {
    when (shapeType) {
        TileShapeType.DIAMOND -> {
            val path = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius * 0.85f, center.y)
                lineTo(center.x, center.y + radius)
                lineTo(center.x - radius * 0.85f, center.y)
                close()
            }
            drawPath(path, color, style = Fill)
        }
        TileShapeType.HEXAGON -> {
            val path = Path().apply {
                for (i in 0 until 6) {
                    val angle = (i * 60.0 - 30.0) * PI / 180.0
                    val x = center.x + (radius * cos(angle)).toFloat()
                    val y = center.y + (radius * sin(angle)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, color, style = Fill)
        }
        TileShapeType.STAR -> {
            val path = Path().apply {
                val points = 5
                val innerRadius = radius * 0.48f
                for (i in 0 until points * 2) {
                    val r = if (i % 2 == 0) radius else innerRadius
                    val angle = (i * 36.0 - 90.0) * PI / 180.0
                    val x = center.x + (r * cos(angle)).toFloat()
                    val y = center.y + (r * sin(angle)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(path, color, style = Fill)
        }
        TileShapeType.CIRCLE -> {
            drawCircle(color = color, radius = radius * 0.8f, center = center)
        }
        TileShapeType.HEART -> {
            val path = Path().apply {
                val w = radius * 1.5f
                val h = radius * 1.5f
                val topX = center.x
                val topY = center.y - h * 0.35f
                moveTo(topX, topY + h * 0.25f)
                cubicTo(
                    topX - w * 0.5f, topY - h * 0.3f,
                    topX - w * 0.6f, topY + h * 0.35f,
                    topX, topY + h * 0.85f
                )
                cubicTo(
                    topX + w * 0.6f, topY + h * 0.35f,
                    topX + w * 0.5f, topY - h * 0.3f,
                    topX, topY + h * 0.25f
                )
                close()
            }
            drawPath(path, color, style = Fill)
        }
        TileShapeType.TRIANGLE -> {
            val path = Path().apply {
                moveTo(center.x, center.y - radius * 0.95f)
                lineTo(center.x + radius, center.y + radius * 0.85f)
                lineTo(center.x - radius, center.y + radius * 0.85f)
                close()
            }
            drawPath(path, color, style = Fill)
        }
    }
}
