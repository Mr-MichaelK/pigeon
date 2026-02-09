package com.example.pigeon.ui.screens.profile

import com.example.pigeon.domain.model.User
import com.example.pigeon.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var userRepository: UserRepository
    private lateinit var viewModel: ProfileViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        userRepository = mock()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadUser updates uiState with user and lock status`() = runTest {
        val user = User(
            displayName = "Test User",
            gender = "Male",
            role = "Civilian",
            nodeName = "NODE-12345678",
            isAnonymous = false,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
        whenever(userRepository.getUser()).thenReturn(flowOf(user))
        whenever(userRepository.isProfileLocked()).thenReturn(true)

        viewModel = ProfileViewModel(userRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(user, state.user)
        assertTrue(state.isLocked)
        assertFalse(state.canEdit)
    }

    @Test
    fun `countdown text updates correctly when locked`() = runTest {
        val tenHoursAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(10)
        val user = User(
            displayName = "Test User",
            gender = "Male",
            role = "Civilian",
            nodeName = "NODE-12345678",
            isAnonymous = false,
            lastUpdatedTimestamp = tenHoursAgo
        )
        whenever(userRepository.getUser()).thenReturn(flowOf(user))
        whenever(userRepository.isProfileLocked()).thenReturn(true)

        viewModel = ProfileViewModel(userRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // 72 - 10 = 62 hours remaining
        assertTrue(state.countdownText.startsWith("61:") || state.countdownText.startsWith("62:"))
        assertTrue(state.isLocked)
    }

    @Test
    fun `countdown shows unlocked after 72 hours`() = runTest {
        val seventyThreeHoursAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(73)
        val user = User(
            displayName = "Test User",
            gender = "Male",
            role = "Civilian",
            nodeName = "NODE-12345678",
            isAnonymous = false,
            lastUpdatedTimestamp = seventyThreeHoursAgo
        )
        whenever(userRepository.getUser()).thenReturn(flowOf(user))
        whenever(userRepository.isProfileLocked()).thenReturn(false)

        viewModel = ProfileViewModel(userRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("IDENTITY UNLOCKED", state.countdownText)
        assertFalse(state.isLocked)
        assertTrue(state.canEdit)
    }
}
