package com.example.scanner.ui.fragments

import android.graphics.BitmapFactory
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImagePreviewHelper(
  private val imageView: ImageView,
    private val placeHolderResId: Int =android.R.drawable.ic_delete) {

    private fun loadPreview(photoPath: String?, sampleSize: Int = 4) {
        if (photoPath.isNullOrEmpty()) {
            showPlaceHolder()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                val bitmap = BitmapFactory.decodeFile(photoPath, options)

                withContext(Dispatchers.Main) {
                    if (bitmap != null) {
                      imageView.setImageBitmap(bitmap)
                    }else{
                        showPlaceHolder()
                    }
                }
            }catch(e:Exception){
                withContext(Dispatchers.Main){
                    showPlaceHolder()
                }
            }
        }
    }
    fun loadFirstPreview(photoPath:List<String>,sampleSize:Int=4){
        val firstPath=photoPath.firstOrNull()
        loadPreview(firstPath,sampleSize)
    }

    private fun showPlaceHolder() {
       imageView.setImageResource( placeHolderResId)
    }
}