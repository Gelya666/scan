package com.example.scanner

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import me.relex.circleindicator.CircleIndicator3

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
    private lateinit var viewPager: ViewPager2
    private lateinit var pageIndicator: CircleIndicator3
    private val bitmaps = mutableListOf<Bitmap>()
    private var currentPage = 0
    private lateinit var adapter: ImagePagerAdapter

    companion object {
        const val REQUEST_IMAGE_CAPTURE = 101
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.edit_image)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.image)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupViewPager()
        handleIntentData()
        btnRotate = findViewById(R.id.btn_rot)
        btnCrop = findViewById(R.id.btn_cr)
        btnFilter = findViewById(R.id.btn_fil)
        btnAddpage = findViewById(R.id.btn_add)
        imageView = findViewById(R.id.imageViewEdit)
        cropOverlayView = findViewById(R.id.cropOverlayView)
        btnCropConfirm = findViewById(R.id.btnCropConfirm)
        btnCropCancel = findViewById(R.id.btnCropCancel)
        cropControls = findViewById(R.id.cropControls)
        viewPager = findViewById(R.id.viewPager)
        pageIndicator = findViewById(R.id.pageIndicator)

        //btnSave=findViewById(R.id.btnSave)

        cropOverlayView?.visibility = View.GONE

        btnRotate.setOnClickListener { rotateImage() }
        btnCrop.setOnClickListener { startCropMode() }
        btnFilter.setOnClickListener { applyFilter() }
        btnAddpage.setOnClickListener { openCameraForNewPage() }
        btnCropConfirm.setOnClickListener { confirmCrop() }
        btnCropCancel.setOnClickListener { cancelCrop() }
        setupViewPager()
    }

    private fun handleIntentData() {
        val byteArray = intent.getByteArrayExtra("image_bites")
        if (byteArray != null) {
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            bitmaps.add(bitmap)
            adapter.notifyDataSetChanged()
            pageIndicator.setViewPager(viewPager)
        }
    }

    private fun setupViewPager() {
        adapter = ImagePagerAdapter(bitmaps)
        viewPager.adapter = adapter
        pageIndicator.setViewPager(viewPager)
    }


    private fun updateCurrentBitmap() {
        currentBitmap = if (bitmaps.isNotEmpty() && currentPage < bitmaps.size) {
            bitmaps[currentPage]
        } else
            null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }


    inner class ImagePagerAdapter(private val bitmaps: List<Bitmap>) :
        RecyclerView.Adapter<ImagePagerAdapter.ViewHolder>() {
        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.imageViewItem)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_image, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.imageView.setImageBitmap(bitmaps[position])
        }

        override fun getItemCount(): Int = bitmaps.size

    }

    private fun openCameraForNewPage() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "Камера не доступна", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                bitmaps.add(it)
                adapter.notifyItemInserted(bitmaps.size - 1)
                pageIndicator.setViewPager(viewPager)
                viewPager.setCurrentItem(bitmaps.size - 1,true)
                Toast.makeText(this, "Страница добавлена!", Toast.LENGTH_SHORT).show()
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
        currentBitmap?.recycle()
    }
}