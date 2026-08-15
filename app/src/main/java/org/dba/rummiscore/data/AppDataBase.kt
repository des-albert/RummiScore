package org.dba.rummiscore.data

import androidx.room.Database
import androidx.room.RoomDatabase
import org.dba.rummiscore.data.dao.MatchDao
import org.dba.rummiscore.data.dao.MatchPlayerScoreDao
import org.dba.rummiscore.data.dao.PlayerDao
import org.dba.rummiscore.data.dao.RoundDao
import org.dba.rummiscore.data.entity.Match
import org.dba.rummiscore.data.entity.MatchPlayerScore
import org.dba.rummiscore.data.entity.Player
import org.dba.rummiscore.data.entity.Round

@Database(
    entities = [
        Player::class,
        Match::class,
        Round::class,
        MatchPlayerScore::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun matchDao(): MatchDao
    abstract fun roundDao(): RoundDao
    abstract fun matchPlayerScoreDao(): MatchPlayerScoreDao
}