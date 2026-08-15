package org.dba.rummiscore.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.dba.rummiscore.data.entity.Round

@Dao
interface RoundDao {
    @Query("SELECT * FROM rounds WHERE matchId = :matchId ORDER BY roundNumber ASC")
    fun getRoundsForMatch(matchId: Long): Flow<List<Round>>

    @Query("SELECT * FROM rounds WHERE matchId = :matchId ORDER BY roundNumber ASC")
    suspend fun getRoundsForMatchOnce(matchId: Long): List<Round>

    @Query("SELECT * FROM rounds WHERE id = :id")
    suspend fun getRoundById(id: Long): Round?

    @Query("SELECT * FROM rounds WHERE matchId = :matchId ORDER BY roundNumber DESC LIMIT 1")
    suspend fun getLastRound(matchId: Long): Round?

    @Insert
    suspend fun insert(round: Round): Long

    @Query("DELETE FROM rounds WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM rounds WHERE matchId = :matchId")
    suspend fun countRounds(matchId: Long): Int
}