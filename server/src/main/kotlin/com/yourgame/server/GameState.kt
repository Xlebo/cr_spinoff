package com.yourgame.server

import com.yourgame.shared.*

const val TICK_MS        = 50L
const val GAME_DURATION_MS = 180_000L
const val MAX_ELIXIR     = 10f
const val ELIXIR_REGEN   = 2.8f / 20f   // per tick

const val PRINCESS_RANGE  = 700f
const val KING_RANGE      = 900f
const val PRINCESS_DAMAGE = 75
const val KING_DAMAGE     = 60
const val TOWER_ATTACK_TICKS = 20       // 1 attack / sec

data class UnitData(
    val id: Int,
    val card: CardType,
    var x: Float,
    var y: Float,
    val maxHp: Int,
    var hp: Int,
    val owner: Int,
    val isFlying: Boolean = false,
    var attackCooldown: Int = 0,
) {
    val alive: Boolean get() = hp > 0
    fun toState() = UnitState(id, card, x, y, hp, maxHp, owner, isFlying)
}

data class TowerData(
    val type: TowerType,
    val x: Float,
    val y: Float,
    val maxHp: Int,
    var hp: Int,
    val owner: Int,
    var attackCooldown: Int = 0,
) {
    val alive: Boolean get() = hp > 0
    val range:  Float get() = if (type == TowerType.KING) KING_RANGE  else PRINCESS_RANGE
    val damage: Int   get() = if (type == TowerType.KING) KING_DAMAGE else PRINCESS_DAMAGE
    fun toState() = TowerState(type, x, y, hp, maxHp, owner, alive)
}

class GameState {
    @Volatile var tick = 0
    @Volatile var timeRemainingMs = GAME_DURATION_MS
    @Volatile var winner = -1               // -1 = ongoing

    val elixir = floatArrayOf(5f, 5f)       // [player0, player1]
    val units  = mutableListOf<UnitData>()
    val towers = mutableListOf<TowerData>()

    private var _nextId = 0
    fun nextUnitId() = ++_nextId

    init {
        // Tower layout from CLAUDE.md (Y=0 = player 0 side)
        towers += listOf(
            TowerData(TowerType.PRINCESS, 200f,  200f, 1400, 1400, owner = 0),
            TowerData(TowerType.KING,     500f,  100f, 2400, 2400, owner = 0),
            TowerData(TowerType.PRINCESS, 800f,  200f, 1400, 1400, owner = 0),
            TowerData(TowerType.PRINCESS, 200f, 1600f, 1400, 1400, owner = 1),
            TowerData(TowerType.KING,     500f, 1700f, 2400, 2400, owner = 1),
            TowerData(TowerType.PRINCESS, 800f, 1600f, 1400, 1400, owner = 1),
        )
    }

    fun isOver() = winner >= 0 || timeRemainingMs <= 0

    fun towerStates(): List<TowerState> = towers.map { it.toState() }
    fun unitStates():  List<UnitState>  = units.map  { it.toState() }

    fun tieWinner(): Int {
        val a0 = towers.count { it.owner == 0 && it.alive }
        val a1 = towers.count { it.owner == 1 && it.alive }
        return if (a0 >= a1) 0 else 1
    }
}
