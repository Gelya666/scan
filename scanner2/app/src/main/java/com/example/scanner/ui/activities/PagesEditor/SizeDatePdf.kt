package com.example.scanner.ui.activities.PagesEditor

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SizeDatePdf {
     fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }
     fun getFileSize(path: String): String {
        val file = File(path)
        val size = file.length()
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

}