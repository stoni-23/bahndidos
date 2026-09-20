package com.stoni.bahndidos.game

enum class ObstacleType(
    val assetPath: String,
    val widthFactor: Float,
    val heightFactor: Float,
    val isAir: Boolean = false
) {
    OPA_WALK("enemies/opa_walk.png", 0.55f, 0.95f, false),
    OMA("enemies/oma.png", 0.58f, 0.92f, false),
    TAUBE("enemies/taube.png", 0.35f, 0.26f, true)
}

enum class ItemType(val assetPath: String, val scoreValue: Int) {
    POWERBANK("items/powerbank.png", 150),
    BIER("items/bier.png", 200),
    KAFFEE("items/kaffee.png", 200),
    TICKET("items/ticket.png", 400),
    WLAN("items/wlan.png", 250),
    HAMSTER("items/hamster.png", 300)
}

data class Obstacle(
    val id: Long,
    val type: ObstacleType,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float
)

data class GameItem(
    val id: Long,
    val type: ItemType,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float
)
