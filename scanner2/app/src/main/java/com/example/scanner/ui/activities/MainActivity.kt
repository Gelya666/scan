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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.scanner.R
import com.example.scanner.databinding.ActivityMainBinding
import com.example.scanner.service.CameraManager
import com.example.scanner.ui.activities.PagesEditor.PdfPagesEditorActivity
import com.example.scanner.ui.adapters.FilesManager
import com.example.scanner.ui.adapters.RecyclerAdapter
import com.example.scanner.ui.fragments.FileOptionsDialogFragment
import com.example.scanner.ui.fragments.OnFileClickListener
import com.example.scanner.utils.getFormattedStackTrace
import com.example.scanner.viewmodel.PdfFile
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity(), FileOptionsDialogFragment.FileOptionsListener,
    OnFileClickListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager
    private val allPhotoPaths = mutableListOf<String>()
    private  lateinit  var adapter: RecyclerAdapter
    //все файлы
    private val allFiles = mutableListOf<PdfFile>()
    //неудаленные файлы
    private  val visibleFiles=mutableListOf<PdfFile>()
    //удалённые файлы
    private val deletedFilesId=mutableSetOf<String>()
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
        Log.d("Angel","take picture result ${success}   ${Thread.currentThread().getFormattedStackTrace()}")
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
        binding.recView.layoutManager = LinearLayoutManager(this)
        val filesManager = FilesManager()
        //удаление всех файлов
        allFiles.clear()
        //к пустому списку добавляются файлы из внешнего хранилища
        allFiles.addAll(filesManager.getPdfFiles(this))
        //создается адаптер для recyclerView и передаются все необходимые данные
        adapter = RecyclerAdapter(this,visibleFiles,this)
        //связваю адаптер с recyclerView для отображения данных
        binding.recView.adapter=adapter
        //добавление элемента в самое начало списка
        val newPosition = 0
        //уведомляю адаптер что добавлен объект с указанным номер чтобы он обновил отображение
        adapter.notifyItemInserted(newPosition)
        //прокрутка recycler к началу чтобы пользователь увидел добавленный элемент
        binding.recView.smoothScrollToPosition(newPosition)
        loadDeletesFilesId()
        filterFiles()
    }
    private fun filterFiles() {
        //очищение неудаленных файлов
        visibleFiles.clear()
        //добавляем к неудаленным файла все файлы и фильтруем
        visibleFiles.addAll(allFiles.filter { it.id !in deletedFilesId })
        Log.d("angel", " После фильтрации: ${visibleFiles.size} файлов из ${allFiles.size}")
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
        Toast.makeText(this,"Файл скрыт:$filename", Toast.LENGTH_SHORT).show()
    }
    override fun onDownload(filename: String) {
        Toast.makeText(this, "Файл сохранен:$filename", Toast.LENGTH_SHORT).show()
    }
    override fun onDelete(filename: String,position:Int) {

       //проверка если элемент от 0 до размера спика
      if( position in 0 until visibleFiles.size) {

          //берем это элемент и добавляет  в множество уадаленного спика
          val deletedFile = visibleFiles[position]
          deletedFilesId.add(deletedFile.id)
          saveDeletesFileId()

          //удаляем по позиции из отображаемых файлов
          visibleFiles.removeAt(position)
          //сообщается адаптеру об удаленном элементе
          adapter.notifyItemRemoved(position)
          //обновление элементов по позициям
          adapter.notifyItemRangeChanged(position, visibleFiles.size - position)
          Toast.makeText(this, "Файл удален:$filename", Toast.LENGTH_SHORT).show()
      }

    }

    override fun onReject(filename: String) {
        Toast.makeText(this,"Файл отклонен:$filename", Toast.LENGTH_SHORT).show()
    }

    override fun onRename(filename: String) {
        Toast.makeText(this,"Файл переименован:$filename", Toast.LENGTH_SHORT).show()
    }
    override fun onFileClick(position: Int, fileName: String, pdfFile: PdfFile) {
        val dialog = FileOptionsDialogFragment.Companion.newInstance(fileName,position)
         dialog.show(supportFragmentManager, "file_options_dialog")
    }

    override fun onPdfNameClick(pdfFile: PdfFile){
        val uri=pdfFile.PathPdfFile
        if(uri!=null){
            val intent=Intent(this,PdfFileActivity::class.java)
            intent.putExtra("pdf_uri",uri.toString())
            startActivity(intent)
        }else{
            Toast.makeText(this,"URI файл пустой",Toast.LENGTH_SHORT).show()
        }
    }
    private fun loadDeletesFilesId(){
        Log.d("TEST", "ДО загрузки: deletedFilesId = $deletedFilesId")
        val prefs=getSharedPreferences("app_prefers",MODE_PRIVATE)

        //берем строку по ключу "deleted_files"если нет то пустая строка
        val deletedIdsString=prefs.getString("deleted_files","")?:""
        Log.d("TEST", "В файле: $deletedIdsString")
        //очищает множество строк
        deletedFilesId.clear()
        Log.d("TEST", "После clear: deletedFilesId = $deletedFilesId")

        //если удаленные id не пустые то,разбиваем строку по запятыми,получаем список строк
        //проход через каждый элемент списка,убриаем пробелы
        if(deletedIdsString.isNotEmpty()){
            deletedIdsString.split(",").forEach{id->
                val trimmedId = id.trim()
                //если id не пустое,добавляю во множество
           if( trimmedId.isNotEmpty()){
               deletedFilesId.add( trimmedId)
               Log.d("TEST", "Добавили: $trimmedId")
           }
            }
            Log.d("TEST", "ПОСЛЕ загрузки: deletedFilesId = $deletedFilesId")
        }
    }
    private fun saveDeletesFileId(){

        //создание блокнота с именем "app_prefers" с доступом только у моего приложения
        val prefs=getSharedPreferences("app_prefers",MODE_PRIVATE)

        //из множества-строка с разделением-запятые
        val idsString = deletedFilesId.joinToString(",")

        //кладу строку idsString в prefs по ключу deleted_files
        prefs.edit().putString("deleted_files", idsString).apply()

        //берем значение по ключу deleted_files если нет-пустая строка
        val saved = prefs.getString("deleted_files", "")
        Log.d("angel", "Проверка: сохранилось '$saved'")
        }


    }






