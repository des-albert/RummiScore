package org.dba.rummiscore.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.dba.rummiscore.data.entity.Player

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players ORDER BY id ASC")
    fun getAllPlayers(): Flow<List<Player>>

    @Query("SELECT * FROM players ORDER BY id ASC")
    suspend fun getAllPlayersOnce(): List<Player>

    @Query("SELECT * FROM players WHERE id = :id")
    suspend fun getPlayerById(id: Long): Player?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(player: Player): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<Player>)

    @Update
    suspend fun update(player: Player)

    @Query("UPDATE players SET totalPoints = totalPoints + :deltaPoints, totalWins = totalWins + :deltaWins WHERE id = :playerId")
    suspend fun updateTotals(playerId: Long, deltaPoints: Int, deltaWins: Int)

    @Query("SELECT COUNT(*) FROM players")
    suspend fun count(): Int
}