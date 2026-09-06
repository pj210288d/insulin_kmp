package com.dj.insulink.shared.feature.meals.photo

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Isti downscale/kompresija princip kao Android-ov postojeći
// app/feature/meals/ui/wrapper/AddMealWrapper.kt (LogMeal odbija prevelike slike) - namerno
// duplirano ovde umesto deljeno, jer je android.graphics.Bitmap Android-only i ne može u
// commonMain, a shared modul ne zavisi od app modula (pogrešan smer zavisnosti).
private const val MAX_MEAL_PHOTO_DIMENSION_PX = 1280
private const val MAX_MEAL_PHOTO_BYTES = 3 * 1024 * 1024 // 3 MB
private const val TAG = "MealPhotoPicker"

@Composable
actual fun rememberMealPhotoPickerLauncher(
    onPhotoPicked: (ByteArray) -> Unit,
    onError: (String) -> Unit
): MealPhotoPickerLauncher {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        coroutineScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                try {
                    val original = context.contentResolver.openInputStream(uri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                    original?.let { downscaleAndCompress(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read photo from gallery", e)
                    null
                }
            }
            if (bytes != null) onPhotoPicked(bytes) else onError("Nije moguće pročitati sliku iz galerije")
        }
    }

    // Vidi identičan komentar/rešenje u AddMealWrapper.kt - FileProvider content:// Uri je samo
    // za spoljnu kamera-aplikaciju da UPIŠE fajl, čita se nazad direktno preko putanje.
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { photoSaved ->
        val photoPath = pendingPhotoPath
        if (photoSaved && photoPath != null) {
            coroutineScope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    try {
                        val file = File(photoPath)
                        if (!file.exists() || file.length() == 0L) return@withContext null
                        BitmapFactory.decodeFile(file.absolutePath)?.let { downscaleAndCompress(it) }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to read photo from camera", e)
                        null
                    }
                }
                if (bytes != null) onPhotoPicked(bytes) else onError("Nije moguće pročitati fotografiju sa kamere")
            }
        } else if (!photoSaved) {
            onError("Fotografisanje otkazano ili neuspešno")
        }
    }

    val isCameraAvailable = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    return remember(context) {
        object : MealPhotoPickerLauncher {
            override val isCameraAvailable: Boolean = isCameraAvailable

            override fun pickFromGallery() {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }

            override fun takePhoto() {
                val photoFile = File(context.cacheDir, "meal_photo_${System.currentTimeMillis()}.jpg")
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                pendingPhotoPath = photoFile.absolutePath
                cameraLauncher.launch(photoUri)
            }
        }
    }
}

private fun downscaleAndCompress(decoded: Bitmap): ByteArray {
    val scale = MAX_MEAL_PHOTO_DIMENSION_PX.toFloat() / maxOf(decoded.width, decoded.height)
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        )
    } else {
        decoded
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
