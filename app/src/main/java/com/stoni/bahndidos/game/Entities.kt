package com.stoni.bahndidos.game

/**
 * Stub entity types for Bahndidos (obstacles + pickups).
 * Collision uses simple AABB placeholders until sprites exist.
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
}
