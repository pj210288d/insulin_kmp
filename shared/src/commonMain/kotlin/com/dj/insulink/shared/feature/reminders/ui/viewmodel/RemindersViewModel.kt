package com.dj.insulink.shared.feature.reminders.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dj.insulink.shared.core.session.UserSession
import com.dj.insulink.shared.core.time.currentTimeMillis
import com.dj.insulink.shared.core.time.localTimeOfDay
import com.dj.insulink.shared.feature.reminders.data.notification.ReminderNotificationScheduler
import com.dj.insulink.shared.feature.reminders.data.repository.ReminderRepository
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType
import com.dj.insulink.shared.feature.reminders.domain.model.notificationMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RemindersViewModel(
    private val reminderRepository: ReminderRepository,
    private val notificationScheduler: ReminderNotificationScheduler
) : ViewModel() {

    init {
        viewModelScope.launch {
            UserSession.currentUserId.collect { userId ->
                if (userId != null) {
                    runCatching {
                        reminderRepository.fetchAllRemindersForUserAndUpdateDatabase(userId)
                    }
                }
            }
        }
    }

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

    private val _newTime = MutableStateFlow(currentTimeMillis())
    val newTime: StateFlow<Long> = _newTime.asStateFlow()

    fun setNewTitle(title: String) {
        if (title.length <= TITLE_MAX_LENGTH) {
            _newTitle.value = title
        }
    }

    fun setNewType(type: ReminderType) {
        _newType.value = type
    }

    fun setNewTime(time: Long) {
        _newTime.value = time
    }

    fun addReminder() {
        val userId = UserSession.currentUserId.value ?: return
        val title = _newTitle.value.trim()
        if (title.isEmpty()) return
        val time = _newTime.value
        val type = _newType.value
        viewModelScope.launch {
            val reminderId = reminderRepository.insert(
                userId,
                Reminder(
                    id = 0,
                    userId = userId,
                    title = title,
                    reminderType = type,
                    isDoneForToday = false,
                    time = time
                )
            )
            val timeOfDay = localTimeOfDay(time)
            runCatching {
                notificationScheduler.scheduleDaily(
                    reminderId = reminderId,
                    title = title,
                    message = type.notificationMessage(),
                    hour = timeOfDay.hour,
                    minute = timeOfDay.minute
                )
            }
        }
        _newTitle.value = ""
        _newTime.value = currentTimeMillis()
    }

    fun toggleDoneForToday(reminder: Reminder) {
        val userId = UserSession.currentUserId.value ?: return
        viewModelScope.launch {
            reminderRepository.setDoneForToday(userId, reminder, !reminder.isDoneForToday)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        val userId = UserSession.currentUserId.value ?: return
        notificationScheduler.cancelReminder(reminder.id)
        viewModelScope.launch {
            reminderRepository.delete(userId, reminder)
        }
    }

}

private const val TITLE_MAX_LENGTH = 20
