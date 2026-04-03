package com.example.scanner.ui.fragments
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class CloudStorageSelector( private val context: Context) {
    val apps =
        listOf("Google Drive" to "com.google.android.apps.docs", "dropBox" to "com.dropbox.android")

    fun show(fileUri: Uri) {
        try {
            //открытие потока для чтения данных из файла
            context.contentResolver.openInputStream(fileUri)?.close()
        } catch (e: Exception) {
            showToast("Файл не найден")
            return
        }
        val available = getAvailableApps()
        if (available.isNotEmpty()) {
            showChoiceDialog(available, fileUri)
        } else {
            showWebOrInstallDialog(fileUri)
        }
        if (available.size == 1) {
            openApp(available[0], fileUri)
            return
        }
    }
    private fun showWebOrInstallDialog(uri: Uri) {
        val options = arrayOf("Открыть в браузере(Google Drive)", "Открыть в браузере(Dropbox)")

        //создание и настраивание диалогового окна
        AlertDialog.Builder(context)
            .setTitle("Установить заголовок")
            .setItems(options) { _, which ->

                //проверка значения
                when (which) {
                    0 -> openGoogleDriveWeb(uri)
                    1 -> openDropBox(uri)
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showChoiceDialog(apps: List<Pair<String, String>>, uri: Uri) {
        val names = apps.map { it.first }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("Сохранить в ...")
            .setItems(names) { _, which ->
                openApp(apps[which], uri)
            }
            .setNegativeButton("отмена", null)
            .show()
    }
    private fun openApp(app: Pair<String, String>, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                setPackage(app.second)
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
            showToast("Открываю ${app.first}")
        } catch (e: Exception) {
            showToast("Не удалось открыть ${app.first}")
        }
    }
    private fun getAvailableApps(): MutableList<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val pm = context.packageManager
        for ((name, packageName) in apps) {
            try {
                pm.getPackageInfo(packageName, 0)
                result.add(name to packageName)
            } catch (e: Exception) {
            }
        }
        return result
    }
    private fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
    private fun openDropBox(uri: Uri) {
        try {
            val dropBoxIntent = Intent(Intent.ACTION_SEND).apply {
                setPackage("com.dropbox.android")
                type="application/pdf"
                putExtra(Intent.EXTRA_STREAM,uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val packageManager=context.packageManager
            if(dropBoxIntent.resolveActivity(packageManager)!=null){
                context.startActivity(dropBoxIntent)
                showToast("Открываю DropBox")
                return
            }
        }catch(e:Exception){}
        val webIntent=Intent(Intent.ACTION_VIEW,Uri.parse("https://www.dropbox.com"))
        context.startActivity(webIntent)
        showToast("Открываю DropBox в браузере")
    }
    private fun openGoogleDriveWeb(uri: Uri) {
        try {

            //отправка одного файла
            val driveIntent = Intent(Intent.ACTION_SEND).apply {

                //установка приложения которого должно открыть
                setPackage("com.google.android.apps.docs")
                type = "application/pdf"

                //передача файла
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            //доступ к системной службе
            val packageManager = context.packageManager
            //поиск приложения для intent
            if(driveIntent.resolveActivity(packageManager)!=null){
              context.startActivity(driveIntent)
                showToast("Открываю Google Drive")
                return
            }
        } catch (e: Exception) {
        }
        //созадение Intent для открытия браузера
        val webIntent=Intent(Intent.ACTION_VIEW,Uri.parse("https://drive.google.com"))
        context.startActivity(webIntent)
        showToast("Открываю Google Drive в браузере")
    }
}