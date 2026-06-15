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

    // All dimensions in screen pixels; updated in resize().
    var trayH   = 100f; private set
    private var cardW     = 56f
    private var cardH     = 84f
    private var gap       = 4f
    private var slotY     = 8f
    private var fontScale = 0.9f

    /** Call whenever the screen size changes (before ArenaRenderer.resize). */
    fun resize(screenW: Float) {
        gap       = maxOf(3f, screenW * 0.007f)
        cardW     = (screenW - (cards.size + 1) * gap) / cards.size
        cardH     = cardW * 1.45f
        slotY     = gap
        trayH     = cardH + slotY * 2f
        fontScale = (cardW / 52f).coerceIn(0.75f, 3f)
    }

    fun costOf(card: CardType): Int = COSTS.getValue(card)
    fun cardAt(index: Int): CardType = cards[index]

    private fun slotX(screenW: Float, i: Int): Float {
        val totalW = cards.size * cardW + (cards.size - 1) * gap
        return (screenW - totalW) / 2f + i * (cardW + gap)
    }

    fun hitTest(touchX: Float, flippedY: Float, screenW: Float): Int? {
        if (flippedY > trayH) return null
        for (i in cards.indices) {
            val x = slotX(screenW, i)
            if (touchX >= x && touchX <= x + cardW) return i
        }
        return null
    }

    fun renderShapes(proj: Matrix4, screenW: Float, elixir: Float, selectedIndex: Int?) {
        sr.projectionMatrix = proj
        sr.begin(ShapeRenderer.ShapeType.Filled)

        sr.color = Color(0.08f, 0.08f, 0.10f, 1f)
        sr.rect(0f, 0f, screenW, trayH)

        for (i in cards.indices) {
            val cost = COSTS.getValue(cards[i])
            sr.color = when {
                i == selectedIndex -> Color(0.95f, 0.82f, 0.08f, 1f)
                elixir < cost      -> Color(0.18f, 0.18f, 0.22f, 1f)
                else               -> Color(0.18f, 0.38f, 0.72f, 1f)
            }
            sr.rect(slotX(screenW, i), slotY, cardW, cardH)
        }

        sr.end()
    }

    fun drawGlyphs(batch: SpriteBatch, screenW: Float, elixir: Float, selectedIndex: Int?) {
        val prevX = font.data.scaleX
        val prevY = font.data.scaleY
        font.data.setScale(fontScale)

        val lineH  = font.lineHeight
        val costH  = lineH * 0.85f

        for (i in cards.indices) {
            val card = cards[i]
            val cost = COSTS.getValue(card)
            val x    = slotX(screenW, i)
            font.color = if (elixir >= cost) Color.WHITE else Color(0.45f, 0.45f, 0.45f, 1f)
            // Name centred horizontally in the card, near the top
            font.draw(batch, LABELS.getValue(card), x + gap / 2f, slotY + cardH - gap)
            // Elixir cost bottom-right
            font.color = ELIXIR_COLOR
            font.data.setScale(fontScale * 0.85f)
            font.draw(batch, cost.toString(), x + cardW - lineH * 0.5f, slotY + costH + gap)
            font.data.setScale(fontScale)
        }

        font.data.setScale(prevX, prevY)
    }

    fun dispose() = sr.dispose()

    companion object {
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
            CardType.MINIONS    to "Minion",
            CardType.BARBARIANS to "Barbs",
            CardType.MUSKETEER  to "Musk",
            CardType.MINI_PEKKA to "M.Pkka",
        )
    }
}
