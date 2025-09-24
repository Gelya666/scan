package com.example.scanner

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class PhotoAdapter(
    private val photoPaths: List<String>,
    private var isCropMode: Boolean = false
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    // Внутренний класс для хранения View
    inner class PhotoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.photoImageView)
        val cropOverlay:SimpleCropOverlayView = itemView.findViewById(R.id.cropOverlay)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        // Загружаем и отображаем фото
        val photoPath = photoPaths[position]
        val bitmap = BitmapFactory.decodeFile(photoPath)
        holder.imageView.setImageBitmap(bitmap)

        // Управляем видимостью overlay обрезки
        if (isCropMode) {
            holder.cropOverlay.visibility = View.VISIBLE
        } else {
            holder.cropOverlay.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = photoPaths.size

    // Метод для переключения режима обрезки
    fun setCropMode(enabled: Boolean) {
        isCropMode = enabled
        notifyDataSetChanged() // Обновляем все элементы
    }
}

