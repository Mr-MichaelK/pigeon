package com.example.pigeon.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.User
import com.example.pigeon.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val countdownText: String = "",
    val isLocked: Boolean = false,
    val canEdit: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
        startCountdown()
    }

    private fun loadUser() {
        viewModelScope.launch {
            userRepository.getUser().collect { user ->
                val isLocked = userRepository.isProfileLocked()
                _uiState.update { 
                    it.copy(
                        user = user, 
                        isLoading = false,
                        isLocked = isLocked,
                        canEdit = !isLocked
                    ) 
                }
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (true) {
                val user = uiState.value.user
                if (user != null && user.lastUpdatedTimestamp > 0) {
                    val remainingMillis = calculateRemainingMillis(user.lastUpdatedTimestamp)
                    if (remainingMillis > 0) {
                        _uiState.update { it.copy(countdownText = formatMillis(remainingMillis), isLocked = true, canEdit = false) }
                    } else {
                        _uiState.update { it.copy(countdownText = "IDENTITY UNLOCKED", isLocked = false, canEdit = true) }
                    }
                }
                delay(1000)
            }
        }
    }

    private fun calculateRemainingMillis(lastUpdated: Long): Long {
        val seventyTwoHours = TimeUnit.HOURS.toMillis(72)
        val elapsed = System.currentTimeMillis() - lastUpdated
        return seventyTwoHours - elapsed
    }

    private fun formatMillis(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun saveChanges(updatedUser: User) {
        viewModelScope.launch {
            userRepository.saveUser(updatedUser)
        }
    }
}
