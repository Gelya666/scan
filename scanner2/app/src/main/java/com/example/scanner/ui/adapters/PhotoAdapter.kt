package com.example.scanner.ui.adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.scanner.PhotoFilters
import com.example.scanner.R
import com.example.scanner.ui.activities.PagesEditor.StateData
import com.example.scanner.ui.customviews.SimpleCropOverlayView
import com.example.scanner.viewmodel.ImageRotate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PhotoAdapter(
    private val context: Context,
    private var photoPaths: List<String>,
    private val stateData: StateData,
    private var isCropMode: Boolean = false,
): RecyclerView.Adapter <PhotoAdapter.PhotoViewHolder>() {
    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.photoImageView)
        val cropOverlay: SimpleCropOverlayView = itemView.findViewById(R.id.cropOverlay)

        //заполнение view данными
        fun bind(
            photoPath: String,
            filterType: PhotoFilters.FilterType,
            intensity: Float,
            isCropMode: Boolean
        ) {
            loadImageWithFilter(photoPath, filterType, intensity, isCropMode)
            cropOverlay.visibility = if (isCropMode) View.VISIBLE else View.GONE
            //  if (!isCropMode) {ч
            //cropOverlay.resetCrop()
            //}
        }

        fun loadImageWithFilter(photoPath: String, filterType: PhotoFilters.FilterType, intensity: Float, isCropMode: Boolean) {
           //создание области scope которая управляет жизненным циклом корутин
            //запуск одной корутины внутри scope
            CoroutineScope(Dispatchers.IO).launch {
                try {

                    //создание настроек с уменьшением в 2 раза
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

    private val imageRotate = ImageRotate()

    override fun getItemCount(): Int = photoPaths.size

    fun setFilterForPosition(position: Int, filterType: PhotoFilters.FilterType, intensity: Float = 1.0f) {

        stateData.SetFilterData(position, filterType, intensity)

        //обновить элемент по позиции
        notifyItemChanged(position)
    }

    fun getFilterForPosition(position: Int): PhotoFilters.FilterType {
        val value = if (position in 0 until itemCount) stateData.GetFilterType(position)else PhotoFilters.FilterType.NONE
        if(value != null){
            return value
        }
        return PhotoFilters.FilterType.NONE
    }

    fun clearFilter(position:Int){
        stateData.removeFilterAndIntensity(position)
        notifyItemChanged(position)
    }

    fun undo(){
        Toast.makeText(context, "отменяем изменения по 1", Toast.LENGTH_SHORT).show()
    }

    fun redo(){
        Toast.makeText(context, "возвращаем изменения по 1", Toast.LENGTH_SHORT).show()
    }

    fun hasAnyChanges() : Boolean {
        return false
    }

    fun hasAnyChangesUndone() : Boolean {
        return false
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
         stateData.SetOriginaImages(position,backupFile.absolutePath)

       }catch(e: Exception){
           Log.e("Rotation", "Ошибка сохранения оригинала: ${e.message}")
       }
    }
    fun setCropMode(value: Boolean){
        this.isCropMode = value
        notifyDataSetChanged()
    }
    fun rotateImage(position:Int,degrees:Float=90f){
        if(position in 0 until photoPaths.size){
            val imagePath =photoPaths[position]

            if(!stateData.getOriginalImagesContainsKey(position)){
                saveOriginalImage(position)
            }
            var currentRotation=stateData.getRotationStates(position)
            val newRotation=(currentRotation+degrees)%360

            imageRotate.rotateImage(
                imagePath,
                degrees,
                object: ImageRotate.RotationListener {
                override fun onRotationStarted(){}

                override fun onRotationSuccess() {
                    stateData.setRotationStates(position, newRotation)
                    notifyItemChanged(position)
                    Toast.makeText(context,"Изображение перевёрнуто успешно", Toast.LENGTH_SHORT).show()
                }

                override fun onRotationError(error: String) {
                    Toast.makeText(context,"Ошибка поврота:$error", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }
    override fun onBindViewHolder(holder: PhotoViewHolder, position:Int) {
        // Загружаем и отображаем фото
        val photoPath = photoPaths[position]
        val filterType = stateData.GetFilterType(position)
        val intensity = stateData.GetFilterIntensity(position)
        holder.bind(photoPath,filterType,intensity,isCropMode)
    }
    fun updateData(newPhotoPath: List<String>) {
        this.photoPaths = newPhotoPath
        notifyDataSetChanged()
    }

}