package com.example.scanner.ui.fragments

import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_EMAIL
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class EmailSender(private val context: Context) {
  fun sendMultipleImages(photoPaths:ArrayList<String>){
        if(photoPaths.isEmpty()){
            //почему используем context a не this
            Toast.makeText(context,"Список изображений пуст",Toast.LENGTH_SHORT).show()
            return
        }
        val uris=arrayListOf<Uri>()

          for (path in photoPaths) {
              try {
              val filePath = File(path)
              if (filePath.exists()) {
                  val fileUri = FileProvider.getUriForFile(
                      context,
                      "${context.packageName}.fileprovider",
                      filePath
                  )
                  uris.add(fileUri)
              }
          }catch(e: Exception){
                  e.printStackTrace()
      }
      }
        if(uris.isNotEmpty()){
            val emailIntent= Intent(Intent.ACTION_SEND_MULTIPLE)
            emailIntent.type="image/*"
            emailIntent.putExtra(EXTRA_EMAIL,arrayOf(""))
            emailIntent.putExtra(EXTRA_SUBJECT,"Изображения из приложения")
            emailIntent.putExtra(EXTRA_TEXT,"Отправляю вам изображение")
            emailIntent.putParcelableArrayListExtra(EXTRA_STREAM,uris)
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val emailPackages=listOf<String>("com.google.android.gm","ru.yandex.mail")
             if(emailIntent.resolveActivity(context.packageManager)!=null){
                 Toast.makeText(context, "Открываю выбор email приложения", Toast.LENGTH_SHORT).show()
                 context.startActivity(Intent.createChooser(emailIntent,"отправить через email"))
             }else{
                 Toast.makeText(context,"Нет доступа к выбору email",Toast.LENGTH_SHORT).show()
             }

        }else{
            Toast.makeText(context,"Нет доступных изображений для отправки",Toast.LENGTH_SHORT).show()
        }
    }
}