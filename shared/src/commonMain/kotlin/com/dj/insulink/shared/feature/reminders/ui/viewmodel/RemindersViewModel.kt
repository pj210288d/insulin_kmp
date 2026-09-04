package com.dj.insulink.shared.feature.reminders.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.feature.reminders.data.repository.ReminderRepository
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Peti deljeni Compose Multiplatform MVP ekran - vidi Glucose/Statistics/Insulin/Settings
// ViewModel-e za obrazac. Namerno SAMO podaci (naslov/tip/vreme/da-li-je-odrađen-danas) -
// pravo zakazivanje OS notifikacija (AlarmManager na Android-u, danas jedini pravi mehanizam u
// aplikaciji, vidi ReminderScheduler u :app) ostaje van ovog ekrana; iOS bi za to trebalo
// UNUserNotificationCenter, van obima ove MVP iteracije - vidi NotImplementedReminderRemoteDataSource
// za isti princip (lokalno da, cloud/OS integracija ne, za sada).
class RemindersViewModel(
    private val reminderRepository: ReminderRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val reminders: StateFlow<List<Reminder>> = UserSession.currentUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                reminderRepository.getAllRemindersForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _newTitle = MutableStateFlow("")
    val newTitle: StateFlow<String> = _newTitle.asStateFlow()

    private val _newType = MutableStateFlow(ReminderType.MEAL_REMINDER)
    val newType: StateFlow<ReminderType> = _newType.asStateFlow()

    fun setNewTitle(title: String) {
        _newTitle.value = title
    }

    fun setNewType(type: ReminderType) {
        _newType.value = type
    }

    fun addReminder() {
        val userId = UserSession.currentUserId.value ?: return
        val title = _newTitle.value.trim()
        if (title.isEmpty()) return
        viewModelScope.launch {
            reminderRepository.insert(
                userId,
                Reminder(
                    id = 0,
                    userId = userId,
                    title = title,
                    reminderType = _newType.value,
                    isDoneForToday = false,
                    time = currentTimeMillis()
                )
            )
        }
        _newTitle.value = ""
    }

    fun toggleDoneForToday(reminder: Reminder) {
        val userId = UserSession.currentUserId.value ?: return
        viewModelScope.launch {
            reminderRepository.insert(userId, reminder.copy(isDoneForToday = !reminder.isDoneForToday))
        }
    }

    fun deleteReminder(reminder: Reminder) {
        val userId = UserSession.currentUserId.value ?: return
        viewModelScope.launch {
            reminderRepository.delete(userId, reminder)
        }
    }
}
