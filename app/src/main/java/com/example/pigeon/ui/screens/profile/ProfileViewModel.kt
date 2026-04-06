package com.example.pigeon.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.Gender
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
    val editedDisplayName: String = "",
    val editedRole: String = "",
    val editedIsAnonymous: Boolean = false,
    val editedGender: Gender = Gender.UNDISCLOSED,
    val editedIsVerified: Boolean = false,
    val defaultAvatarName: String = "avatar_male",
    val isSaving: Boolean = false,
    val showSaveConfirmation: Boolean = false
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
                
                val currentGender = user?.gender ?: Gender.UNDISCLOSED
                val defaultAvatarName = if (currentGender == Gender.FEMALE) "avatar_female" else "avatar_male"
                
                _uiState.update { 
                    it.copy(
                        user = user, 
                        // Initialize draft state if not already set or if user changed
                        editedDisplayName = if (it.editedDisplayName.isBlank()) user?.displayName ?: "" else it.editedDisplayName,
                        editedRole = if (it.editedRole.isBlank()) user?.role ?: "Civilian" else it.editedRole,
                        editedIsAnonymous = user?.isAnonymous ?: false,
                        editedGender = currentGender,
                        editedIsVerified = user?.isVerified ?: false,
                        defaultAvatarName = defaultAvatarName,
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

    fun onDisplayNameChange(name: String) {
        _uiState.update { it.copy(editedDisplayName = name) }
    }

    fun onRoleChange(role: String) {
        _uiState.update { it.copy(editedRole = role) }
    }

    fun onAnonymousToggle(isAnonymous: Boolean) {
        _uiState.update { it.copy(editedIsAnonymous = isAnonymous) }
    }

    fun onGenderChange(gender: Gender) {
        _uiState.update { it.copy(editedGender = gender) }
    }

    fun onVerifiedToggle(isVerified: Boolean) {
        _uiState.update { it.copy(editedIsVerified = isVerified) }
    }

    fun onSaveClick() {
        _uiState.update { it.copy(showSaveConfirmation = true) }
    }

    fun dismissSaveConfirmation() {
        _uiState.update { it.copy(showSaveConfirmation = false) }
    }

    fun saveAndLockIdentity() {
        viewModelScope.launch {
            _uiState.update { it.copy(showSaveConfirmation = false, isSaving = true) }
            val validUser = _uiState.value.user ?: return@launch
            val updatedUser = validUser.copy(
                displayName = _uiState.value.editedDisplayName,
                role = _uiState.value.editedRole,
                isAnonymous = _uiState.value.editedIsAnonymous,
                gender = _uiState.value.editedGender,
                isVerified = _uiState.value.editedIsVerified,
                lastUpdatedTimestamp = System.currentTimeMillis() // This locks it
            )
            userRepository.saveUser(updatedUser)
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    fun debugResetTimer() {
        viewModelScope.launch {
            userRepository.debugResetTimer()
        }
    }

    fun debugLockProfile() {
        viewModelScope.launch {
            userRepository.debugLockProfile()
        }
    }
}
