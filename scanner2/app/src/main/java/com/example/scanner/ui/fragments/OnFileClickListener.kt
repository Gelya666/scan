package com.example.scanner.ui.fragments

import com.example.scanner.viewmodel.PdfFile

interface OnFileClickListener {
    fun onFileClick(position: Int, fileName: String, pdfFile: PdfFile)
    fun onItemClick(pdfFile:PdfFile)
    fun sharePdfFile(pdfFile:PdfFile)
}