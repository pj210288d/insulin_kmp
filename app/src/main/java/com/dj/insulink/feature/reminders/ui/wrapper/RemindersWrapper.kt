package com.dj.insulink.feature.reminders.ui.wrapper

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dj.insulink.auth.domain.models.User
import com.dj.insulink.feature.reminders.ui.RemindersScreen
import com.dj.insulink.feature.reminders.ui.RemindersScreenParams
import com.dj.insulink.feature.reminders.ui.viewmodel.RemindersViewModel

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun RemindersWrapper(
    currentUser: User?
) {
    val viewModel: RemindersViewModel = hiltViewModel()

    val allRemindersForUser = viewModel.allRemindersForUser.collectAsStateWithLifecycle()
    val showAddReminderDialog = viewModel.showAddReminderDialog.collectAsStateWithLifecycle()
    val reminderTitle = viewModel.reminderTitle.collectAsStateWithLifecycle()
    val reminderType = viewModel.reminderType.collectAsStateWithLifecycle()
    val reminderTime = viewModel.reminderTime.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let {
            viewModel.fetchReminderDataAndUpdateDatabase(it)
        }
    }

    currentUser?.let {
        RemindersScreen(
            params = RemindersScreenParams(
                reminders = allRemindersForUser.value,
                showAddReminderDialog = showAddReminderDialog.value,
                reminderTitle = reminderTitle.value,
                reminderType = reminderType.value,
                reminderTime = reminderTime.value,
                setShowAddReminderDialog = viewModel::setShowAddReminderDialog,
                setReminderTitle = viewModel::setReminderTitle,
                setReminderType = viewModel::setReminderType,
                setReminderTime = viewModel::setReminderTime,
                onSwipeFromStartToEnd = {
                    viewModel.deleteReminder(currentUser.uid, it)
                },
                onToggleDone = {
                    viewModel.toggleDoneForToday(currentUser.uid, it)
                },
                onAddReminderClick = {
                    viewModel.addReminder(currentUser.uid)
                }
            )
        )
    }
}