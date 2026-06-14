package com.yourgame.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.yourgame.shared.CardType

class CardHandUI(private val font: BitmapFont) {

    private val sr = ShapeRenderer()
    private val cards = CardType.entries.toList()

    fun costOf(card: CardType): Int = COSTS.getValue(card)
    fun cardAt(index: Int): CardType = cards[index]

    private fun slotX(screenW: Float, i: Int): Float {
        val totalW = cards.size * CARD_W + (cards.size - 1) * GAP
        return (screenW - totalW) / 2f + i * (CARD_W + GAP)
    }

    // flippedY = screenH - rawTouchY  (Y=0 at bottom)
    fun hitTest(touchX: Float, flippedY: Float, screenW: Float): Int? {
        if (flippedY > TRAY_H) return null
        for (i in cards.indices) {
            val x = slotX(screenW, i)
            if (touchX >= x && touchX <= x + CARD_W) return i
        }
        return null
    }

    fun renderShapes(proj: Matrix4, screenW: Float, elixir: Float, selectedIndex: Int?) {
        sr.projectionMatrix = proj
        sr.begin(ShapeRenderer.ShapeType.Filled)

        sr.color = Color(0.08f, 0.08f, 0.10f, 1f)
        sr.rect(0f, 0f, screenW, TRAY_H)

        for (i in cards.indices) {
            val cost = COSTS.getValue(cards[i])
            sr.color = when {
                i == selectedIndex -> Color(0.95f, 0.82f, 0.08f, 1f)
                elixir < cost      -> Color(0.18f, 0.18f, 0.22f, 1f)
                else               -> Color(0.18f, 0.38f, 0.72f, 1f)
            }
            sr.rect(slotX(screenW, i), SLOT_Y, CARD_W, CARD_H)
        }

        sr.end()
    }

    // Call with an already-open SpriteBatch.
    fun drawGlyphs(batch: SpriteBatch, screenW: Float, elixir: Float, selectedIndex: Int?) {
        val prevX = font.data.scaleX
        val prevY = font.data.scaleY
        font.data.setScale(0.55f)

        for (i in cards.indices) {
            val card = cards[i]
            val cost = COSTS.getValue(card)
            val x = slotX(screenW, i)
            font.color = if (elixir >= cost) Color.WHITE else Color(0.45f, 0.45f, 0.45f, 1f)
            font.draw(batch, LABELS.getValue(card), x + 3f, SLOT_Y + CARD_H - 4f)
            font.color = ELIXIR_COLOR
            font.draw(batch, cost.toString(), x + CARD_W - 11f, SLOT_Y + 15f)
        }

        font.data.setScale(prevX, prevY)
    }

    fun dispose() = sr.dispose()

    companion object {
        const val TRAY_H = 88f
        private const val CARD_W = 52f
        private const val CARD_H = 72f
        private const val GAP = 4f
        private const val SLOT_Y = 8f

        private val ELIXIR_COLOR = Color(0.65f, 0.10f, 0.85f, 1f)

        val COSTS = mapOf(
            CardType.KNIGHT     to 3,
            CardType.ARCHER     to 3,
            CardType.GIANT      to 5,
            CardType.FIREBALL   to 4,
            CardType.MINIONS    to 3,
            CardType.BARBARIANS to 5,
            CardType.MUSKETEER  to 4,
            CardType.MINI_PEKKA to 4,
        )

        private val LABELS = mapOf(
            CardType.KNIGHT     to "Knight",
            CardType.ARCHER     to "Archer",
            CardType.GIANT      to "Giant",
            CardType.FIREBALL   to "Fire",
            CardType.MINIONS    to "Minio",
            CardType.BARBARIANS to "Barbs",
            CardType.MUSKETEER  to "Musk",
            CardType.MINI_PEKKA to "M.Pkka",
        )
    }
}
