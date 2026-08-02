package com.dj.insulink.feature.reminders.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.dj.insulink.auth.data.AuthRepository
import com.dj.insulink.core.notification.ReminderScheduler
import com.dj.insulink.shared.feature.reminders.data.repository.ReminderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import com.dj.insulink.feature.reminders.ui.displayNameRes
import com.dj.insulink.shared.feature.reminders.domain.model.Reminder
import com.dj.insulink.shared.feature.reminders.domain.model.ReminderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    private val reminderRepository: ReminderRepository,
    private val authRepository: AuthRepository,
    private val reminderScheduler: ReminderScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val allRemindersForUser: StateFlow<List<Reminder>> = authRepository.getCurrentUserFlow()
        .flatMapLatest { userId ->
            if (userId != null) {
                reminderRepository.getAllRemindersForUser(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _showAddReminderDialog = MutableStateFlow(false)
    val showAddReminderDialog = _showAddReminderDialog.asStateFlow()

    private val _reminderTitle = MutableStateFlow("")
    val reminderTitle = _reminderTitle.asStateFlow()

    private val _reminderType = MutableStateFlow(ReminderType.MEAL_REMINDER)
    val reminderType = _reminderType.asStateFlow()

    private val _reminderTime = MutableStateFlow(0L)
    val reminderTime = _reminderTime.asStateFlow()

    fun setShowAddReminderDialog(isVisible: Boolean) {
        _showAddReminderDialog.value = isVisible
    }

    fun setReminderTitle(title: String) {
        if (title.length <= 20) {
            _reminderTitle.value = title
        }
    }

    fun setReminderType(type: ReminderType) {
        _reminderType.value = type
    }

    fun setReminderTime(time: Long) {
        _reminderTime.value = time
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addReminder(userId: String) {
        viewModelScope.launch {
            val reminderTime = _reminderTime.value

            val reminderLocalTime = Instant.ofEpochMilli(reminderTime)
                .atZone(ZoneId.systemDefault())
                .toLocalTime()

            val reminderId = reminderRepository.insert(
                userId = userId,
                reminder = Reminder(
                    id = 0,
                    userId = userId,
                    title = _reminderTitle.value,
                    reminderType = _reminderType.value,
                    isDoneForToday = reminderLocalTime.isBefore(LocalTime.now()),
                    time = _reminderTime.value
                )
            )

            reminderScheduler.scheduleDaily(
                reminderId = reminderId,
                title = _reminderTitle.value,
                message = context.getString(_reminderType.value.displayNameRes),
                hour = reminderLocalTime.hour,
                minute = reminderLocalTime.minute
            )
            resetAddReminderFields()
        }
    }

    fun deleteReminder(userId: String?, reminder: Reminder) {
        viewModelScope.launch {
            userId?.let {
                reminderScheduler.cancelReminder(reminder.id)
                reminderRepository.delete(
                    userId = userId,
                    reminder = reminder
                )
            }
        }
    }

    fun fetchReminderDataAndUpdateDatabase(userId: String) {
        viewModelScope.launch {
            reminderRepository.fetchAllRemindersForUserAndUpdateDatabase(userId)
        }
    }

    fun resetAddReminderFields() {
        _reminderTitle.value = ""
        _reminderType.value = ReminderType.MEAL_REMINDER
        _reminderTime.value = System.currentTimeMillis()
    }

}