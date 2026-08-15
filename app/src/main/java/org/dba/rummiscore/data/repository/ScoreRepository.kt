package org.dba.rummiscore.data.repository


import org.dba.rummiscore.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.dba.rummiscore.data.dao.MatchDao
import org.dba.rummiscore.data.dao.MatchPlayerScoreDao
import org.dba.rummiscore.data.dao.PlayerDao
import org.dba.rummiscore.data.dao.RoundDao
import org.dba.rummiscore.data.entity.Match
import org.dba.rummiscore.data.entity.MatchPlayerScore
import org.dba.rummiscore.data.entity.Player
import org.dba.rummiscore.data.entity.Round
import org.dba.rummiscore.domain.ScoreCalculator
import javax.inject.Inject
import javax.inject.Singleton

data class PlayerWithMatchScore(
    val player: Player,
    val matchPoints: Int,
    val matchWins: Int
)

data class MatchSummary(
    val match: Match,
    val scores: List<PlayerWithMatchScore>
)

@Singleton
class ScoreRepository @Inject constructor(
    private val playerDao: PlayerDao,
    private val matchDao: MatchDao,
    private val roundDao: RoundDao,
    private val matchPlayerScoreDao: MatchPlayerScoreDao
) {
    val players: Flow<List<Player>> = playerDao.getAllPlayers()
    val activeMatch: Flow<Match?> = matchDao.getActiveMatch()
    val allMatches: Flow<List<Match>> = matchDao.getAllMatches()

    suspend fun ensureDefaultPlayers() {
        if (playerDao.count() == 0) {
            playerDao.insertAll(
                listOf(
                    Player(name = "DB", imageRes = R.drawable.db),
                    Player(name = "Bo", imageRes = R.drawable.bo),
                    Player(name = "Steve", imageRes = R.drawable.steve)
                )
            )
        }
    }

    suspend fun updatePlayerName(playerId: Long, newName: String) {
        val player = playerDao.getPlayerById(playerId) ?: return
        playerDao.update(player.copy(name = newName))
    }

    suspend fun startNewMatch(): Long {
        // Properly finish any unfinished match before opening a new one
        matchDao.getUnfinishedMatchOnce()?.let { existing ->
            finishMatch(existing.id)
        }
        val matchId = matchDao.insert(Match())
        val players = playerDao.getAllPlayersOnce()
        matchPlayerScoreDao.insertAll(
            players.map { p ->
                MatchPlayerScore(matchId = matchId, playerId = p.id, points = 0, wins = 0)
            }
        )
        return matchId
    }

    fun getRoundsForMatch(matchId: Long): Flow<List<Round>> =
        roundDao.getRoundsForMatch(matchId)

    fun getScoresForMatch(matchId: Long): Flow<List<MatchPlayerScore>> =
        matchPlayerScoreDao.getScoresForMatch(matchId)

    fun getMatchWithScores(matchId: Long): Flow<MatchSummary?> =
        combine(
            matchDao.getAllMatches(),
            matchPlayerScoreDao.getScoresForMatch(matchId),
            playerDao.getAllPlayers()
        ) { matches, scores, players ->
            val match = matches.find { it.id == matchId } ?: return@combine null
            val playerMap = players.associateBy { it.id }
            MatchSummary(
                match = match,
                scores = scores.mapNotNull { s ->
                    playerMap[s.playerId]?.let { p ->
                        PlayerWithMatchScore(p, s.points, s.wins)
                    }
                }.sortedBy { it.player.id }
            )
        }

    /** All matches that have been played, each with per-player points and wins. */
    fun getMatchHistory(): Flow<List<MatchSummary>> =
        combine(
            matchDao.getAllMatches(),
            matchPlayerScoreDao.getAllScores(),
            playerDao.getAllPlayers()
        ) { matches, allScores, players ->
            val playerMap = players.associateBy { it.id }
            val scoresByMatch = allScores.groupBy { it.matchId }
            matches
                .filter { it.isFinished || it.roundsPlayed > 0 }
                .sortedByDescending { it.startedAt }
                .map { match ->
                    val scores = (scoresByMatch[match.id] ?: emptyList()).mapNotNull { s ->
                        playerMap[s.playerId]?.let { p ->
                            PlayerWithMatchScore(p, s.points, s.wins)
                        }
                    }.sortedByDescending { it.matchPoints }
                    MatchSummary(match = match, scores = scores)
                }
        }

    /**
     * Record a round using [ScoreCalculator].
     *
     * Winner: +(loser1 + loser2) points and +1 win
     * Loser1: −loser1Points
     * Loser2: −loser2Points
     */
    suspend fun recordRound(
        matchId: Long,
        winnerId: Long,
        loser1Id: Long,
        loser1Points: Int,
        loser2Id: Long,
        loser2Points: Int
    ): Result<Unit> {
        val match = matchDao.getMatchById(matchId)
            ?: return Result.failure(IllegalStateException("Match not found"))
        if (match.isFinished) {
            return Result.failure(IllegalStateException("Match already finished"))
        }
        val currentRounds = roundDao.countRounds(matchId)
        if (currentRounds >= 6) {
            return Result.failure(IllegalStateException("Maximum 6 rounds reached"))
        }

        val input = try {
            ScoreCalculator.RoundInput(
                winnerId = winnerId,
                loser1Id = loser1Id,
                loser1RemainingPoints = loser1Points,
                loser2Id = loser2Id,
                loser2RemainingPoints = loser2Points
            )
        } catch (e: IllegalArgumentException) {
            return Result.failure(e)
        }

        val result = ScoreCalculator.calculate(input)
        val roundNumber = currentRounds + 1

        roundDao.insert(
            Round(
                matchId = matchId,
                roundNumber = roundNumber,
                winnerPlayerId = winnerId,
                loser1PlayerId = loser1Id,
                loser1Points = loser1Points,
                loser2PlayerId = loser2Id,
                loser2Points = loser2Points
            )
        )

        applyDeltas(matchId, result.deltas)

        matchDao.update(match.copy(roundsPlayed = roundNumber))

        // Auto-finish after the 6th round
        if (roundNumber >= 6) {
            finishMatch(matchId)
        }

        return Result.success(Unit)
    }

    /**
     * Undo the last round: reverse score deltas, delete the round row, fix match metadata.
     */
    suspend fun undoLastRound(matchId: Long): Result<Unit> {
        val last = roundDao.getLastRound(matchId)
            ?: return Result.failure(IllegalStateException("No rounds to undo"))
        val match = matchDao.getMatchById(matchId)
            ?: return Result.failure(IllegalStateException("Match not found"))

        val input = ScoreCalculator.inputFromRound(
            winnerPlayerId = last.winnerPlayerId,
            loser1PlayerId = last.loser1PlayerId,
            loser1Points = last.loser1Points,
            loser2PlayerId = last.loser2PlayerId,
            loser2Points = last.loser2Points
        )
        val reverseResult = ScoreCalculator.reverse(input)

        applyDeltas(matchId, reverseResult.deltas)
        roundDao.delete(last.id)

        matchDao.update(
            match.copy(
                roundsPlayed = last.roundNumber - 1,
                isFinished = false,
                finishedAt = null
            )
        )
        return Result.success(Unit)
    }

    /** Apply the same deltas to match-scoped scores and long-term career totals. */
    private suspend fun applyDeltas(matchId: Long, deltas: List<ScoreCalculator.PlayerDelta>) {
        for (d in deltas) {
            matchPlayerScoreDao.updateScore(
                matchId = matchId,
                playerId = d.playerId,
                deltaPoints = d.pointsDelta,
                deltaWins = d.winsDelta
            )
            playerDao.updateTotals(
                playerId = d.playerId,
                deltaPoints = d.pointsDelta,
                deltaWins = d.winsDelta
            )
        }
    }

    suspend fun finishMatch(matchId: Long) {
        val match = matchDao.getMatchById(matchId) ?: return
        if (match.isFinished) return
        matchDao.update(
            match.copy(
                isFinished = true,
                finishedAt = System.currentTimeMillis()
            )
        )
    }
}