package com.dj.insulink.feature.meals.ui.wrapper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dj.insulink.auth.domain.models.User
import com.dj.insulink.feature.meals.ui.AddMealScreen
import com.dj.insulink.feature.meals.ui.AddMealScreenParams
import com.dj.insulink.feature.meals.ui.viewmodel.MealsViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Camera photos on modern phones can be tens of MB at full resolution, which LogMeal rejects as
// too large. Downscale to a reasonable longer-side dimension and JPEG-compress before uploading;
// this stays comfortably above the resolution LogMeal needs to recognize food (confirmed down to
// ~660px during manual API testing, see dnevnik.md) while keeping the upload well under typical
// API size limits.
private const val MAX_MEAL_PHOTO_DIMENSION_PX = 1280
private const val MAX_MEAL_PHOTO_BYTES = 3 * 1024 * 1024 // 3 MB
private const val TAG = "AddMealWrapper"

@Composable
fun AddMealWrapper(
    currentUser: User?,
    navigateBack: () -> Unit
) {
    val viewModel: MealsViewModel = hiltViewModel(LocalContext.current as ComponentActivity)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val newMealName = viewModel.newMealName.collectAsStateWithLifecycle()
    val newMealComment = viewModel.newMealComment.collectAsStateWithLifecycle()
    val newMealTimestamp = viewModel.newMealTimestamp.collectAsStateWithLifecycle()
    val searchResults = viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedIngredients = viewModel.selectedIngredients.collectAsStateWithLifecycle()
    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle()
    val showCreateIngredientDialog = viewModel.showCreateIngredientDialog.collectAsStateWithLifecycle()
    val showMyIngredientsDialog = viewModel.showMyIngredientsDialog.collectAsStateWithLifecycle()
    val userIngredients = viewModel.userIngredients.collectAsStateWithLifecycle()
    val searchQuery = viewModel.searchQuery.collectAsStateWithLifecycle()
    val isAnalyzingMealPhoto = viewModel.isAnalyzingMealPhoto.collectAsStateWithLifecycle()
    val mealPhotoAnalysis = viewModel.mealPhotoAnalysis.collectAsStateWithLifecycle()
    val mealPhotoAnalysisError = viewModel.mealPhotoAnalysisError.collectAsStateWithLifecycle()

    // The FileProvider content:// Uri is only needed to let the external camera app WRITE the
    // photo into our sandboxed cache dir. Reading it back through ContentResolver right after
    // proved unreliable on-device (openInputStream returned null even though the file was
    // confirmed present on disk with real content, see dnevnik.md) — so we read our own file
    // directly by path instead, bypassing ContentResolver entirely for the read side.
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    val takePhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { photoSaved ->
        val photoPath = pendingPhotoPath
        Log.d(TAG, "TakePicture result: photoSaved=$photoSaved, photoPath=$photoPath")
        if (photoSaved && photoPath != null) {
            coroutineScope.launch(Dispatchers.IO) {
                val imageBytes = try {
                    downscaleAndCompressPhoto(File(photoPath))
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to downscale/compress meal photo", e)
                    null
                }
                Log.d(TAG, "Compressed meal photo size: ${imageBytes?.size ?: "null"} bytes")
                if (imageBytes != null) {
                    viewModel.analyzeMealPhoto(imageBytes)
                } else {
                    viewModel.reportMealPhotoReadError()
                }
            }
        } else if (!photoSaved) {
            viewModel.reportMealPhotoReadError()
        }
    }

    AddMealScreen(
        params = AddMealScreenParams(
            mealName = newMealName,
            onMealNameChange = viewModel::setNewMealName,
            mealComment = newMealComment,
            onMealCommentChange = viewModel::setNewMealComment,
            mealTimestamp = newMealTimestamp,
            onMealTimestampChange = viewModel::setNewMealTimestamp,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::setSearchQuery,
            searchResults = searchResults,
            selectedIngredients = selectedIngredients,
            onAddIngredient = viewModel::addIngredient,
            onRemoveIngredient = viewModel::removeIngredient,
            onUpdateIngredientQuantity = viewModel::updateIngredientQuantity,
            isLoading = isLoading,
            showCreateIngredientDialog = showCreateIngredientDialog,
            setShowCreateIngredientDialog = viewModel::setShowCreateIngredientDialog,
            showMyIngredientsDialog = showMyIngredientsDialog,
            setShowMyIngredientsDialog = viewModel::setShowMyIngredientsDialog,
            userIngredients = userIngredients,
            onSave = {
                viewModel.submitNewMeal(currentUser?.uid) {
                    navigateBack()
                }
            },
            onNavigateBack = navigateBack,
            createCustomIngredient = {
                viewModel.createCustomIngredient(currentUser?.uid, it)
            },
            deleteCustomIngredient = viewModel::deleteCustomIngredient,
            onTakeMealPhoto = {
                val photoFile = File(context.cacheDir, "meal_photo_${System.currentTimeMillis()}.jpg")
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                pendingPhotoPath = photoFile.absolutePath
                takePhotoLauncher.launch(photoUri)
            },
            isAnalyzingMealPhoto = isAnalyzingMealPhoto,
            mealPhotoAnalysis = mealPhotoAnalysis,
            mealPhotoAnalysisError = mealPhotoAnalysisError,
            onAcceptMealPhotoAnalysis = viewModel::acceptMealPhotoAnalysis,
            onDismissMealPhotoAnalysis = viewModel::dismissMealPhotoAnalysis
        )
    )
}

/**
 * Reads the photo at [file], downscales it to [MAX_MEAL_PHOTO_DIMENSION_PX] on its longer side,
 * and JPEG-compresses it, stepping the quality down further if needed to stay under
 * [MAX_MEAL_PHOTO_BYTES]. Must be called off the main thread — decoding a full-resolution camera
 * photo is expensive. Reads directly by file path (not through ContentResolver/FileProvider) —
 * see the comment at the call site for why.
 */
private fun downscaleAndCompressPhoto(file: File): ByteArray? {
    if (!file.exists() || file.length() == 0L) {
        Log.e(TAG, "Meal photo file missing or empty: ${file.absolutePath} (exists=${file.exists()}, length=${file.length()})")
        return null
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        Log.e(TAG, "Could not decode bounds for ${file.absolutePath}")
        return null
    }
    Log.d(TAG, "Meal photo bounds: ${bounds.outWidth}x${bounds.outHeight}")

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > MAX_MEAL_PHOTO_DIMENSION_PX * 2 ||
        bounds.outHeight / sampleSize > MAX_MEAL_PHOTO_DIMENSION_PX * 2
    ) {
        sampleSize *= 2
    }

    val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decodedBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: run {
        Log.e(TAG, "Could not decode bitmap for ${file.absolutePath} (sampleSize=$sampleSize)")
        return null
    }

    val scale = MAX_MEAL_PHOTO_DIMENSION_PX.toFloat() / maxOf(decodedBitmap.width, decodedBitmap.height)
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            decodedBitmap,
            (decodedBitmap.width * scale).toInt().coerceAtLeast(1),
            (decodedBitmap.height * scale).toInt().coerceAtLeast(1),
            true
        )
    } else {
        decodedBitmap
    }

    var quality = 90
    var bytes: ByteArray
    do {
        bytes = ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            output.toByteArray()
        }
        quality -= 15
    } while (bytes.size > MAX_MEAL_PHOTO_BYTES && quality > 30)

    return bytes
}
