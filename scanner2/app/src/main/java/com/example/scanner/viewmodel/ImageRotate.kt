package com.example.scanner.viewmodel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream

class ImageRotate {

    interface RotationListener {
        fun onRotationStarted()
        fun onRotationSuccess()
        fun onRotationError(error: String)
    }
    fun rotateImage(imagePath:String,
                    degrees:Float=90f,
                    listener:RotationListener?=null){
       CoroutineScope(Dispatchers.IO).launch {
           try {
               withContext(Dispatchers.Main) {
                   listener?.onRotationStarted()
               }
               val originalBitmap = BitmapFactory.decodeFile(imagePath) ?: throw Exception ("Не удалось загрузить изображение")
               val matrix= Matrix()
               matrix.postRotate(degrees)
               val rotatedBitmap = Bitmap.createBitmap(originalBitmap,0,0,originalBitmap.width,originalBitmap.height,matrix,true)
               FileOutputStream(imagePath).use{ out->
                   rotatedBitmap.compress(Bitmap.CompressFormat.JPEG,90,out)
                   originalBitmap.recycle()
                   rotatedBitmap.recycle()
                   withContext(Dispatchers.Main) {
                       listener?.onRotationSuccess()
                   }
               }
           }catch(e: Exception) {
               Log.e("Rotate", "Ошибка поворота ${e.message}")
               withContext(Dispatchers.Main) {
                   listener?.onRotationError(e.message ?: "Неизвестная ошибка")
               }
           }
       }

    }
    fun rotate90Clockwise(imagePath:String,listener:RotationListener?=null){
        rotateImage(imagePath,-90f,listener)
    }
    fun rotate90CounterClockwise(imagePath:String,listener: RotationListener?=null){
        rotateImage(imagePath,90f,listener)
    }
    fun rotate180(imagePath:String,listener: RotationListener?=null){
        rotateImage(imagePath,180f,listener)

        fun cancelAllRotations() {
            CoroutineScope(Dispatchers.IO).cancel()
        }
    }

}