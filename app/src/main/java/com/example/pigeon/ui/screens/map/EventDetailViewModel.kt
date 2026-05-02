package com.example.pigeon.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pigeon.domain.model.TrustScore
import com.example.pigeon.domain.repository.VerificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val verificationRepository: VerificationRepository
) : ViewModel() {

    private val _selectedEventId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val trustScore: StateFlow<TrustScore> = _selectedEventId
        .filterNotNull()
        .flatMapLatest { id -> verificationRepository.getTrustScore(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TrustScore.EMPTY
        )

    fun onEventSelected(eventId: String) {
        _selectedEventId.value = eventId
    }

    fun onVerify(isConfirm: Boolean) {
        val eventId = _selectedEventId.value ?: return
        viewModelScope.launch {
            verificationRepository.verifyEvent(eventId, isConfirm)
        }
    }
}
