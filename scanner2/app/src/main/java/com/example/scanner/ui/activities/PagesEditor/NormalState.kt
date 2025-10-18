package com.example.scanner.ui.activities.PagesEditor

import android.util.Log
import android.widget.Toast
import java.io.File

class NormalState(override val activity: PdfPagesEditorActivity) : PhotoViewPagerState {
    override val stateData = StateData()

    override fun enter() {
        activity.viewPager.isUserInputEnabled = true
        updateUI()
    }

    override fun exit() {
        // Cleanup if needed
    }

    override fun onBackPressed(): Boolean {
        activity.clearAllData()
        return false
    }

    override fun onSaveClicked() {
        activity.showHalfScreenDialog()
    }

    override fun updateUI() {
        activity.updateNormalUI()
    }

    override fun handleEvent(event: ViewPagerEvent) {
        when (event) {
            is ViewPagerEvent.UndoClicked -> undoLastCrop()
            else -> {}
        }
    }

    private fun undoLastCrop() {
        try {
            val position = stateData.currentPosition
            val backupPath = stateData.originalImagesBackup[position] ?: return
            val currentPath = activity.photoPaths[position]

            val backupFile = File(backupPath)
            val currentFile = File(currentPath)

            if (!backupFile.exists()) {
                stateData.originalImagesBackup.remove(position)
                return
            }

            backupFile.copyTo(currentFile, overwrite = true)
            backupFile.delete()
            stateData.originalImagesBackup.remove(position)

            activity.adapter.notifyItemChanged(position)
            Toast.makeText(activity, "Обрезка отменена", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("NormalState", "Ошибка отмены обрезки: ${e.message}")
        }
    }
}