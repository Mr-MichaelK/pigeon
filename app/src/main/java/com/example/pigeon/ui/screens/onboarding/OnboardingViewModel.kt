package com.example.pigeon.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.User
import com.example.pigeon.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val displayName: String = "",
    val gender: String = "Male",
    val role: String = "Civilian",
    val isAnonymous: Boolean = false,
    val isSaving: Boolean = false,
    val isProfileCreated: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onDisplayNameChange(newName: String) {
        _uiState.update { it.copy(displayName = newName) }
    }

    fun onGenderChange(newGender: String) {
        _uiState.update { it.copy(gender = newGender) }
    }

    fun onRoleChange(newRole: String) {
        _uiState.update { it.copy(role = newRole) }
    }

    fun onAnonymousToggle(enabled: Boolean) {
        _uiState.update { it.copy(isAnonymous = enabled) }
    }

    fun joinMesh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            
            val user = User(
                displayName = _uiState.value.displayName,
                gender = _uiState.value.gender,
                role = _uiState.value.role,
                nodeName = "", // Repository will generate this
                isAnonymous = _uiState.value.isAnonymous,
                lastUpdatedTimestamp = 0 // Repository will set this
            )
            
            userRepository.saveUser(user)
            _uiState.update { it.copy(isSaving = false, isProfileCreated = true) }
        }
    }
}
