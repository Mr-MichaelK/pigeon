package com.example.pigeon.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pigeon.data.local.dao.UserDao
import com.example.pigeon.data.local.dao.EventDao
import com.example.pigeon.data.local.dao.VerificationDao
import com.example.pigeon.data.local.entities.UserEntity
import com.example.pigeon.data.local.entities.EventEntity
import com.example.pigeon.data.local.entities.VerificationEntity
import androidx.room.TypeConverters

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `verifications` (
                `id` TEXT NOT NULL,
                `eventId` TEXT NOT NULL,
                `signerId` TEXT NOT NULL,
                `isConfirm` INTEGER NOT NULL,
                `timestamp` INTEGER NOT NULL,
                PRIMARY KEY (`id`),
                FOREIGN KEY (`eventId`) REFERENCES `events`(`eventId`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_verifications_eventId` ON `verifications` (`eventId`)"
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_verifications_eventId_signerId` ON `verifications` (`eventId`, `signerId`)"
        )
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_verifications_eventId_signerId` ON `verifications` (`eventId`, `signerId`)"
        )
    }
}

@Database(
    entities = [UserEntity::class, EventEntity::class, VerificationEntity::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class PigeonDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
    abstract fun verificationDao(): VerificationDao
}
