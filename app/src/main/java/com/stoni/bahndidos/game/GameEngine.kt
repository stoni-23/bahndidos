package com.stoni.bahndidos.game

import kotlin.random.Random

class GameEngine(val state: GameState = GameState()) {

    var screenWidth: Float = 1080f
    var groundY: Float = 1500f

    private val gravity: Float = 2600f
    private val jumpVelocity: Float = 980f
    private val saltoExtraVelocity: Float = 880f

    private var nextObstacleTimer: Float = 1.6f
    private var nextItemTimer: Float = 2.4f
    private var idCounter: Long = 0L

    fun jump() {
        if (state.isGameOver) return

        when (state.action) {
            PlayerAction.RUN, PlayerAction.DUCK -> {
                state.action = PlayerAction.JUMP
                state.playerVy = jumpVelocity
                state.saltoAngle = 0f
            }
            PlayerAction.JUMP -> {
                state.action = PlayerAction.SALTO
                state.playerVy = saltoExtraVelocity
            }
            PlayerAction.SALTO -> {}
        }
    }

    fun setDucking(ducking: Boolean) {
        if (state.isGameOver) return
        if (ducking) {
            if (state.action == PlayerAction.RUN) {
                state.action = PlayerAction.DUCK
            }
        } else {
            if (state.action == PlayerAction.DUCK) {
                state.action = PlayerAction.RUN
            }
        }
    }

    fun update(dt: Float) {
        if (state.isGameOver) return

        val delta = dt.coerceAtMost(0.05f)
        val activeSpeed = if (state.turboTimer > 0f) state.speed * 1.65f else state.speed

        if (state.turboTimer > 0f) state.turboTimer -= delta
        if (state.hamsterTimer > 0f) state.hamsterTimer -= delta

        if (state.hamsterTimer <= 0f) {
            val drainRate = if (state.turboTimer > 0f) 2.6f else 1.35f
            state.battery -= drainRate * delta
            if (state.battery <= 0f) {
                state.battery = 0f
                state.isGameOver = true
                state.gameOverReason = "Akku leer! Jetzt schieben im Kuschelkurs!"
                return
            }
        }

        state.distanceMeters += (activeSpeed * delta) / 10f
        state.score += (activeSpeed * delta * 0.15f).toInt()

        state.bgSkyOffset += activeSpeed * 0.15f * delta
        state.bgCityOffset += activeSpeed * 0.40f * delta
        state.bgParkOffset += activeSpeed * 0.85f * delta
        state.bgGroundOffset += activeSpeed * 1.15f * delta

        if (state.action == PlayerAction.JUMP || state.action == PlayerAction.SALTO) {
            state.playerVy -= gravity * delta
            state.playerY += state.playerVy * delta

            if (state.action == PlayerAction.SALTO) {
                state.saltoAngle += 680f * delta
                if (state.saltoAngle > 360f) state.saltoAngle = 360f
            }

            if (state.playerY <= 0f) {
                state.playerY = 0f
                state.playerVy = 0f
                state.saltoAngle = 0f
                state.action = PlayerAction.RUN
            }
        }

        nextObstacleTimer -= delta
        if (nextObstacleTimer <= 0f) {
            spawnObstacle()
            nextObstacleTimer = Random.nextFloat() * 1.6f + 1.4f
        }

        nextItemTimer -= delta
        if (nextItemTimer <= 0f) {
            spawnItem()
            nextItemTimer = Random.nextFloat() * 2.5f + 2.0f
        }

        val playerLeft = state.playerX
        val playerRight = state.playerX + state.currentWidth
        val playerBottom = groundY - state.playerY
        val playerTop = playerBottom - state.currentHeight

        val obsIter = state.obstacles.iterator()
        while (obsIter.hasNext()) {
            val obs = obsIter.next()
            obs.x -= activeSpeed * delta

            val oLeft = obs.x + 8f
            val oRight = obs.x + obs.width - 8f
            val oTop = obs.y + 8f
            val oBottom = obs.y + obs.height - 4f

            val hit = playerLeft < oRight && playerRight > oLeft && playerTop < oBottom && playerBottom > oTop

            if (hit) {
                if (state.turboTimer > 0f) {
                    obsIter.remove()
                    state.score += 300
                } else if (state.shieldActive) {
                    state.shieldActive = false
                    obsIter.remove()
                } else {
                    state.isGameOver = true
                    state.gameOverReason = when (obs.type) {
                        ObstacleType.OPA_WALK -> "Opa mit Gehstock: 'Sons of Anarchy? Wohl eher Sons of Akkuladegerät!'"
                        ObstacleType.OMA -> "Oma mit Handtasche: 'Keine Harley, aber in der Bahn rumbimmeln!'"
                        ObstacleType.TAUBE -> "Taubenschlag! Sonnenbrille verloren!"
                    }
                    return
                }
            } else if (obs.x + obs.width < -80f) {
                obsIter.remove()
            }
        }

        val itemIter = state.items.iterator()
        while (itemIter.hasNext()) {
            val item = itemIter.next()
            item.x -= activeSpeed * delta

            val iLeft = item.x
            val iRight = item.x + item.width
            val iTop = item.y
            val iBottom = item.y + item.height

            val collected = playerLeft < iRight && playerRight > iLeft && playerTop < iBottom && playerBottom > iTop

            if (collected) {
                state.score += item.type.scoreValue
                when (item.type) {
                    ItemType.POWERBANK -> state.battery = (state.battery + 30f).coerceAtMost(100f)
                    ItemType.BIER, ItemType.KAFFEE -> state.turboTimer = 5.0f
                    ItemType.TICKET -> state.score += 500
                    ItemType.WLAN -> state.shieldActive = true
                    ItemType.HAMSTER -> state.hamsterTimer = 8.0f
                }
                itemIter.remove()
            } else if (item.x + item.width < -80f) {
                itemIter.remove()
            }
        }
    }

    private fun spawnObstacle() {
        val roll = Random.nextInt(100)
        val type = when {
            roll < 45 -> ObstacleType.OPA_WALK
            roll < 75 -> ObstacleType.OMA
            else -> ObstacleType.TAUBE
        }

        val obsHeight = state.basePlayerHeight * type.heightFactor
        val obsWidth = obsHeight * type.widthFactor

        val yPos = if (type.isAir) {
            groundY - state.basePlayerHeight * 0.95f
        } else {
            groundY - obsHeight
        }

        state.obstacles.add(
            Obstacle(
                id = ++idCounter,
                type = type,
                x = screenWidth + 30f,
                y = yPos,
                width = obsWidth,
                height = obsHeight
            )
        )
    }

    private fun spawnItem() {
        val types = ItemType.values()
        val type = types[Random.nextInt(types.size)]
        val inAir = Random.nextBoolean()
        val itemSize = state.basePlayerHeight * 0.35f
        val yPos = if (inAir) groundY - state.basePlayerHeight * 1.15f else groundY - itemSize - 10f

        state.items.add(
            GameItem(
                id = ++idCounter,
                type = type,
                x = screenWidth + 40f,
                y = yPos,
                width = itemSize,
                height = itemSize
            )
        )
    }
}
