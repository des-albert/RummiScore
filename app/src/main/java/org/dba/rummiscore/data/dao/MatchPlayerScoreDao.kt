package org.dba.rummiscore.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.dba.rummiscore.data.entity.MatchPlayerScore

@Dao
interface MatchPlayerScoreDao {
    @Query("SELECT * FROM match_player_scores WHERE matchId = :matchId")
    fun getScoresForMatch(matchId: Long): Flow<List<MatchPlayerScore>>

    @Query("SELECT * FROM match_player_scores")
    fun getAllScores(): Flow<List<MatchPlayerScore>>

    @Query("SELECT * FROM match_player_scores WHERE matchId = :matchId")
    suspend fun getScoresForMatchOnce(matchId: Long): List<MatchPlayerScore>

    @Query("SELECT * FROM match_player_scores WHERE matchId = :matchId AND playerId = :playerId")
    suspend fun getScore(matchId: Long, playerId: Long): MatchPlayerScore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(score: MatchPlayerScore): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scores: List<MatchPlayerScore>)

    @Update
    suspend fun update(score: MatchPlayerScore)

    @Query(
        """
        UPDATE match_player_scores 
        SET points = points + :deltaPoints, wins = wins + :deltaWins 
        WHERE matchId = :matchId AND playerId = :playerId
        """
    )
    suspend fun updateScore(matchId: Long, playerId: Long, deltaPoints: Int, deltaWins: Int)
}