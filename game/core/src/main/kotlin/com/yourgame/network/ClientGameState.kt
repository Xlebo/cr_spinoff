package com.yourgame.network

import com.yourgame.shared.GameEnded
import com.yourgame.shared.GameStateUpdate

enum class Phase { CONNECTING, MATCHMAKING, IN_GAME, GAME_OVER }

data class ClientGameState(
    val phase: Phase = Phase.CONNECTING,
    val playerIndex: Int = -1,
    val roomId: String = "",
    val lastUpdate: GameStateUpdate? = null,
    val gameEnded: GameEnded? = null,
)
