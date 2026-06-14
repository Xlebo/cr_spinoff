package com.yourgame.server

import io.ktor.server.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

object MatchmakingService {
    private val log = LoggerFactory.getLogger(MatchmakingService::class.java)

    private data class WaitingPlayer(
        val playerId: String,
        val session: DefaultWebSocketServerSession,
        val matched: CompletableDeferred<Pair<Int, GameRoom>>,
    )

    private val mutex = Mutex()
    private var waiting: WaitingPlayer? = null

    suspend fun join(playerId: String, session: DefaultWebSocketServerSession) {
        log.info("Player $playerId joined queue")
        val myDeferred = CompletableDeferred<Pair<Int, GameRoom>>()

        mutex.withLock {
            val w = waiting
            if (w == null) {
                waiting = WaitingPlayer(playerId, session, myDeferred)
                log.info("Player $playerId is waiting for an opponent")
            } else {
                waiting = null
                val room = GameRoom(w.playerId, w.session, playerId, session)
                log.info("Match found: ${w.playerId} vs $playerId → room ${room.roomId}")
                w.matched.complete(Pair(0, room))
                myDeferred.complete(Pair(1, room))
            }
        }

        val (playerIndex, room) = myDeferred.await()
        room.run(playerIndex)
    }
}
