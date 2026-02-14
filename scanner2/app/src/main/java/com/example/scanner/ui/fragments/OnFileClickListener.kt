package com.example.scanner.ui.fragments

import com.example.scanner.viewmodel.PdfFile

interface OnFileClickListener {
    fun onFileClick(position: Int, fileName: String, pdfFile: PdfFile)
}