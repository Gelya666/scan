package com.example.scanner.ui.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.scanner.R
import com.example.scanner.databinding.ActivityMainBinding
import com.example.scanner.service.CameraManager
import com.example.scanner.ui.activities.PagesEditor.PdfPagesEditorActivity
import com.example.scanner.ui.fragments.FileOptionsDialogFragment
import com.example.scanner.utils.getFormattedStackTrace
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), FileOptionsDialogFragment.FileOptionsListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager

    private val allPhotoPaths = mutableListOf<String>()

    val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            openPdfFile(uri)
        }
    }

    private var currentPhotoPath: String = ""
    private var photoUri: Uri? = null

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        Log.d("Angel","take picture result ${success}    ${Thread.currentThread().getFormattedStackTrace()}")
        if (success) {
            allPhotoPaths.add(currentPhotoPath)
            Log.d("Angel", "Фото сохранено: $photoUri")
            Toast.makeText(this, "Фото сделано", Toast.LENGTH_SHORT).show()
            goToPdfPagesEditorActivity()
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
        cameraManager = CameraManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Log.d("angelina", "запуск permsissons")

        binding.btnOpenCamera.setOnClickListener {
            createPdfFromPhoto()
        }

        binding.btnPdf.setOnClickListener {
            loadPdfFromFiles()
        }

        binding.points.setOnClickListener {
            val dialog = FileOptionsDialogFragment.Companion.newInstance("document.pdf")
            dialog.show(supportFragmentManager, "file_options_dialog")
        }

        binding.points.setOnClickListener {
            val dialog = FileOptionsDialogFragment.Companion.newInstance("document.pdf")
            dialog.show(supportFragmentManager, "file_options_dialog")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("DEBUG", "MainActivity onResume - фото: ${allPhotoPaths.size}")
        allPhotoPaths.clear()
        Log.d("DEBUG", "После очистки - фото: ${allPhotoPaths.size}")
    }

    // создание пдф из фото
    private fun createPdfFromPhoto() {
        if(!cameraManager.checkCameraPermission()){
            cameraManager.requestCameraPermission()
        }
        try {
            val photoFile = createImageFile()
            currentPhotoPath = photoFile.absolutePath
            photoUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )//владелец название тип значение
            Log.d("Angel","Текущий путь ${photoUri}")
            takePicture.launch(photoUri!!)
            Log.d("Angel","Записать ${photoUri}")
        } catch (e: IOException) {
            Log.e("CameraDebug", "Ошибка создания файла: ${e.message}")
            Toast.makeText(this, "Ошибка создания файла", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("CameraDebug", "Ошибка запуска камеры: ${e.message}")
            Toast.makeText(this, "Ошибка запуска камеры", Toast.LENGTH_SHORT).show()
        }
    }
    // загрузка пдф из URI
    private fun loadPdfFromFiles() {
        filePickerLauncher.launch(arrayOf("*/*"))
    }
    private fun openPdfFile(uri: Uri){
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            try {
                val bytes = stream.readBytes()
                val file = File(cacheDir, "selected_${System.currentTimeMillis()}.pdf")
                file.outputStream().use { it.write(bytes) }
                goToPdf_FileActivity(file.absolutePath)
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка обработки PDF: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                Log.e("PDF", "Error processing PDF", e)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun goToPdfPagesEditorActivity() {
        val intent = Intent(this, PdfPagesEditorActivity::class.java).apply {
            putStringArrayListExtra("photo_paths", ArrayList(allPhotoPaths))
            putExtra("current_position", allPhotoPaths.size - 1)
        }
        startActivity(intent)
        allPhotoPaths.clear()
    }
    private fun goToPdf_FileActivity(absolutePath: String) {
        Log.d("PDF", "Saved to: ${absolutePath}")
        val intent = Intent(this, PdfFileActivity::class.java).apply {
            putExtra("PDF_FILE_PATH", absolutePath)
        }
        startActivity(intent)
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

    override fun onRename(filename: String) {
        Toast.makeText(this, "Файл переименован:$filename", Toast.LENGTH_SHORT).show()
    }
}




