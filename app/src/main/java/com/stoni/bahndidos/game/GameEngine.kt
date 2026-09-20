package com.stoni.bahndidos.game

import kotlin.math.max
import kotlin.math.min

/**
 * 60fps-oriented update: gravity, jump / double-jump salto, duck, scroll, stub AABB.
 * Spawn sizes from [Hitboxes] (UEBERGABE / Asset-Pack d2f602d).
 */
class GameEngine(
    val state: GameState = GameState(),
) {
    private var scoreAcc: Float = 0f

    companion object {
        const val TARGET_DT = 1f / 60f
        const val GRAVITY = 2800f
        const val JUMP_VELOCITY = 920f
        /** Higher impulse for second jump (salto / high obstacles). */
        const val SALTO_VELOCITY = 1120f
        const val SALTO_SPIN_DEG_PER_SEC = 720f
        const val BATTERY_DRAIN_PER_SEC = 1.2f
        const val SCORE_PER_SEC = 10f
        const val MAX_DT = 0.05f
    }

    /**
     * Advance simulation by [dt] seconds (clamped). Call ~60×/s from the UI loop.
     */
    fun update(dtRaw: Float) {
        if (!state.alive) return
        val dt = min(max(dtRaw, 0f), MAX_DT)

        state.elapsedSec += dt
        state.scrollX += state.scrollSpeed * dt
        state.battery = max(0f, state.battery - BATTERY_DRAIN_PER_SEC * dt)
        scoreAcc += SCORE_PER_SEC * dt
        if (scoreAcc >= 1f) {
            val add = scoreAcc.toInt()
            state.score += add
            scoreAcc -= add
        }
        if (state.battery <= 0f) {
            state.alive = false
            return
        }

        applyVerticalPhysics(dt)
        applySaltoSpin(dt)
        scrollEntities(dt)
        stubSpawn(dt)
        stubCollisions()
        pruneOffscreen()
    }

    /** Tap right half: bunny hop, or second tap in air → 360° salto. */
    fun onJumpTap() {
        if (!state.canJump()) return
        when (state.jumpsUsed) {
            0 -> {
                state.velocityY = JUMP_VELOCITY
                state.jumpsUsed = 1
                state.jumpPhase = JumpPhase.JUMP
                state.isDucking = false
                state.rotationDeg = 0f
            }
            1 -> {
                if (state.jumpPhase == JumpPhase.GROUNDED) return
                state.velocityY = SALTO_VELOCITY
                state.jumpsUsed = 2
                state.jumpPhase = JumpPhase.SALTO
                state.rotationDeg = 0f
            }
        }
    }

    /** Hold left half or swipe-down: duck (smaller hitbox) while grounded. */
    fun setDucking(ducking: Boolean) {
        if (ducking) {
            if (state.canDuck()) state.isDucking = true
        } else {
            state.isDucking = false
        }
    }

    private fun applyVerticalPhysics(dt: Float) {
        if (state.jumpPhase == JumpPhase.GROUNDED && state.jumpsUsed == 0) {
            state.playerY = GameState.GROUND_Y
            state.velocityY = 0f
            state.rotationDeg = 0f
            return
        }

        state.velocityY -= GRAVITY * dt
        state.playerY += state.velocityY * dt

        if (state.playerY <= GameState.GROUND_Y) {
            state.playerY = GameState.GROUND_Y
            state.velocityY = 0f
            state.jumpPhase = JumpPhase.GROUNDED
            state.jumpsUsed = 0
            state.rotationDeg = 0f
        }
    }

    private fun applySaltoSpin(dt: Float) {
        if (state.jumpPhase != JumpPhase.SALTO) return
        if (state.rotationDeg >= 360f) {
            state.rotationDeg = 360f
            return
        }
        state.rotationDeg = min(360f, state.rotationDeg + SALTO_SPIN_DEG_PER_SEC * dt)
    }

    private fun scrollEntities(dt: Float) {
        val dx = state.scrollSpeed * dt
        state.obstacles.forEach { it.x -= dx }
        state.items.forEach { it.x -= dx }
    }

    /** Minimal stub spawner — sizes from [Hitboxes] / UEBERGABE. */
    private fun stubSpawn(dt: Float) {
        state.spawnCooldown -= dt
        if (state.spawnCooldown > 0f) return
        state.spawnCooldown = 1.2f + (state.elapsedSec % 1.1f)

        val id = state.nextSpawnId++
        val spawnX = 420f + (id % 3) * 40f
        if (id % 3L == 0L) {
            val type = ItemType.entries[(id % ItemType.entries.size).toInt()]
            state.items.add(Item.spawn(id = id, type = type, x = spawnX))
        } else {
            val type = ObstacleType.entries[(id % ObstacleType.entries.size).toInt()]
            state.obstacles.add(Obstacle.spawn(id = id, type = type, x = spawnX))
        }
    }

    private fun stubCollisions() {
        val box = state.playerHitbox()
        val hitObs = state.obstacles.firstOrNull { it.active && it.hitbox().intersects(box) }
        if (hitObs != null) {
            state.battery = max(0f, state.battery - 15f)
            state.obstacles.remove(hitObs)
            if (state.battery <= 0f) state.alive = false
        }
        val hitItem = state.items.firstOrNull { it.active && it.hitbox().intersects(box) }
        if (hitItem != null) {
            when (hitItem.type) {
                ItemType.POWERBANK -> state.battery = min(GameState.MAX_BATTERY, state.battery + 25f)
                ItemType.COFFEE_BEER -> state.score += 50
                ItemType.WIFI -> state.score += 30
                ItemType.TICKET -> state.score += 100
                ItemType.HAMSTER -> state.score += 75
            }
            state.items.remove(hitItem)
        }
    }

    private fun pruneOffscreen() {
        state.obstacles.removeAll { it.x + it.width < -40f }
        state.items.removeAll { it.x + it.width < -40f }
    }
}
