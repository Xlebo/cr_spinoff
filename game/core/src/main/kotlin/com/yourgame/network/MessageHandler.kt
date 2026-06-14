package com.yourgame.network

import com.yourgame.shared.*
import java.util.concurrent.atomic.AtomicReference

object MessageHandler {

    fun handle(msg: ServerMessage, stateRef: AtomicReference<ClientGameState>) {
        val cur = stateRef.get()
        val next = when (msg) {
            is MatchFound -> {
                println("[Network] MATCH_FOUND roomId=${msg.roomId} playerIndex=${msg.playerIndex}")
                cur.copy(phase = Phase.IN_GAME, playerIndex = msg.playerIndex, roomId = msg.roomId)
            }
            is GameStateUpdate -> {
                if (msg.tick % 20 == 0) {
                    println("[Network] tick=${msg.tick} timeMs=${msg.timeRemainingMs} elixir=${"%.1f".format(msg.myElixir)} units=${msg.units.size}")
                }
                cur.copy(lastUpdate = msg)
            }
            is GameEnded -> {
                println("[Network] GAME_ENDED winner=${msg.winner} reason=${msg.reason}")
                cur.copy(phase = Phase.GAME_OVER, gameEnded = msg)
            }
            is CardPlayRejected -> {
                println("[Network] CARD_PLAY_REJECTED reason=${msg.reason}")
                cur
            }
        }
        stateRef.set(next)
    }
}
