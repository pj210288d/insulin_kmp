package com.dj.insulink.shared.core.auth

import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.auth.domain.model.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Gate-uje App() root (shared Auth ekrani kad je null, tab traka kad nije) - NE zamenjuje
// UserSession (taj i dalje čita svaki postojeći ViewModel radi lokalnog user-scoped Room
// upita, ostaje netaknut da se ne pokvari nijedan od 8 postojećih ekrana). setCurrentUser ovde
// UVEK ažurira i UserSession, tako da su dva stanja uvek sinhronizovana - jedno mesto istine za
// "koji je uid", dva mesta koja ga čitaju iz istorijskih razloga.
object AuthSession {
    private val _currentUser = MutableStateFlow<AuthUser?>(null)
    val currentUser: StateFlow<AuthUser?> = _currentUser

    fun setCurrentUser(user: AuthUser?) {
        _currentUser.value = user
        UserSession.setCurrentUserId(user?.uid)
    }
}
