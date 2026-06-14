package com.yourgame.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.yourgame.ClashGame
import com.yourgame.input.CardPlacementController
import com.yourgame.network.GameWebSocketClient
import com.yourgame.network.Phase
import com.yourgame.render.ArenaRenderer
import com.yourgame.ui.CardHandUI

class GameScreen(private val game: ClashGame) : ScreenAdapter() {

    private val arena = ArenaRenderer()
    private val cardUI = CardHandUI(game.font)
    private val controller = CardPlacementController(cardUI, arena)

    override fun show() {
        arena.resize(Gdx.graphics.width, Gdx.graphics.height)
        Gdx.input.inputProcessor = controller
    }

    override fun resize(width: Int, height: Int) {
        arena.resize(width, height)
    }

    override fun render(delta: Float) {
        val gs = GameWebSocketClient.state
        if (gs.phase == Phase.GAME_OVER) {
            game.setScreen(ResultScreen(game))
            return
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val elixir = gs.lastUpdate?.myElixir ?: 0f
        val sel = controller.selectedCardIndex
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        arena.render(gs, game.camera.combined)
        if (sel != null) arena.renderSelectionOverlay(gs.playerIndex, game.camera.combined)
        cardUI.renderShapes(game.camera.combined, w, elixir, sel)

        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()
        game.font.color = com.badlogic.gdx.graphics.Color.WHITE
        game.font.data.setScale(1.5f)
        gs.lastUpdate?.let { u ->
            val secs = (u.timeRemainingMs / 1000).toInt()
            game.font.draw(game.batch, "${secs / 60}:${"%02d".format(secs % 60)}", w / 2f - 18f, h - 6f)
        }
        cardUI.drawGlyphs(game.batch, w, elixir, sel)
        game.batch.end()
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        arena.dispose()
        cardUI.dispose()
    }
}
