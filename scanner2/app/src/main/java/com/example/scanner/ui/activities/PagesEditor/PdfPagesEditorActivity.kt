package com.example.scanner.ui.activities.PagesEditor

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.viewpager2.widget.ViewPager2
import com.example.scanner.R
import com.example.scanner.ui.activities.MainActivity
import com.example.scanner.ui.adapters.PhotoAdapter
import com.example.scanner.ui.fragments.HalfScreenDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.properties.HorizontalAlignment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfPagesEditorActivity : AppCompatActivity() {
    // UI Components only - exposed for states
    lateinit var viewPager: ViewPager2
    lateinit var tabLayout: TabLayout
    lateinit var btnExit: Button
    lateinit var btnUndo: Button
    lateinit var btnRedo: Button
    lateinit var btnCrop: ImageButton
    lateinit var btnSave: Button
    lateinit var btnApply: Button
    lateinit var btnReturnToNormalMode: Button
    lateinit var btnAddPage: ImageButton
    lateinit var btnFilters: ImageButton
    lateinit var btnRotate: ImageButton
    lateinit var filtersContainer: LinearLayout
    lateinit var filterPanel: LinearLayout
    lateinit var intensityPanel: LinearLayout
    lateinit var intensitySeekBar: SeekBar
    lateinit var intensityValue: TextView
   var stateData=StateData()

    lateinit var adapter: PhotoAdapter

    var currentPhotoFile: File? = null

    // State Management only
    private lateinit var currentState: PhotoViewPagerState

    // Permission handlers
    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        onTakePictureResult(success)
    }

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        onPermissionResult(isGranted)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_view_pager)

        //photoPaths
        val stringPhotoPaths = intent.getStringArrayListExtra("photo_paths") ?: ArrayList()
        stateData.setPhotoPaths(stringPhotoPaths)
        val initialPosition = intent.getIntExtra("current_position", 0)
        // Initialize state with current position
        currentState = NormalState(this).apply {

            //stateData.currentPosition = initialPosition
            stateData.setCurrentPosition(initialPosition)
        }
        initViews()
        setupViewPager()
        setupButtons()
        currentState.enter()

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = "Page ${position + 1}"
        }.attach()
    }

    // Only essential UI setup methods remain
    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        btnExit = findViewById(R.id.btnExit)
        btnUndo = findViewById(R.id.btn_undo)
        btnRedo = findViewById(R.id.btn_redo)
        btnSave = findViewById(R.id.btnSave)

        btnAddPage = findViewById(R.id.btn_add_page)
        btnCrop = findViewById(R.id.btn_crop)
        btnFilters = findViewById(R.id.btn_filter)
        btnRotate = findViewById(R.id.btn_rotate)

        btnApply = findViewById(R.id.btnApply)
        btnReturnToNormalMode = findViewById(R.id.btn_return_to_normal_mode)

        filterPanel = findViewById(R.id.filterpanel)
        intensityPanel = findViewById(R.id.intensity_Panel)
        intensitySeekBar = findViewById(R.id.intensity_SeekBar)
        intensityValue = findViewById(R.id.intensity_Value)
        filtersContainer = findViewById(R.id.filters_Container)

        tabLayout = findViewById(R.id.tabLayout)
    }

    private fun setupButtons() {
        btnExit.setOnClickListener { exitEditMode() }
        btnSave.setOnClickListener { showHalfScreenDialog() }

        btnUndo.setOnClickListener { handleEvent(ViewPagerEvent.UndoClicked) }
        btnRedo.setOnClickListener { handleEvent(ViewPagerEvent.RedoClicked) }

        btnApply.setOnClickListener { handleEvent(ViewPagerEvent.ApplyClicked) }
        btnReturnToNormalMode.setOnClickListener {
            val handled = currentState.onBackPressed()
           if (!handled) {
                transitionTo(NormalState(this))
            }
            updateAllUI()
        }
        btnFilters.setOnClickListener { handleEvent(ViewPagerEvent.FilterClicked) }
        btnRotate.setOnClickListener { handleEvent(ViewPagerEvent.RotateClicked) }
        btnCrop.setOnClickListener { handleEvent(ViewPagerEvent.CropClicked) }
        btnAddPage.setOnClickListener { handleEvent(ViewPagerEvent.AddPageClicked) }
    }

    private fun setupViewPager() {
        //adapter = PhotoAdapter(this, photoPaths, false)

        adapter = PhotoAdapter(this,stateData.getPhotoPaths(),stateData ,false)
        viewPager.adapter = adapter
       // viewPager.setCurrentItem(currentState.stateData.currentPosition, false)
        viewPager.setCurrentItem(currentState.stateData.getCurrentPosition(), false)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                handleEvent(ViewPagerEvent.PageSelected(position))
            }
        })
    }

    // State Management
    fun transitionTo(newState: PhotoViewPagerState) {
        Log.d("TRANSITION", "Переход из ${currentState::class.simpleName} в ${newState::class.simpleName}")
        val currentPosition=currentState.stateData.getCurrentPosition()
        currentState.exit()
        newState.stateData.setCurrentPosition(currentPosition)
        currentState= newState
        currentState.enter()
        // Должно быть: newState.stateData.currentPosition = currentState.stateData.currentPosition
        // Пример: сохранить номер страницы при переходе между состояниями
    }

    // Event handling - delegate to current state
    fun handleEvent(event: ViewPagerEvent) {
        //проверка какой именно тип событий произошёл
        when (event) {

            is ViewPagerEvent.CropClicked -> transitionTo(CropState(this,stateData))
            is ViewPagerEvent.FilterClicked -> transitionTo(FilterState(this,stateData))
            is ViewPagerEvent.RotateClicked -> transitionTo(RotateState(this,stateData))
            is ViewPagerEvent.PageSelected -> {
                currentState.stateData.setCurrentPosition(event.position)
                if (currentState !is NormalState) {
                    transitionTo(NormalState(this).apply {
                        stateData.setCurrentPosition(event.position)
                    })
                }
            }
            is ViewPagerEvent.AddPageClicked->{
                checkCameraPermissionAndTakePhoto()
            }
            is ViewPagerEvent.ApplyClicked -> currentState.onApplyClicked()
            is ViewPagerEvent.UndoClicked -> adapter.undo()
            is ViewPagerEvent.RedoClicked -> adapter.redo()
            else -> currentState.handleEvent(event)
        }
    }

    fun updateAllUI(){
        btnRedo.visibility = if (adapter.hasAnyChangesUndone()) View.VISIBLE else View.GONE
        btnUndo.visibility = if (adapter.hasAnyChanges()) View.VISIBLE else View.GONE

        btnExit.visibility = if (currentState is NormalState) View.VISIBLE else View.GONE
        btnSave.visibility = if (currentState is NormalState) View.VISIBLE else View.GONE

        btnReturnToNormalMode.visibility = if (currentState is NormalState) View.GONE else View.VISIBLE
        btnApply.visibility = if (currentState is CropState || currentState is FilterState) View.VISIBLE else View.GONE
    }
    // UI Update methods only
    fun updateNormalUI() {
        filterPanel.visibility = View.GONE
        intensityPanel.visibility = View.GONE
        updateAllUI()
    }

    fun updateCropUI() {
        filterPanel.visibility = View.GONE
        intensityPanel.visibility = View.GONE
        updateAllUI()
    }

    fun updateFilterUI() {

        //изменения цвета кнопки
        btnSave.setBackgroundColor(ContextCompat.getColor(this, R.color.color_filter))
        // Filter panel visibility is managed by FilterState
        updateAllUI()
    }

    fun updateRotateUI() {
        filterPanel.visibility = View.GONE
        intensityPanel.visibility = View.GONE
        updateAllUI()
    }

    private fun returnToNormalState(){
        transitionTo(NormalState(this))
    }

    private fun exitEditMode() {
        clearAllData()
        val intent = Intent(this, MainActivity::class.java).apply {

        }
        startActivity(intent)
    }

    // Permission and file methods
    private fun checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            takePhoto()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun takePhoto() {
        val photoFile = createImageFile()
        if (photoFile != null) {
            currentPhotoFile = photoFile
            val photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
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
            null
        }
    }
    private fun onTakePictureResult(success: Boolean) {
        if (success) {

            //если currentPhotoFile не null
            currentPhotoFile?.let { photoFile ->
                if (photoFile.exists()) {
                    //photoPaths.add(photoFile.absolutePath)
                    stateData.getPhotoPaths().add(photoFile.absolutePath)
                    //adapter.updateData(photoPaths)
                    adapter.updateData( stateData.getPhotoPaths())

                    //переключение viewPager на последнюю страницу
                    //viewPager.setCurrentItem(photoPaths.size - 1, true)
                    viewPager.setCurrentItem(stateData.getPhotoPaths().size - 1, true)
                    Toast.makeText(this, "Фото добавлено", Toast.LENGTH_SHORT).show()
                }
            }
            currentPhotoFile = null
        }
    }
    private fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            takePhoto()
        } else {
            Toast.makeText(this, "Необходимо разрешение для фото", Toast.LENGTH_LONG).show()
        }
    }
    fun clearAllData() {
        //photoPaths.clear()
        stateData.clearPhotoPaths()
        adapter.updateData(ArrayList())
    }
    fun saveImageToPdf() {
        val currentDate=Date()
        val timeStamp = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val timeStampForDisplay=timeStamp.format(currentDate)
        val pdfFileName = "Photo from $timeStampForDisplay.pdf"
        //val pdfFilePath = createAndSavePdfNow(photoPaths, pdfFileName)
        val pdfFilePath = createAndSavePdfNow(stateData.getPhotoPaths(), pdfFileName)
        if (pdfFilePath != null ) {
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("pdf_file_path", pdfFilePath)
                putExtra("pdf_file_name", pdfFileName)
            }
                startActivity(intent)
                finish()
        } else {
            Toast.makeText(this, "Ошибка создания PDF", Toast.LENGTH_SHORT).show()
        }
    }
    private fun createAndSavePdfNow(imagePaths: ArrayList<String>,fileName: String):Uri?{
        return try{
            val contentValues=ContentValues().apply{
                put(MediaStore.Files.FileColumns.DISPLAY_NAME,fileName)
                put(MediaStore.Files.FileColumns.MIME_TYPE,"application/pdf")
                put(MediaStore.Files.FileColumns.DATE_ADDED,System.currentTimeMillis()/1000)
                put(MediaStore.Files.FileColumns.SIZE,0)
            }

            val pdfUri=contentResolver.insert(MediaStore.Files.getContentUri("external"),contentValues)
            if(pdfUri==null){
                Log.e("angel","Не удалось загрузить файл в хранилище")
                return null
            }

            //открытие потока для записи данных по адресу pdfUri
            contentResolver.openOutputStream(pdfUri)?.use{outputStream ->

                //создает пдф документ и писателя которые будет записывать в поток
                val pdfDocument = PdfDocument(
                    PdfWriter(outputStream)
                )
                //создание страниц для pdfDocument
                val document = Document(pdfDocument)
                // перебираем все изображения находящиеся в массиве
                for (imagePath in imagePaths) {
                    try {
                        //объект imageData внутренне представление(формат,размер,байты)
                        val imageData = ImageDataFactory.create(imagePath)
                        // создания объекта изображения для добавления в пдф
                        val image = Image(imageData)
                        // Настраиваем размер
                        image.scaleToFit(500f, 500f)
                        image.setHorizontalAlignment(HorizontalAlignment.CENTER)
                        document.add(image)
                        document.add(Paragraph("\n"))
                    } catch (e: Exception) {
                        Log.e("PDF", "Ошибка добавления изображения $imagePath: ${e.message}")
                    }
                }
                document.close()
            }
           pdfUri
        } catch (e: Exception) {
            Log.e("PDF", "Ошибка создания PDF: ${e.message}")
            null
        }
    }
    fun  showHalfScreenDialog() {
        val dialog = HalfScreenDialogFragment.newInstance(stateData.getPhotoPaths())
        dialog.show(supportFragmentManager, "half_screen_dialog")
    }
    override fun onDestroy() {
        super.onDestroy()
        // Clear backups from all states
        currentState.stateData.clearOriginalImagesBackup()
        if (isFinishing) {
            clearAllData()
            viewPager.adapter = null
        }
    }

}

