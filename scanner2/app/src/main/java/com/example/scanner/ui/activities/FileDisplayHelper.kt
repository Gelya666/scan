package com.example.scanner.ui.activities
import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
class FileDisplayHelper(private val contentResolver: ContentResolver){
    fun getFileDisplayName(uri: Uri):String{
        val name=getFileName(uri)?:"Документ"
        val date=getFileDate(uri)
        name.replace(".pdf","",true)
        return if (date!=null){
            "Photo from $date"
        }else{
            name
        }
    }
    private fun getFileDate(uri: Uri):String? {
        return try{
            //запрос даты изменения файла
            val cursor=contentResolver.query(uri,arrayOf(MediaStore.MediaColumns.DATE_MODIFIED),null,null,null)
           //вызов функции use чтобы автоматически закрыть cursor
            cursor?.use{
                //проверка есть ли строки и перемещение на первую
                if(it.moveToFirst()){

                    //определяем номер колонки с датой изменения файла
                    val dateIndex=it.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    if(dateIndex>=0){
                        //преобразование из сек в миллисек.
                        val timestamp=it.getLong(dateIndex)*1000
                        val date= Date(timestamp)
                        val format= SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                        return format.format(date)
                    }
                }
            }
            null
        }catch(e:Exception) {
            null
        }
    }
    fun getFileName(uri: Uri): String? {
        return try{
            //запрос на uri
          val cursor=contentResolver.query(uri,null,null,null,null)
            cursor?.use{
                if(it.moveToFirst()){
                    val nameIndex=it.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if(nameIndex>=0){
                        it.getString(nameIndex)
                    }
                }
            }
            uri.lastPathSegment
        }catch(e:Exception){
            uri.lastPathSegment
        }
    }
    fun getFileNameOnly(uri:Uri):String?{
        val name=getFileNameOnly(uri)?:"Документ"
        return name.replace(".pdf","",true)
    }
}