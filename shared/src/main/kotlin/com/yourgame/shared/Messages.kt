package com.yourgame.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val GameJson = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
    encodeDefaults = false
}

// ── Enums ────────────────────────────────────────────────────────────────────

enum class CardType { KNIGHT, ARCHER, GIANT, FIREBALL, MINIONS, BARBARIANS, MUSKETEER, MINI_PEKKA }
enum class TowerType { KING, PRINCESS }
enum class RejectionReason { NOT_ENOUGH_ELIXIR, INVALID_PLACEMENT, INVALID_CARD }
enum class GameEndReason { KING_DESTROYED, TIMER_EXPIRED }

// ── Game state snapshots ──────────────────────────────────────────────────────

@Serializable
data class UnitState(
    val id: Int,
    val card: CardType,
    val x: Float,
    val y: Float,
    val hp: Int,
    val maxHp: Int,
    val owner: Int,
    val isFlying: Boolean = false,
)

@Serializable
data class TowerState(
    val type: TowerType,
    val x: Float,
    val y: Float,
    val hp: Int,
    val maxHp: Int,
    val owner: Int,
    val alive: Boolean,
)

// ── Client → Server ───────────────────────────────────────────────────────────

@Serializable
sealed class ClientMessage

@Serializable
@SerialName("JOIN_QUEUE")
data class JoinQueue(val playerId: String) : ClientMessage()

@Serializable
@SerialName("PLAY_CARD")
data class PlayCard(val card: CardType, val x: Float, val y: Float) : ClientMessage()

@Serializable
@SerialName("LEAVE_MATCH")
data object LeaveMatch : ClientMessage()

// ── Server → Client ───────────────────────────────────────────────────────────

@Serializable
sealed class ServerMessage

@Serializable
@SerialName("MATCH_FOUND")
data class MatchFound(val roomId: String, val playerIndex: Int) : ServerMessage()

@Serializable
@SerialName("GAME_STATE_UPDATE")
data class GameStateUpdate(
    val tick: Int,
    val timeRemainingMs: Long,
    val myElixir: Float,
    val opponentElixir: Float,
    val units: List<UnitState>,
    val towers: List<TowerState>,
) : ServerMessage()

@Serializable
@SerialName("GAME_ENDED")
data class GameEnded(val winner: Int, val reason: GameEndReason) : ServerMessage()

@Serializable
@SerialName("CARD_PLAY_REJECTED")
data class CardPlayRejected(val reason: RejectionReason) : ServerMessage()
