package com.example.scanner

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
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

    private lateinit var btnOpenCamera: ImageButton
    private lateinit var binding: ActivityMainBinding
    private var currentPhotoPath: String = ""

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_IMAGE_CAPTURE = 1002
        const val EXTRA_PHOTO_PATHS = "photo_paths"
    }

    val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Файл выбран, открываем
            readPdfFromUri(uri)
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

        findViewById<LinearLayout>(R.id.btn_pdf_item).setOnClickListener {
            filePickerLauncher.launch(arrayOf("*/*"))
        }

        findViewById<ImageButton>(R.id.points).setOnClickListener {
            val dialog = FileOptionsDialogFragment.newInstance("document.pdf")
            dialog.show(supportFragmentManager, "file_options_dialog")
        }

        findViewById<ImageButton>(R.id.points_sec).setOnClickListener {
            val dialog = FileOptionsDialogFragment.newInstance("document.pdf")
            dialog.show(supportFragmentManager, "file_options_dialog")
        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.e("angel", "requestCode == REQUEST_CAMERA_PERMISSION")
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            Log.d("angel", "grantResults.isEmpty()${grantResults.joinToString()}")
            if (grantResults.isEmpty()) {
                Log.e("angel", "grantResults пустой!")
                return
            }

            // Детальная диагностика
            permissions.forEachIndexed { index, permission ->
                val result = grantResults.getOrNull(index)
                val status = when (result) {
                    PackageManager.PERMISSION_GRANTED -> "✅ РАЗРЕШЕНО"
                    PackageManager.PERMISSION_DENIED -> "❌ ОТКЛОНЕНО"
                    else -> "❓ НЕИЗВЕСТНО"
                }
                Log.d("angel", "$permission: $status")
            }

            // Общая проверка
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            Log.d("angel", "Все разрешения получены: $allGranted")

            if (allGranted) {
                openCamera()
            } else {
                Toast.makeText(this, "Разрешение нет", Toast.LENGTH_SHORT).show()

            }
        }
    }

    private fun checkCameraPermission() {
        Log.d("angel", "проверить разрешение")
        val permissions = arrayOf(
          Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
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
        Log.d("angelina", "Запуск openCamera()")
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) == null) {
            Log.e("CameraDebug", "Не найдено приложение для обработки intent")
            Toast.makeText(this, "Камера недоступна на устройстве", Toast.LENGTH_SHORT).show()
            return
        }
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(this, "Ошибка создания файла", Toast.LENGTH_SHORT).show()
            null
        }
        if (photoFile == null) {
            Log.e("CameraDebug", "Файл не создан")
            return
        }

        try {
            val photoURI: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
            Log.d("CameraDebug", "Приложение для камеры найдено")
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            Log.d("CameraDebug", "startActivityForResult вызван")
        }catch (e: Exception) {
            Log.e("CameraDebug", "Ошибка FileProvider: ${e.message}")
            Toast.makeText(this, "Ошибка настройки камеры", Toast.LENGTH_SHORT).show()
        }
    }
        @Throws(IOException::class)
        private fun createImageFile(): File {
            val timeStamp: String =
                SimpleDateFormat("ууууMMdd-HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File? = getExternalFilesDir(null)
            return File.createTempFile("JPEG,_${timeStamp}_", ".jpeg", storageDir).apply {
                currentPhotoPath = absolutePath
            }
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
                        putExtra("PDF_FILE_PATH", uri)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Ошибка обработки PDF: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                    Log.e("PDF", "Error processing PDF", e)
                }
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                val intent = Intent(this, ScreenPfilterAcitivty::class.java)
                intent.putExtra(EXTRA_PHOTO_PATHS, arrayOf(currentPhotoPath))
                startActivity(intent)
            }
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




