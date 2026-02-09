package com.example.pigeon.data.repository.local

import com.example.pigeon.data.local.dao.UserDao
import com.example.pigeon.data.local.entities.UserEntity
import com.example.pigeon.domain.model.User
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

class LocalUserRepositoryTest {

    private lateinit var userDao: UserDao
    private lateinit var repository: LocalUserRepository

    @Before
    fun setup() {
        userDao = mock()
        repository = LocalUserRepository(userDao)
    }

    @Test
    fun saveUserGeneratesNodeNameIfEmpty() = runBlocking {
        whenever(userDao.getUserSync()).thenReturn(null)
        
        val user = User(
            displayName = "Alpha-One",
            gender = "Male",
            role = "Lead Medic",
            nodeName = "",
            isAnonymous = false,
            lastUpdatedTimestamp = 0
        )
        
        repository.saveUser(user)
        
        verify(userDao).upsertUser(any())
    }

    @Test
    fun saveUserPreservesExistingNodeName() = runBlocking {
        val existingNodeName = "NODE-EXISTING"
        val existingUser = UserEntity(
            displayName = "Old Name",
            gender = "Female",
            role = "Old Role",
            nodeName = existingNodeName,
            isAnonymous = false,
            lastUpdatedTimestamp = 0
        )
        whenever(userDao.getUserSync()).thenReturn(existingUser)
        
        val newUser = User(
            displayName = "New Name",
            gender = "Male",
            role = "New Role",
            nodeName = "", // Should be ignored in favor of existing
            isAnonymous = false,
            lastUpdatedTimestamp = 0
        )
        
        repository.saveUser(newUser)
        
        verify(userDao).upsertUser(org.mockito.kotlin.argThat { 
            this.nodeName == existingNodeName 
        })
    }

    @Test
    fun isProfileLockedReturnsTrueIfWithin72Hours() = runBlocking {
        val recentTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(10)
        val user = UserEntity(
            displayName = "User",
            gender = "Male",
            role = "Role",
            nodeName = "Node",
            isAnonymous = false,
            lastUpdatedTimestamp = recentTimestamp
        )
        whenever(userDao.getUserSync()).thenReturn(user)
        
        assertTrue(repository.isProfileLocked())
    }

    @Test
    fun isProfileLockedReturnsFalseIfAfter72Hours() = runBlocking {
        val oldTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(73)
        val user = UserEntity(
            displayName = "User",
            gender = "Female",
            role = "Role",
            nodeName = "Node",
            isAnonymous = false,
            lastUpdatedTimestamp = oldTimestamp
        )
        whenever(userDao.getUserSync()).thenReturn(user)
        
        assertFalse(repository.isProfileLocked())
    }
}
