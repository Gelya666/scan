package com.example.scanner.ui.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class ImageShareHelper( val context: Context) {
    fun shareImages(imagePath:List<String>){
        if(imagePath.isEmpty()){
            showToast("Нет изображений для отправки")
            return
        }
        val uris=convertPathToUris(imagePath)
        if(uris.isEmpty()){
            showToast("Не удалось получить доступ к файлам")
        }
        val shareIntent=createShareIntent(uris)
        showChooserDialog(shareIntent)

    }

    private fun showChooserDialog(intent: Intent) {
        val chooser=Intent.createChooser(intent,"Поделиться изображением")
       context.startActivity(chooser)
    }

    private fun createShareIntent(uris:ArrayList<Uri>): Intent {
        return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(""))
            putExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_TEXT,"Посмотрите мои фотографии")
        }

        }

    private fun convertPathToUris(imagePath: List<String>): ArrayList<Uri> {
        val uris= arrayListOf<Uri>()
        for(path in imagePath){
            try{
                val file= File(path)
                if(file.exists()){
                    val uri= FileProvider.getUriForFile(context,"com.example.scanner.fileprovider",file)
                    uris.add(uri)
                }

            }catch(e:Exception){
                showToast("Ошибка ${e.message}")
            }
        }
        return uris
    }
    private fun showToast(message: String) {
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show()
    }
}