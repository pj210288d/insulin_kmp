package com.dj.insulink.shared.feature.meals.photo

import androidx.compose.runtime.Composable

interface MealPhotoPickerLauncher {
    val isCameraAvailable: Boolean
    fun pickFromGallery()
    fun takePhoto()
}

@Composable
expect fun rememberMealPhotoPickerLauncher(
    onPhotoPicked: (ByteArray) -> Unit,
    onError: (String) -> Unit
): MealPhotoPickerLauncher
