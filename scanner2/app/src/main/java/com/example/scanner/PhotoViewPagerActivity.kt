package com.example.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.io.File
import java.io.FileOutputStream

class PhotoViewPagerActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnBack: Button
    private lateinit var btnCrop: ImageButton
    private lateinit var btnSave:Button

    private lateinit var photoPaths: ArrayList<String>
    private var currentPosition: Int = 0
    private var isCropMode = false
    private lateinit var adapter: PhotoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view_pager)

        // Получаем данные из CameraActivity
        photoPaths = intent.getStringArrayListExtra("photo_paths") ?: ArrayList()
        currentPosition = intent.getIntExtra("current_position", 0)

        initViews()
        setupViewPager()
        setupButtons()
    }

    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        btnBack = findViewById(R.id.btnUndo)
        btnCrop=findViewById(R.id.btn_crop)
    }

    private fun setupViewPager() {
        adapter=PhotoAdapter(photoPaths,false)
        viewPager.adapter=adapter
        viewPager.setCurrentItem(currentPosition,false)
        viewPager.registerOnPageChangeCallback(object:ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
              currentPosition = position
                if(isCropMode){
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
        //btnCrop.text = "Применить"
        btnBack.text = "Отмена"
        // Блокируем перелистывание в режиме обрезки
        viewPager.isUserInputEnabled = false
    }

    private fun exitCropMode() {
        isCropMode = false
        adapter.setCropMode(false)
        btnSave.text = "Обрезать"
        btnBack.text = "Назад"
        viewPager.isUserInputEnabled = true
    }

    private fun performCrop() {
        try {
            val currentPhotoPath = photoPaths[currentPosition]

            // Получаем текущий ViewHolder для доступа к cropOverlay
            val recyclerView = viewPager.getChildAt(0) as? RecyclerView
            val currentViewHolder =
                recyclerView?.findViewHolderForAdapterPosition(currentPosition) as? PhotoAdapter.PhotoViewHolder

            currentViewHolder?.let { holder ->
                // Получаем область обрезки из overlay
                val cropRect = holder.cropOverlay.getCropRect()

                // Загружаем оригинальное изображение
                val originalBitmap = BitmapFactory.decodeFile(currentPhotoPath)

                // Масштабируем координаты обрезки под размер изображения
                val scaleX = originalBitmap.width.toFloat() / holder.cropOverlay.width
                val scaleY = originalBitmap.height.toFloat() / holder.cropOverlay.height

                val scaledCropRect = Rect(
                    (cropRect.left * scaleX).toInt(),
                    (cropRect.top * scaleY).toInt(),
                    (cropRect.right * scaleX).toInt(),
                    (cropRect.bottom * scaleY).toInt()
                )

                // Проверяем, чтобы область обрезки была в пределах изображения
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

                // Сохраняем обрезанное изображение
                saveCroppedImage(croppedBitmap, currentPhotoPath)

                // Освобождаем память
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
