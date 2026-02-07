package com.example.pigeon.di

import android.content.Context
import androidx.room.Room
import com.example.pigeon.data.local.PigeonDatabase
import com.example.pigeon.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providePigeonDatabase(
        @ApplicationContext context: Context
    ): PigeonDatabase {
        return Room.databaseBuilder(
            context,
            PigeonDatabase::class.java,
            "pigeon_ledger.db"
        ).build()
    }

    @Provides
    fun provideUserDao(db: PigeonDatabase): UserDao {
        return db.userDao()
    }
}
