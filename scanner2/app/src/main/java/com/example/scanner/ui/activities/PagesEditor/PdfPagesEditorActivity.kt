package com.example.scanner.ui.activities.PagesEditor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
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

    // Data only
    lateinit var photoPaths: ArrayList<String>
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

        photoPaths = intent.getStringArrayListExtra("photo_paths") ?: ArrayList()
        val initialPosition = intent.getIntExtra("current_position", 0)
        // Initialize state with current position
        currentState = NormalState(this).apply {
            stateData.currentPosition = initialPosition
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
        btnReturnToNormalMode.setOnClickListener { returnToNormalState() }

        btnFilters.setOnClickListener { handleEvent(ViewPagerEvent.FilterClicked) }
        btnRotate.setOnClickListener { handleEvent(ViewPagerEvent.RotateClicked) }
        btnCrop.setOnClickListener { handleEvent(ViewPagerEvent.CropClicked) }
        btnAddPage.setOnClickListener { handleEvent(ViewPagerEvent.AddPageClicked) }
    }

    private fun setupViewPager() {
        adapter = PhotoAdapter(this, photoPaths, false)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(currentState.stateData.currentPosition, false)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                handleEvent(ViewPagerEvent.PageSelected(position))
            }
        })
    }

    // State Management
    fun transitionTo(newState: PhotoViewPagerState) {
        currentState.exit()
        currentState = newState
        currentState.stateData.currentPosition = currentState.stateData.currentPosition
        currentState.enter()
    }

    // Event handling - delegate to current state
    fun handleEvent(event: ViewPagerEvent) {
        when (event) {
            is ViewPagerEvent.CropClicked -> transitionTo(CropState(this).apply {
                stateData.currentPosition = currentState.stateData.currentPosition
            })
            is ViewPagerEvent.FilterClicked -> transitionTo(FilterState(this).apply {
                stateData.currentPosition = currentState.stateData.currentPosition
            })
            is ViewPagerEvent.RotateClicked -> transitionTo(RotateState(this).apply {
                stateData.currentPosition = currentState.stateData.currentPosition
            })
            is ViewPagerEvent.PageSelected -> {
                currentState.stateData.currentPosition = event.position
                if (currentState !is NormalState) {
                    transitionTo(NormalState(this).apply {
                        stateData.currentPosition = event.position
                    })
                }
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
        btnApply.visibility = if (currentState is CropState) View.VISIBLE else View.GONE
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
            currentPhotoFile?.let { photoFile ->
                if (photoFile.exists()) {
                    photoPaths.add(photoFile.absolutePath)
                    adapter.updateData(photoPaths)
                    viewPager.setCurrentItem(photoPaths.size - 1, true)
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
        photoPaths.clear()
        adapter.updateData(ArrayList())
    }

    fun saveImageToPdf() {
        val timeStamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())
        val pdfFileName = "photos_$timeStamp.pdf"
        val resultIntent = Intent().apply {
            putStringArrayListExtra("imagePath_for_pdf", photoPaths)
            putExtra("pdf_file_name", pdfFileName)
            putExtra("action", "create_pdf")
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    fun showHalfScreenDialog() {
        val dialog = HalfScreenDialogFragment.newInstance(ArrayList())
        dialog.show(supportFragmentManager, "half_screen_dialog")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear backups from all states
        currentState.stateData.originalImagesBackup.clear()
        if (isFinishing) {
            clearAllData()
            viewPager.adapter = null
        }
    }
}

