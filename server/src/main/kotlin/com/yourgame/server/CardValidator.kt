package com.yourgame.server

import com.yourgame.shared.CardType
import com.yourgame.shared.RejectionReason

object CardValidator {

    sealed class Result {
        data object Ok : Result()
        data class Rejected(val reason: RejectionReason) : Result()
    }

    fun validate(state: GameState, playerIndex: Int, card: CardType, x: Float, y: Float): Result {
        val def = CardDefs[card]
        if (state.elixir[playerIndex] < def.elixirCost) return Result.Rejected(RejectionReason.NOT_ENOUGH_ELIXIR)
        if (!validPlacement(playerIndex, x, y))          return Result.Rejected(RejectionReason.INVALID_PLACEMENT)
        return Result.Ok
    }

    // Cards must be placed inside the arena and in the player's own half.
    // A 50 wu buffer at the X edges and a gap around the centre line prevent
    // accidental placement on top of base towers or across the river.
    fun validPlacement(playerIndex: Int, x: Float, y: Float): Boolean {
        if (x !in 50f..950f) return false
        return if (playerIndex == 0) y in 100f..850f else y in 950f..1700f
    }
}
