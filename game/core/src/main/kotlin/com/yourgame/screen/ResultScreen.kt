package com.yourgame.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.yourgame.ClashGame
import com.yourgame.network.GameWebSocketClient

class ResultScreen(private val game: ClashGame) : ScreenAdapter() {

    private val sr = ShapeRenderer()

    private val btnX get() = Gdx.graphics.width  / 2f - BTN_W / 2f
    private val btnY get() = Gdx.graphics.height / 2f - BTN_H / 2f - 80f

    override fun show() {
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val fx = screenX.toFloat()
                val fy = Gdx.graphics.height - screenY.toFloat()
                if (fx >= btnX && fx <= btnX + BTN_W && fy >= btnY && fy <= btnY + BTN_H) {
                    game.setScreen(MatchmakingScreen(game))
                }
                return true
            }
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val gs = GameWebSocketClient.state
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        sr.projectionMatrix = game.camera.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0.18f, 0.38f, 0.72f, 1f)
        sr.rect(btnX, btnY, BTN_W, BTN_H)
        sr.end()

        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()
        gs.gameEnded?.let { e ->
            val isWin = e.winner == gs.playerIndex
            game.font.data.setScale(3f)
            game.font.color = if (isWin) Color.YELLOW else Color(1f, 0.3f, 0.3f, 1f)
            game.font.draw(game.batch, if (isWin) "YOU WIN!" else "YOU LOSE", w / 2f - 115f, h / 2f + 80f)
        }
        game.font.data.setScale(1.5f)
        game.font.color = Color.WHITE
        game.font.draw(game.batch, "PLAY AGAIN", btnX + 18f, btnY + BTN_H - 14f)
        game.batch.end()
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun dispose() = sr.dispose()

    companion object {
        private const val BTN_W = 180f
        private const val BTN_H = 60f
    }
}
