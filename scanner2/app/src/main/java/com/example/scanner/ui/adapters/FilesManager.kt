package com.example.scanner.ui.adapters

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.scanner.viewmodel.PdfFile

class FilesManager()
{
    fun getPdfFiles(context: Context):MutableList<PdfFile>{
        //создаю список с пдф файлами
        var pdfList = mutableListOf<PdfFile>()
        //передается параметром в query,указывается явная(т к указываю какие 4 столбца мне нужны)
        // проекция(выбираю только нужное,проецирую из большой таблицы необходимые данные)с ID,
        // именем,размером,датой создания файла,чтобы предотвратить чтение данных из хранилища, которые
        // не будут использоваться.
        val projection=arrayOf(MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED)

        //указание формата данных-PDF
        val mimeType="application/pdf"

        //передается параметром в query,фильтр указывающий какую строку надо вернуть,фильтрует строки
        // с MIME_TYPE-"application/pdf"
        val selection="${MediaStore.Files.FileColumns.MIME_TYPE}=?"

        //передается параметром в query,заменяет ? в selection значением mimeType
        val selectionArgs=arrayOf(mimeType)

        //передается параметром в query,возвращает Uri для доступа к файлам внешнего хранилища
        // для расширения памяти,используется для медиафалов,документов(SD-карта,встроенная память)

        val allFilesUri=MediaStore.Files.getContentUri("external")

        //передается параметром в query,сортирует пдф файлы по дате добавление от нового к старому
        val sortOrder="${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        //через context доступ к contentResolver(в классе context есть класс contentResolver) через
        // contentResolver доступ к query возвращает Cursor(содержит ссылку на данные в базе,
        // информацию о строках)
        //если указатель не null создание анонимной функции с параметром cursor
        context.contentResolver.query( allFilesUri,projection,selection,selectionArgs,sortOrder)?.use{cursor->

            //получение ID файла который будет предан параметром в withAppendedPath ,используется для
            // идентификации файла
            val idColumn=cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)

            //получение имени файла,будет добавлен в список пдф фалйов
            val nameColumn=cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)

            //получаю размер файла,будет добавлен в список пдф файлов
            val sizeColumn=cursor.getColumnIndexOrThrow( MediaStore.Files.FileColumns.SIZE)

            //получаю дату создания  файла,будет добавлен в список пдф файлов
            val dateColumn=cursor.getColumnIndexOrThrow( MediaStore.Files.FileColumns.DATE_ADDED)

            //перемещение курсора на следующую строку
            while(cursor.moveToNext()){

                //Возвращает значение idColumn в виде типа long.
                val id=cursor.getString(idColumn)

                //Возвращает значение nameColumn в виде типа String.
                val name=cursor.getString(nameColumn)

                //Возвращает значение sizeColumn в виде типа long.
                val size=cursor.getString(sizeColumn)

                //Возвращает значение dateColumn в виде типа long.
                val date=cursor.getString(dateColumn)

                //создания Uri конкретного файла добавляя pathSegment(id.toString()) к базовому URI(allFilesUri)
                val contentUri=Uri.withAppendedPath(allFilesUri,id.toString())

                //добавление список пдф фадйов uri конкретного файла,имя,размер,дату создания
                pdfList.add(PdfFile(contentUri,name,size,date,id))
            }
        }
        return pdfList
    }
}