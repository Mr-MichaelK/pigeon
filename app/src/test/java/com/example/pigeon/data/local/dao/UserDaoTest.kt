package com.example.pigeon.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.pigeon.data.local.PigeonDatabase
import com.example.pigeon.data.local.entities.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserDaoTest {

    private lateinit var database: PigeonDatabase
    private lateinit var userDao: UserDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PigeonDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun upsertAndGetUser() = runBlocking {
        val user = UserEntity(
            displayName = "Alpha-One",
            role = "Lead Medic",
            nodeName = "NODE-12345678",
            isAnonymous = false,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        
        userDao.upsertUser(user)
        
        val retrievedUser = userDao.getUser().first()
        assertNotNull(retrievedUser)
        assertEquals(user.displayName, retrievedUser?.displayName)
        assertEquals(user.role, retrievedUser?.role)
    }

    @Test
    fun getUserSyncReturnsCorrectUser() = runBlocking {
        val user = UserEntity(
            displayName = "Alpha-One",
            role = "Lead Medic",
            nodeName = "NODE-12345678",
            isAnonymous = false,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        
        userDao.upsertUser(user)
        
        val retrievedUser = userDao.getUserSync()
        assertNotNull(retrievedUser)
        assertEquals(user.displayName, retrievedUser?.displayName)
    }
}
