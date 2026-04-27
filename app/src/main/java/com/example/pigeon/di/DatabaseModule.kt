package com.example.pigeon.di

import android.content.Context
import androidx.room.Room
import com.example.pigeon.data.local.MIGRATION_2_3
import com.example.pigeon.data.local.MIGRATION_3_4
import com.example.pigeon.data.local.PigeonDatabase
import com.example.pigeon.data.local.dao.UserDao
import com.example.pigeon.data.local.dao.EventDao
import com.example.pigeon.data.local.dao.VerificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
        )
        .addMigrations(
            MIGRATION_2_3, 
            MIGRATION_3_4, 
            com.example.pigeon.data.local.MIGRATION_4_5,
            com.example.pigeon.data.local.MIGRATION_5_6,
            com.example.pigeon.data.local.MIGRATION_6_7,
            com.example.pigeon.data.local.MIGRATION_7_8,
            com.example.pigeon.data.local.MIGRATION_8_9
        )
        .build()
    }

    @Provides
    fun provideUserDao(db: PigeonDatabase): UserDao = db.userDao()

    @Provides
    fun provideEventDao(db: PigeonDatabase): EventDao = db.eventDao()

    @Provides
    fun provideVerificationDao(db: PigeonDatabase): VerificationDao = db.verificationDao()
}
