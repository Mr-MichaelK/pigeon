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
    val canEdit: Boolean = false,
    // Draft state for editing
    val editedRole: String = "",
    val editedIsAnonymous: Boolean = false
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
                        // Initialize draft state if not already set or if user changed
                        editedRole = if (it.editedRole.isBlank()) user?.role ?: "Civilian" else it.editedRole,
                        editedIsAnonymous = user?.isAnonymous ?: false,
                        isLoading = false,
                        isLocked = isLocked,
                        canEdit = !isLocked
                    ) 
                }
                // Determine implicit draft sync on first load
                if (user != null && _uiState.value.editedRole.isBlank()) {
                     _uiState.update { it.copy(editedRole = user.role, editedIsAnonymous = user.isAnonymous) }
                }
            }
        }
    }

    private fun startCountdown() {
        viewModelScope.launch {
            while (true) {
                val user = uiState.value.user
                if (user != null && user.lastUpdatedTimestamp > 0) {
                    // Check actual lock status dynamically
                    val isLocked = userRepository.isProfileLocked()
                    val remainingMillis = calculateRemainingMillis(user.lastUpdatedTimestamp)
                    
                    if (isLocked) {
                        _uiState.update { it.copy(countdownText = formatMillis(remainingMillis), isLocked = true, canEdit = false) }
                    } else {
                        // When unlocked, ensure we sync draft state once if we haven't
                        _uiState.update { 
                             // If we just transitioned to unlocked, maybe we should reset drafts to current user? 
                             // For now just unlock.
                            it.copy(countdownText = "IDENTITY UNLOCKED", isLocked = false, canEdit = true) 
                        }
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

    fun onRoleChange(role: String) {
        _uiState.update { it.copy(editedRole = role) }
    }

    fun onAnonymousToggle(isAnonymous: Boolean) {
        _uiState.update { it.copy(editedIsAnonymous = isAnonymous) }
    }

    fun saveAndLockIdentity() {
        viewModelScope.launch {
            val validUser = _uiState.value.user ?: return@launch
            val updatedUser = validUser.copy(
                role = _uiState.value.editedRole,
                isAnonymous = _uiState.value.editedIsAnonymous,
                lastUpdatedTimestamp = System.currentTimeMillis() // This locks it
            )
            userRepository.saveUser(updatedUser)
            // State update will happen via flow collection in loadUser
        }
    }

    fun debugResetTimer() {
        viewModelScope.launch {
            userRepository.debugResetTimer()
        }
    }
}
