package com.example.pigeon.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.pigeon.domain.model.Gender
import com.example.pigeon.domain.model.User

/**
 * Room entity for storing user identity.
 */
@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val displayName: String,
    val role: String,
    val nodeName: String,
    val isAnonymous: Boolean,
    val gender: String = Gender.UNDISCLOSED.name,
    val nodeId: String = "",
    val isVerified: Boolean = false,
    val totalSyncs: Int = 0,
    val trustScore: Float = 0.0f,
    val lastUpdatedTimestamp: Long
)

fun UserEntity.toDomain(): User = User(
    id = id,
    displayName = displayName,
    role = role,
    nodeName = nodeName,
    isAnonymous = isAnonymous,
    gender = try { Gender.valueOf(gender) } catch (e: Exception) { Gender.UNDISCLOSED },
    nodeId = nodeId,
    isVerified = isVerified,
    totalSyncs = totalSyncs,
    trustScore = trustScore,
    lastUpdatedTimestamp = lastUpdatedTimestamp
)

fun User.toEntity(): UserEntity = UserEntity(
    id = id,
    displayName = displayName,
    role = role,
    nodeName = nodeName,
    isAnonymous = isAnonymous,
    gender = gender.name,
    nodeId = nodeId,
    isVerified = isVerified,
    totalSyncs = totalSyncs,
    trustScore = trustScore,
    lastUpdatedTimestamp = lastUpdatedTimestamp
)
