package com.yourgame.network

import com.yourgame.shared.*
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

object GameWebSocketClient {

    private val _state = AtomicReference(ClientGameState())
    val state: ClientGameState get() = _state.get()

    val playerId: String = UUID.randomUUID().toString()

    private val httpClient = HttpClient {
        install(WebSockets)
    }

    private val txQueue = Channel<String>(capacity = Channel.UNLIMITED)

    // Set to true when the user explicitly presses PLAY.
    // The connect loop waits on this before sending JOIN_QUEUE, so a second
    // player who hasn't tapped PLAY yet never enters the matchmaking queue.
    private val wantsToQueue = AtomicBoolean(false)
    private val joinSignal   = Channel<Unit>(Channel.CONFLATED)

    fun start(serverUrl: String, scope: CoroutineScope) {
        scope.launch { connectLoop(serverUrl) }
    }

    /** Call when the user taps PLAY. Safe to call before the WebSocket connects. */
    fun joinQueue() {
        wantsToQueue.set(true)
        joinSignal.trySend(Unit)
    }

    fun enqueue(msg: ClientMessage) {
        txQueue.trySend(GameJson.encodeToString(msg))
    }

    private suspend fun connectLoop(serverUrl: String) {
        while (true) {
            _state.set(ClientGameState(phase = Phase.CONNECTING))
            println("[Network] Connecting to $serverUrl ...")
            try {
                httpClient.webSocket(serverUrl) {
                    // Wait until the user has pressed PLAY before joining the queue.
                    if (!wantsToQueue.get()) joinSignal.receive()

                    _state.set(ClientGameState(phase = Phase.MATCHMAKING))
                    println("[Network] Connected. Sending JOIN_QUEUE playerId=$playerId")
                    send(Frame.Text(GameJson.encodeToString<ClientMessage>(JoinQueue(playerId))))

                    val senderJob = launch {
                        while (true) send(Frame.Text(txQueue.receive()))
                    }

                    for (frame in incoming) {
                        if (frame !is Frame.Text) continue
                        val text = frame.readText()
                        val msg = try {
                            GameJson.decodeFromString<ServerMessage>(text)
                        } catch (e: Exception) {
                            println("[Network] Unparseable frame: ${e.message}")
                            continue
                        }
                        MessageHandler.handle(msg, _state)
                    }

                    senderJob.cancel()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("[Network] Connection error: ${e.message}")
            }
            println("[Network] Disconnected. Reconnecting in 3s...")
            delay(3_000)
        }
    }
}
