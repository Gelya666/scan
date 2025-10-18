package com.example.scanner.ui.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.scanner.viewmodel.ImageRotate
import com.example.scanner.PhotoFilters
import com.example.scanner.R
import com.example.scanner.ui.customviews.SimpleCropOverlayView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PhotoAdapter(
    private val context: Context,
    private var photoPaths: List<String>,
    private var isCropMode: Boolean = false): RecyclerView.Adapter <PhotoAdapter.PhotoViewHolder>(){
    private val filtersMap = mutableMapOf<Int, PhotoFilters.FilterType>()
     val filterIntensityMap = mutableMapOf<Int, Float>()
    val rotationStates= mutableMapOf<Int,Float>()
    private val originalImages =mutableMapOf<Int,String>()
    private val imageRotate = ImageRotate()
    private val cropStates = mutableMapOf<Int, Rect>() // состояния обрезки
    private val originalBitmapsMap = mutableMapOf<String, Bitmap>()
    fun setFilterForPosition(position: Int, filterType: PhotoFilters.FilterType, intensity: Float = 1.0f) {
        filtersMap[position] = filterType
        filterIntensityMap[position] = intensity
        notifyItemChanged(position)
    }
    fun getCurrentPosition(position:Int): PhotoFilters.FilterType{
        return filtersMap[position]?: PhotoFilters.FilterType.NONE
    }

    fun clearFilter(position:Int){
        filtersMap.remove(position)
        filterIntensityMap.remove(position)
        notifyItemChanged(position)
    }
    fun clearAllRotations() {
        // Восстанавливаем оригиналы для всех позиций
        for ((position, originalPath) in originalImages) {
            try {
                if (position in 0 until photoPaths.size) {
                    val currentPath = photoPaths[position]
                    File(originalPath).copyTo(File(currentPath), overwrite = true)
                }
            } catch (e: Exception) {
            }
        }
        rotationStates.clear()
        originalImages.clear()
        notifyDataSetChanged()

        Toast.makeText(context, "Все повороты отменены", Toast.LENGTH_SHORT).show()
    }

    fun resetImageRotation(position: Int) {
        if (position in 0 until photoPaths.size) {
            originalImages[position]?.let { originalPath ->
                try{
                    File(originalPath).copyTo(File(photoPaths[position]), overwrite = true)
                rotationStates[position] = 0f
                notifyItemChanged(position) // Перезагрузит изображение с rotation=0
                Toast.makeText(context, "Поворот сброшен", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(context, "Нет оригинала для сброса", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun saveOriginalImage(position: Int) {
       try{
           val originalPath=photoPaths[position]
           val backupDir = File(context.cacheDir, "backup_dir")
           if(!backupDir.exists()){
               backupDir.mkdirs()
           }
           val backupFile= File(backupDir, "original_${position}.jpg")
          File(originalPath).copyTo(backupFile,true)
           originalImages[position]=backupFile.absolutePath

       }catch(e: Exception){
           Log.e("Rotation", "Ошибка сохранения оригинала: ${e.message}")
       }
        }
    fun cancelRotation(position: Int) {
        if (position in 0 until photoPaths.size) {
            originalImages[position]?.let { originalPath ->
                try {
                    // Восстанавливаем оригинальный файл
                    val currentPath = photoPaths[position]
                    File(originalPath).copyTo(File(currentPath), overwrite = true)

                    // Очищаем состояние поворота
                    rotationStates.remove(position)
                    originalImages.remove(position)

                    notifyItemChanged(position)
                    Toast.makeText(context, "Поворот отменен", Toast.LENGTH_SHORT).show()

                } catch (e: Exception) {
                    Log.e("Rotation", "Ошибка отмены поворота: ${e.message}")
                    Toast.makeText(context, "Ошибка отмены поворота", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                // Если оригинала нет, просто сбрасываем состояние
                rotationStates.remove(position)
                notifyItemChanged(position)
                Toast.makeText(context, "Поворот отменен", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun rotateImage(position:Int,degrees:Float=90f){
        if(position in 0 until photoPaths.size){
            val imagePath =photoPaths[position]

            if(!originalImages.containsKey(position)){
                saveOriginalImage(position)
            }
            val currentRotation=rotationStates[position] ?: 0f
            val newRotation=(currentRotation+degrees)%360

            imageRotate.rotateImage(imagePath,degrees,object: ImageRotate.RotationListener{
                override fun onRotationStarted(){}
                override fun onRotationSuccess() {
                    rotationStates[position]=newRotation
                   notifyItemChanged(position)
                    Toast.makeText(context,"Изображение перевёрнуто успешно", Toast.LENGTH_SHORT).show()
                }

                override fun onRotationError(error: String) {
                    Toast.makeText(context,"Ошибка поврота:$error", Toast.LENGTH_SHORT).show()
                }
            } )
        }
    }
    fun saveRotation(position:Int){
        val currentRotation=rotationStates[position] ?: 0f
        if(currentRotation!=0f){
            rotationStates.remove(position)
        }
        notifyItemChanged(position)

    }
    fun getCurrentRotation(position:Int):Float{
        return rotationStates[position]?:0f
    }

    fun rotate90Clockwise(position:Int){
        rotateImage(position,-90f)
    }
    fun rotate90CounterClockwise(position:Int){
        rotateImage(position,90f)
    }


    // Внутренний класс для хранения View
    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.photoImageView)
        val cropOverlay: SimpleCropOverlayView = itemView.findViewById(R.id.cropOverlay)
        fun bind(
            photoPath: String,
            filterType: PhotoFilters.FilterType,
            intensity: Float,
            isCropMode: Boolean
        ) {
            loadImageWithFilter(photoPath, filterType, intensity, isCropMode)
            cropOverlay.visibility = if (isCropMode) View.VISIBLE else View.GONE
          //  if (!isCropMode) {
                //cropOverlay.resetCrop()
            //}
        }

        fun loadImageWithFilter(photoPath: String, filterType: PhotoFilters.FilterType, intensity: Float, isCropMode: Boolean) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2
                    }
                    //тут ошибка не все фильтры работают
                    val originalBitmap = BitmapFactory.decodeFile(photoPath, options)
                    val filteredBitmap = if (filterType != PhotoFilters.FilterType.NONE) {
                        PhotoFilters.applyFilter(this@PhotoAdapter.context, originalBitmap, filterType, intensity)
                    } else {
                        originalBitmap
                    }
                    withContext(Dispatchers.Main) {
                        imageView.setImageBitmap(filteredBitmap)
                    }
                    if (filteredBitmap != originalBitmap) {
                        originalBitmap.recycle()
                    }

                } catch (e: Exception) {
                    Log.e("PhotoAdapter", "Error loading image: ${e.message}")
                }
            }
        }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        // Загружаем и отображаем фото
        val photoPath = photoPaths[position]
        val filterType = filtersMap[position]?: PhotoFilters.FilterType.NONE
        val intensity = filterIntensityMap[position]?:1.0f
        holder.bind(photoPath,filterType,intensity,isCropMode)

    }

    override fun getItemCount(): Int = photoPaths.size

    // Метод для переключения режима обрезки
    fun setCropMode(enabled: Boolean) {
        isCropMode = enabled
        notifyDataSetChanged() // Обновляем все элементы
    }
    fun updateData(newPhotoPath: List<String>) {
        this.photoPaths = newPhotoPath
        notifyDataSetChanged()
    }
}