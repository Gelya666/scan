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
import java.io.File
import java.io.FileOutputStream
import java.util.Date

class FilterState(override val activity: PdfPagesEditorActivity) : PhotoViewPagerState {

    override val stateData = StateData()
    private var currentFilter: PhotoFilters.FilterType = PhotoFilters.FilterType.NONE

    @SuppressLint("SuspiciousIndentation")
    override fun enter(){
        Log.d("NORMAL_STATE", "=== enter() NormalState ===")
        val position = activity.viewPager.currentItem
                Log.d("NORMAL_STATE", "Текущая позиция при входе: $position")
        val filterInAdapter = activity.adapter.getFilterForPosition(position)
        Log.d("NORMAL_STATE", "adapter.filtersMap[$position] = $filterInAdapter")

        // Какой файл на диске?
        val photoPath = activity.photoPaths.getOrNull(position)
        Log.d("NORMAL_STATE", "photoPath = $photoPath")

        val file = File(photoPath ?: "")
        Log.d("NORMAL_STATE", "Файл существует: ${file.exists()}")
        Log.d("NORMAL_STATE", "Размер файла: ${file.length()}")

        // Загружаем файл для проверки
        try {
            val bitmap = BitmapFactory.decodeFile(photoPath)
            Log.d("NORMAL_STATE", "Загруженный bitmap: ${bitmap?.width}x${bitmap?.height}, ${bitmap != null}")
            bitmap?.recycle()
        } catch (e: Exception) {
            Log.d("NORMAL_STATE", "Ошибка загрузки: ${e.message}")
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

        Log.d("FILTER_FLOW", "★★★★★ onApplyClicked() НАЧАЛО ★★★★★")

        // Получаем информацию о текущих фильтрах через методы адаптера
        val filtersInfo = activity.adapter.getFiltersInfo()
        val position = activity.viewPager.currentItem
        Log.d("FILTER_FLOW", "Текущая позиция: $position")
        Log.d("FilterDebug", "Текущее состояние ДО сохранения:\n$filtersInfo")
        Log.d("FILTER_FLOW", "currentFilter ПЕРЕД saveFilterChanges: $currentFilter")
        debugFullState("ДО onApplyClicked", position)


        // Также можем проверить конкретную позицию
        val currentPosition = activity.viewPager.currentItem
        val currentFilter = activity.adapter.getFilterForPosition(currentPosition)
        val currentBitmap = activity.adapter.getBitmapAtPosition(currentPosition)

        Log.d("FilterDebug", "Текущая позиция: $currentPosition")
        Log.d("FilterDebug", "Фильтр на текущей позиции: ${currentFilter ?: "NONE"}")
        Log.d("FilterDebug", "Bitmap присутствует: ${currentBitmap != null}")

        saveFilterChanges()

        debugFullState("ПОСЛЕ saveFilterChanges", position)

        val afterFiltersInfo = activity.adapter.getFiltersInfo()
        Log.d("FilterDebug", "Состояние ПОСЛЕ сохранения:\n$afterFiltersInfo")

        activity.transitionTo(NormalState(activity))
    }

    override fun updateUI() {
        activity.updateFilterUI()
    }

    override fun handleEvent(event: ViewPagerEvent) {
        when (event) {

            //если event выбор фото
            is ViewPagerEvent.FilterSelected -> {

                //сохраняем в перемeнную currentFilter выбранный фильтр
                currentFilter = event.filterType

                //применить фильтр к текущему фото
                applyFilterToCurrentPhoto(event.filterType)

                //обновить панель регулировки интенсивности в зависимости от выбранного фильтра
                updateIntensityPanelVisibility(event.filterType)
            }

            //если event изменение интенсивности
            is ViewPagerEvent.IntensityChanged -> {

                //сохраняем новое значение  интенсивности в stateData
                stateData.currentIntensity = event.intensity

                //применяем выбранную интенсивность к фото
                applyFilterWithIntensity(event.intensity)
            }
            else -> {}
        }
    }

    private fun setupFilterPanel() {
        val filters = PhotoFilters.getAllFilters()

        //проход по каждому элементу в списке filters
        filters.forEach { filterItem ->

            //создание нового view из XML шаблона
            val filterView = activity.layoutInflater.inflate(R.layout.item_filter, activity.filtersContainer, false)

            //ищем imageView для иконки
            val filterIcon: ImageView = filterView.findViewById(R.id.filterIcon)

            //ищем TextView имя для фильтра
            val filterName: TextView = filterView.findViewById(R.id.filterName)

            loadFilterPreview(filterIcon, filterItem.type)
            filterName.text = filterItem.name

            filterView.setOnClickListener {
                handleEvent(ViewPagerEvent.FilterSelected(filterItem.type))
            }

            //добавляем в конейнетр filterView настроенный для отображения
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

            //корутины работают в фоновом потоке операций ввода/вывода
            CoroutineScope(Dispatchers.IO).launch {
                try {

                    //настройки для загрузки картинки в 4 раза уменьшение
                    val options = BitmapFactory.Options().apply { inSampleSize = 4 }

                    //получаем изображение для показа на экране
                    val bitmap = BitmapFactory.decodeFile(activity.photoPaths[0], options)

                    //применение фильтра
                    val previewBitmap = PhotoFilters.applyFilter(
                        activity,
                        bitmap,
                        filterType,
                        0.7f
                    )

                    //переключение на главный поток
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

    //сохранение оригинала фото для текущей позиции
    private fun saveOriginalBitmapForCurrentPosition() {

        //берёт путь к текущему фото из списка всех путей
        val photoPath = activity.photoPaths[stateData.currentPosition]

        //загрука изображения из файла в оперативную память
        val bitmap = BitmapFactory.decodeFile(photoPath)
        bitmap?.let {

            //сохранение копии изображения в originalBitmaps
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
        Log.d("FILTER_FLOW", "=== applyFilterToCurrentPhoto($filterType) для позиции $position ===")

        val freshBitmap = getFreshOriginalBitmap(stateData.currentPosition)

        Log.d("FILTER_FLOW", "freshBitmap = ${freshBitmap != null}")

        freshBitmap?.let {
            stateData.originalBitmaps[stateData.currentPosition] = it.copy(Bitmap.Config.ARGB_8888, true)

            Log.d("FILTER_FLOW", "originalBitmaps обновлен")
        }
        activity.adapter.setFilterForPosition(stateData.currentPosition, filterType, stateData.currentIntensity)

        Log.d("FILTER_FLOW", "adapter.filtersMap[$position] установлен в $filterType")

        debugFullState("ПОСЛЕ applyFilterToCurrentPhoto", position)
    }

    private fun applyFilterWithIntensity(intensity: Float) {
        if (currentFilter != PhotoFilters.FilterType.NONE) {
            activity.adapter.setFilterForPosition(stateData.currentPosition, currentFilter, intensity)
        }
    }

    private fun saveFilterChanges(){
        saveFilterImageToFile()
        Toast.makeText(activity, "фильтр сохранён", Toast.LENGTH_SHORT).show()
    }

    private fun exitFilterModeWithoutSaving() {
        Log.d("FILTER","🚪 exitFilterModeWithoutSaving() вызван")
        restoreOriginalImageForCurrentPosition()
        Log.d("FILTER", "✅ restoreOriginalImageForCurrentPosition() выполнен")
        Toast.makeText(activity, "Фильтр отменен", Toast.LENGTH_SHORT).show()
    }

    private fun restoreOriginalImageForCurrentPosition() {
        val position=stateData.currentPosition
        Log.d("FILTER_FLOW", "=== restoreOriginalImageForCurrentPosition() для позиции $position ===")
        debugFullState("ДО restoreOriginalImage", position)
        val beforeFilter = activity.adapter.getFilterForPosition(position)
        Log.d("FILTER_FLOW", "Фильтр до очистки: $beforeFilter")

        activity.adapter.clearFilter(position)
        Log.d("FILTER_FLOW", "clearFilter вызван")

        val afterFilter = activity.adapter.getFilterForPosition(position)
        Log.d("FILTER_FLOW", "Фильтр после очистки: $afterFilter")

        stateData.originalBitmaps.remove(position)
        Log.d("FILTER_FLOW", "originalBitmaps удален для позиции $position")

        // ДИАГНОСТИКА ПОСЛЕ ОЧИСТКИ
        debugFullState("ПОСЛЕ restoreOriginalImage", position)

    }

    private fun saveFilterImageToFile() {
        //какое по счёту фото выбрано
        val position = stateData.currentPosition

        Log.d("FILTER_FLOW", "=== saveFilterImageToFile() для позиции $position ===")
        Log.d("FILTER_FLOW", "currentFilter = $currentFilter")

        //путь к файлу этого фота
        val photoPath = activity.photoPaths[position]

        //на карте интенсивности значение для текущей позиции
        val currentIntensity = activity.adapter.filterIntensityMap[position] ?: 1.0f

        val filterToApply=activity.adapter.getFilterForPosition(position)

        //если выбран не "пустой" фильтр
        if (filterToApply!= PhotoFilters.FilterType.NONE) {

            //запускаем фоновую обработку
            CoroutineScope(Dispatchers.IO).launch {
                try {

                    Log.d("FILTER_FLOW", "Запущена корутина сохранения")

                    //берем оригинал фото по текущей позииции
                    val originalBitmap = stateData.originalBitmaps[position]

                    Log.d("FILTER_FLOW", "originalBitmap = ${originalBitmap != null}")

                    //создание изображения с фильтром
                    originalBitmap?.let { bitmap ->

                        Log.d("FILTER_FLOW", "Применяем фильтр $currentFilter с интенсивностью ${stateData.currentIntensity}")

                        val filteredBitmap = PhotoFilters.applyFilter(
                            activity,
                            bitmap,
                            currentFilter,
                            currentIntensity
                        )

                        Log.d("FILTER_FLOW", "filteredBitmap создан: ${filteredBitmap.width}x${filteredBitmap.height}")
                        Log.d("FILTER_FLOW", "Сохраняем в файл: $photoPath")


                        //сохранение отфильтрованного изображения на диске сжимая его в JPEG с качеством 90%
                        FileOutputStream(photoPath).use { out ->
                            filteredBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                        }
                        Log.d("FILTER_FLOW", "Файл сохранен")

                        //сохранение копии filteredBitmap в originalBitmaps
                        stateData.originalBitmaps[position] =
                            filteredBitmap.copy(Bitmap.Config.ARGB_8888, true)

                        Log.d("FILTER_FLOW", "originalBitmaps обновлен копией")

                        withContext(Dispatchers.Main) {

                            Log.d("FILTER_FLOW", "Перед clearFilter: adapter.filtersMap[$position] = ${activity.adapter.getFilterForPosition(position)}")

                            Log.d("FILTER_FLOW", "После clearFilter: adapter.filtersMap[$position] = ${activity.adapter.getFilterForPosition(position)}")

                            Toast.makeText(activity, "Фильтр сохранён", Toast.LENGTH_SHORT).show()

                            activity.adapter.notifyItemChanged(position)
                            Log.d("FILTER_FLOW", "notifyItemChanged($position) вызван")

                            debugFullState("ПОСЛЕ saveFilterImageToFile", position)
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
    private fun debugFullState(tag: String, position: Int) {
        Log.d("SUPER_DEBUG", "========== $tag ==========")

        // 1. Что в адаптере (filtersMap)
        val filterInAdapter = activity.adapter.getFilterForPosition(position)
        Log.d("SUPER_DEBUG", "1. adapter.filtersMap[$position] = $filterInAdapter")

        // 2. Что в intensityMap адаптера
        val intensityInAdapter = activity.adapter.filterIntensityMap[position]
        Log.d("SUPER_DEBUG", "2. adapter.filterIntensityMap[$position] = $intensityInAdapter")

        // 3. Что в originalBitmaps (кэш в памяти)
        val originalBitmap = stateData.originalBitmaps[position]
        Log.d("SUPER_DEBUG", "3. stateData.originalBitmaps[$position] = ${if (originalBitmap != null) "✅ есть (${originalBitmap.width}x${originalBitmap.height})" else "❌ null"}")

        // 4. Какой файл лежит на диске
        val photoPath = activity.photoPaths.getOrNull(position)
        Log.d("SUPER_DEBUG", "4. photoPath[$position] = $photoPath")

        val file = File(photoPath ?: "")
        Log.d("SUPER_DEBUG", "   Файл существует: ${file.exists()}")
        Log.d("SUPER_DEBUG", "   Размер файла: ${file.length()} байт")
        Log.d("SUPER_DEBUG", "   Дата модификации: ${Date(file.lastModified())}")

        // 5. Загружаем файл и проверяем, есть ли в нем фильтр (невозможно напрямую, но можно загрузить и сравнить с оригиналом)
        try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photoPath, options)
            Log.d("SUPER_DEBUG", "5. Файл на диске: ${options.outWidth}x${options.outHeight}, формат: ${options.outMimeType}")
        } catch (e: Exception) {
            Log.d("SUPER_DEBUG", "5. Ошибка чтения файла: ${e.message}")
        }

        // 6. Текущий currentFilter в FilterState
        Log.d("SUPER_DEBUG", "6. FilterState.currentFilter = $currentFilter")

        // 7. stateData.currentIntensity
        Log.d("SUPER_DEBUG", "7. stateData.currentIntensity = ${stateData.currentIntensity}")

        // 8. stateData.currentPosition
        Log.d("SUPER_DEBUG", "8. stateData.currentPosition = ${stateData.currentPosition}")

        Log.d("SUPER_DEBUG", "========== КОНЕЦ ==========")
    }
}