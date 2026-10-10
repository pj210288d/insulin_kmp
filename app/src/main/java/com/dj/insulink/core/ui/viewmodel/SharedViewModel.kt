package com.dj.insulink.core.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.auth.domain.models.User
import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import com.dj.insulink.shared.feature.auth.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _currentUser: MutableStateFlow<User?> = MutableStateFlow(null)
    val currentUser = _currentUser.asStateFlow()

    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    fun getCurrentUser() {
        viewModelScope.launch {
            val authRepository = GlobalContext.get().get<AuthRepository>()
            val authUser = runCatching { authRepository.restoreSession() }.getOrNull()
            _currentUser.value = authUser?.toNativeUser()
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            val authRepository = GlobalContext.get().get<AuthRepository>()
            runCatching { authRepository.signOut() }
            _currentUser.value = null
        }
    }
}

private fun AuthUser.toNativeUser(): User = User(
    uid = uid,
    firstName = firstName,
    lastName = lastName,
    email = email,
    friendCode = friendCode,
    isEmailVerified = isEmailVerified
)
