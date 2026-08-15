package org.dba.rummiscore.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.dba.rummiscore.data.AppDatabase
import org.dba.rummiscore.data.dao.MatchDao
import org.dba.rummiscore.data.dao.MatchPlayerScoreDao
import org.dba.rummiscore.data.dao.PlayerDao
import org.dba.rummiscore.data.dao.RoundDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "rummiscore.db"
        ).fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun providePlayerDao(db: AppDatabase): PlayerDao = db.playerDao()

    @Provides
    fun provideMatchDao(db: AppDatabase): MatchDao = db.matchDao()

    @Provides
    fun provideRoundDao(db: AppDatabase): RoundDao = db.roundDao()

    @Provides
    fun provideMatchPlayerScoreDao(db: AppDatabase): MatchPlayerScoreDao = db.matchPlayerScoreDao()
}