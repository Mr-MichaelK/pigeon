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

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `gender` TEXT NOT NULL DEFAULT 'UNDISCLOSED'")
        database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `nodeId` TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `events` ADD COLUMN `creatorName` TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `isVerified` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `totalSyncs` INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE `user_profile` ADD COLUMN `trustScore` REAL NOT NULL DEFAULT 100.0")
    }
}

@Database(
    entities = [UserEntity::class, EventEntity::class, VerificationEntity::class],
    version = 8,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class PigeonDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
    abstract fun verificationDao(): VerificationDao
}
