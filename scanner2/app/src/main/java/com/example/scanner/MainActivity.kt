package com.example.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.scanner.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), FileOptionsDialogFragment.FileOptionsListener {

    private lateinit var binding: ActivityMainBinding
    private var currentPhotoPath: String = ""
    private lateinit var cameraButton: ImageButton


    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_IMAGE_CAPTURE = 101
        const val EXTRA_PHOTO_PATHS = "photo_paths"
    }
    private val allPhotoPaths = mutableListOf<String>()
    val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            readPdfFromUri(uri)
        }
    }
    private var photoUri: Uri? = null
    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                allPhotoPaths.add(currentPhotoPath)
                Log.d("CameraDebug", "Фото сохранено: $photoUri")
                Toast.makeText(this, "Фото сделано", Toast.LENGTH_SHORT).show()
                goToPhotoViewPagerActivity()
            } else {
                Log.e("CameraDebug", "Фото не сделано")
                Toast.makeText(this, "Фото не сделано", Toast.LENGTH_SHORT).show()
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Log.d("angelina", "запуск permsissons")
        binding.btnOpenCamera.setOnClickListener { checkCameraPermission() }
        binding.btnHOme.setOnClickListener {
            val intent = Intent(this, ScreenPfilterAcitivty::class.java)
            startActivity(intent)
        }

        binding.btnPdf.setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }

        binding.points.setOnClickListener {
            val dialog = FileOptionsDialogFragment.newInstance("document.pdf")
            dialog.show(supportFragmentManager, "file_options_dialog")
        }

        binding.points.setOnClickListener {
            val dialog = FileOptionsDialogFragment.newInstance("document.pdf")
            dialog.show(supportFragmentManager, "file_options_dialog")
        }

    }
    override fun onResume() {
        super.onResume()
        Log.d("DEBUG", "MainActivity onResume - фото: ${allPhotoPaths.size}")
        // Если нужно очищать при каждом показе - раскомментируйте:
         allPhotoPaths.clear()
         Log.d("DEBUG", "После очистки - фото: ${allPhotoPaths.size}")
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("angel", "=== onRequestPermissionsResult ===")
        Log.d("angel", "requestCode: $requestCode")
        Log.d("angel", "REQUEST_CAMERA_PERMISSION: $REQUEST_CAMERA_PERMISSION")
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            Log.d("angel", "grantResults.isEmpty()${grantResults.joinToString()}")
            if (requestCode != REQUEST_CAMERA_PERMISSION) {
                Log.d("angel", "Это не наш requestCode, игнорируем")
                return
            }
            Log.d("angel", "Обрабатываем запрос камеры")
            Log.d("angel", "grantResults: ${grantResults.joinToString()}")
            Log.d("angel", "grantResults.isEmpty(): ${grantResults.isEmpty()}")

            if (grantResults.isEmpty()) {
                Log.e("angel", "grantResults пустой! Возможно ошибка системы")
            }

            permissions.forEachIndexed { index, permission ->
                val result = grantResults.getOrNull(index)
                val status = when (result) {
                    PackageManager.PERMISSION_GRANTED -> "✅ РАЗРЕШЕНО"
                    PackageManager.PERMISSION_DENIED -> "❌ ОТКЛОНЕНО"
                    null -> "❓ NULL (нет результата)"
                    else -> "❓ НЕИЗВЕСТНО"
                }
                Log.d("angel", "$permission: $status")
            }


            val allGranted = if (grantResults.isEmpty()) {
                Log.d("angel", "grantResults пустой, считаем что не все разрешены")
                false
            } else {

                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            }
            Log.d("angel", "Все разрешения получены: $allGranted")

            if (allGranted) {
                Log.d("angel", "УСПЕХ: Открываем камеру")
                openCamera()
            } else {
                Toast.makeText(this, "Разрешение нет", Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun checkCameraPermission() {
        Log.d("angel", "проверить разрешение")
        val permissions = arrayOf(
            Manifest.permission.CAMERA
        )
        Log.d("angel", "фильтр разрешений")
        val missingPermissions = permissions.filter { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        }
        Log.d("angel", "разрешения которых нет")
        if (missingPermissions.isNotEmpty()) {
            Log.d("angel", "доступ")
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            Log.d("angel", "открыть камеру")
            openCamera()
        }
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            currentPhotoPath = photoFile.absolutePath
            photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            takePicture.launch(photoUri!!)
        } catch (e: IOException) {
            Log.e("CameraDebug", "Ошибка создания файла: ${e.message}")
            Toast.makeText(this, "Ошибка создания файла", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("CameraDebug", "Ошибка запуска камеры: ${e.message}")
            Toast.makeText(this, "Ошибка запуска камеры", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun goToPhotoViewPagerActivity() {
        val intent = Intent(this, PhotoViewPagerActivity::class.java).apply {
            putStringArrayListExtra("photo_paths", ArrayList(allPhotoPaths))
            putExtra("current_position", allPhotoPaths.size - 1)
        }
        startActivity(intent)
        // Очищаем после перехода если нужно
        // allPhotoPaths.clear()
        // Log.d("DEBUG", "После перехода - фото: ${allPhotoPaths.size}")
    }

    private fun readPdfFromUri(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            try {
                // You now have the raw PDF bytes
                val bytes = stream.readBytes()

                // Example: save to a local file if you want
                val file = File(cacheDir, "selected_${System.currentTimeMillis()}.pdf")
                file.outputStream().use { it.write(bytes) }

                Log.d("PDF", "Saved to: ${file.absolutePath}")
                val intent = Intent(this, PdfFileActivity::class.java).apply {
                    putExtra("PDF_FILE_PATH", file.absolutePath)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка обработки PDF: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                Log.e("PDF", "Error processing PDF", e)
            }
        }
    }
    override fun onSavePdf(imagePaths: ArrayList<String>) {
        saveImageToPdf(imagePaths)
    }
    private fun saveImageToPdf(imagePaths: ArrayList<String>) {
        // Реализация создания PDF
        // Используйте библиотеки вроде iText или Android PDF Document API
        Toast.makeText(this, "Сохранение ${imagePaths.size} изображений в PDF", Toast.LENGTH_SHORT).show()
    }



    override fun onHide(filename: String) {
        Toast.makeText(this, "Файл скрыт:$filename", Toast.LENGTH_SHORT).show()
    }

    override fun onDownload(filename: String) {
        Toast.makeText(this, "Файл сохранен:$filename", Toast.LENGTH_SHORT).show()
    }

    override fun onDelete(filename: String) {
        Toast.makeText(this, "Файл удален:$filename", Toast.LENGTH_SHORT).show()
    }

    override fun onReject(filename: String) {
        Toast.makeText(this, "Файл отклонен:$filename", Toast.LENGTH_SHORT).show()
    }

    private fun showRenameDialog(filename: String) {
        val builder = AlertDialog.Builder(this)
        val view = layoutInflater.inflate(R.layout.dialog_rename, null)
        val editText = view.findViewById<EditText>(R.id.rename_edittext)
        editText.setText(filename)
        builder.setView(view)
            .setTitle("Переименовать")
            .setPositiveButton("Сохранить") { dialog, _ ->
                val newName = editText.text.toString()
                Toast.makeText(this, "Переименован в:$filename", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onRename(filename: String) {
        showRenameDialog(filename)
    }
}




