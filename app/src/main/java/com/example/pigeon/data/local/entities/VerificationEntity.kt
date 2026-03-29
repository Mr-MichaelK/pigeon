package com.example.pigeon.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "verifications",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["eventId"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["eventId", "signerId"], unique = true), Index(value = ["eventId"])]
)
data class VerificationEntity(
    @PrimaryKey val id: String,
    val eventId: String,
    val signerId: String,
    val isConfirm: Boolean,
    val timestamp: Long
)
