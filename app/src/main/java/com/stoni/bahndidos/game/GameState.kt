package com.stoni.bahndidos.game

/**
 * Mutable runtime state for Bahndidos play loop.
 * Player is fixed on the left; world scrolls right→left.
 * Hitbox heights aligned to UEBERGABE (~128h characters).
 */
enum class JumpPhase {
    GROUNDED,
    JUMP,
    SALTO,
}

data class GameState(
    /** World Y of player feet (0 = ground). Positive = up. */
    var playerY: Float = 0f,
    var velocityY: Float = 0f,
    /** Degrees; 360° during salto backflip. */
    var rotationDeg: Float = 0f,
    var jumpPhase: JumpPhase = JumpPhase.GROUNDED,
    /** 0 = none used, 1 = single jump, 2 = salto used. */
    var jumpsUsed: Int = 0,
    var isDucking: Boolean = false,
    /** Battery / Akku 0..100. */
    var battery: Float = 100f,
    var score: Int = 0,
    /** How far the world has scrolled (pixels). */
    var scrollX: Float = 0f,
    var scrollSpeed: Float = 220f,
    var obstacles: MutableList<Obstacle> = mutableListOf(),
    var items: MutableList<Item> = mutableListOf(),
    var elapsedSec: Float = 0f,
    var nextSpawnId: Long = 1L,
    var spawnCooldown: Float = 1.5f,
    var alive: Boolean = true,
) {
    companion object {
        const val DEFAULT_SCROLL_SPEED = 220f
        const val PLAYER_WORLD_X = 72f
        /** bahndidos_scooter.png — UEBERGABE ~128h. */
        val PLAYER_WIDTH: Float = Hitboxes.PLAYER.width
        val PLAYER_HEIGHT: Float = Hitboxes.PLAYER.height
        /** Duck ~50% of standing height at ~128h scale. */
        const val PLAYER_DUCK_HEIGHT = Hitboxes.PLAYER_DUCK_HEIGHT
        const val GROUND_Y = 0f
        const val MAX_BATTERY = 100f
    }

    fun playerHeight(): Float =
        if (isDucking && jumpPhase == JumpPhase.GROUNDED) PLAYER_DUCK_HEIGHT else PLAYER_HEIGHT

    fun playerHitbox(): Aabb {
        val h = playerHeight()
        return Aabb(
            left = PLAYER_WORLD_X,
            bottom = playerY,
            right = PLAYER_WORLD_X + PLAYER_WIDTH,
            top = playerY + h,
        )
    }

    fun canJump(): Boolean = alive && jumpsUsed < 2 && !isDucking

    fun canDuck(): Boolean = alive && jumpPhase == JumpPhase.GROUNDED

    fun reset() {
        playerY = 0f
        velocityY = 0f
        rotationDeg = 0f
        jumpPhase = JumpPhase.GROUNDED
        jumpsUsed = 0
        isDucking = false
        battery = MAX_BATTERY
        score = 0
        scrollX = 0f
        scrollSpeed = DEFAULT_SCROLL_SPEED
        obstacles.clear()
        items.clear()
        elapsedSec = 0f
        nextSpawnId = 1L
        spawnCooldown = 1.5f
        alive = true
    }
}
