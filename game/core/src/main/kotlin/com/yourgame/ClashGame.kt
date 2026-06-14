package com.yourgame

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.yourgame.network.GameWebSocketClient
import com.yourgame.screen.MenuScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class ClashGame(private val serverUrl: String = "ws://localhost:8080/ws") : Game() {

    lateinit var batch: SpriteBatch
    lateinit var font: BitmapFont
    lateinit var camera: OrthographicCamera
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun create() {
        batch = SpriteBatch()
        font = BitmapFont()
        camera = OrthographicCamera()
        resize(Gdx.graphics.width, Gdx.graphics.height)
        GameWebSocketClient.start(serverUrl, scope)
        setScreen(MenuScreen(this))
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.update()
        super.resize(width, height)
    }

    // Dispose the departing screen automatically so each screen can clean up its GL resources.
    override fun setScreen(screen: Screen?) {
        val old = this.screen
        super.setScreen(screen)
        old?.dispose()
    }

    override fun dispose() {
        screen?.dispose()
        batch.dispose()
        font.dispose()
        scope.cancel()
    }
}
