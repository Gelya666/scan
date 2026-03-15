package com.example.scanner.ui.activities.PagesEditor

import android.graphics.Bitmap

data class StateData(

    //оригиналы в памяти для быстрой работы
    val originalBitmaps: MutableMap<Int, Bitmap> = mutableMapOf(),

    //резкрвные пути для восстановления
    val originalImagesBackup: MutableMap<Int, String> = mutableMapOf(),
    var currentPosition: Int = 0,
    var currentIntensity: Float = 0.5f
)