package com.example.scanner.ui.activities
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileDisplayHelper(private val contentResolver: ContentResolver){
    fun getFileDisplayName(uri: Uri):String{
        val name=getFileName(uri)?:"Документ"
        val date=getFileDate(uri)?: extractDateFromFileName(name)
        Log.d("date","date=$date name=$name")
        name.replace(".pdf","",true)
        if(date!=null){
        return "Photo from $date"}
        return "Document"
    }

    private fun extractDateFromFileName(fileName: String): String? {
        Log.d("DATE_EXTRACT","исходное имя файла $fileName")
        val datePattern= Regex("""(\d{2})\.(\d{2})\.(\d{4})""")
        Log.d("DATE_EXTRACT","паттерн: ${datePattern.pattern}")

        val matchResult=datePattern.find(fileName)
        Log.d("DATE_EXTRACT","matchResult: ${matchResult}")
        return matchResult?.value
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
    @SuppressLint("SuspiciousIndentation")
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