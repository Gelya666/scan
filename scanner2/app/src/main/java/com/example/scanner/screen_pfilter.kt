package com.example.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class screen_pfilter : AppCompatActivity() {
    private lateinit var btnRotate: ImageButton
    private var cropOverlayView: CropOverlayView? = null
    private lateinit var btnCrop: ImageButton
    private lateinit var btnFilter: ImageButton
    private lateinit var btnAddpage: ImageButton
    private lateinit var imageView: ImageView
    private lateinit var btnCropConfirm: Button
    private lateinit var btnCropCancel: Button
    private var currentBitmap: Bitmap? = null
    private var rotationAngel = 0f
    private var isCropMode = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.edit_image)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.image)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        btnRotate = findViewById(R.id.btn_rot)
        btnCrop = findViewById(R.id.btn_cr)
        btnFilter = findViewById(R.id.btn_fil)
        btnAddpage = findViewById(R.id.btn_add)
        imageView = findViewById(R.id.imageViewEdit)
        cropOverlayView = findViewById(R.id.cropOverlayView)
        btnCropConfirm = findViewById(R.id.btnCropConfirm)
        btnCropCancel = findViewById(R.id.btnCropCancel)

        //btnSave=findViewById(R.id.btnSave)
        val byteArray = intent.getByteArrayExtra("image_bites")
        if (byteArray != null) {
            currentBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            imageView.setImageBitmap(currentBitmap)
        } else {
            Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show()
            finish()
        }
        btnCropConfirm.visibility = View.GONE
        btnCropCancel.visibility = View.GONE
        cropOverlayView?.visibility = View.GONE


        fun startCropMode() {
            isCropMode = true
            cropOverlayView?.visibility = View.VISIBLE
            btnCropConfirm.visibility = View.VISIBLE
            btnCropCancel.visibility = View.VISIBLE
            btnCrop.visibility = View.GONE
            btnRotate.visibility = View.GONE
            btnFilter.visibility = View.GONE
        }

        fun rotateImage() {
            rotationAngel = (rotationAngel * 90) % 360
            imageView.rotation = rotationAngel
            Toast.makeText(this, "rotated 90 degrees", Toast.LENGTH_SHORT).show()
        }

        fun applyFilter() {
            val matrix = ColorMatrix()
            matrix.setSaturation(0f)
            val filter = ColorMatrixColorFilter(matrix)
            imageView.colorFilter = filter
        }

        fun exitCropMode() {
            isCropMode = false
            cropOverlayView?.visibility = View.GONE
            btnCropConfirm.visibility = View.GONE
            btnCropCancel.visibility = View.GONE
            btnCrop.visibility = View.VISIBLE
            btnRotate.visibility = View.VISIBLE
            btnFilter.visibility = View.VISIBLE
        }


        fun confirmCrop() {
            currentBitmap?.let { bitmap ->
                val cropArea = cropOverlayView?.getCroppedArea(bitmap, imageView) ?: return@let
                if (cropArea.width() > 0 && cropArea.height() > 0) {
                    val croppedBitmap = Bitmap.createBitmap(
                        bitmap, cropArea.left.coerceIn(0, bitmap.width - 1),
                        cropArea.top.coerceIn(0, bitmap.height - 1),
                        cropArea.width().coerceIn(1, bitmap.width - cropArea.left),
                        cropArea.height().coerceIn(1, bitmap.height - cropArea.top)
                    )
                    currentBitmap = croppedBitmap
                    imageView.setImageBitmap(croppedBitmap)
                    Toast.makeText(this, "Image is cropped", Toast.LENGTH_SHORT).show()
                }
            }
            exitCropMode()
        }

        fun cancelCrop() {
            exitCropMode()
        }

        fun saveImage() {}
        btnRotate.setOnClickListener{ rotateImage() }
        btnCrop.setOnClickListener{ startCropMode() }
        btnFilter.setOnClickListener{ applyFilter() }
        // btnAddpage.setOnClickListener{}
        btnCropConfirm.setOnClickListener{ confirmCrop() }
        btnCropCancel.setOnClickListener{ cancelCrop() }


        }
            override fun onDestroy() {
                super.onDestroy()
                currentBitmap?.recycle()
    }
}