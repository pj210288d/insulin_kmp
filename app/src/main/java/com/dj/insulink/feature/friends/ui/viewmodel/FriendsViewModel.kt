package com.dj.insulink.feature.friends.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import com.dj.insulink.shared.feature.friends.data.repository.FriendRepository
import com.dj.insulink.shared.feature.friends.domain.model.Friend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    private val _glucoseUnit = MutableStateFlow(settingsPreferences.getGlucoseUnit())
    val glucoseUnit: StateFlow<GlucoseUnit> = _glucoseUnit.asStateFlow()

    fun refreshGlucoseUnit() {
        _glucoseUnit.value = settingsPreferences.getGlucoseUnit()
    }

    val allFriendsForUser = authRepository.getCurrentUserFlow()
        .flatMapLatest { userId ->
            if (userId != null) {
                friendRepository.getAllFriendsForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Companion.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _showAddNewFriendDialog = MutableStateFlow(false)
    val showAddNewFriendDialog = _showAddNewFriendDialog.asStateFlow()

    private val _enteredCode = MutableStateFlow("")
    val enteredCode = _enteredCode.asStateFlow()

    fun setShowAddNewFriendDialog(isVisible: Boolean) {
        _showAddNewFriendDialog.value = isVisible
    }

    fun setEnteredCode(code: String) {
        if (code.length <= 6) {
            _enteredCode.value = code.uppercase()
        }
    }

    fun onAddFriendClick(userId: String) {
        viewModelScope.launch {
            val candidate = friendRepository.findFriendCandidateByFriendCode(_enteredCode.value)
            candidate?.let {
                friendRepository.addFriend(
                    Friend(
                        id = 0,
                        userId = userId,
                        friendId = candidate.uid,
                        friendName = "${candidate.firstName} ${candidate.lastName}",
                        friendLastGlucoseReadingValue = candidate.latestReading?.value,
                        friendsLastGlucoseReadingTime = candidate.latestReading?.timestamp
                    )
                )

                friendRepository.pushFriendToFirestoreForUser(userId, candidate.uid)
                friendRepository.pushFriendToFirestoreForUser(candidate.uid, userId)
            }
        }
        setShowAddNewFriendDialog(false)
    }

    fun fetchFriendDataAndUpdateDatabase(userId: String) {
        viewModelScope.launch {
            friendRepository.fetchFriendDataAndUpdateDatabase(userId)
        }
    }

}