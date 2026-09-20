package com.stoni.bahndidos.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stoni.bahndidos.game.*

@Composable
fun PlayScreen(
    onNavigateBack: () -> Unit = {},
    onBack: () -> Unit = onNavigateBack
) {
    val context = LocalContext.current
    val engine = remember { GameEngine() }
    val state = engine.state

    val playerBmp = remember { loadAssetBitmap(context, "player/bahndidos_scooter.png") }
    val bgSky = remember { loadAssetBitmap(context, "bg/bg_sky.png") }
    val bgCity = remember { loadAssetBitmap(context, "bg/bg_city_far.png") }
    val bgPark = remember { loadAssetBitmap(context, "bg/bg_park_mid.png") }
    val bgGround = remember { loadAssetBitmap(context, "bg/bg_ground.png") }

    val obstacleBitmaps = remember {
        ObstacleType.values().associateWith { loadAssetBitmap(context, it.assetPath) }
    }
    val itemBitmaps = remember {
        ItemType.values().associateWith { loadAssetBitmap(context, it.assetPath) }
    }

    var renderTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state.isGameOver) {
        if (!state.isGameOver) {
            var lastTime = 0L
            while (!state.isGameOver) {
                withFrameNanos { now ->
                    if (lastTime != 0L) {
                        val dt = ((now - lastTime) / 1_000_000_000f).coerceAtMost(0.05f)
                        engine.update(dt)
                        renderTick = now
                    }
                    lastTime = now
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF141414))) {
        val density = LocalDensity.current
        val screenW = with(density) { maxWidth.toPx() }
        val screenH = with(density) { maxHeight.toPx() }
        val groundY = screenH * 0.74f

        engine.screenWidth = screenW
        engine.groundY = groundY

        val baseH = screenH * 0.18f
        state.basePlayerHeight = baseH
        val playerAspect = playerBmp?.let { it.width.toFloat() / it.height.toFloat() } ?: 0.9f
        state.basePlayerWidth = baseH * playerAspect
        state.playerX = screenW * 0.12f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (offset.x > size.width * 0.45f) {
                                engine.jump()
                            }
                        }
                    )
                }
        ) {
            val currentFrame = renderTick

            drawParallax(bgSky, state.bgSkyOffset, 0f, groundY * 0.85f)
            drawParallax(bgCity, state.bgCityOffset, groundY * 0.22f, groundY * 0.68f)
            drawParallax(bgPark, state.bgParkOffset, groundY * 0.32f, groundY * 0.68f)
            drawParallax(bgGround, state.bgGroundOffset, groundY, size.height - groundY)

            for (item in state.items) {
                val bmp = itemBitmaps[item.type]
                if (bmp != null) {
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(item.x.toInt(), item.y.toInt()),
                        dstSize = IntSize(item.width.toInt(), item.height.toInt())
                    )
                }
            }

            for (obs in state.obstacles) {
                val bmp = obstacleBitmaps[obs.type]
                if (bmp != null) {
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(obs.x.toInt(), obs.y.toInt()),
                        dstSize = IntSize(obs.width.toInt(), obs.height.toInt())
                    )
                }
            }

            playerBmp?.let { bmp ->
                val px = state.playerX
                val py = groundY - state.playerY
                val pw = state.currentWidth
                val ph = state.currentHeight

                if (state.action == PlayerAction.SALTO) {
                    rotate(
                        degrees = -state.saltoAngle,
                        pivot = Offset(px + pw / 2f, py - ph / 2f)
                    ) {
                        drawImage(
                            image = bmp,
                            dstOffset = IntOffset(px.toInt(), (py - ph).toInt()),
                            dstSize = IntSize(pw.toInt(), ph.toInt())
                        )
                    }
                } else {
                    drawImage(
                        image = bmp,
                        dstOffset = IntOffset(px.toInt(), (py - ph).toInt()),
                        dstSize = IntSize(pw.toInt(), ph.toInt())
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ECO-AKKU: ${state.battery.toInt()}%",
                        color = when {
                            state.battery > 40f -> Color(0xFF4CAF50)
                            state.battery > 15f -> Color(0xFFFFC107)
                            else -> Color(0xFFFF5252)
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(135.dp)
                            .height(10.dp)
                            .background(Color(0x44FFFFFF), RoundedCornerShape(3.dp))
                            .padding(1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((state.battery / 100f).coerceIn(0f, 1f))
                                .background(
                                    when {
                                        state.battery > 40f -> Color(0xFF4CAF50)
                                        state.battery > 15f -> Color(0xFFFFC107)
                                        else -> Color(0xFFFF5252)
                                    },
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${state.distanceMeters.toInt()} m",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "SCORE: ${state.score}",
                        color = Color(0xFFFFD54F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.turboTimer > 0f) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xCCFF9800)) {
                        Text(
                            "TURBO (${state.turboTimer.toInt()}s)",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (state.shieldActive) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xCC00B0FF)) {
                        Text(
                            "WLAN-SCHILD",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (state.hamsterTimer > 0f) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xCCE040FB)) {
                        Text(
                            "HAMSTER-STROM",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = {},
                modifier = Modifier
                    .width(140.dp)
                    .height(72.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                engine.setDucking(true)
                                tryAwaitRelease()
                                engine.setDucking(false)
                            }
                        )
                    },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC37474F))
            ) {
                Text("DUCKEN\n(Halten)", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { engine.jump() },
                modifier = Modifier
                    .width(155.dp)
                    .height(72.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xCC00897B))
            ) {
                Text("SPRUNG /\nSALTO", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            }
        }

        if (state.isGameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF212121)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ENDSTATION!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF5252)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = state.gameOverReason,
                            textAlign = TextAlign.Center,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Distanz: ${state.distanceMeters.toInt()} Meter\nScore: ${state.score}",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { engine.state.reset() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853))
                        ) {
                            Text("NOCHMAL FAHREN", fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { onBack() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("ZUM MENÜ", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun loadAssetBitmap(context: Context, path: String): ImageBitmap? {
    return try {
        context.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream)?.asImageBitmap()
        }
    } catch (e: Exception) {
        null
    }
}

private fun DrawScope.drawParallax(bitmap: ImageBitmap?, scrollOffset: Float, y: Float, height: Float) {
    if (bitmap == null || height <= 0f) return
    val aspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    val tileW = height * aspect
    if (tileW <= 0f) return
    val offset = ((scrollOffset % tileW) + tileW) % tileW
    var cx = -offset
    while (cx < size.width) {
        drawImage(
            image = bitmap,
            dstOffset = IntOffset(cx.toInt(), y.toInt()),
            dstSize = IntSize(tileW.toInt() + 2, height.toInt())
        )
        cx += tileW
    }
}
