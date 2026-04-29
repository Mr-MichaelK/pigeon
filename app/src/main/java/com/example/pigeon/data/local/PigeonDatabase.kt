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

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add the expiryTimestamp column with a default 0
        database.execSQL("ALTER TABLE `events` ADD COLUMN `expiryTimestamp` INTEGER NOT NULL DEFAULT 0")
        // Update existing rows: default to 72 hours from original timestamp
        database.execSQL("UPDATE `events` SET `expiryTimestamp` = timestamp + 259200000")
    }
}

// Distributed Trust: persist Ed25519 public key + signature on every signed
// row so manifest re-broadcasts can resend the original signature intact.
// Defaults to empty BLOBs for legacy rows (mocks, pre-migration data); those
// rows are filtered out at egress in NearbySyncManagerImpl, but they still
// render locally so a database wipe isn't required.
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE `events` ADD COLUMN `creatorPublicKey` BLOB NOT NULL DEFAULT (X'')")
        database.execSQL("ALTER TABLE `events` ADD COLUMN `signature` BLOB NOT NULL DEFAULT (X'')")
        database.execSQL("ALTER TABLE `verifications` ADD COLUMN `signerPublicKey` BLOB NOT NULL DEFAULT (X'')")
        database.execSQL("ALTER TABLE `verifications` ADD COLUMN `signature` BLOB NOT NULL DEFAULT (X'')")
    }
}

// Distributed Trust: trustScore default flips from 100.0 → 0.0. Existing rows
// (from pre-trust installs) are reset to 0 so the local user has to earn
// reputation through signed verifications going forward — a one-time wipe of
// pre-trust reputation that was inflated by the now-retired model.
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("UPDATE `user_profile` SET `trustScore` = 0.0")
    }
}

@Database(
    entities = [UserEntity::class, EventEntity::class, VerificationEntity::class],
    version = 11,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class PigeonDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun eventDao(): EventDao
    abstract fun verificationDao(): VerificationDao
}
