package com.example.scanner.ui.activities
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.scanner.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs


class PdfFileActivity : AppCompatActivity() {
    private lateinit var pdfImageView: ImageView
    private var progressBar: ProgressBar? = null
    private lateinit var tvPageInfo: TextView
    private lateinit var tvFileName: TextView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var btnBack: Button
    private lateinit var btnAdd:Button
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var currentPageIndex = 0
    private var pageCount = 0
    private var pdfFile: File? = null
    private var currentFileName:String?=null
    private var  filePath:String?=null
    private lateinit var fileDisplayHelper: FileDisplayHelper
    @SuppressLint("ClickableViewAccessibility", "MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pdf_file)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pdf)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        pdfImageView = findViewById(R.id.pdfImageView)
        progressBar = findViewById(R.id.progressBar)
        tvPageInfo = findViewById(R.id.tvPageInfo)
        tvFileName = findViewById(R.id.tvFileName)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        btnBack = findViewById(R.id.btnBack)
        btnAdd=findViewById(R.id.btnAdd)
        fileDisplayHelper= FileDisplayHelper(contentResolver)
        filePath = intent.extras?.getString("PDF_FILE_PATH")
         currentFileName=intent.getStringExtra("PDF_FILE_NAME")

        Log.d("NAME","Имя файл $currentFileName")
        Log.d("NAME","Путь файла $filePath")

        if(currentFileName==null&&filePath!=null){
            val uglyName=File(filePath!!).name
            currentFileName=formatTimeStamp(uglyName)
        }
        val uriString=intent.getStringExtra("pdf_uri")
        Log.d("danil_logs", "onCreate: $uriString")
        when {
            // Вариант 1: есть путь к файлу
            !filePath.isNullOrEmpty() -> {
                pdfFile = File(filePath)
                loadPdfDocument()
            }
            // Вариант 2: есть URI
            !uriString.isNullOrEmpty() -> {
                val uri = Uri.parse(uriString)
                loadPdfFromUri(uri)
            }
            // Вариант 3: ничего нет
            else -> {
                Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        //слушатель касаний для imageView реагирование на касание
        pdfImageView.setOnTouchListener(object : View.OnTouchListener {

            //хранит координату x первого касания пальца
            private var startX = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    //при первом касании запоминаем координату
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        return true
                    }
                     //убрали касание
                    MotionEvent.ACTION_UP -> {
                        //координата в момент когда прекратили касаться
                        val endX = event.x
                        val deltaX = endX - startX
                        //проверка достаточно ли большой сдвиг на 1 см и более(исключить дрожание)
                        //abc-убирает знак минус
                        if (abs(deltaX) > 100) {
                            //если разность положительная свайп право возвращаемсяна пред страницу
                            if (deltaX > 0) {
                                showPage(currentPageIndex - 1)
                            } else {
                                showPage(currentPageIndex + 1)
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })
        btnPrev.setOnClickListener { showPage(currentPageIndex - 1) }
        btnNext.setOnClickListener { showPage(currentPageIndex + 1) }
        btnBack.setOnClickListener { finish() }
        btnAdd.setOnClickListener{loadFileToMainActivity()}
    }
    private fun loadFileToMainActivity(){
        if(filePath==null && currentFileName ==null){
            Toast.makeText(this,"Нет файла добавления",Toast.LENGTH_SHORT).show()
        return
        }
        val pdfDir=File(getExternalFilesDir(null),"MyPDFs")
        if(!pdfDir.exists()){
            pdfDir.mkdirs()
            Log.d("PDF_DIR","Создана папка ${pdfDir.absolutePath}")
        }
        val sourceFile=File(filePath)
       val destFile=File(pdfDir,"pdf_${System.currentTimeMillis()}.pdf")
      try{
          sourceFile.copyTo(destFile,overwrite=true)
          if(destFile.exists()){
           Log.d("PDF_DIR","Размер файла ${destFile.length()} байт")
          }else{
              return
          }
      }catch(e:Exception){
          Log.e("PDF_DIR","Ошибка копирования ${e.message}")
      }
        val uri = FileProvider.getUriForFile(
            this,
            "com.example.scanner.fileprovider",  // ваш authority из манифеста
            destFile
        )

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("PDF_URI",uri.toString())
        intent.putExtra("PDF_FILE_PATH", destFile.absolutePath)
        intent.putExtra("PDF_FILE_NAME",currentFileName)

        Log.d("PDF_ACTIVITY","PDF_FILE_PATH:${intent.getStringExtra("PDF_FILE_PATH")}")
        Log.d("PDF_ACTIVITY","PDF_FILE_NAME:${intent.getStringExtra("PDF_FILE_NAME")}")
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }
    private fun loadPdfDocument() {
        progressBar?.visibility = View.VISIBLE
        try {
            if (pdfFile?.exists() == true) {
                val parcelFileDescriptor = ParcelFileDescriptor.open(
                    pdfFile!!, ParcelFileDescriptor.MODE_READ_ONLY
                )
                pdfRenderer = PdfRenderer(parcelFileDescriptor)
                pageCount = pdfRenderer!!.pageCount
                //tvFileName.text = pdfFile!!.name
                tvFileName.text = formatTimeStamp(pdfFile!!.name)
                tvPageInfo.text = "1/$pageCount"
                showPage(0)
            } else {
                Toast.makeText(this, "PDF файл не существует", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка загрузки PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("PDF_VIEWER", "Error loading PDF", e)
            finish()
        } finally {
            progressBar?.visibility = View.GONE
        }
    }
    @SuppressLint("SetTextI18n")
    private fun updateUI() {
        //показать текущую старницу/кол-во страниц
        tvPageInfo.text = "${currentPageIndex + 1}/$pageCount"
        //кнопка назад доступна если текущая страница больше 0
        btnPrev.isEnabled = currentPageIndex > 0
        //кнопка вперед доступна если не на последней странице
        btnNext.isEnabled = currentPageIndex < pageCount - 1
        val colorEnabled = ContextCompat.getColor(this, R.color.white)
        val colorDisabled = ContextCompat.getColor(this, android.R.color.darker_gray)
        btnPrev.setTextColor(if (btnPrev.isEnabled) colorEnabled else colorDisabled)
        btnNext.setTextColor(if (btnNext.isEnabled) colorEnabled else colorDisabled)
    }

    private fun showPage(pageIndex: Int) {
        //проверка если индек страницы<0 и больше всего страниц текущая страница закрывется
        if (pageIndex < 0 || pageIndex >= pageCount) return
        currentPage?.close()
        try {
            //открыть текущую страницу
            currentPage = pdfRenderer!!.openPage(pageIndex)
            //запомнили номер страницы
            currentPageIndex = pageIndex
            //создание чистого листа с шириной и высотой текущей страницы
            val bitmap = createBitmap(currentPage!!.width, currentPage!!.height)
            currentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
           //установить изображение в ImageView
            pdfImageView.setImageBitmap(bitmap)
            updateUI()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка отображения страницы", Toast.LENGTH_SHORT).show()
            Log.e("PDF_VIEWER", "Error showing page $pageIndex", e)
        }
    }
    private fun loadPdfFromUri(uri: Uri) {
        progressBar?.visibility = View.VISIBLE
        try {
            // открытие файла и полкчение дескриптора
            val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")

            if (parcelFileDescriptor != null) {
                //передача дескриптора pdfRenderer чтобы он мог работать с файлами
                pdfRenderer = PdfRenderer(parcelFileDescriptor)
                //количество страниц в файле
                pageCount = pdfRenderer!!.pageCount

                // Получаем имя файла из URI
                //val fileName = uri.lastPathSegment ?: "Документ.pdf"
                //val newName=formatTimeStamp(fileName)
                tvFileName.text = fileDisplayHelper.getFileDisplayName(uri)
                //количество страниц-всего/текущая
                tvPageInfo.text = "1/$pageCount"
                showPage(0)
            } else {
                Toast.makeText(this, "Не удалось открыть PDF", Toast.LENGTH_SHORT).show()
                finish()
            }
        } catch (e:Exception) {
            Toast.makeText(this, "Ошибка загрузки PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("PDF_VIEWER", "Error loading PDF from URI", e)
            finish()
        } finally {
            progressBar?.visibility = View.GONE
        }
    }
    private fun formatTimeStamp(uglyName:String):String{
        val number=uglyName.replace(Regex("[^0-9]"), "")
        return (if(number.isNotEmpty()){
            try{
                val dateStamp=number.toLong()
                val date= Date(dateStamp)
                val format= SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                "Photo from ${format.format(date)}.pdf"
            }catch(e:Exception){
                    "Документ.pdf"
            }
        }else{
         "Документ.pdf"
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        currentPage?.close()
        pdfRenderer?.close()
        pdfFile?.delete()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}






