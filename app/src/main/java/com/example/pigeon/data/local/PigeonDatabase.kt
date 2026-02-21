package com.example.pigeon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pigeon.data.local.dao.UserDao
import com.example.pigeon.data.local.entities.UserEntity
import com.example.pigeon.data.local.entities.EventEntity
import com.example.pigeon.data.local.dao.EventDao
import androidx.room.TypeConverters

/**
 * Main database for Project Pigeon.
 */
@Database(
    entities = [UserEntity::class, EventEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class PigeonDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
}
