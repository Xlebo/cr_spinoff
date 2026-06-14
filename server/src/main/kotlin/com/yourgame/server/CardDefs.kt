package com.yourgame.server

import com.yourgame.shared.CardType

enum class TargetType { GROUND, ALL, BUILDINGS }

data class CardDef(
    val elixirCost: Int = 0,
    val hp: Int = 0,
    val damage: Int,
    val speed: Float = 0f,          // world units per second
    val range: Float,               // world units (melee ~120, ranged 200-600)
    val targets: TargetType = TargetType.ALL,
    val attackSpeedTicks: Int = 20, // ticks between attacks (20 = 1/sec)
    val spawnCount: Int = 1,
    val isFlying: Boolean = false,
    val isSpell: Boolean = false,
    val aoeRadius: Float = 0f,      // for spells
)

// All distances in world units (arena = 1000 × 1800).
// Ranges are scaled ×100 from the CLAUDE.md tile-based values so they feel correct.
object CardDefs {
    private val map = mapOf(
        CardType.KNIGHT     to CardDef(elixirCost=3, hp=1200, damage=120, speed=60f,  range=120f, targets=TargetType.GROUND,    attackSpeedTicks=22),
        CardType.ARCHER     to CardDef(elixirCost=3, hp=340,  damage=60,  speed=60f,  range=500f, targets=TargetType.ALL,       attackSpeedTicks=24, spawnCount=2),
        CardType.GIANT      to CardDef(elixirCost=5, hp=3000, damage=140, speed=40f,  range=120f, targets=TargetType.BUILDINGS, attackSpeedTicks=30),
        CardType.FIREBALL   to CardDef(elixirCost=4, damage=600, range=0f, isSpell=true, aoeRadius=350f),
        CardType.MINIONS    to CardDef(elixirCost=3, hp=240,  damage=55,  speed=80f,  range=200f, targets=TargetType.ALL,       attackSpeedTicks=20, spawnCount=3, isFlying=true),
        CardType.BARBARIANS to CardDef(elixirCost=5, hp=900,  damage=100, speed=60f,  range=120f, targets=TargetType.GROUND,    attackSpeedTicks=20, spawnCount=4),
        CardType.MUSKETEER  to CardDef(elixirCost=4, hp=580,  damage=180, speed=60f,  range=600f, targets=TargetType.ALL,       attackSpeedTicks=22),
        CardType.MINI_PEKKA to CardDef(elixirCost=4, hp=1200, damage=380, speed=60f,  range=120f, targets=TargetType.GROUND,    attackSpeedTicks=36),
    )

    operator fun get(card: CardType): CardDef = map.getValue(card)
}

// Spawn position offsets per count, so multi-troop cards don't stack on a single point.
fun spawnOffsets(count: Int): List<Pair<Float, Float>> = when (count) {
    1 -> listOf(0f to 0f)
    2 -> listOf(-35f to 0f, 35f to 0f)
    3 -> listOf(-35f to -25f, 35f to -25f, 0f to 25f)
    4 -> listOf(-45f to -20f, 45f to -20f, -20f to 20f, 20f to 20f)
    else -> listOf(0f to 0f)
}
