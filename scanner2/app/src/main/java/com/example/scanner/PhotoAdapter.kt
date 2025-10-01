package com.example.scanner

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PhotoAdapter(
    private val context: Context,
    private var photoPaths: List<String>,
    private var isCropMode: Boolean = false): RecyclerView.Adapter <PhotoAdapter.PhotoViewHolder>(){
    private val filtersMap = mutableMapOf<Int, PhotoFilters.FilterType>()
     val filterIntensityMap = mutableMapOf<Int, Float>()
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



