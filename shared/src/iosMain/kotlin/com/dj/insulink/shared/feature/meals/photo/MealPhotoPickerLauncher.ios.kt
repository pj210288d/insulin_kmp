package com.dj.insulink.shared.feature.meals.photo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import platform.posix.memcpy
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

// Isti presentation obrazac kao IosPdfShareCoordinator.kt (UIApplication.sharedApplication.
// keyWindow?.rootViewController) - jedini način da se native UIKit view controller prikaže iz
// Compose Multiplatform ekrana bez sopstvenog UIViewController-a. Vidi MealPhotoPickerLauncher.kt
// (commonMain) za kontekst zašto ovo postoji - galerija radi na simulatoru, kamera se testira
// sutra na fizičkom uređaju.
private const val MAX_MEAL_PHOTO_DIMENSION_PX = 1280.0
private const val MAX_MEAL_PHOTO_BYTES = 3 * 1024 * 1024 // 3 MB

@Composable
actual fun rememberMealPhotoPickerLauncher(
    onPhotoPicked: (ByteArray) -> Unit,
    onError: (String) -> Unit
): MealPhotoPickerLauncher {
    return remember { MealPhotoPickerCoordinator(onPhotoPicked, onError) }
}

private class MealPhotoPickerCoordinator(
    private val onPhotoPicked: (ByteArray) -> Unit,
    private val onError: (String) -> Unit
) : MealPhotoPickerLauncher {

    // UIImagePickerController.delegate je slab (weak) ObjC pokazivač - bez ove jake reference
    // delegat bi bio dealociran čim se present() vrati, pre nego što korisnik uopšte izabere
    // sliku, i callback nikad ne bi stigao.
    private var activeDelegate: ImagePickerDelegate? = null

    override val isCameraAvailable: Boolean
        get() = UIImagePickerController.isSourceTypeAvailable(
            UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        )

    override fun pickFromGallery() {
        present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary)
    }

    override fun takePhoto() {
        if (!isCameraAvailable) {
            onError("Kamera nije dostupna (Simulator nema kameru - testiraj na fizičkom uređaju)")
            return
        }
        present(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)
    }

    private fun present(sourceType: UIImagePickerControllerSourceType) {
        val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
        if (rootViewController == null) {
            onError("Nije moguće otvoriti biranje slike")
            return
        }

        val picker = UIImagePickerController()
        picker.sourceType = sourceType

        val delegate = ImagePickerDelegate(
            onPicked = { image ->
                activeDelegate = null
                val bytes = resizeAndCompressImage(image)
                if (bytes != null) {
                    onPhotoPicked(bytes)
                } else {
                    onError("Nije moguće obraditi izabranu sliku")
                }
            },
            onCancelledOrFailed = { activeDelegate = null }
        )
        activeDelegate = delegate
        picker.delegate = delegate

        rootViewController.presentViewController(picker, animated = true, completion = null)
    }
}

private class ImagePickerDelegate(
    private val onPicked: (UIImage) -> Unit,
    private val onCancelledOrFailed: () -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        if (image != null) onPicked(image) else onCancelledOrFailed()
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onCancelledOrFailed()
    }
}

// Isti downscale-cilj kao Android-ova varijanta (LogMeal odbija prevelike slike) - stara
// UIGraphicsBeginImageContextWithOptions/UIGraphicsGetImageFromCurrentImageContext C-API umesto
// modernijeg UIGraphicsImageRenderer, iz istog opreznog razloga kao CGContextShowTextAtPoint u
// IosPdfReportGenerator.kt (nesigurna cinterop rezolucija novijih API-ja u ovom projektu).
@OptIn(ExperimentalForeignApi::class)
private fun resizeAndCompressImage(image: UIImage): ByteArray? {
    val (width, height) = image.size.useContents { this.width to this.height }
    if (width <= 0.0 || height <= 0.0) return null

    val longerSide = maxOf(width, height)
    val scale = if (longerSide > MAX_MEAL_PHOTO_DIMENSION_PX) MAX_MEAL_PHOTO_DIMENSION_PX / longerSide else 1.0
    val targetWidth = width * scale
    val targetHeight = height * scale

    UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), false, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    val finalImage = resizedImage ?: image

    var quality = 0.9
    var data = UIImageJPEGRepresentation(finalImage, quality)
    while (data != null && data.length.toInt() > MAX_MEAL_PHOTO_BYTES && quality > 0.3) {
        quality -= 0.15
        data = UIImageJPEGRepresentation(finalImage, quality)
    }
    return data?.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val len = length.toInt()
    if (len <= 0) return ByteArray(0)
    val result = ByteArray(len)
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, length)
    }
    return result
}
