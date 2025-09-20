package com.example.scanner

import android.annotation.SuppressLint
import android.graphics.Bitmap
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import kotlin.math.abs

class PdfFileActivity : AppCompatActivity() {
    private lateinit var pdfImageView: ImageView
    private var progressBar: ProgressBar? = null
    private lateinit var tvPageInfo: TextView
    private lateinit var tvFileName: TextView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var btnBack: Button
    private var pdfRenderer: PdfRenderer? = null
    private var currentPage: PdfRenderer.Page? = null
    private var currentPageIndex = 0
    private var pageCount = 0
    private var pdfFile: File? = null

    @SuppressLint("ClickableViewAccessibility")
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
        val filePath = intent.extras?.getString("PDF_FILE_PATH")
        Log.d("danil_logs", "onCreate: $filePath")
        if (filePath != null) {
            pdfFile = File(filePath)
            loadPdfDocument()
        } else {
            Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show()
            finish()
        }
        pdfImageView.setOnTouchListener(object : View.OnTouchListener {
            private var startX = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        startX = event.x
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val endX = event.x
                        val deltaX = endX - startX
                        if (abs(deltaX) > 100) {
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
                tvFileName.text = pdfFile!!.name
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

    private fun updateUI() {
        tvPageInfo.text = "${currentPageIndex + 1}/$pageCount"
        btnPrev.isEnabled = currentPageIndex > 0
        btnNext.isEnabled = currentPageIndex < pageCount - 1
        val colorEnabled = ContextCompat.getColor(this, R.color.colorPrimary)
        val colorDisabled = ContextCompat.getColor(this, android.R.color.darker_gray)
        btnPrev.setTextColor(if (btnPrev.isEnabled) colorEnabled else colorDisabled)
        btnNext.setTextColor(if (btnNext.isEnabled) colorEnabled else colorDisabled)
    }

    private fun showPage(pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= pageCount) return
        currentPage?.close()
        try {
            currentPage = pdfRenderer!!.openPage(pageIndex)
            currentPageIndex = pageIndex
            val bitmap = Bitmap.createBitmap(
                currentPage!!.width, currentPage!!.height, Bitmap.Config.ARGB_8888
            )
            currentPage!!.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            pdfImageView.setImageBitmap(bitmap)
            updateUI()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка отображения страницы", Toast.LENGTH_SHORT).show()
            Log.e("PDF_VIEWER", "Error showing page $pageIndex", e)
        }
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






