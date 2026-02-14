package com.example.scanner.viewmodel
import android.net.Uri

data class PdfFile(
    val PathPdfFile: Uri?,
    val pdfName: String?,
    val pdfSize: String,
    val pdfDate:String?,
    val id:String
    )
