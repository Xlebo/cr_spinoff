package com.yourgame.server

import com.yourgame.shared.CardType
import com.yourgame.shared.TowerType
import kotlin.math.max
import kotlin.math.sqrt

object GameLogic {

    // ── Main tick ─────────────────────────────────────────────────────────────

    fun tick(state: GameState) {
        state.tick++

        // Advance elixir for both players
        for (i in state.elixir.indices) {
            state.elixir[i] = minOf(MAX_ELIXIR, state.elixir[i] + ELIXIR_REGEN)
        }

        // Countdown timer
        state.timeRemainingMs -= TICK_MS

        // Decrease all cooldowns first so units/towers that just finished
        // their cooldown this tick can act immediately
        for (u in state.units)  if (u.attackCooldown  > 0) u.attackCooldown--
        for (t in state.towers) if (t.attackCooldown > 0) t.attackCooldown--

        // Move and attack units
        for (unit in state.units) {
            if (!unit.alive) continue
            val target = findUnitTarget(unit, state) ?: continue
            val tx = tx(target); val ty = ty(target)
            val dist = dist(unit.x, unit.y, tx, ty)
            val def  = CardDefs[unit.card]

            if (dist <= def.range) {
                if (unit.attackCooldown == 0) {
                    dealDamage(target, def.damage)
                    unit.attackCooldown = def.attackSpeedTicks
                }
                // In range but on cooldown — stand still
            } else {
                moveToward(unit, tx, ty, def.speed / 20f)
            }
        }

        // Tower auto-attack — targets nearest enemy unit in range
        for (tower in state.towers) {
            if (!tower.alive || tower.attackCooldown > 0) continue
            val target = findNearestEnemyUnit(tower, state) ?: continue
            if (dist(tower.x, tower.y, target.x, target.y) <= tower.range) {
                target.hp = max(0, target.hp - tower.damage)
                tower.attackCooldown = TOWER_ATTACK_TICKS
            }
        }

        // Remove dead units
        state.units.removeAll { !it.alive }

        // Win condition: opponent's king tower destroyed
        checkWin(state)
    }

    // ── Card play ─────────────────────────────────────────────────────────────

    fun applyCardPlay(state: GameState, playerIndex: Int, card: CardType, x: Float, y: Float) {
        val def = CardDefs[card]

        if (def.isSpell) {
            applyAoe(state, x, y, def.aoeRadius, def.damage)
            return
        }

        for ((dx, dy) in spawnOffsets(def.spawnCount)) {
            state.units += UnitData(
                id       = state.nextUnitId(),
                card     = card,
                x        = x + dx,
                y        = y + dy,
                maxHp    = def.hp,
                hp       = def.hp,
                owner    = playerIndex,
                isFlying = def.isFlying,
            )
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // Returns the nearest valid target for this unit (enemy unit or tower).
    // Giant only targets buildings; ground-targeting units ignore flying units.
    private fun findUnitTarget(unit: UnitData, state: GameState): Any? {
        val opp = 1 - unit.owner
        val def = CardDefs[unit.card]

        val candidateUnits: List<UnitData> = when (def.targets) {
            TargetType.BUILDINGS -> emptyList()
            TargetType.GROUND    -> state.units.filter { it.owner == opp && it.alive && !it.isFlying }
            TargetType.ALL       -> state.units.filter { it.owner == opp && it.alive }
        }

        val nearestUnit = candidateUnits.minByOrNull { distSq(unit.x, unit.y, it.x, it.y) }
        if (nearestUnit != null) return nearestUnit

        return state.towers.filter { it.owner == opp && it.alive }
            .minByOrNull { distSq(unit.x, unit.y, it.x, it.y) }
    }

    private fun findNearestEnemyUnit(tower: TowerData, state: GameState): UnitData? =
        state.units
            .filter { it.owner != tower.owner && it.alive }
            .minByOrNull { distSq(tower.x, tower.y, it.x, it.y) }

    private fun dealDamage(target: Any, damage: Int) {
        when (target) {
            is UnitData  -> target.hp = max(0, target.hp - damage)
            is TowerData -> target.hp = max(0, target.hp - damage)
        }
    }

    private fun tx(target: Any): Float = when (target) { is UnitData -> target.x; is TowerData -> target.x; else -> 0f }
    private fun ty(target: Any): Float = when (target) { is UnitData -> target.y; is TowerData -> target.y; else -> 0f }

    private fun moveToward(unit: UnitData, tx: Float, ty: Float, speedPerTick: Float) {
        val dx = tx - unit.x; val dy = ty - unit.y
        val d = sqrt(dx * dx + dy * dy)
        if (d <= speedPerTick) { unit.x = tx; unit.y = ty }
        else { unit.x += dx / d * speedPerTick; unit.y += dy / d * speedPerTick }
    }

    private fun applyAoe(state: GameState, cx: Float, cy: Float, radius: Float, damage: Int) {
        val r2 = radius * radius
        state.units.filter  { distSq(cx, cy, it.x, it.y) <= r2 }.forEach { it.hp  = max(0, it.hp  - damage) }
        state.towers.filter { distSq(cx, cy, it.x, it.y) <= r2 }.forEach { it.hp  = max(0, it.hp  - damage) }
    }

    private fun checkWin(state: GameState) {
        val p0KingDead = state.towers.any { it.owner == 0 && it.type == TowerType.KING && !it.alive }
        val p1KingDead = state.towers.any { it.owner == 1 && it.type == TowerType.KING && !it.alive }
        if (p1KingDead) state.winner = 0
        if (p0KingDead) state.winner = 1
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2; val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }

    private fun distSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2; val dy = y1 - y2
        return dx * dx + dy * dy
    }
}
