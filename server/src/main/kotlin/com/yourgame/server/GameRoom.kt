package com.yourgame.server

import com.yourgame.shared.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import org.slf4j.LoggerFactory
import java.util.UUID

class GameRoom(
    private val player0Id: String,
    private val player0: DefaultWebSocketServerSession,
    private val player1Id: String,
    private val player1: DefaultWebSocketServerSession,
) {
    val roomId: String = UUID.randomUUID().toString().take(8)
    private val log = LoggerFactory.getLogger(GameRoom::class.java)
    private val state = GameState()
    private val scope = CoroutineScope(Dispatchers.Default)

    // Card plays arrive from WebSocket coroutines and are drained at the start of each tick.
    private val cardPlays = Channel<Pair<Int, PlayCard>>(capacity = Channel.UNLIMITED)

    // ── Entry point ───────────────────────────────────────────────────────────

    suspend fun run(playerIndex: Int) {
        if (playerIndex == 1) {
            sendMatchFound()
            startGameLoop()
        }

        val session  = if (playerIndex == 0) player0  else player1
        val playerId = if (playerIndex == 0) player0Id else player1Id

        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) continue
                val text = frame.readText()
                val msg = try {
                    GameJson.decodeFromString<ClientMessage>(text)
                } catch (e: Exception) {
                    log.warn("[$roomId] Bad message from player $playerIndex: $text")
                    continue
                }
                onClientMessage(playerIndex, msg)
            }
        } finally {
            log.info("[$roomId] Player $playerId (index $playerIndex) disconnected")
            scope.cancel()
        }
    }

    // ── Game loop ─────────────────────────────────────────────────────────────

    private fun startGameLoop() {
        scope.launch {
            try {
                runLoop()
                sendGameEnded()
            } finally {
                withContext(NonCancellable) { closeAll() }
            }
        }
    }

    private suspend fun runLoop() {
        var nextTick = System.currentTimeMillis() + TICK_MS
        while (!state.isOver()) {
            processCardPlays()
            GameLogic.tick(state)
            broadcastState()
            val wait = nextTick - System.currentTimeMillis()
            if (wait > 0) delay(wait)
            nextTick += TICK_MS
        }
    }

    // ── Card play processing ──────────────────────────────────────────────────

    // Drains the card play channel at the start of each tick, validating and
    // spawning units. Runs inside the game loop coroutine so state access is safe.
    private suspend fun processCardPlays() {
        while (true) {
            val (playerIndex, play) = cardPlays.tryReceive().getOrNull() ?: break
            when (val r = CardValidator.validate(state, playerIndex, play.card, play.x, play.y)) {
                is CardValidator.Result.Ok -> {
                    state.elixir[playerIndex] -= CardDefs[play.card].elixirCost
                    GameLogic.applyCardPlay(state, playerIndex, play.card, play.x, play.y)
                    log.info("[$roomId] Player $playerIndex played ${play.card} at (${play.x}, ${play.y})")
                }
                is CardValidator.Result.Rejected -> sendRejection(playerIndex, r.reason)
            }
        }
    }

    private fun onClientMessage(playerIndex: Int, msg: ClientMessage) {
        when (msg) {
            is PlayCard   -> cardPlays.trySend(playerIndex to msg)
            is LeaveMatch -> { state.winner = 1 - playerIndex; scope.cancel() }
            else          -> {}
        }
    }

    // ── Broadcast helpers ─────────────────────────────────────────────────────

    private suspend fun broadcastState() {
        val towers = state.towerStates()
        val units  = state.unitStates()
        for (p in 0..1) {
            val session = if (p == 0) player0 else player1
            session.send(
                GameJson.encodeToString<ServerMessage>(
                    GameStateUpdate(
                        tick              = state.tick,
                        timeRemainingMs   = state.timeRemainingMs,
                        myElixir          = state.elixir[p],
                        opponentElixir    = state.elixir[1 - p],
                        units             = units,
                        towers            = towers,
                    )
                )
            )
        }
    }

    private suspend fun sendMatchFound() {
        player0.send(GameJson.encodeToString<ServerMessage>(MatchFound(roomId, playerIndex = 0)))
        player1.send(GameJson.encodeToString<ServerMessage>(MatchFound(roomId, playerIndex = 1)))
        log.info("[$roomId] MATCH_FOUND sent to both players")
    }

    private suspend fun sendGameEnded() {
        val (winner, reason) = if (state.winner >= 0)
            state.winner to GameEndReason.KING_DESTROYED
        else
            state.tieWinner() to GameEndReason.TIMER_EXPIRED
        log.info("[$roomId] Game over — winner=$winner reason=$reason")
        val msg = GameJson.encodeToString<ServerMessage>(GameEnded(winner, reason))
        runCatching { player0.send(msg) }
        runCatching { player1.send(msg) }
    }

    private suspend fun sendRejection(playerIndex: Int, reason: RejectionReason) {
        val session = if (playerIndex == 0) player0 else player1
        runCatching {
            session.send(GameJson.encodeToString<ServerMessage>(CardPlayRejected(reason)))
        }
    }

    private suspend fun closeAll() {
        val reason = CloseReason(CloseReason.Codes.NORMAL, "Game over")
        runCatching { player0.close(reason) }
        runCatching { player1.close(reason) }
    }
}
