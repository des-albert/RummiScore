package org.dba.rummiscore.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rounds",
    foreignKeys = [
        ForeignKey(
            entity = Match::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("matchId")]
)
data class Round(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,
    val roundNumber: Int, // 1..6
    val winnerPlayerId: Long,
    val loser1PlayerId: Long,
    val loser1Points: Int, // positive value entered; stored as negative for loser
    val loser2PlayerId: Long,
    val loser2Points: Int,
    val timestamp: Long = System.currentTimeMillis()
)