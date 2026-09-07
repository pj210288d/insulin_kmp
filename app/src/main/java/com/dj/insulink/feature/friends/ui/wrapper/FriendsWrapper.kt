package com.dj.insulink.feature.friends.ui.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dj.insulink.auth.domain.models.User
import com.dj.insulink.feature.friends.ui.FriendsScreen
import com.dj.insulink.feature.friends.ui.FriendsScreenParams
import com.dj.insulink.feature.friends.ui.viewmodel.FriendsViewModel

@Composable
fun FriendsWrapper(
    currentUser: User?
) {
    val viewModel: FriendsViewModel = hiltViewModel()

    val allFriendsForUser = viewModel.allFriendsForUser.collectAsStateWithLifecycle()
    val showAddNewFriendDialog = viewModel.showAddNewFriendDialog.collectAsStateWithLifecycle()
    val enteredCode = viewModel.enteredCode.collectAsStateWithLifecycle()
    val glucoseUnit = viewModel.glucoseUnit.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let {
            viewModel.fetchFriendDataAndUpdateDatabase(it)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshGlucoseUnit()
    }

    currentUser?.let {
        FriendsScreen(
            params = FriendsScreenParams(
                friendsList = allFriendsForUser.value,
                usersFriendCode = it.friendCode,
                showAddNewFriendDialog = showAddNewFriendDialog.value,
                enteredCode = enteredCode.value,
                setShowAddNewFriendDialog = viewModel::setShowAddNewFriendDialog,
                setEnteredCode = viewModel::setEnteredCode,
                onAddFriendClick = {
                    viewModel.onAddFriendClick(userId = it.uid)
                },
                onDeleteFriend = { friend ->
                    viewModel.removeFriend(userId = it.uid, friend = friend)
                },
                glucoseUnit = glucoseUnit.value
            )
        )
    }
}