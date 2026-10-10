package com.dj.insulink.shared.core.auth

import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AuthSession {
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser

    fun setCurrentUser(user: AuthUser?) {
        _currentUser.value = user
        UserSession.setCurrentUserId(user?.uid)
    }
}
