package com.example.scanner.ui.activities.PagesEditor

import android.annotation.SuppressLint
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
import java.io.IOException

class FilterState(override val activity: PdfPagesEditorActivity) : PhotoViewPagerState {

    override val stateData = StateData()
    private var currentFilter: PhotoFilters.FilterType = PhotoFilters.FilterType.NONE

    @SuppressLint("SuspiciousIndentation")
    override fun enter(){
        val position = activity.viewPager.currentItem

        val photoPath = activity.photoPaths.getOrNull(position)

        try {
            val bitmap = BitmapFactory.decodeFile(photoPath)
            bitmap?.recycle()
            Toast.makeText(activity, "bitmap found", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(activity, "bitmap not found", Toast.LENGTH_SHORT).show()
        }
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
        activity.transitionTo(NormalState(activity))
        return true
    }

    override fun onApplyClicked() {
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

    private fun loadFilterPreview(imageView: ImageView, filterType: PhotoFilters.FilterType){
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

    private fun getFreshOriginalBitmap(position: Int): Bitmap? {
        val photoPath = activity.photoPaths[position]
        return BitmapFactory.decodeFile(photoPath)
    }

    private fun applyFilterToCurrentPhoto(filterType: PhotoFilters.FilterType) {
        currentFilter = filterType
        val position = stateData.currentPosition
        val freshBitmap = getFreshOriginalBitmap(stateData.currentPosition)

        freshBitmap?.let {
            stateData.originalBitmaps[stateData.currentPosition] = it.copy(Bitmap.Config.ARGB_8888, true)
        }
        activity.adapter.setFilterForPosition(stateData.currentPosition, filterType, stateData.currentIntensity)
    }

    private fun applyFilterWithIntensity(intensity: Float) {
        if (currentFilter != PhotoFilters.FilterType.NONE) {
            activity.adapter.setFilterForPosition(stateData.currentPosition, currentFilter, intensity)
        }
    }

    private fun saveFilterChanges(){
        saveFilterImageToFile()
    }

    private fun exitFilterModeWithoutSaving() {
        restoreOriginalImageForCurrentPosition()
        Toast.makeText(activity, "Фильтр отменен", Toast.LENGTH_SHORT).show()
    }

    private fun restoreOriginalImageForCurrentPosition() {
        val position = stateData.currentPosition
        activity.adapter.clearFilter(position)
       // stateData.originalBitmaps.remove(position)
    }

    private fun saveFilterImageToFile() {
        val position = stateData.currentPosition
        val photoPath = activity.photoPaths[position]
        val currentIntensity = activity.adapter.filterIntensityMap[position] ?: 1.0f
        val filterToApply = activity.adapter.getFilterForPosition(position)

        if (filterToApply != PhotoFilters.FilterType.NONE) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val originalBitmap = stateData.originalBitmaps[position]

                    originalBitmap?.let { bitmap ->

                        val filteredBitmap = PhotoFilters.applyFilter(
                            activity,
                            bitmap,
                            filterToApply,
                            currentIntensity
                        )
                        withContext(Dispatchers.Main){
                            Toast.makeText(activity, "применяем фильтр $filterToApply", Toast.LENGTH_SHORT).show()
                        }

                        try {
                            FileOutputStream(photoPath).use { out ->
                                val success = filteredBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                if (!success) {
                                    throw IOException("файл НЕ обновлен")
                                }
                                else{
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(activity, "файл обновлен", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            stateData.originalBitmaps[position] =
                                filteredBitmap.copy(Bitmap.Config.ARGB_8888, true)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(activity, "Фильтр сохранён", Toast.LENGTH_SHORT).show()
                                activity.adapter.notifyItemChanged(position)
                            }
                        } catch (e: Exception) {
                            Log.e("SaveError", "Failed to save filtered image", e)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(activity, "Ошибка при сохранении фильтра", Toast.LENGTH_SHORT).show()
                            }
                        }
                        finally {
                            filteredBitmap.recycle()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(activity, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("SaveError", "$e.message")
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