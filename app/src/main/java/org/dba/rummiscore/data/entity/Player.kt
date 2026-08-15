package org.dba.rummiscore.data.entity


import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "players")
data class Player(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imageRes: Int, // e.g. "player1", "player2", "player3"
    val totalPoints: Int = 0,
    val totalWins: Int = 0
)