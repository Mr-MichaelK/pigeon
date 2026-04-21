package com.example.pigeon.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.Gender
import com.example.pigeon.domain.model.MeshPowerState
import com.example.pigeon.domain.model.User
import com.example.pigeon.domain.network.NearbySyncManager
import com.example.pigeon.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val displayName: String = "",
    val role: String = "Civilian",
    val gender: Gender = Gender.UNDISCLOSED,
    val isAnonymous: Boolean = false,
    val isSaving: Boolean = false,
    val isProfileCreated: Boolean = false,
    val isVerified: Boolean = false,
    val nodeName: String = "GENERATING..."
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val nearbySyncManager: NearbySyncManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Check if user already exists and redirect (Task 7.5)
            userRepository.getUser().collectLatest { user ->
                if (user != null) {
                    _uiState.update { it.copy(isProfileCreated = true) }
                }
            }
        }
        viewModelScope.launch {
            val nodeName = userRepository.getOrGenerateNodeName()
            _uiState.update { it.copy(nodeName = nodeName) }
        }
    }

    fun onDisplayNameChange(newName: String) {
        _uiState.update { it.copy(displayName = newName) }
    }

    fun onRoleChange(newRole: String) {
        _uiState.update { it.copy(role = newRole) }
    }

    fun onGenderChange(newGender: Gender) {
        _uiState.update { it.copy(gender = newGender) }
    }

    fun onAnonymousToggle(enabled: Boolean) {
        _uiState.update { it.copy(isAnonymous = enabled) }
    }

    fun joinMesh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            val user = User(
                displayName = _uiState.value.displayName,
                role = _uiState.value.role,
                gender = _uiState.value.gender,
                nodeName = "", // Repository will generate this
                isAnonymous = _uiState.value.isAnonymous,
                isVerified = false, // Task 7.5: Default false
                lastUpdatedTimestamp = 0 // Repository will set this
            )
            
            userRepository.saveUser(user)
            nearbySyncManager.togglePowerState(MeshPowerState.PASSIVE, isSticky = true)
            _uiState.update { it.copy(isSaving = false, isProfileCreated = true) }
        }
    }
}
