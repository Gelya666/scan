package com.example.scanner.ui.activities.PagesEditor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.example.scanner.PhotoFilters
import com.example.scanner.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class FilterState(override val activity: PdfPagesEditorActivity) : PhotoViewPagerState {
    override val stateData = StateData()
    private var currentFilter: PhotoFilters.FilterType = PhotoFilters.FilterType.NONE

    override fun enter() {
        saveOriginalBitmapForCurrentPosition()
        setupFilterPanel()
        showFilterPanel()
        updateUI()
    }

    override fun exit() {
        hideFilterPanel()
    }

    override fun onBackPressed(): Boolean {
        exitFilterModeWithoutSaving()
        return true
    }

    override fun onSaveClicked() {
        saveFilterChanges()
        activity.transitionTo(NormalState(activity))
    }

    override fun updateUI() {
        activity.updateFilterUI()
    }

    override fun handleEvent(event: ViewPagerEvent) {
        when (event) {
            is ViewPagerEvent.FilterSelected -> {
                currentFilter = event.filterType
                applyFilterToCurrentPhoto(event.filterType)
                updateIntensityPanelVisibility(event.filterType)
            }
            is ViewPagerEvent.IntensityChanged -> {
                stateData.currentIntensity = event.intensity
                applyFilterWithIntensity(event.intensity)
            }
            else -> {}
        }
    }

    private fun setupFilterPanel() {
        val filters = PhotoFilters.getAllFilters()
        filters.forEach { filterItem ->
            val filterView = activity.layoutInflater.inflate(R.layout.item_filter, activity.filtersContainer, false)
            val filterIcon: ImageView = filterView.findViewById(R.id.filterIcon)
            val filterName: TextView = filterView.findViewById(R.id.filterName)

            loadFilterPreview(filterIcon, filterItem.type)
            filterName.text = filterItem.name

            filterView.setOnClickListener {
                handleEvent(ViewPagerEvent.FilterSelected(filterItem.type))
            }
            activity.filtersContainer.addView(filterView)
        }

        activity.intensitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val intensity = progress / 100f
                    activity.intensityValue.text = "${progress}%"
                    handleEvent(ViewPagerEvent.IntensityChanged(intensity))
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun loadFilterPreview(imageView: ImageView, filterType: PhotoFilters.FilterType) {
        if (activity.photoPaths.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                    val bitmap = BitmapFactory.decodeFile(activity.photoPaths[0], options)
                    val previewBitmap = PhotoFilters.applyFilter(
                        activity,
                        bitmap,
                        filterType,
                        0.7f
                    )
                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(previewBitmap)
                    }
                    bitmap.recycle()
                } catch (e: Exception) {
                    Log.e("FilterState", "Error loading preview: ${e.message}")
                }
            }
        }
    }

    private fun saveOriginalBitmapForCurrentPosition() {
        val photoPath = activity.photoPaths[stateData.currentPosition]
        val bitmap = BitmapFactory.decodeFile(photoPath)
        bitmap?.let {
            stateData.originalBitmaps[stateData.currentPosition] = it.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    private fun applyFilterToCurrentPhoto(filterType: PhotoFilters.FilterType) {
        currentFilter = filterType
        activity.adapter.setFilterForPosition(stateData.currentPosition, filterType, stateData.currentIntensity)
    }

    private fun applyFilterWithIntensity(intensity: Float) {
        if (currentFilter != PhotoFilters.FilterType.NONE) {
            activity.adapter.setFilterForPosition(stateData.currentPosition, currentFilter, intensity)
        }
    }

    private fun saveFilterChanges() {
        saveFilterImageToFile()
        Toast.makeText(activity, "фильтр сохранён", Toast.LENGTH_SHORT).show()
    }

    private fun exitFilterModeWithoutSaving() {
        restoreOriginalImageForCurrentPosition()
        Toast.makeText(activity, "Фильтр отменен", Toast.LENGTH_SHORT).show()
    }

    private fun restoreOriginalImageForCurrentPosition() {
        activity.adapter.clearFilter(stateData.currentPosition)
        stateData.originalBitmaps.remove(stateData.currentPosition)
    }

    private fun saveFilterImageToFile() {
        val position = stateData.currentPosition
        val photoPath = activity.photoPaths[position]
        val currentIntensity = activity.adapter.filterIntensityMap[position] ?: 1.0f

        if (currentFilter != PhotoFilters.FilterType.NONE) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val originalBitmap = stateData.originalBitmaps[position]
                    originalBitmap?.let { bitmap ->
                        val filteredBitmap = PhotoFilters.applyFilter(
                            activity,
                            bitmap,
                            currentFilter,
                            currentIntensity
                        )
                        FileOutputStream(photoPath).use { out ->
                            filteredBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        stateData.originalBitmaps[position] =
                            filteredBitmap.copy(Bitmap.Config.ARGB_8888, true)
                        withContext(Dispatchers.Main) {
                            activity.adapter.clearFilter(position)
                        }
                        filteredBitmap.recycle()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateIntensityPanelVisibility(filterType: PhotoFilters.FilterType) {
        val hasIntensity = when (filterType) {
            PhotoFilters.FilterType.SEPIA,
            PhotoFilters.FilterType.SATURATE,
            PhotoFilters.FilterType.BRIGHTNESS,
            PhotoFilters.FilterType.CONTRAST,
            PhotoFilters.FilterType.BLUR,
            PhotoFilters.FilterType.SHARPEN,
            PhotoFilters.FilterType.EMBOSS,
            PhotoFilters.FilterType.TOON,
            PhotoFilters.FilterType.SWIRL -> true
            else -> false
        }
        activity.intensityPanel.visibility = if (hasIntensity) View.VISIBLE else View.GONE
    }

    private fun showFilterPanel() {
        activity.filterPanel.visibility = View.VISIBLE
    }

    private fun hideFilterPanel() {
        activity.filterPanel.visibility = View.GONE
        activity.intensityPanel.visibility = View.GONE
    }
}