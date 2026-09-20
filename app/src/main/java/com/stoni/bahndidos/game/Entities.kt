package com.stoni.bahndidos.game

/**
 * Stub entity types for Bahndidos (obstacles + pickups).
 * AABB sizes aligned to Artiflux UEBERGABE.md / Asset-Pack d2f602d.
 */

enum class ObstacleType {
    OPA,
    OMA,
    BENCH,
    BIN,
    PIGEON,
}

enum class ItemType {
    POWERBANK,
    COFFEE_BEER,
    WIFI,
    TICKET,
    HAMSTER,
}

/** Axis-aligned bounding box in world units (origin bottom-left, Y up). */
data class Aabb(
    val left: Float,
    val bottom: Float,
    val right: Float,
    val top: Float,
) {
    val width: Float get() = right - left
    val height: Float get() = top - bottom

    fun intersects(other: Aabb): Boolean =
        left < other.right && right > other.left && bottom < other.top && top > other.bottom
}

/** Width/height (+ default spawn Y) for gameplay hitboxes / stub spawns. */
data class HitboxSpec(
    val width: Float,
    val height: Float,
    /** World Y of entity bottom (0 = ground). */
    val spawnY: Float = 0f,
)

/**
 * Canonical sizes from UEBERGABE / asset PNGs (nearest-neighbor game height).
 * Draw/sprites remain Kacki’s job — these drive AABB + stub spawn only.
 */
object Hitboxes {
    /** bahndidos_scooter.png (~128h, facing RIGHT). */
    val PLAYER = HitboxSpec(width = 92f, height = 128f)

    /** Optional larger variant (bahndidos_scooter_160.png). */
    val PLAYER_160 = HitboxSpec(width = 115f, height = 160f)

    /** Duck shrink ~50% of standing height (relative to ~128h). */
    const val PLAYER_DUCK_HEIGHT = 64f

    val OBSTACLE: Map<ObstacleType, HitboxSpec> = mapOf(
        ObstacleType.OPA to HitboxSpec(width = 52f, height = 128f, spawnY = 0f),
        ObstacleType.OMA to HitboxSpec(width = 56f, height = 128f, spawnY = 0f),
        ObstacleType.BENCH to HitboxSpec(width = 64f, height = 32f, spawnY = 0f),
        ObstacleType.BIN to HitboxSpec(width = 40f, height = 40f, spawnY = 0f),
        // Flying: bottom above duck height so duck-under works at ~128h scale.
        ObstacleType.PIGEON to HitboxSpec(width = 40f, height = 28f, spawnY = 72f),
    )

    val ITEM: Map<ItemType, HitboxSpec> = mapOf(
        ItemType.POWERBANK to HitboxSpec(width = 48f, height = 48f, spawnY = 8f),
        ItemType.TICKET to HitboxSpec(width = 48f, height = 48f, spawnY = 8f),
        ItemType.COFFEE_BEER to HitboxSpec(width = 40f, height = 40f, spawnY = 8f),
        ItemType.WIFI to HitboxSpec(width = 40f, height = 40f, spawnY = 120f),
        ItemType.HAMSTER to HitboxSpec(width = 40f, height = 40f, spawnY = 8f),
    )

    fun obstacle(type: ObstacleType): HitboxSpec =
        OBSTACLE.getValue(type)

    fun item(type: ItemType): HitboxSpec =
        ITEM.getValue(type)
}

data class Obstacle(
    val id: Long,
    val type: ObstacleType,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val active: Boolean = true,
) {
    fun hitbox(): Aabb = Aabb(x, y, x + width, y + height)

    companion object {
        fun spawn(id: Long, type: ObstacleType, x: Float): Obstacle {
            val spec = Hitboxes.obstacle(type)
            return Obstacle(
                id = id,
                type = type,
                x = x,
                y = spec.spawnY,
                width = spec.width,
                height = spec.height,
            )
        }
    }
}

data class Item(
    val id: Long,
    val type: ItemType,
    var x: Float,
    var y: Float,
    val width: Float,
    val height: Float,
    val active: Boolean = true,
) {
    fun hitbox(): Aabb = Aabb(x, y, x + width, y + height)

    companion object {
        fun spawn(id: Long, type: ItemType, x: Float): Item {
            val spec = Hitboxes.item(type)
            return Item(
                id = id,
                type = type,
                x = x,
                y = spec.spawnY,
                width = spec.width,
                height = spec.height,
            )
        }
    }
}
