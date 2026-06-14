package com.yourgame.server

import com.yourgame.shared.CardType
import com.yourgame.shared.RejectionReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CardValidatorTest {

    private fun state(elixir0: Float = 5f, elixir1: Float = 5f) =
        GameState().also { it.elixir[0] = elixir0; it.elixir[1] = elixir1 }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test fun `valid knight play accepted for player 0`() {
        val result = CardValidator.validate(state(), 0, CardType.KNIGHT, 500f, 400f)
        assertIs<CardValidator.Result.Ok>(result)
    }

    @Test fun `valid giant play accepted for player 1`() {
        val result = CardValidator.validate(state(elixir1 = 8f), 1, CardType.GIANT, 500f, 1000f)
        assertIs<CardValidator.Result.Ok>(result)
    }

    @Test fun `exactly enough elixir is accepted`() {
        // Knight costs 3; elixir = 3.0 exactly
        val result = CardValidator.validate(state(elixir0 = 3f), 0, CardType.KNIGHT, 500f, 400f)
        assertIs<CardValidator.Result.Ok>(result)
    }

    // ── NOT_ENOUGH_ELIXIR ─────────────────────────────────────────────────────

    @Test fun `too little elixir rejected`() {
        val result = CardValidator.validate(state(elixir0 = 2f), 0, CardType.KNIGHT, 500f, 400f)
        val rejected = assertIs<CardValidator.Result.Rejected>(result)
        assertEquals(RejectionReason.NOT_ENOUGH_ELIXIR, rejected.reason)
    }

    @Test fun `giant rejected when only 4 elixir available`() {
        val result = CardValidator.validate(state(elixir0 = 4f), 0, CardType.GIANT, 500f, 400f)
        val rejected = assertIs<CardValidator.Result.Rejected>(result)
        assertEquals(RejectionReason.NOT_ENOUGH_ELIXIR, rejected.reason)
    }

    // ── INVALID_PLACEMENT ─────────────────────────────────────────────────────

    @Test fun `player 0 placing in opponent half rejected`() {
        val result = CardValidator.validate(state(), 0, CardType.KNIGHT, 500f, 1000f)
        val rejected = assertIs<CardValidator.Result.Rejected>(result)
        assertEquals(RejectionReason.INVALID_PLACEMENT, rejected.reason)
    }

    @Test fun `player 1 placing in opponent half rejected`() {
        val result = CardValidator.validate(state(), 1, CardType.KNIGHT, 500f, 400f)
        val rejected = assertIs<CardValidator.Result.Rejected>(result)
        assertEquals(RejectionReason.INVALID_PLACEMENT, rejected.reason)
    }

    @Test fun `x too small rejected`() {
        val result = CardValidator.validate(state(), 0, CardType.KNIGHT, 10f, 400f)
        val rejected = assertIs<CardValidator.Result.Rejected>(result)
        assertEquals(RejectionReason.INVALID_PLACEMENT, rejected.reason)
    }

    @Test fun `x too large rejected`() {
        val result = CardValidator.validate(state(), 0, CardType.KNIGHT, 990f, 400f)
        val rejected = assertIs<CardValidator.Result.Rejected>(result)
        assertEquals(RejectionReason.INVALID_PLACEMENT, rejected.reason)
    }

    @Test fun `centre line y=900 rejected for both players`() {
        // Neither player may place exactly on the centre line
        assertIs<CardValidator.Result.Rejected>(CardValidator.validate(state(), 0, CardType.KNIGHT, 500f, 900f))
        assertIs<CardValidator.Result.Rejected>(CardValidator.validate(state(), 1, CardType.KNIGHT, 500f, 900f))
    }

    @Test fun `placement too close to own base rejected`() {
        // P0: y < 100 is too close to own king tower
        assertIs<CardValidator.Result.Rejected>(CardValidator.validate(state(), 0, CardType.KNIGHT, 500f, 50f))
        // P1: y > 1700 is too close to own king tower
        assertIs<CardValidator.Result.Rejected>(CardValidator.validate(state(), 1, CardType.KNIGHT, 500f, 1750f))
    }

    @Test fun `all 8 card types pass placement check in own half`() {
        val state = GameState().also { it.elixir[0] = MAX_ELIXIR }
        CardType.entries.forEach { card ->
            val result = CardValidator.validate(state, 0, card, 500f, 400f)
            // Every card should be valid — not rejected for INVALID_CARD
            if (result is CardValidator.Result.Rejected) {
                assertEquals(
                    RejectionReason.NOT_ENOUGH_ELIXIR, result.reason,
                    "Card $card unexpectedly rejected with ${result.reason}"
                )
            }
        }
    }
}
