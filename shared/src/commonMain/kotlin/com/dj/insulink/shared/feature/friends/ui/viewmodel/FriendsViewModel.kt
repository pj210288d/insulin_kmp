package com.dj.insulink.shared.feature.friends.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.auth.AuthSession
import com.dj.insulink.shared.core.session.SettingsSession
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.friends.data.repository.FriendRepository
import com.dj.insulink.shared.feature.friends.domain.model.Friend
import com.dj.insulink.shared.feature.settings.data.SettingsPreferences
import com.dj.insulink.shared.feature.settings.domain.model.GlucoseUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FriendsViewModel(
    private val friendRepository: FriendRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    init {
        viewModelScope.launch {
            UserSession.currentUserId.collect { userId ->
                if (userId != null) {
                    runCatching { friendRepository.fetchFriendDataAndUpdateDatabase(userId) }
                }
            }
        }
    }

    val glucoseUnit: StateFlow<GlucoseUnit> = SettingsSession.currentGlucoseUnit

    val usersFriendCode: StateFlow<String> = AuthSession.currentUser
        .map { it?.friendCode.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    @OptIn(ExperimentalCoroutinesApi::class)
    val allFriendsForUser: StateFlow<List<Friend>> = UserSession.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                friendRepository.getAllFriendsForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddNewFriendDialog = MutableStateFlow(false)
    val showAddNewFriendDialog: StateFlow<Boolean> = _showAddNewFriendDialog.asStateFlow()

    private val _enteredCode = MutableStateFlow("")
    val enteredCode: StateFlow<String> = _enteredCode.asStateFlow()

    fun setShowAddNewFriendDialog(isVisible: Boolean) {
        _showAddNewFriendDialog.value = isVisible
    }

    fun setEnteredCode(code: String) {
        if (code.length <= FRIEND_CODE_MAX_LENGTH) {
            _enteredCode.value = code.uppercase()
        }
    }

    fun addFriend() {
        val userId = UserSession.currentUserId.value ?: return
        val code = _enteredCode.value
        viewModelScope.launch {
            val candidate = friendRepository.findFriendCandidateByFriendCode(code)
            if (candidate != null) {
                if (friendRepository.isFriendAlready(userId, candidate.uid)) {
                    return@launch
                }
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
        _enteredCode.value = ""
        _showAddNewFriendDialog.value = false
    }

    fun removeFriend(friend: Friend) {
        val userId = UserSession.currentUserId.value ?: return
        viewModelScope.launch {
            friendRepository.deleteFriend(userId, friend.friendId)
        }
    }
}

private const val FRIEND_CODE_MAX_LENGTH = 6
