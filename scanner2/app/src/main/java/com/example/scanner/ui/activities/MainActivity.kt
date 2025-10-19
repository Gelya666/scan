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
    //объявление неизменяемой переменной в классе MainActivty с названием takePicture
    //присваивается возвращаемое заечение функции registerForActivityResult ActivityResultLauncher<I>
    //параметры с название contracts типа  ActivityResultContract<I, O>,со значением ??ActivityResultContracts.TakePicture()
    //лямбда выражение с параметрам success типа Bollean тело лямбда выражение

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        //условнфй оператор if c параметрам succsecc типа bollean
        Log.d("Angel","take picture result ${success}    ${Thread.currentThread().getFormattedStackTrace()}")
        if (success) {
            //вызов метода add c параметрами currentPhotoPath типа String который принадлежит перменной
            // allPhotoPaths. типа  MutableList<String> поле класса MainActivity
            allPhotoPaths.add(currentPhotoPath)
            Log.d("Angel", "Фото сохранено: $photoUri")
            Toast.makeText(this, "Фото сделано", Toast.LENGTH_SHORT).show()
            goToPdfPagesEditorActivity()
        } else {
            Log.e("CameraDebug", "Фото не сделано")
            Toast.makeText(this, "Фото не сделано", Toast.LENGTH_SHORT).show()
        }
    }
    //переопредление метода с наванием onCreate в классе MainActivity
    //с параметрами название savedInstanceState типом Bundle
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
    //переопределение метода с названием onResume в классе MainActivity
    override fun onResume() {
        //вызов метода onResume в методе с навзанием onResume который принадлежит классу super
        super.onResume()
        //вызов метода d в медоте с названием onResume который принадлежит классу Log
        Log.d("DEBUG", "MainActivity onResume - фото: ${allPhotoPaths.size}")
        // Если нужно очищать при каждом показе - раскомментируйте:
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
        //функция loadPdfFromFiles  вызывает метод launch(запускает новую коротину c вводом данных) который относиться
        //к переменной класс MainActivity c названием filePickerLauncher тип ActivityResultLauncher
        //метод имеет ??параметры с названием input тип I со значением массива arrayOf
        filePickerLauncher.launch(arrayOf("*/*"))
    }

   //объявление метод с названием openPdfFile в классе MainActivity
   // c параметрами с названием uri типа Uri возвращает ничего
    private fun openPdfFile(uri: Uri){
        //объявление неизменяемой переменной с названием inputStream в методе с названием openPdfFile тип  InputStream?
        //присваивается значение ??Uri метода openInputStream(uri) который относится к переменной с названием
        //contentResolver ??тип
        val inputStream = contentResolver.openInputStream(uri)
       //вызвов функции расширения use в методе  с названием openPdfFile
       //относится к переменной inputStream метода  openPdfFile c типом  InputStream?
       //функция расширения с именем use принимает лямбда-выражение с одним параметром — объектом, содержащим ресурс
        inputStream?.use { stream ->
            try {
                //объявление неизменяемой переменной  функции openPdfFile с именем bytes типа  ByteArray
                //присиваивается возвращаемое значение  ByteArray метода readBytes
                // который относится к переменной stream типа InputStream
                val bytes = stream.readBytes()
                //в методе с названием openPdfFile рбъявляется неизменяема переменная с названием file  типа File
                //присваивается возвращемое значение File функции File имеющей параметры с название parent типа file
                //со значением cacheDir,название child типа String со значением
                val file = File(cacheDir, "selected_${System.currentTimeMillis()}.pdf")
                //переменная file типа File вызывает метод outputStream() возвращает  FileOutputStream и
                // функция расширения с именем use принимает лямбда-выражение с одним параметром — объектом, содержащим ресурс
                //неявная переменная it вызывает метод write c параметрами с названием b типа byte со значением bytes
                file.outputStream().use { it.write(bytes) }
               //объявление функции с именем  goToPdf_FileActivity в методе с названием openPdfFile
                //с параметрами нзаванием absolutePath c типом String со значением file.absolutePath
                goToPdf_FileActivity(file.absolutePath)
                //функция catch с парметрами e типа Exception
            } catch (e: Exception) {
                //объявление функции makeText которая относится к классу Toast с парметрами context типа Context,text типа CharSequence
                //название типа int cо значением свойства LENGTH_SHORT класс Toast
                //вызов метода show()
                Toast.makeText(this, "Ошибка обработки PDF: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
                //вызов функции e класс Log c параметрами tag типа String со значением"",параметры msg тип String
                //tr типа trowable со значением e
                Log.e("PDF", "Error processing PDF", e)
            }
        }
    }
//вызов конструктора  Throws с параметрами  exceptionClasses: KClass<out Throwable> со значением IOException::class
    @Throws(IOException::class)
    //вызов метода с названием createImageFile в классе MainActivity который возваращает File
    private fun createImageFile(): File {
        //объявление неизменяемой переменной название  timeStamp в методе с названием createImageFile
        //типа String присваивается значение конструктора  SimpleDateFormat с праметрами название  pattern тип String
        //Locale тип locale которая вызвает метод getDefault()  SimpleDateFormat вызыват метод format с праметрами
        //date типа Date со значением класса Date()
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        //обявление неизменяемой переменной storageDir типа File? в методе с названием createImageFile():
        //присваивается возвращаемое  значение getExternalFilesDir  метода??absolute path параметры с названием type типа String со значением DIRECTORY_PICTURES
       //относящийся к классу Environment
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
         //вызов метода в методе createImageFile с названием createTempFile относящийся к классу File с параметрами
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }
    //вызов метода с названием goToPdfPagesEditorActivity()
    private fun goToPdfPagesEditorActivity() {
        //объявление переменной intent типа Intent в методе с названием goToPdfPagesEditorActivity()
        //присваивается значение объекта Intent c параметрами название packageContext типа Сontext значение this
        //название cls тип Class значение PdfPagesEditorActivity::class.java
        //вызов функции расширения apply которая принадлежит классу Intent
        val intent = Intent(this, PdfPagesEditorActivity::class.java).apply {
            //несколько операций над одним объектом, не повторяя его имени
            //вызов функци с названием  putStringArrayListExtra возвращает объект Intent
            //параметры название name типа String со значением "photo_paths",название value тип .ArrayList<String> со значением ArrayList
            putStringArrayListExtra("photo_paths", ArrayList(allPhotoPaths))
            //вызов метода с названием  putExtra которые возвращает объект Intent
            //парметры название name типа String значение "current_position,название value c типом int со значением allPhotoPaths.size - 1
            putExtra("current_position", allPhotoPaths.size - 1)
        }
        //вызов метода с названием  startActivity в методе с названием goToPdfPagesEditorActivity()
        //с параметрами intent типа Intent
        startActivity(intent)
        //вызов функции clear() в методе goToPdfPagesEditorActivity() которая принадлежит переменной  allPhotoPaths типа  MutableList<String> подя класса MainActivity
        // ? в переменной  allPhotoPaths есть метод clear
        allPhotoPaths.clear()
    }
   //вызов метода с названием goToPdf_FileActivity в классе MainActivity
    //параметры название absolutePath тип String
    private fun goToPdf_FileActivity(absolutePath: String) {
        //вызов метода с названием e в методе с названием goToPdf_FileActivity который принадлежит классу LOg
        //параметры название tag тип String значение "",название msg типа String со заначением "Saved to: ${absolutePath}"
        Log.d("PDF", "Saved to: ${absolutePath}")
       //объявление переменной intent типа Intent в методе с названием goToPdf_FileActivity
       //присваивается значение объекта Intent c параметрами название packageContext типа Сontext значение this
       //название cls тип Class значение PdfPagesEditorActivity::class.java
       //вызов функции расширения apply которая принадлежит классу Intent
        val intent = Intent(this, PdfFileActivity::class.java).apply {
            //несколько операций над одним объектом, не повторяя его имени
            //вызов функци с названием  putExtra возвращает объект Intent
            //параметры название name типа String значение  "PDF_FILE_PATH",value типа String значение absolutePath
            putExtra("PDF_FILE_PATH", absolutePath)
        }
       //вызов метода с названием  startActivity в методе с названием  goToPdf_FileActivity
       //с параметрами intent типа Intent
        startActivity(intent)
    }

    // some interface methods for fragment?
    //переопредление метода c названием onHide в классе MainActivity с парметрами названием fileName типа Ыекштп
    override fun onHide(filename: String) {
        //вызов метода с названием makeText в методе onHide которая принадлежит классу Toast c парметрами
        //с парметрами context типа Context,text типа CharSequence,duration типа int со значение LENGTH_SHORT
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




