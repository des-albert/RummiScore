package org.dba.rummiscore.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.dba.rummiscore.data.entity.Match

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY startedAt DESC")
    fun getAllMatches(): Flow<List<Match>>

    /** Latest match (finished or not) — used so a completed 6-round match stays on screen for undo. */
    @Query("SELECT * FROM matches ORDER BY startedAt DESC LIMIT 1")
    fun getActiveMatch(): Flow<Match?>

    @Query("SELECT * FROM matches ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveMatchOnce(): Match?

    @Query("SELECT * FROM matches WHERE isFinished = 0 ORDER BY startedAt DESC LIMIT 1")
    suspend fun getUnfinishedMatchOnce(): Match?

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Long): Match?

    @Insert
    suspend fun insert(match: Match): Long

    @Update
    suspend fun update(match: Match)

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun delete(id: Long)
}