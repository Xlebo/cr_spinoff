package com.yourgame.shared

import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MessagesTest {

    // ── Client → Server ───────────────────────────────────────────────────────

    @Test
    fun `JoinQueue serializes with type discriminator`() {
        val msg: ClientMessage = JoinQueue(playerId = "device-uuid-123")
        val json = GameJson.encodeToString(msg)
        assertEquals("""{"type":"JOIN_QUEUE","playerId":"device-uuid-123"}""", json)
    }

    @Test
    fun `JoinQueue deserializes from JSON`() {
        val json = """{"type":"JOIN_QUEUE","playerId":"device-uuid-123"}"""
        val msg = GameJson.decodeFromString<ClientMessage>(json)
        assertIs<JoinQueue>(msg)
        assertEquals("device-uuid-123", msg.playerId)
    }

    @Test
    fun `PlayCard serializes correctly`() {
        val msg: ClientMessage = PlayCard(card = CardType.KNIGHT, x = 512f, y = 620f)
        val json = GameJson.encodeToString(msg)
        assertEquals("""{"type":"PLAY_CARD","card":"KNIGHT","x":512.0,"y":620.0}""", json)
    }

    @Test
    fun `PlayCard deserializes correctly`() {
        val json = """{"type":"PLAY_CARD","card":"MINI_PEKKA","x":300.0,"y":900.0}"""
        val msg = GameJson.decodeFromString<ClientMessage>(json)
        assertIs<PlayCard>(msg)
        assertEquals(CardType.MINI_PEKKA, msg.card)
        assertEquals(300f, msg.x)
        assertEquals(900f, msg.y)
    }

    @Test
    fun `LeaveMatch serializes correctly`() {
        val msg: ClientMessage = LeaveMatch
        val json = GameJson.encodeToString(msg)
        assertEquals("""{"type":"LEAVE_MATCH"}""", json)
    }

    @Test
    fun `LeaveMatch deserializes correctly`() {
        val msg = GameJson.decodeFromString<ClientMessage>("""{"type":"LEAVE_MATCH"}""")
        assertIs<LeaveMatch>(msg)
    }

    // ── Server → Client ───────────────────────────────────────────────────────

    @Test
    fun `MatchFound serializes correctly`() {
        val msg: ServerMessage = MatchFound(roomId = "abc123", playerIndex = 0)
        val json = GameJson.encodeToString(msg)
        assertEquals("""{"type":"MATCH_FOUND","roomId":"abc123","playerIndex":0}""", json)
    }

    @Test
    fun `MatchFound deserializes correctly`() {
        val json = """{"type":"MATCH_FOUND","roomId":"abc123","playerIndex":1}"""
        val msg = GameJson.decodeFromString<ServerMessage>(json)
        assertIs<MatchFound>(msg)
        assertEquals("abc123", msg.roomId)
        assertEquals(1, msg.playerIndex)
    }

    @Test
    fun `GameStateUpdate serializes with nested objects`() {
        val msg: ServerMessage = GameStateUpdate(
            tick = 42,
            timeRemainingMs = 163000L,
            myElixir = 7.4f,
            opponentElixir = 5.1f,
            units = listOf(
                UnitState(id = 1, card = CardType.KNIGHT, x = 500f, y = 400f, hp = 900, maxHp = 1200, owner = 0)
            ),
            towers = emptyList(),
        )
        val json = GameJson.encodeToString(msg)
        val decoded = GameJson.decodeFromString<ServerMessage>(json)
        assertIs<GameStateUpdate>(decoded)
        assertEquals(42, decoded.tick)
        assertEquals(163000L, decoded.timeRemainingMs)
        assertEquals(1, decoded.units.size)
        assertEquals(CardType.KNIGHT, decoded.units[0].card)
    }

    @Test
    fun `GameEnded serializes correctly`() {
        val msg: ServerMessage = GameEnded(winner = 0, reason = GameEndReason.KING_DESTROYED)
        val json = GameJson.encodeToString(msg)
        assertEquals("""{"type":"GAME_ENDED","winner":0,"reason":"KING_DESTROYED"}""", json)
    }

    @Test
    fun `GameEnded timer expiry deserializes correctly`() {
        val json = """{"type":"GAME_ENDED","winner":1,"reason":"TIMER_EXPIRED"}"""
        val msg = GameJson.decodeFromString<ServerMessage>(json)
        assertIs<GameEnded>(msg)
        assertEquals(1, msg.winner)
        assertEquals(GameEndReason.TIMER_EXPIRED, msg.reason)
    }

    @Test
    fun `CardPlayRejected deserializes all rejection reasons`() {
        for (reason in RejectionReason.entries) {
            val json = """{"type":"CARD_PLAY_REJECTED","reason":"$reason"}"""
            val msg = GameJson.decodeFromString<ServerMessage>(json)
            assertIs<CardPlayRejected>(msg)
            assertEquals(reason, msg.reason)
        }
    }

    @Test
    fun `ignoreUnknownKeys allows extra fields in JSON`() {
        val json = """{"type":"JOIN_QUEUE","playerId":"x","futureField":"ignored"}"""
        val msg = GameJson.decodeFromString<ClientMessage>(json)
        assertIs<JoinQueue>(msg)
    }
}
