package com.example.pigeon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.pigeon.data.local.dao.UserDao
import com.example.pigeon.data.local.entities.UserEntity

/**
 * Main database for Project Pigeon.
 */
@Database(
    entities = [UserEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PigeonDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
