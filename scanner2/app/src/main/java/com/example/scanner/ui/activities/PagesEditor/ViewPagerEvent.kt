package com.example.scanner.ui.activities.PagesEditor

import android.graphics.Rect
import com.example.scanner.PhotoFilters

sealed class ViewPagerEvent {
    object CropClicked : ViewPagerEvent()
    object FilterClicked : ViewPagerEvent()
    object RotateClicked : ViewPagerEvent()
    object ApplyClicked : ViewPagerEvent()
    object UndoClicked : ViewPagerEvent()
    object RedoClicked : ViewPagerEvent()
    object AddPageClicked : ViewPagerEvent()
    data class PageSelected(val position: Int) : ViewPagerEvent()
    data class FilterSelected(val filterType: PhotoFilters.FilterType) : ViewPagerEvent()
    data class IntensityChanged(val intensity: Float) : ViewPagerEvent()
    data class CropRectChanged(val rect: Rect) : ViewPagerEvent()
}