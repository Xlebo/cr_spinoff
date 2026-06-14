package com.yourgame.desktop

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.yourgame.ClashGame

fun main() {
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Clash MVP")
        setWindowedMode(480, 854)
        setResizable(false)
        setForegroundFPS(60)
    }
    Lwjgl3Application(ClashGame(), config)
}
