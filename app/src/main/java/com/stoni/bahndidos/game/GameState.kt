package com.stoni.bahndidos.game

enum class PlayerAction { RUN, JUMP, SALTO, DUCK }

class GameState {
    var isGameOver: Boolean = false
    var gameOverReason: String = ""

    var playerX: Float = 60f
    var playerY: Float = 0f
    var playerVy: Float = 0f
    var action: PlayerAction = PlayerAction.RUN
    var saltoAngle: Float = 0f

    var basePlayerWidth: Float = 160f
    var basePlayerHeight: Float = 210f

    val currentWidth: Float get() = basePlayerWidth
    val currentHeight: Float get() = if (action == PlayerAction.DUCK) basePlayerHeight * 0.54f else basePlayerHeight

    var battery: Float = 100f
    var score: Int = 0
    var distanceMeters: Float = 0f
    var speed: Float = 420f

    var turboTimer: Float = 0f
    var hamsterTimer: Float = 0f
    var shieldActive: Boolean = false

    var bgSkyOffset: Float = 0f
    var bgCityOffset: Float = 0f
    var bgParkOffset: Float = 0f
    var bgGroundOffset: Float = 0f

    val obstacles = mutableListOf<Obstacle>()
    val items = mutableListOf<GameItem>()

    fun reset() {
        isGameOver = false
        gameOverReason = ""
        playerY = 0f
        playerVy = 0f
        action = PlayerAction.RUN
        saltoAngle = 0f
        battery = 100f
        score = 0
        distanceMeters = 0f
        speed = 420f
        turboTimer = 0f
        hamsterTimer = 0f
        shieldActive = false
        obstacles.clear()
        items.clear()
    }
}
