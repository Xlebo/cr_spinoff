package com.yourgame.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.yourgame.BuildConfig
import com.yourgame.ClashGame

class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initialize(ClashGame(BuildConfig.SERVER_URL), AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
        })
    }
}
