package com.example.scanner.ui.activities.PagesEditor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.scanner.ui.adapters.PhotoAdapter
import java.io.File
import java.io.FileOutputStream

class CropState(override val activity: PdfPagesEditorActivity) : PhotoViewPagerState {
    override val stateData = StateData()
    private var currentCropRect: Rect? = null

    override fun enter() {
        activity.viewPager.isUserInputEnabled = false
        activity.adapter.setCropMode(true)
        saveOriginalBackup()
        updateUI()
    }

    override fun exit() {
        activity.viewPager.isUserInputEnabled = true
        activity.adapter.setCropMode(false)
    }

    override fun onBackPressed(): Boolean {
        exitCropModeWithoutSaving()
        return true
    }

    override fun onApplyClicked() {
        performCrop()
        activity.transitionTo(NormalState(activity))
    }

    override fun updateUI() {
        activity.updateCropUI()
    }

    override fun handleEvent(event: ViewPagerEvent) {
        when (event) {
            is ViewPagerEvent.CropRectChanged -> currentCropRect = event.rect
            else -> {}
        }
    }

    private fun saveOriginalBackup() {
        try {
            val position = stateData.currentPosition
            val photoPath = activity.photoPaths[position]
            val originalFile = File(photoPath)
            if (!originalFile.exists()) return

            val backupDir = File(activity.cacheDir, "crop_backup")
            if (!backupDir.exists()) backupDir.mkdirs()

            val backupFile = File(backupDir, "backup_${originalFile.name}")
            originalFile.copyTo(backupFile, overwrite = true)
            stateData.originalImagesBackup[position] = backupFile.absolutePath
        } catch (e: Exception) {
            Log.e("CropState", "Error creating backup: ${e.message}")
        }
    }

    private fun performCrop() {
        try {
            val position = stateData.currentPosition
            val currentPhotoPath = activity.photoPaths[position]
            val recyclerView = activity.viewPager.getChildAt(0) as? RecyclerView
            val currentViewHolder = recyclerView?.findViewHolderForAdapterPosition(position) as? PhotoAdapter.PhotoViewHolder

            currentViewHolder?.let { holder ->
                val cropRect = holder.cropOverlay.getCropRect()
                if (cropRect.width() <= 0 || cropRect.height() <= 0) {
                    Toast.makeText(activity, "Выберите область для обрезки", Toast.LENGTH_SHORT).show()
                    return
                }

                val originalBitmap = BitmapFactory.decodeFile(currentPhotoPath)
                val scaleX = originalBitmap.width.toFloat() / holder.cropOverlay.width
                val scaleY = originalBitmap.height.toFloat() / holder.cropOverlay.height

                val scaledCropRect = Rect(
                    (cropRect.left * scaleX).toInt(),
                    (cropRect.top * scaleY).toInt(),
                    (cropRect.right * scaleX).toInt(),
                    (cropRect.bottom * scaleY).toInt()
                )

                val safeCropRect = Rect(
                    scaledCropRect.left.coerceIn(0, originalBitmap.width),
                    scaledCropRect.top.coerceIn(0, originalBitmap.height),
                    scaledCropRect.right.coerceIn(0, originalBitmap.width),
                    scaledCropRect.bottom.coerceIn(0, originalBitmap.height)
                )

                val croppedBitmap = Bitmap.createBitmap(
                    originalBitmap,
                    safeCropRect.left,
                    safeCropRect.top,
                    safeCropRect.width(),
                    safeCropRect.height()
                )

                saveCroppedImage(croppedBitmap, currentPhotoPath)
                originalBitmap.recycle()
                croppedBitmap.recycle()

                deleteBackupForPosition(position)
                Toast.makeText(activity, "Фото обрезано!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("CropState", "Ошибка обрезки: ${e.message}")
            Toast.makeText(activity, "Ошибка обрезки", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCroppedImage(bitmap: Bitmap, filePath: String) {
        try {
            FileOutputStream(filePath).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private fun exitCropModeWithoutSaving() {
        Toast.makeText(activity, "Обрезка отменена", Toast.LENGTH_SHORT).show()
    }

    private fun deleteBackupForPosition(position: Int) {
        try {
            val backupPath = stateData.originalImagesBackup[position]
            backupPath?.let {
                File(it).delete()
                stateData.originalImagesBackup.remove(position)
            }
        } catch (e: Exception) {
            Log.e("CropState", "Error deleting backup: ${e.message}")
        }
    }
}