package com.yourgame.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.yourgame.ClashGame
import com.yourgame.network.GameWebSocketClient

class MenuScreen(private val game: ClashGame) : ScreenAdapter() {

    private val sr = ShapeRenderer()

    private val btnX get() = Gdx.graphics.width  / 2f - BTN_W / 2f
    private val btnY get() = Gdx.graphics.height / 2f - BTN_H / 2f

    override fun show() {
        Gdx.input.inputProcessor = object : InputAdapter() {
            override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
                val fx = screenX.toFloat()
                val fy = Gdx.graphics.height - screenY.toFloat()   // flip: Y=0 at bottom
                if (fx >= btnX && fx <= btnX + BTN_W && fy >= btnY && fy <= btnY + BTN_H) {
                    GameWebSocketClient.joinQueue()
                    game.setScreen(MatchmakingScreen(game))
                }
                return true
            }
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        sr.projectionMatrix = game.camera.combined
        sr.begin(ShapeRenderer.ShapeType.Filled)
        sr.color = Color(0.18f, 0.38f, 0.72f, 1f)
        sr.rect(btnX, btnY, BTN_W, BTN_H)
        sr.end()

        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()
        game.font.color = Color.WHITE
        game.font.data.setScale(2.5f)
        game.font.draw(game.batch, "CLASH MVP", w / 2f - 95f, h / 2f + 180f)
        game.font.data.setScale(1.8f)
        game.font.draw(game.batch, "PLAY", btnX + BTN_W / 2f - 28f, btnY + BTN_H - 14f)
        game.batch.end()
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun dispose() = sr.dispose()

    companion object {
        private const val BTN_W = 160f
        private const val BTN_H = 60f
    }
}
