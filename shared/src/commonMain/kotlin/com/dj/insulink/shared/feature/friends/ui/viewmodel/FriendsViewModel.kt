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

// Deveti deljeni Compose Multiplatform MVP ekran - vidi ostale ViewModel-e u shared/commonMain
// za obrazac. Isti obim kao Android-ov app/feature/friends (traži se prijatelj po friendCode,
// lista prijatelja + poslednje očitavanje) - vidi FriendsViewModel.kt/FriendRepository.kt tamo.
class FriendsViewModel(
    private val friendRepository: FriendRepository,
    private val settingsPreferences: SettingsPreferences
) : ViewModel() {

    // Vidi identičan komentar u drugim shared ViewModel-ima - bez ovoga lokalna baza na novom
    // uređaju/instalaciji ostaje prazna, iako je nalog isti kao na uređaju gde su podaci uneti.
    init {
        viewModelScope.launch {
            UserSession.currentUserId.collect { userId ->
                if (userId != null) {
                    runCatching { friendRepository.fetchFriendDataAndUpdateDatabase(userId) }
                }
            }
        }
    }

    // Direktno izloženo iz SettingsSession - vidi SettingsSession za kontekst (bug: promena
    // jedinice se ranije primenjivala tek posle restarta aplikacije).
    val glucoseUnit: StateFlow<GlucoseUnit> = SettingsSession.currentGlucoseUnit

    // Sopstveni friend code korisnika - iz AuthUser profila (vidi core/auth/AuthSession), ne iz
    // lokalne baze - isti podatak koji Android čita sa currentUser.friendCode.
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
        viewModelScope.launch {
            val candidate = friendRepository.findFriendCandidateByFriendCode(_enteredCode.value)
            if (candidate != null) {
                // Poznat bug (namerno OSTAVLJEN aktivan na zahtev korisnika 2026-09-07 - beta
                // testiranje je u toku, ovo treba da ostane kao bug koji beta korisnici sami
                // otkriju, ne da se tiho popravi unapred): isti prijatelj dodat dva puta pravi
                // dve instance u lokalnoj bazi (Firestore "friends" niz već ima dedup - vidi
                // FirestoreRestFriendRemoteDataSource.pushFriendToFirestoreForUser - ali
                // FriendRepository.addFriend() ovde ispod nema odgovarajuću proveru). Fix je
                // spreman, samo namerno isključen:
                // val alreadyFriend = friendRepository.isFriendAlready(userId, candidate.uid)
                // if (alreadyFriend) {
                //     _enteredCode.value = ""
                //     _showAddNewFriendDialog.value = false
                //     return@launch
                // }
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
