package com.example.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



class PhotoViewPagerActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnBack: Button
    private lateinit var btnCrop: ImageButton
    private lateinit var btnSave: Button
    private lateinit var btnUnd: Button
    private lateinit var btnCancelCrop: Button
    private lateinit var btnAddPage: ImageButton
    private lateinit var photoPaths: ArrayList<String>
    private lateinit var btnFilters: ImageButton
    private lateinit var btnSaveCrop: Button
    private lateinit var btnRotate: ImageButton
    private lateinit var filtersContainer: LinearLayout
    private var originalBitmap: Bitmap? = null
    private lateinit var filterPanel: LinearLayout
    private lateinit var intensityPanel: LinearLayout
    private lateinit var intensitySeekBar: SeekBar
    private lateinit var intensityValue: TextView
    private var currentPosition: Int = 0
    private var isCropMode = false
    private var isFilterMode = false
    private var isRotateMode = false
    private lateinit var adapter: PhotoAdapter
    private var currentFilePath: String? = null
    private var currentPhotoFile: File? = null
    private var currentIntensity: Float = 0.5f
    private var isFilterApplied: Boolean = false
    private var originalBitmaps: HashMap<Int, Bitmap> = HashMap()
    private var imageRotate = ImageRotate()
    private val originalImagesBackup = mutableMapOf<Int, String>()

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            Log.e("angel", "добавление файла")
            if (success) {
                currentPhotoFile?.let { photoFile ->
                    if (photoFile.exists()) {
                        Log.e("angel", "добавление нового файла")
                        photoPaths.add(photoFile.absolutePath)
                        Log.e("angel", "добавление нового файла")
                        adapter.updateData(photoPaths)
                        Log.e("angel", "установление текущей картинки")
                        viewPager.setCurrentItem(photoPaths.size - 1, true)
                        Toast.makeText(this, "Фото  добавлено", Toast.LENGTH_SHORT).show()
                    }

                }
                currentPhotoFile = null

            }
        }
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                takePhoto()
            } else {
                Toast.makeText(this, "Необходимо разрешение для фото", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view_pager)

        // Получаем данные из CameraActivity
        photoPaths = intent.getStringArrayListExtra("photo_paths") ?: ArrayList()
        currentPosition = intent.getIntExtra("current_position", 0)
        initViews()
        setupViewPager()
        setupButtons()
        setupUndoButton()
        setupFilterPanel()
        updateUI()
        btnAddPage.setOnClickListener {
            if (isCropMode) {
                exitCropMode()
            }
            checkCameraPermissionAndTakePhoto()
        }
        updateSaveButton()

        /*btnRotate.setOnClickListener {
            rotateCurrentImage()
        }*/
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = "Page ${position + 1}"
        }.attach()
    }


    private fun rotateCurrentImage() {
        adapter.rotateImage(currentPosition, 90f)
    }

    private fun saveCurrentMode() {
        when {
            isFilterMode -> saveOnlyFilter()
        }
    }

    private fun saveOnlyFilter() {
        saveFilterChanges()
        exitFilterMode()
        Toast.makeText(this, "Сохранить", Toast.LENGTH_SHORT).show()
    }

    private fun updateSaveButton() {
        when {
            isCropMode -> {
                btnSave.text = "Save"
                btnSave.visibility = View.VISIBLE
                btnSave.setBackgroundColor(ContextCompat.getColor(this, R.color.color_crop))
            }

            isFilterMode -> {
                btnSave.text = "Save"
                btnSave.visibility = View.VISIBLE
                btnSave.setBackgroundColor(ContextCompat.getColor(this, R.color.color_filter))
            }

            isRotateMode -> {
                btnSave.text = "Save"
                btnSave.visibility = View.VISIBLE
                btnSave.setBackgroundColor(ContextCompat.getColor(this, R.color.color_primary))
            }
        }
    }

    private fun saveFilterChanges() {
        saveFilterImageToFile()
        hideFilterPanel()
        isFilterMode = false
        Toast.makeText(this, "фильтр сохранён", Toast.LENGTH_SHORT).show()
    }

    private fun saveFilterImageToFile() {
        if (currentPosition in 0 until photoPaths.size) {
            val photoPath = photoPaths[currentPosition]
            val currentFilter = adapter.getCurrentPosition(currentPosition)
            val currentIntensity = adapter.filterIntensityMap[currentPosition] ?: 1.0f
            if (currentFilter != PhotoFilters.FilterType.NONE)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        originalBitmap = originalBitmaps[currentPosition]
                        originalBitmap?.let { bitmap ->
                            val filteredBitmap = PhotoFilters.applyFilter(
                                this@PhotoViewPagerActivity,
                                bitmap,
                                currentFilter,
                                currentIntensity
                            )
                            FileOutputStream(photoPath).use { out ->
                                filteredBitmap.compress(
                                    Bitmap.CompressFormat.JPEG,
                                    90,
                                    out
                                )
                            }
                            originalBitmaps[currentPosition] =
                                filteredBitmap.copy(Bitmap.Config.ARGB_8888, true)
                            withContext(Dispatchers.Main) {
                                adapter.clearFilter(currentPosition)
                            }
                            filteredBitmap.recycle()
                        }
                    } catch (e: Exception) {
                        Log.e("PhotoViewPager", "Error saving filtered image: ${e.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PhotoViewPagerActivity,
                                "Ошибка сохранения",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
        }
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        btnBack = findViewById(R.id.btnUndo)
        btnCrop = findViewById(R.id.btn_crop)
        btnSave = findViewById(R.id.btnSave_Pager)
        btnAddPage = findViewById(R.id.btn_add_page)
        btnFilters = findViewById(R.id.btn_filter) // Инициализация кнопки фильтров
        filterPanel = findViewById(R.id.filterpanel)
        intensityPanel = findViewById(R.id.intensity_Panel)
        intensitySeekBar = findViewById(R.id.intensity_SeekBar)
        intensityValue = findViewById(R.id.intensity_Value)
        filtersContainer = findViewById(R.id.filters_Container)
        btnRotate = findViewById(R.id.btn_rotate)
        btnUnd = findViewById(R.id.btnUnd)
        btnCancelCrop = findViewById(R.id.btn_сancel_crop)
        btnSaveCrop = findViewById(R.id.btn_save_crop)
         tabLayout = findViewById(R.id.tabLayout)
    }

    private fun setupFilterPanel() {
        val filters = PhotoFilters.getAllFilters()
        filters.forEach { filterItem ->
            val filterView = layoutInflater.inflate(R.layout.item_filter, filtersContainer, false)
            val filterIcon: ImageView = filterView.findViewById(R.id.filterIcon)
            val filterName: TextView = filterView.findViewById(R.id.filterName)

            // Загружаем превью с фильтром
            loadFilterPreview(filterIcon, filterItem.type)
            filterName.text = filterItem.name

            filterView.setOnClickListener {
                applyFilterToCurrentPhoto(filterItem.type)
                updateIntensityPanelVisibility(filterItem.type)
            }
            filtersContainer.addView(filterView)
        }
        intensitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val intensity = progress / 100f
                    intensityValue.text = "${progress}%"
                    applyFilterWithIntensity(intensity)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun loadFilterPreview(imageView: ImageView, filterType: PhotoFilters.FilterType) {
        // Загрузка превью для фильтра (можно использовать первое фото или стандартную картинку)
        if (photoPaths.isNotEmpty()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 4 // Маленькое превью
                    }
                    val bitmap = BitmapFactory.decodeFile(photoPaths[0], options)
                    val previewBitmap = PhotoFilters.applyFilter(
                        this@PhotoViewPagerActivity,
                        bitmap,
                        filterType,
                        0.7f
                    )

                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(previewBitmap)
                    }
                    bitmap.recycle()
                } catch (e: Exception) {
                    Log.e("FilterPreview", "Error loading preview: ${e.message}")
                }
            }
        }
    }

    private fun applyFilterToCurrentPhoto(filterType: PhotoFilters.FilterType) {
        val currentIntensity = intensitySeekBar.progress / 100f
        // Сохраняем оригинал перед первым применением фильтра
        if (!originalBitmaps.containsKey(currentPosition)) {
            saveOriginalBitmapForCurrentPosition()
        }

        // Применяем фильтр через адаптер
        adapter.setFilterForPosition(currentPosition, filterType, currentIntensity)
    }

    private fun applyFilterWithIntensity(intensity: Float) {
        val currentFilter = adapter.getCurrentPosition(currentPosition)
        if (currentFilter != PhotoFilters.FilterType.NONE) {
            adapter.setFilterForPosition(currentPosition, currentFilter, intensity)
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

        intensityPanel.visibility = if (hasIntensity) View.VISIBLE else View.GONE
    }

    private fun setupViewPager() {
        adapter = PhotoAdapter(this, photoPaths, false)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(currentPosition, false)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPosition = position
                if (isCropMode) exitCropMode()
                if (isFilterMode) exitFilterModeWithoutSaving()

            }
        })
        viewPager.setCurrentItem(currentPosition, false)
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        when {
            isCropMode -> exitCropModeWithoutSaving()
            isFilterMode -> exitFilterModeWithoutSaving()
            else -> super.onBackPressed()
        }
    }

    private fun enterFilterMode() {
        if (isCropMode) {
            exitCropMode()
        }
        isFilterMode = true
        saveOriginalBitmapForCurrentPosition()
        showFilterPanel()
        updateSaveButton()
    }

    private fun enterRotateMode() {
        if (isCropMode) {
            exitCropMode()
        }
        if (isFilterMode) {
            exitFilterMode()
        }
        isRotateMode = true
        updateSaveButton()
        rotateCurrentImage()
    }

    private fun showFilterPanel() {
        filterPanel.visibility = View.VISIBLE
        Log.d("Filter", "Filter panel shown")
    }

    private fun hideFilterPanel() {
        filterPanel.visibility = View.GONE
        intensityPanel.visibility = View.GONE
    }

    private fun exitFilterModeWithoutSaving() {
        isFilterMode = false
        hideFilterPanel()
        restoreOriginalImageForCurrentPosition()
        Toast.makeText(this, "Фильтр отменен", Toast.LENGTH_SHORT).show()

    }

    private fun exitRotateModeWithoutSaving() {
        isRotateMode = false
        restoreRotateImage()
        Toast.makeText(this, "Поворот отменена", Toast.LENGTH_SHORT).show()
    }

    private fun exitFilterMode() {
        isFilterMode = false
        hideFilterPanel()
        updateSaveButton()
    }

    private fun restoreOriginalImageForCurrentPosition() {
        adapter.clearFilter(currentPosition)

        // Также очищаем сохраненный оригинал, чтобы не занимать память
        originalBitmaps.remove(currentPosition)
    }

    private fun restoreRotateImage() {
        adapter.clearAllRotations()
    }

    private fun saveOriginalBitmapForCurrentPosition() {
        val photoPath = photoPaths[currentPosition]
        val bitmap = BitmapFactory.decodeFile(photoPath)
        bitmap?.let {
            // Сохраняем оригинал в памяти
            originalBitmaps[currentPosition] = it.copy(Bitmap.Config.ARGB_8888, true)
        }
    }

    private fun setupButtons() {
        btnBack.setOnClickListener {
            when {
                isCropMode -> exitCropMode()
                isFilterMode -> exitFilterModeWithoutSaving()
                isRotateMode -> exitRotateModeWithoutSaving()
                else -> finish()
            }
        }
        btnFilters.setOnClickListener {
            enterFilterMode()
        }
        btnRotate.setOnClickListener {
            enterRotateMode()
        }

        btnCrop.setOnClickListener {
          enterCropMode()
        }
        btnSave.setOnClickListener {
            saveCurrentMode()
        }

        btnCancelCrop.setOnClickListener {
            exitCropModeWithoutSaving()
        }
        btnUnd.setOnClickListener {
            undoLastCrop()
        }
        btnSaveCrop.setOnClickListener {
            performCrop()
        }
    }

    /*
    private fun enterCropMode() {
        if (isFilterMode) {
            exitFilterModeWithoutSaving()
        }
        isCropMode=true
        adapter.setCropMode(true)
        //showCropOverlay()
        updateSaveButton()
        viewPager.isUserInputEnabled=false
    }
    private fun exitCropMode(){
        isCropMode = false
        adapter.setCropMode(false)
        btnBack.text = "Назад"
        viewPager.isUserInputEnabled = true
    }

     */
    private fun checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            takePhoto()
        }
        requestPermission.launch(Manifest.permission.CAMERA)
    }

    private fun takePhoto() {
        // Создаем временный файл для фото
        val photoFile = createImageFile()
        if (photoFile != null) {
            currentPhotoFile = photoFile
            val photoUri =
                FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
            takePicture.launch(photoUri)
        }
    }

    private fun createImageFile(): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(imageFileName, ".jpg", storageDir)

        } catch (ex: Exception) {
            Log.e("PhotoDebug", "Ошибка создания файла: ${ex.message}")
            null
        }
    }

    private fun getLatestPhotoPath(): String? {
        val pictureDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return pictureDir?.listFiles()?.maxByOrNull { it.lastModified() }?.absolutePath
    }

    private fun performCrop() {
        try {
            val currentPhotoPath = photoPaths[currentPosition]
            val recyclerView = viewPager.getChildAt(0) as? RecyclerView
            val currentViewHolder =
                recyclerView?.findViewHolderForAdapterPosition(currentPosition) as? PhotoAdapter.PhotoViewHolder

            currentViewHolder?.let { holder ->
                // Получаем область обрезки из улучшенного overlay
                val cropRect = holder.cropOverlay.getCropRect()

                if (cropRect.width() <= 0 || cropRect.height() <= 0) {
                    Toast.makeText(this, "Выберите область для обрезки", Toast.LENGTH_SHORT).show()
                    return
                }
                saveOriginalBackup(currentPosition, currentPhotoPath)
                // Загружаем оригинальное изображение
                val originalBitmap = BitmapFactory.decodeFile(currentPhotoPath)

                // Масштабируем координаты обрезки
                val scaleX = originalBitmap.width.toFloat() / holder.cropOverlay.width
                val scaleY = originalBitmap.height.toFloat() / holder.cropOverlay.height

                val scaledCropRect = Rect(
                    (cropRect.left * scaleX).toInt(),
                    (cropRect.top * scaleY).toInt(),
                    (cropRect.right * scaleX).toInt(),
                    (cropRect.bottom * scaleY).toInt()
                )
                // Проверяем границы
                val safeCropRect = Rect(
                    scaledCropRect.left.coerceIn(0, originalBitmap.width),
                    scaledCropRect.top.coerceIn(0, originalBitmap.height),
                    scaledCropRect.right.coerceIn(0, originalBitmap.width),
                    scaledCropRect.bottom.coerceIn(0, originalBitmap.height)
                )
                // Выполняем обрезку
                val croppedBitmap = Bitmap.createBitmap(
                    originalBitmap,
                    safeCropRect.left,
                    safeCropRect.top,
                    safeCropRect.width(),
                    safeCropRect.height()
                )
                // Сохраняем
                saveCroppedImage(croppedBitmap, currentPhotoPath)
                originalBitmap.recycle()
                showUndoButton()
                setupUndoButton()
                exitCropMode()

                Toast.makeText(this, "Фото обрезано!", Toast.LENGTH_SHORT).show()
            } ?: run {
                Toast.makeText(this, "Ошибка доступа к элементу", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e("CropDebug", "Ошибка обрезки: ${e.message}")
            Toast.makeText(this, "Ошибка обрезки: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCroppedImage(bitmap: Bitmap, filePath: String) {
        try {
            val file = File(filePath)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            bitmap.recycle()
        } catch (e: Exception) {
            Log.e("CropDebug", "Ошибка сохранения: ${e.message}")
            throw e
        }
    }

    private fun saveOriginalBackup(position: Int, originalPath: String) {
        try {
            if (position !in 0 until photoPaths.size) return
            val originalFile = File(originalPath)
            if (!originalFile.exists()) return

            // Создаем временный файл для бэкапа
            val backupDir = File(cacheDir, "crop_backup")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val backupFile = File(backupDir, "backup_${originalFile.name}")
            // Копируем оригинальный файл
            originalFile.copyTo(backupFile, overwrite = true)
            // Сохраняем путь к бэкапу
            originalImagesBackup[position] = backupFile.absolutePath
            Log.d("CropDebug", "Создан бэкап: ${backupFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("CropDebug", "Ошибка создания бэкапа: ${e.message}")
        }
    }

    private fun undoLastCrop() {
        try {
            val position = currentPosition
            val backupPath = originalImagesBackup[position] ?: return

            val backupFile = File(backupPath)
            val currentFile = File(photoPaths[position])

            if (!backupFile.exists()) {
                originalImagesBackup.remove(position)
                return
            }

            // Восстанавливаем оригинал
            backupFile.copyTo(currentFile, overwrite = true)
            backupFile.delete()
            originalImagesBackup.remove(position)

            adapter.notifyItemChanged(position)
            if (originalImagesBackup.isEmpty())
                hideUndoButton()

            Toast.makeText(this, "Обрезка отменена", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e("CropDebug", "Ошибка отмены обрезки: ${e.message}")
        }
    }

    private fun clearBackups() {
        try {
            val backupDir = File(cacheDir, "crop_backup")
            if (backupDir.exists()) {
                backupDir.deleteRecursively()
            }
            originalImagesBackup.clear()
        } catch (e: Exception) {
            Log.e("CropDebug", "Ошибка очистки бэкапов: ${e.message}")
        }
    }

    private fun setupUndoButton() {
        btnUnd.setOnClickListener {
            undoLastCrop() // ✅ Вызов метода отмены
        }
        btnUnd.visibility = View.GONE // Изначально скрыта
    }

    private fun showUndoButton() {
        if (!isCropMode && originalImagesBackup.isNotEmpty()) {
            btnUnd.visibility = View.VISIBLE
            Log.d("CropDebug", "Показана кнопка Undo, backup count: ${originalImagesBackup.size}")
        } else {
            Log.d("CropDebug", "Undo не показана: isCropMode=$isCropMode, backupCount=${originalImagesBackup.size}")
        }
    }

    private fun enterCropMode() {
      when{
          isFilterMode->exitFilterMode()
          isRotateMode->exitRotateModeWithoutSaving()
      }
        val position = viewPager.currentItem
        currentPosition = position
        isCropMode = true
        adapter.setCropMode(true)
        viewPager.isUserInputEnabled = false
        if (originalImagesBackup.containsKey(position)) {
            deleteBackupForPosition(position)
        }
        hideUndoButton()
        updateUI()
    }
    private fun deleteBackupForPosition(position: Int) {
        try {
            val backupPath = originalImagesBackup[position]
            if (backupPath != null) {
                val backupFile = File(backupPath)
                if (backupFile.exists()) {
                    backupFile.delete()
                }
                originalImagesBackup.remove(position)
                Log.d("CropDebug", "Бэкап удален для позиции $position")
            }
        } catch (e: Exception) {
            Log.e("CropDebug", "Ошибка удаления бэкапа: ${e.message}")
        }
    }

    private fun exitCropMode() {
        isCropMode = false
        adapter.setCropMode(false)
        viewPager.isUserInputEnabled = true
        updateUI()
    }

    private fun exitCropModeWithoutSaving() {
        isCropMode = false
        adapter.setCropMode(false)
        viewPager.isUserInputEnabled = true
        updateUI()
        Toast.makeText(this, "Обрезка отменена", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI() {
        if (isCropMode) {
            // РЕЖИМ ОБРЕЗКИ - показываем кнопки Сохранить/Отмена
            btnSaveCrop.visibility = View.VISIBLE
            btnCancelCrop.visibility = View.VISIBLE
            btnUnd.visibility = View.GONE
        } else {
            // ОБЫЧНЫЙ РЕЖИМ - показываем кнопку Обрезать
            btnSaveCrop.visibility = View.GONE
            btnCancelCrop.visibility = View.GONE

            // Показываем кнопку Отмена только если есть что отменять
            if (originalImagesBackup.isNotEmpty()) {
            btnUnd.visibility = View.VISIBLE
        }
    }
}

    private fun hideUndoButton() {
        btnUnd.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                btnUnd.visibility = View.GONE
            }
            .start()
    }
    override fun onDestroy() {
        super.onDestroy()
        viewPager.adapter = null
       // Очищаем адаптер
        clearBackups()
    }
}


