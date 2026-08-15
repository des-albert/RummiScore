package org.dba.rummiscore.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey



@Entity(tableName = "matches")
data class Match(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val isFinished: Boolean = false,
    val roundsPlayed: Int = 0
)