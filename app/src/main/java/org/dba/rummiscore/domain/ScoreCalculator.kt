package org.dba.rummiscore.domain

object ScoreCalculator {

    data class RoundInput(
        val winnerId: Long,
        val loser1Id: Long,
        val loser1RemainingPoints: Int,
        val loser2Id: Long,
        val loser2RemainingPoints: Int
    ) {
        init {
            require(loser1RemainingPoints >= 0) { "Loser points must be >= 0" }
            require(loser2RemainingPoints >= 0) { "Loser points must be >= 0" }
            require(winnerId != loser1Id && winnerId != loser2Id && loser1Id != loser2Id) {
                "Winner and losers must be three distinct players"
            }
        }
    }

    /**
     * Per-player deltas applied after a round is scored.
     * [pointsDelta] is added to running score; [winsDelta] is added to win count.
     */
    data class PlayerDelta(
        val playerId: Long,
        val pointsDelta: Int,
        val winsDelta: Int
    )

    data class RoundResult(
        val deltas: List<PlayerDelta>,
        /** Points awarded to the winner (always >= 0). */
        val winnerPoints: Int
    ) {
        val winnerDelta: PlayerDelta get() = deltas.first { it.winsDelta == 1 }
        val loserDeltas: List<PlayerDelta> get() = deltas.filter { it.winsDelta == 0 }
    }

    /**
     * Compute score deltas for a round where [input] describes winner + two losers.
     */
    fun calculate(input: RoundInput): RoundResult {
        val winnerPoints = input.loser1RemainingPoints + input.loser2RemainingPoints
        return RoundResult(
            winnerPoints = winnerPoints,
            deltas = listOf(
                PlayerDelta(
                    playerId = input.winnerId,
                    pointsDelta = winnerPoints,
                    winsDelta = 1
                ),
                PlayerDelta(
                    playerId = input.loser1Id,
                    pointsDelta = -input.loser1RemainingPoints,
                    winsDelta = 0
                ),
                PlayerDelta(
                    playerId = input.loser2Id,
                    pointsDelta = -input.loser2RemainingPoints,
                    winsDelta = 0
                )
            )
        )
    }

    /**
     * Inverse of [calculate] for undo. Given the original input used to score the round,
     * returns deltas that cancel that round.
     */
    fun reverse(input: RoundInput): RoundResult {
        val forward = calculate(input)
        return RoundResult(
            winnerPoints = forward.winnerPoints,
            deltas = forward.deltas.map { d ->
                d.copy(pointsDelta = -d.pointsDelta, winsDelta = -d.winsDelta)
            }
        )
    }

    /**
     * Rebuild [RoundInput] from a persisted [org.dba.rummiscore.data.entity.Round].
     */
    fun inputFromRound(
        winnerPlayerId: Long,
        loser1PlayerId: Long,
        loser1Points: Int,
        loser2PlayerId: Long,
        loser2Points: Int
    ): RoundInput = RoundInput(
        winnerId = winnerPlayerId,
        loser1Id = loser1PlayerId,
        loser1RemainingPoints = loser1Points,
        loser2Id = loser2PlayerId,
        loser2RemainingPoints = loser2Points
    )
}