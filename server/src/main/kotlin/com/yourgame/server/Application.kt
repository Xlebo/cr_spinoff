package com.yourgame.server

import com.yourgame.shared.ClientMessage
import com.yourgame.shared.GameJson
import com.yourgame.shared.JoinQueue
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
}

fun Application.module() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(30)
    }

    routing {
        get("/health") {
            call.respondText("OK")
        }

        webSocket("/ws") {
            val frame = runCatching { incoming.receive() }.getOrNull() as? Frame.Text ?: run {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Expected text frame"))
                return@webSocket
            }
            val msg = runCatching {
                GameJson.decodeFromString<ClientMessage>(frame.readText())
            }.getOrElse {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Invalid JSON"))
                return@webSocket
            }
            if (msg !is JoinQueue) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Expected JOIN_QUEUE"))
                return@webSocket
            }
            MatchmakingService.join(msg.playerId, this)
        }
    }
}
