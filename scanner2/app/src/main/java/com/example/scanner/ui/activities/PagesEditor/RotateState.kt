package com.example.scanner.ui.activities.PagesEditor

import android.widget.Toast

class RotateState(override val activity: PdfPagesEditorActivity) : PhotoViewPagerState {
    override val stateData = StateData()

    override fun enter() {
        rotateCurrentImage()
        updateUI()
        activity.transitionTo(NormalState(activity))
    }

    override fun exit() {
        // Nothing to clean up
    }

    override fun onBackPressed(): Boolean {
        exitRotateModeWithoutSaving()
        return true
    }

    override fun onApplyClicked() {
        activity.transitionTo(NormalState(activity))
    }

    override fun updateUI() {
        activity.updateRotateUI()
    }

    override fun handleEvent(event: ViewPagerEvent) {
        when (event) {
            is ViewPagerEvent.RotateClicked -> rotateCurrentImage()
            else -> {}
        }
    }

    private fun rotateCurrentImage() {
        activity.adapter.rotateImage(stateData.currentPosition, 90f)
    }

    private fun saveOnlyRotate() {
        Toast.makeText(activity, "Поворот сохранен", Toast.LENGTH_SHORT).show()
    }

    private fun exitRotateModeWithoutSaving() {
        Toast.makeText(activity, "Поворот отменен", Toast.LENGTH_SHORT).show()
    }
}