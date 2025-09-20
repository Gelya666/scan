package com.example.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import me.relex.circleindicator.CircleIndicator3
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScreenPfilterAcitivty : AppCompatActivity() {
    private lateinit var btnRotate: ImageButton
    private var cropOverlayView: CropOverlayView? = null
    private lateinit var btnCrop: ImageButton
    private lateinit var btnFilter: ImageButton
    private lateinit var btnAddpage: ImageButton
    private lateinit var imageView: ImageView
    private lateinit var btnCropConfirm: Button
    private lateinit var btnCropCancel: Button
    private lateinit var cropControls: LinearLayout
    private var currentBitmap: Bitmap? = null
    private var rotationAngel = 0f
    private var isCropMode = false
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var viewFinder: PreviewView
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
    private lateinit var imageCapture: ImageCapture

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
        cropControls = findViewById(R.id.cropControls)

        //btnSave=findViewById(R.id.btnSave)

        cropOverlayView?.visibility = View.GONE

        btnRotate.setOnClickListener { rotateImage() }
        btnCrop.setOnClickListener { startCropMode() }
        btnFilter.setOnClickListener { applyFilter() }
        btnAddpage.setOnClickListener { }
        btnCropConfirm.setOnClickListener { confirmCrop() }
        btnCropCancel.setOnClickListener { cancelCrop() }
        if (!hasPermissions()) {
            // Запрашиваем разрешения, если их нет
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CODE_PERMISSIONS)
        } else {
            // Если разрешения есть, запускаем камеру
            startCamera()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        btnAddpage.setOnClickListener { takePhoto() }
    }
    private fun hasPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()


            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))

    }
    private fun takePhoto() {
        // Получаем imageCapture из метода startCamera() (нужно будет его вынести в поле класса)
        // val imageCapture = this.imageCapture ?: return

        // Create time-stamped output file and metadata
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            "${System.currentTimeMillis()}.jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
       imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraX", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d("CameraX", msg)
                }
            }
        )
    }



    // Обработчик ответа на запрос разрешений
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                // Разрешение не дано, можно показать сообщение пользователю
                Toast.makeText(this, "Разрешения не получены", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun saveImage() {}

    fun startCropMode() {
        isCropMode = true
        cropOverlayView?.visibility = View.VISIBLE
        cropControls.visibility = View.VISIBLE
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
        cropControls.visibility = View.GONE
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


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

    }
}