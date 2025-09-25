package com.example.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoViewPagerActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnBack: Button
    private lateinit var btnCrop: ImageButton
    private lateinit var btnSave: Button
    private lateinit var btnAddPage: ImageButton
    private lateinit var photoPaths: ArrayList<String>
    private var currentPosition: Int = 0
    private var isCropMode = false
    private lateinit var adapter: PhotoAdapter
    private var currentPhotoFile: File? = null

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
        btnAddPage.setOnClickListener {
            if (isCropMode) {
                exitCropMode()
            }
            checkCameraPermissionAndTakePhoto()
        }
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        btnBack = findViewById(R.id.btnUndo)
        btnCrop = findViewById(R.id.btn_crop)
        btnSave = findViewById(R.id.btnSave)
        btnAddPage = findViewById(R.id.btn_add_page)
    }

    private fun setupViewPager() {
        adapter = PhotoAdapter(photoPaths, false)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(currentPosition, false)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPosition = position
                if (isCropMode) {
                    exitCropMode()
                }
            }
        })
    }

    private fun setupButtons() {
        btnBack.setOnClickListener {
            if (isCropMode) {
                exitCropMode()
            } else {
                finish() // Возврат в CameraActivity
            }
        }

        btnCrop.setOnClickListener {
            if (isCropMode) {
                // Выполняем обрезку текущего фото
                performCrop()
            } else {
                // Входим в режим обрезки
                enterCropMode()
            }
        }
    }

    private fun enterCropMode() {
        isCropMode = true
        adapter.setCropMode(true)
        btnBack.text = "Undo"
        viewPager.isUserInputEnabled = false
    }

    private fun exitCropMode() {
        isCropMode = false
        adapter.setCropMode(false)
        btnSave.text = "Обрезать"
        btnBack.text = "Назад"
        viewPager.isUserInputEnabled = true
    }

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

                Toast.makeText(this, "Фото обрезано!", Toast.LENGTH_SHORT).show()
                exitCropMode()

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

    override fun onDestroy() {
        super.onDestroy()
        viewPager.adapter = null // Очищаем адаптер
    }
}
