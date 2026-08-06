package com.dj.insulink.feature.insulin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.shared.feature.insulin.data.repository.InsulinTypeRepository
import com.dj.insulink.shared.feature.insulin.domain.model.InsulinType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class InsulinViewModel @Inject constructor(
    private val insulinTypeRepository: InsulinTypeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val allInsulinTypesForUser: StateFlow<List<InsulinType>> = authRepository.getCurrentUserFlow()
        .flatMapLatest { userId ->
            if (userId != null) {
                insulinTypeRepository.getAllInsulinTypesForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _showAddInsulinTypeDialog = MutableStateFlow(false)
    val showAddInsulinTypeDialog = _showAddInsulinTypeDialog.asStateFlow()

    private val _insulinTypeName = MutableStateFlow("")
    val insulinTypeName = _insulinTypeName.asStateFlow()

    fun setShowAddInsulinTypeDialog(isVisible: Boolean) {
        _showAddInsulinTypeDialog.value = isVisible
    }

    fun setInsulinTypeName(name: String) {
        if (name.length <= NAME_MAXIMUM_LENGTH) {
            _insulinTypeName.value = name
        }
    }

    fun addInsulinType(userId: String) {
        viewModelScope.launch {
            insulinTypeRepository.insert(
                userId = userId,
                insulinType = InsulinType(id = 0, userId = userId, name = _insulinTypeName.value)
            )
            resetAddInsulinTypeFields()
        }
    }

    fun deleteInsulinType(userId: String?, insulinType: InsulinType) {
        viewModelScope.launch {
            userId?.let {
                insulinTypeRepository.delete(userId = userId, insulinType = insulinType)
            }
        }
    }

    fun fetchInsulinTypesForUserAndUpdateDatabase(userId: String) {
        viewModelScope.launch {
            insulinTypeRepository.fetchAllInsulinTypesForUserAndUpdateDatabase(userId)
        }
    }

    private fun resetAddInsulinTypeFields() {
        _insulinTypeName.value = ""
    }
}

private const val NAME_MAXIMUM_LENGTH = 30
