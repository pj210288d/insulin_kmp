package com.dj.insulink.core.ui.viewmodel

import android.content.Context
import androidx.compose.material3.DrawerState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.auth.domain.models.User
import com.dj.insulink.shared.core.session.UserSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser: MutableStateFlow<User?> = MutableStateFlow(null)
    val currentUser = _currentUser.asStateFlow()

    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }

    fun getCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _currentUser.value = user
            // Puni UserSession u :shared - vidi taj fajl. Deljeni Compose Multiplatform Glucose
            // ekran (GlucoseViewModel u shared/.../glucose/ui/viewmodel) čita odatle koji je
            // korisnik trenutno prijavljen umesto da direktno zavisi od Firebase Auth-a.
            UserSession.setCurrentUserId(user?.uid)
        }
    }

    fun signOut(context: Context) {
        authRepository.signOut(context)
    }
}