package com.yourgame.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.yourgame.network.GameWebSocketClient
import com.yourgame.network.Phase
import com.yourgame.render.ArenaRenderer
import com.yourgame.shared.PlayCard
import com.yourgame.ui.CardHandUI

class CardPlacementController(
    private val ui: CardHandUI,
    private val arena: ArenaRenderer,
) : InputAdapter() {

    var selectedCardIndex: Int? = null
        private set

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val state = GameWebSocketClient.state
        if (state.phase != Phase.IN_GAME) return false

        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        val flippedY = h - screenY   // LibGDX touch Y=0 is top; flip so Y=0 is bottom

        // Card tray tap
        val tapped = ui.hitTest(screenX.toFloat(), flippedY, w)
        if (tapped != null) {
            selectedCardIndex = if (selectedCardIndex == tapped) null else tapped
            return true
        }

        // Arena placement — only if a card is selected
        val sel = selectedCardIndex ?: return false
        val card = ui.cardAt(sel)
        val elixir = state.lastUpdate?.myElixir ?: 0f
        if (elixir < ui.costOf(card)) return true  // pre-check; stay selected

        val wx = arena.toWorldX(screenX.toFloat())
        val wy = arena.toWorldY(flippedY)

        if (wx !in 0f..1000f || wy !in 0f..1800f) {
            selectedCardIndex = null
            return false
        }

        val pi = state.playerIndex
        val inOwnHalf = if (pi == 0) wy in 100f..850f else wy in 950f..1700f
        if (!inOwnHalf) return true  // wrong half, stay selected so the player can try again

        GameWebSocketClient.enqueue(PlayCard(card, wx, wy))
        selectedCardIndex = null
        return true
    }
}
