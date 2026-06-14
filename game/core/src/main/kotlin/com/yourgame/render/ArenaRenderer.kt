package com.yourgame.render

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.yourgame.network.ClientGameState
import com.yourgame.shared.CardType
import com.yourgame.shared.TowerType

class ArenaRenderer {

    private val sr = ShapeRenderer()

    private var scale     = 1f
    private var arenaLeft = 0f

    // Called from ClashGame.resize()
    fun resize(screenW: Int, screenH: Int) {
        scale     = screenH / 1800f
        arenaLeft = (screenW - 1000f * scale) / 2f
    }

    private fun wx(x: Float) = arenaLeft + x * scale
    private fun wy(y: Float) = y * scale               // Y=0 at screen bottom
    private fun ws(s: Float) = s * scale

    // Screen-to-world converters used by CardPlacementController.
    fun toWorldX(screenX: Float) = (screenX - arenaLeft) / scale
    fun toWorldY(flippedScreenY: Float) = flippedScreenY / scale

    fun renderSelectionOverlay(playerIndex: Int, proj: Matrix4) {
        val (yMin, yMax) = if (playerIndex == 0) 100f to 850f else 950f to 1700f
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        sr.projectionMatrix = proj
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(1f, 1f, 0.3f, 0.12f)
        sr.rect(arenaLeft + ws(50f), wy(yMin), ws(900f), wy(yMax) - wy(yMin))
        sr.end()
        Gdx.gl.glDisable(GL20.GL_BLEND)
    }

    fun render(state: ClientGameState, proj: Matrix4) {
        val u = state.lastUpdate ?: return

        sr.projectionMatrix = proj
        sr.begin(ShapeRenderer.ShapeType.Filled)

        // ── Field ──────────────────────────────────────────────────────────────
        sr.color = COLOR_ARENA
        sr.rect(arenaLeft, 0f, ws(1000f), ws(1800f))

        // River (centre divider at Y 870–930)
        sr.color = COLOR_RIVER
        sr.rect(arenaLeft, wy(870f), ws(1000f), ws(60f))

        // ── Towers ─────────────────────────────────────────────────────────────
        for (t in u.towers) {
            if (!t.alive) continue
            val isKing = t.type == TowerType.KING
            val tw = if (isKing) ws(80f) else ws(60f)
            val th = if (isKing) ws(80f) else ws(60f)
            val cx = wx(t.x)
            val cy = wy(t.y)
            sr.color = if (t.owner == 0) COLOR_P0_TOWER else COLOR_P1_TOWER
            sr.rect(cx - tw / 2, cy - th / 2, tw, th)
            drawHpBar(cx, cy + th / 2 + ws(6f), tw, ws(10f), t.hp, t.maxHp)
        }

        // ── Units ──────────────────────────────────────────────────────────────
        for (unit in u.units) {
            val r  = unitRadius(unit.card)
            val cx = wx(unit.x)
            val cy = wy(unit.y)
            sr.color = if (unit.owner == 0) COLOR_P0_UNIT else COLOR_P1_UNIT
            sr.circle(cx, cy, r, 12)
            // White dot marks flying units (Minions)
            if (unit.isFlying) {
                sr.color = Color.WHITE
                sr.circle(cx, cy, r * 0.3f, 8)
            }
            drawHpBar(cx, cy + r + ws(4f), ws(36f), ws(7f), unit.hp, unit.maxHp)
        }

        // ── Elixir bar (overlaid at bottom of arena) ───────────────────────────
        val barW = ws(700f)
        val barH = ws(28f)
        val barX = arenaLeft + (ws(1000f) - barW) / 2f
        val barY = ws(20f)
        sr.color = Color.DARK_GRAY
        sr.rect(barX, barY, barW, barH)
        sr.color = COLOR_ELIXIR
        sr.rect(barX, barY, barW * (u.myElixir / 10f).coerceIn(0f, 1f), barH)
        sr.color = Color(0.1f, 0.1f, 0.1f, 1f)
        for (i in 1..9) {
            sr.rect(barX + barW * i / 10f - ws(1f), barY, ws(2f), barH)
        }

        sr.end()
    }

    private fun drawHpBar(cx: Float, y: Float, w: Float, h: Float, hp: Int, maxHp: Int) {
        val frac = (hp.toFloat() / maxHp).coerceIn(0f, 1f)
        sr.color = Color.DARK_GRAY
        sr.rect(cx - w / 2, y, w, h)
        sr.color = when {
            frac > 0.5f  -> Color.GREEN
            frac > 0.25f -> Color.YELLOW
            else         -> Color.RED
        }
        sr.rect(cx - w / 2, y, w * frac, h)
    }

    private fun unitRadius(card: CardType): Float = ws(
        when (card) {
            CardType.GIANT    -> 28f
            CardType.ARCHER,
            CardType.MUSKETEER,
            CardType.MINIONS  -> 14f
            else              -> 20f
        }
    )

    fun dispose() = sr.dispose()

    companion object {
        private val COLOR_ARENA    = Color(0.12f, 0.45f, 0.12f, 1f)
        private val COLOR_RIVER    = Color(0.20f, 0.35f, 0.65f, 1f)
        private val COLOR_P0_TOWER = Color(0.20f, 0.40f, 0.90f, 1f)
        private val COLOR_P1_TOWER = Color(0.90f, 0.20f, 0.20f, 1f)
        private val COLOR_P0_UNIT  = Color(0.45f, 0.75f, 1.00f, 1f)
        private val COLOR_P1_UNIT  = Color(1.00f, 0.50f, 0.25f, 1f)
        private val COLOR_ELIXIR   = Color(0.65f, 0.10f, 0.85f, 1f)
    }
}
