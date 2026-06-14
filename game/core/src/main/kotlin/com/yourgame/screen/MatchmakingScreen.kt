package com.yourgame.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.yourgame.ClashGame
import com.yourgame.network.GameWebSocketClient
import com.yourgame.network.Phase

class MatchmakingScreen(private val game: ClashGame) : ScreenAdapter() {

    override fun render(delta: Float) {
        if (GameWebSocketClient.state.phase == Phase.IN_GAME) {
            game.setScreen(GameScreen(game))
            return
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        val msg = when (GameWebSocketClient.state.phase) {
            Phase.CONNECTING  -> "Connecting..."
            Phase.MATCHMAKING -> "Waiting for opponent..."
            else              -> ""
        }

        game.batch.projectionMatrix = game.camera.combined
        game.batch.begin()
        game.font.color = Color.WHITE
        game.font.data.setScale(1.3f)
        game.font.draw(game.batch, msg, w / 2f - 110f, h / 2f)
        game.batch.end()
    }
}
