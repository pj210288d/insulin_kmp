package com.dj.insulink.feature.insulin.ui.wrapper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dj.insulink.auth.domain.models.User
import com.dj.insulink.feature.insulin.ui.InsulinScreen
import com.dj.insulink.feature.insulin.ui.InsulinScreenParams
import com.dj.insulink.feature.insulin.ui.viewmodel.InsulinViewModel

@Composable
fun InsulinWrapper(
    currentUser: User?
) {
    val viewModel: InsulinViewModel = hiltViewModel()

    val allInsulinTypesForUser = viewModel.allInsulinTypesForUser.collectAsStateWithLifecycle()
    val showAddInsulinTypeDialog = viewModel.showAddInsulinTypeDialog.collectAsStateWithLifecycle()
    val insulinTypeName = viewModel.insulinTypeName.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let {
            viewModel.fetchInsulinTypesForUserAndUpdateDatabase(it)
        }
    }

    currentUser?.let {
        InsulinScreen(
            params = InsulinScreenParams(
                insulinTypes = allInsulinTypesForUser.value,
                showAddInsulinTypeDialog = showAddInsulinTypeDialog.value,
                insulinTypeName = insulinTypeName.value,
                setShowAddInsulinTypeDialog = viewModel::setShowAddInsulinTypeDialog,
                setInsulinTypeName = viewModel::setInsulinTypeName,
                onSwipeFromStartToEnd = {
                    viewModel.deleteInsulinType(currentUser.uid, it)
                },
                onAddInsulinTypeClick = {
                    viewModel.addInsulinType(currentUser.uid)
                }
            )
        )
    }
}
