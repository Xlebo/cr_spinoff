package com.yourgame.server

import com.yourgame.shared.CardType
import com.yourgame.shared.TowerType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameLogicTest {

    // All towers are disabled so tower auto-attack doesn't interfere with unit-only tests.
    private fun stateWithoutTowers(): GameState =
        GameState().also { s -> s.towers.forEach { it.hp = 0 } }

    // ── Elixir & timer ────────────────────────────────────────────────────────

    @Test fun `elixir regenerates each tick`() {
        val state = GameState().also { it.elixir[0] = 5f; it.elixir[1] = 3f }
        GameLogic.tick(state)
        assertEquals(5f + ELIXIR_REGEN, state.elixir[0], 0.001f)
        assertEquals(3f + ELIXIR_REGEN, state.elixir[1], 0.001f)
    }

    @Test fun `elixir caps at MAX_ELIXIR`() {
        val state = GameState().also { it.elixir[0] = MAX_ELIXIR - 0.01f }
        GameLogic.tick(state)
        assertEquals(MAX_ELIXIR, state.elixir[0], 0.001f)
    }

    @Test fun `timer decrements by one tick interval`() {
        val state = GameState()
        val before = state.timeRemainingMs
        GameLogic.tick(state)
        assertEquals(before - TICK_MS, state.timeRemainingMs)
    }

    // ── Unit spawning ─────────────────────────────────────────────────────────

    @Test fun `applyCardPlay spawns knight at given position`() {
        val state = GameState()
        GameLogic.applyCardPlay(state, 0, CardType.KNIGHT, 500f, 400f)
        assertEquals(1, state.units.size)
        with(state.units[0]) {
            assertEquals(CardType.KNIGHT, card)
            assertEquals(500f, x)
            assertEquals(400f, y)
            assertEquals(1200, maxHp)
            assertEquals(1200, hp)
            assertEquals(0, owner)
            assertFalse(isFlying)
        }
    }

    @Test fun `archer spawns exactly 2 units`() {
        val state = GameState()
        GameLogic.applyCardPlay(state, 0, CardType.ARCHER, 500f, 400f)
        assertEquals(2, state.units.size)
        assertTrue(state.units.all { it.card == CardType.ARCHER && it.owner == 0 })
    }

    @Test fun `barbarians spawn exactly 4 units`() {
        val state = GameState()
        GameLogic.applyCardPlay(state, 1, CardType.BARBARIANS, 500f, 1000f)
        assertEquals(4, state.units.size)
        assertTrue(state.units.all { it.owner == 1 })
    }

    @Test fun `minions spawn 3 flying units`() {
        val state = GameState()
        GameLogic.applyCardPlay(state, 0, CardType.MINIONS, 500f, 400f)
        assertEquals(3, state.units.size)
        assertTrue(state.units.all { it.isFlying })
    }

    @Test fun `multi-spawn units do not all share the same position`() {
        val state = GameState()
        GameLogic.applyCardPlay(state, 0, CardType.BARBARIANS, 500f, 400f)
        val positions = state.units.map { it.x to it.y }.toSet()
        assertTrue(positions.size > 1, "Barbarians should spread across multiple positions")
    }

    // ── Fireball ──────────────────────────────────────────────────────────────

    @Test fun `fireball deals damage to unit inside aoe`() {
        val state = GameState()
        state.units += UnitData(state.nextUnitId(), CardType.KNIGHT, 500f, 900f, 1200, 1200, owner = 1)
        GameLogic.applyCardPlay(state, 0, CardType.FIREBALL, 500f, 900f)
        assertEquals(1200 - 600, state.units[0].hp)
    }

    @Test fun `fireball does not damage unit outside aoe`() {
        val state = GameState()
        val radius = CardDefs[CardType.FIREBALL].aoeRadius
        state.units += UnitData(state.nextUnitId(), CardType.KNIGHT, 500f + radius + 1f, 900f, 1200, 1200, owner = 1)
        GameLogic.applyCardPlay(state, 0, CardType.FIREBALL, 500f, 900f)
        assertEquals(1200, state.units[0].hp)
    }

    @Test fun `fireball damages tower inside aoe`() {
        val state = GameState()
        val tower = state.towers.first { it.owner == 1 && it.type == TowerType.PRINCESS }
        GameLogic.applyCardPlay(state, 0, CardType.FIREBALL, tower.x, tower.y)
        assertEquals(1400 - 600, tower.hp)
    }

    @Test fun `fireball does not spawn a unit`() {
        val state = GameState()
        GameLogic.applyCardPlay(state, 0, CardType.FIREBALL, 500f, 900f)
        assertTrue(state.units.isEmpty())
    }

    // ── Unit movement ─────────────────────────────────────────────────────────

    @Test fun `player 0 unit moves toward player 1 side`() {
        val state = GameState()
        GameLogic.applyCardPlay(state, 0, CardType.KNIGHT, 500f, 500f)
        val unit = state.units[0]
        GameLogic.tick(state)
        assertTrue(unit.y > 500f, "P0 knight should advance toward P1 side (higher Y)")
    }

    @Test fun `player 1 unit moves toward player 0 side`() {
        val state = GameState()
        GameLogic.applyCardPlay(state, 1, CardType.KNIGHT, 500f, 1300f)
        val unit = state.units[0]
        GameLogic.tick(state)
        assertTrue(unit.y < 1300f, "P1 knight should advance toward P0 side (lower Y)")
    }

    // ── Unit combat ───────────────────────────────────────────────────────────

    @Test fun `unit attacks target in melee range`() {
        val state = GameState()
        // Two opposing knights within melee range (100 wu < range 120 wu)
        state.units += UnitData(state.nextUnitId(), CardType.KNIGHT, 500f, 500f,  1200, 1200, owner = 0)
        state.units += UnitData(state.nextUnitId(), CardType.KNIGHT, 500f, 600f,  1200, 1200, owner = 1)
        val (k0, k1) = state.units
        GameLogic.tick(state)
        assertTrue(k0.hp < 1200, "k0 should have taken damage")
        assertTrue(k1.hp < 1200, "k1 should have taken damage")
    }

    @Test fun `unit does not attack when on cooldown`() {
        // Towers disabled so only unit-vs-unit combat occurs.
        val state = stateWithoutTowers()
        state.units += UnitData(state.nextUnitId(), CardType.KNIGHT, 500f, 500f, 1200, 1200, owner = 0, attackCooldown = 10)
        state.units += UnitData(state.nextUnitId(), CardType.KNIGHT, 500f, 600f, 1200, 1200, owner = 1)
        val (k0, k1) = state.units
        GameLogic.tick(state)
        assertEquals(1200, k1.hp, "k1 should not have taken damage — k0 is on cooldown")
        assertTrue(k0.hp < 1200, "k1 (no cooldown) should still attack k0")
    }

    @Test fun `giant ignores enemy troops and targets tower`() {
        val state = GameState()
        // Enemy troop directly in path
        state.units += UnitData(state.nextUnitId(), CardType.KNIGHT, 500f, 1400f, 1200, 1200, owner = 1)
        // Giant for player 0
        GameLogic.applyCardPlay(state, 0, CardType.GIANT, 500f, 500f)
        val giant = state.units.first { it.card == CardType.GIANT }
        val yBefore = giant.y

        GameLogic.tick(state)

        // Giant should still be moving toward a tower, not attacking the troop
        assertEquals(1200, state.units.first { it.card == CardType.KNIGHT }.hp,
            "Giant should ignore troops")
        assertTrue(giant.y > yBefore, "Giant should still be advancing")
    }

    // ── Tower combat ──────────────────────────────────────────────────────────

    @Test fun `tower attacks nearest enemy unit in range`() {
        val state = GameState()
        val p1Princess = state.towers.first { it.owner == 1 && it.type == TowerType.PRINCESS }
        // x = princess.x - 700 puts the unit exactly at princess range (700 wu)
        // but ~1005 wu from the P1 King, outside its 900 range — only this tower fires.
        state.units += UnitData(state.nextUnitId(), CardType.KNIGHT, p1Princess.x - 700f, p1Princess.y, 1200, 1200, owner = 0)
        val unit = state.units[0]

        GameLogic.tick(state)

        assertEquals(1200 - PRINCESS_DAMAGE, unit.hp, "Tower should deal exactly PRINCESS_DAMAGE")
        assertEquals(TOWER_ATTACK_TICKS, p1Princess.attackCooldown, "Tower cooldown should reset")
    }

    @Test fun `tower does not attack unit outside range`() {
        val state = GameState()
        val p1Princess = state.towers.first { it.owner == 1 && it.type == TowerType.PRINCESS }
        // Place unit outside range (1000 wu > 700 range)
        state.units += UnitData(state.nextUnitId(), CardType.KNIGHT, p1Princess.x, p1Princess.y - 1000f, 1200, 1200, owner = 0)

        GameLogic.tick(state)

        assertEquals(1200, state.units[0].hp, "Tower should not reach unit outside range")
    }

    @Test fun `tower does not attack on cooldown`() {
        val state = GameState()
        val p1Princess = state.towers.first { it.owner == 1 && it.type == TowerType.PRINCESS }
        p1Princess.attackCooldown = 5  // mid-cooldown
        // Same isolated position as the attack test — only this princess in range.
        state.units += UnitData(state.nextUnitId(), CardType.KNIGHT, p1Princess.x - 700f, p1Princess.y, 1200, 1200, owner = 0)

        GameLogic.tick(state)

        assertEquals(1200, state.units[0].hp, "Tower should not attack while on cooldown")
        assertEquals(4, p1Princess.attackCooldown, "Cooldown should decrement by 1")
    }

    // ── Dead unit removal ─────────────────────────────────────────────────────

    @Test fun `dead units are removed after tick`() {
        val state = GameState()
        state.units += UnitData(state.nextUnitId(), CardType.KNIGHT, 500f, 500f, 1200, 0, owner = 0)
        GameLogic.tick(state)
        assertTrue(state.units.isEmpty(), "Unit with hp=0 should be removed after tick")
    }

    @Test fun `unit killed this tick is removed same tick`() {
        val state = GameState()
        // Knight with 1 HP in melee range of a Mini PEKKA (380 dmg)
        state.units += UnitData(state.nextUnitId(), CardType.KNIGHT,    500f, 500f, 1200, 1,    owner = 0)
        state.units += UnitData(state.nextUnitId(), CardType.MINI_PEKKA, 500f, 600f, 1200, 1200, owner = 1)
        GameLogic.tick(state)
        assertFalse(state.units.any { it.card == CardType.KNIGHT }, "Killed knight should be removed")
    }

    // ── Win condition ─────────────────────────────────────────────────────────

    @Test fun `destroying p1 king tower sets winner to 0`() {
        val state = GameState()
        state.towers.first { it.owner == 1 && it.type == TowerType.KING }.hp = 0
        GameLogic.tick(state)
        assertEquals(0, state.winner)
    }

    @Test fun `destroying p0 king tower sets winner to 1`() {
        val state = GameState()
        state.towers.first { it.owner == 0 && it.type == TowerType.KING }.hp = 0
        GameLogic.tick(state)
        assertEquals(1, state.winner)
    }

    @Test fun `princess tower death alone does not end the game`() {
        val state = GameState()
        state.towers.first { it.owner == 1 && it.type == TowerType.PRINCESS }.hp = 0
        GameLogic.tick(state)
        assertEquals(-1, state.winner, "Only king destruction ends the game")
    }
}
