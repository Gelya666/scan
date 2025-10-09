package com.example.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.Log
import android.util.Log.e
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.camera.core.internal.utils.ImageUtil.rotateBitmap
import androidx.recyclerview.widget.RecyclerView
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
    fun getCurrentPosition(position:Int):PhotoFilters.FilterType{
        return filtersMap[position]?:PhotoFilters.FilterType.NONE
    }

    fun clearFilter(position:Int){
        filtersMap.remove(position)
        filterIntensityMap.remove(position)
        notifyItemChanged(position)
    }
    fun clearAllRotations() {
        // Восстанавливаем оригиналы для всех позиций
        for (position in rotationStates.keys) {
            originalImages[position]?.let { originalPath ->
                try {
                    File(originalPath).copyTo(File(photoPaths[position]), overwrite = true)
                } catch (e: Exception) {
                    // Игнорируем ошибки для отдельных файлов
                }
            }
        }

        // Очищаем все повороты
        rotationStates.clear()
        notifyDataSetChanged()
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
        if (position !in 0 until photoPaths.size) {
            return
        }
            val originalFile = File(photoPaths[position])
            val backupFile = File(originalFile.parent, "backup_${originalFile.name}")
            originalFile.copyTo(backupFile, overwrite = true)
            originalImages[position] = backupFile.absolutePath
        }




    fun rotateImage(position:Int,degrees:Float=90f){
        saveOriginalImage(position)
        if(position in 0 until photoPaths.size){
            val imagePath =photoPaths[position]
            val currentRotation=rotationStates[position] ?: 0f
            val newRotation=(currentRotation+degrees)%360
            imageRotate.rotateImage(imagePath,degrees,object: ImageRotate.RotationListener{
                override fun onRotationStarted(){}
                override fun onRotationSuccess() { rotationStates[position]=newRotation
                   notifyItemChanged(position)
                    Toast.makeText(context,"Изображение перевёрнуто успешно",Toast.LENGTH_SHORT).show()
                }

                override fun onRotationError(error: String) {
                    Toast.makeText(context,"Ошибка поврота:$error",Toast.LENGTH_SHORT).show()
                }
            } )
        }
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

        fun loadImageWithFilter(photoPath: String,filterType: PhotoFilters.FilterType,intensity: Float,isCropMode: Boolean) {
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
        val filterType = filtersMap[position]?:PhotoFilters.FilterType.NONE
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



