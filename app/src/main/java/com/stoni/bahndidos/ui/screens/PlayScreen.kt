package com.stoni.bahndidos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stoni.bahndidos.game.GameEngine
import com.stoni.bahndidos.game.GameState
import com.stoni.bahndidos.game.JumpPhase
import kotlinx.coroutines.isActive

/**
 * Play loop: engine @ ~60fps, color-box debug draw, touch controls.
 * Tap right half = jump / salto; hold left half or swipe down = duck.
 */
@Composable
fun PlayScreen(
    onBack: () -> Unit,
) {
    val engine = remember { GameEngine() }
    var frame by remember { mutableIntStateOf(0) }
    var duckHeld by remember { mutableStateOf(false) }

    LaunchedEffect(engine) {
        var lastNanos = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (lastNanos != 0L) {
                    val dt = ((now - lastNanos) / 1_000_000_000.0).toFloat()
                    engine.setDucking(duckHeld)
                    engine.update(dt)
                    frame++
                }
                lastNanos = now
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        val rightHalf = offset.x >= size.width * 0.5f
                        if (rightHalf) {
                            engine.onJumpTap()
                            tryAwaitRelease()
                        } else {
                            duckHeld = true
                            tryAwaitRelease()
                            duckHeld = false
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        if (offset.x < size.width * 0.5f) duckHeld = true
                    },
                    onDragEnd = { duckHeld = false },
                    onDragCancel = { duckHeld = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragAmount.y > 12f) duckHeld = true
                    },
                )
            },
    ) {
        // Read frame so Canvas recomposes each tick.
        val tick = frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            @Suppress("UNUSED_EXPRESSION")
            tick
            val groundScreenY = size.height * 0.82f
            fun worldToScreen(wx: Float, wy: Float): Offset =
                Offset(wx, groundScreenY - wy)

            drawRect(Color(0xFF16213E), size = Size(size.width, groundScreenY))
            drawRect(
                Color(0xFF0F3460),
                topLeft = Offset(0f, groundScreenY),
                size = Size(size.width, size.height - groundScreenY),
            )
            val scroll = engine.state.scrollX
            val stripeW = 48f
            var sx = -((scroll * 0.5f) % stripeW)
            while (sx < size.width) {
                drawRect(
                    Color(0xFF1F4068),
                    topLeft = Offset(sx, groundScreenY),
                    size = Size(stripeW * 0.45f, 8f),
                )
                sx += stripeW
            }

            val st = engine.state
            st.obstacles.forEach { o ->
                val p = worldToScreen(o.x, o.y + o.height)
                drawRoundRect(
                    color = Color(0xFFE94560),
                    topLeft = p,
                    size = Size(o.width, o.height),
                    cornerRadius = CornerRadius(4f, 4f),
                )
            }
            st.items.forEach { item ->
                val p = worldToScreen(item.x, item.y + item.height)
                drawRoundRect(
                    color = Color(0xFF53D8FB),
                    topLeft = p,
                    size = Size(item.width, item.height),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }

            val box = st.playerHitbox()
            val ph = box.height
            val pw = box.width
            val topLeft = worldToScreen(box.left, box.top)
            val pivot = Offset(topLeft.x + pw / 2f, topLeft.y + ph / 2f)
            rotate(degrees = -st.rotationDeg, pivot = pivot) {
                drawRoundRect(
                    color = if (st.isDucking) Color(0xFFFFC857) else Color(0xFF4ECCA3),
                    topLeft = topLeft,
                    size = Size(pw, ph),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }
            drawRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = topLeft,
                size = Size(pw, ph),
                style = Stroke(width = 2f),
            )

            drawLine(
                Color.White.copy(alpha = 0.12f),
                start = Offset(size.width * 0.5f, 0f),
                end = Offset(size.width * 0.5f, size.height),
                strokeWidth = 2f,
            )
        }

        val st = engine.state
        DebugHud(
            state = st,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        )

        TextButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
        ) {
            Text("Back", color = Color.White)
        }

        if (!st.alive) {
            Text(
                text = "Akku leer — Score ${st.score}",
                color = Color.White,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun DebugHud(state: GameState, modifier: Modifier = Modifier) {
    val phase = when (state.jumpPhase) {
        JumpPhase.GROUNDED -> "GROUND"
        JumpPhase.JUMP -> "JUMP"
        JumpPhase.SALTO -> "SALTO"
    }
    val duck = if (state.isDucking) "DUCK" else "-"
    Text(
        text = buildString {
            appendLine("Y=${"%.0f".format(state.playerY)}  vY=${"%.0f".format(state.velocityY)}")
            appendLine("phase=$phase  jumps=${state.jumpsUsed}  $duck")
            appendLine("rot=${"%.0f".format(state.rotationDeg)}°  hitH=${"%.0f".format(state.playerHeight())}")
            appendLine("Akku=${"%.0f".format(state.battery)}  Score=${state.score}")
            appendLine("scroll=${"%.0f".format(state.scrollX)}  obs=${state.obstacles.size}")
            append("L:Duck  R:Jump/Salto")
        },
        color = Color(0xFFCCFFCC),
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        lineHeight = 14.sp,
        modifier = modifier,
    )
}
