package com.yourgame.network

import com.yourgame.shared.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.encodeToString
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

object GameWebSocketClient {

    private val _state = AtomicReference(ClientGameState())
    val state: ClientGameState get() = _state.get()

    val playerId: String = UUID.randomUUID().toString()

    private val httpClient = HttpClient(CIO) {
        install(WebSockets)
    }

    // Messages queued by the game logic to be sent to the server.
    // Named txQueue to avoid shadowing DefaultClientWebSocketSession.outgoing.
    private val txQueue = Channel<String>(capacity = Channel.UNLIMITED)

    fun start(serverUrl: String, scope: CoroutineScope) {
        scope.launch { connectLoop(serverUrl) }
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
