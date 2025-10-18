package com.example.scanner.ui.activities.PagesEditor

sealed interface PhotoViewPagerState {
    val activity: PdfPagesEditorActivity
    val stateData: StateData

    fun enter()
    fun exit()
    fun onBackPressed(): Boolean
    fun onSaveClicked()
    fun updateUI()
    fun handleEvent(event: ViewPagerEvent)
}