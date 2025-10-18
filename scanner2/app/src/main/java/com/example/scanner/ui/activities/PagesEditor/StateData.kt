package com.example.scanner.ui.activities.PagesEditor

import android.graphics.Bitmap

data class StateData(
    val originalBitmaps: MutableMap<Int, Bitmap> = mutableMapOf(),
    val originalImagesBackup: MutableMap<Int, String> = mutableMapOf(),
    var currentPosition: Int = 0,
    var currentIntensity: Float = 0.5f
)