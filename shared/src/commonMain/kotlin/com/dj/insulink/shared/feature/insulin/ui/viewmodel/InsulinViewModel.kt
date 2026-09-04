package com.dj.insulink.shared.feature.insulin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.feature.insulin.data.repository.InsulinTypeRepository
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Treći deljeni Compose Multiplatform MVP ekran - vidi GlucoseViewModel/StatisticsViewModel za
// obrazac. InsulinType je najjednostavniji preostali entitet u :shared (samo `name` polje, bez
// izmene - repository podržava samo insert/delete), zato je izabran kao poslednji dodatak pre
// nego što stigne Mac.
class InsulinViewModel(
    private val insulinTypeRepository: InsulinTypeRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val insulinTypes: StateFlow<List<InsulinType>> = UserSession.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                insulinTypeRepository.getAllInsulinTypesForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _newTypeName = MutableStateFlow("")
    val newTypeName: StateFlow<String> = _newTypeName.asStateFlow()

    fun setNewTypeName(name: String) {
        _newTypeName.value = name
    }

    fun addInsulinType() {
        val userId = UserSession.currentUserId.value ?: return
        val name = _newTypeName.value.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            insulinTypeRepository.insert(userId, InsulinType(id = 0, userId = userId, name = name))
        }
        _newTypeName.value = ""
    }

    fun deleteInsulinType(insulinType: InsulinType) {
        val userId = UserSession.currentUserId.value ?: return
        viewModelScope.launch {
            insulinTypeRepository.delete(userId, insulinType)
        }
    }
}
