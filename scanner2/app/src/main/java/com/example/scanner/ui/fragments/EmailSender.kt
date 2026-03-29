package com.example.scanner.ui.fragments

import android.content.Intent
import android.content.Intent.EXTRA_EMAIL
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.EXTRA_SUBJECT
import android.content.Intent.EXTRA_TEXT
import android.net.Uri
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import java.io.File

class EmailSender(private val fragment: DialogFragment) {

    private val emailLauncher=fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult())
    {_ ->
        android.os.Handler(
            Looper.getMainLooper()).postDelayed({
            Toast.makeText(
                fragment.requireContext(), "Вернулись", Toast.LENGTH_SHORT
            ).show()
            fragment . dismiss ()
        },500)
    }
    fun sendMultipleImages(photoPaths:ArrayList<String>){

        if(photoPaths.isEmpty()){
            //почему используем context a не this
            Toast.makeText(fragment.requireContext(),"Список изображений пуст",Toast.LENGTH_SHORT).show()
            return
        }
        val uris=arrayListOf<Uri>()

        for (path in photoPaths) {
            try {
                val filePath = File(path)

                if (filePath.exists()) {
                    val fileUri = FileProvider.getUriForFile(
                        fragment.requireContext(),
                        "com.example.scanner.fileprovider",
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
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            emailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            val emailPackages=listOf("com.google.android.gm","ru.yandex.mail")
            var appFound=false

            for (packageName in emailPackages) {
                try {
                    emailIntent.setPackage(packageName)
                    emailLauncher.launch(emailIntent)
                    Toast.makeText(
                        fragment.requireContext(),
                        "Открываю приложение $packageName",
                        Toast.LENGTH_SHORT
                    ).show()
                    appFound = true
                    break
                } catch (e: Exception) {
                    Toast.makeText(
                        fragment.requireContext(),
                        "Это приложени не установлено пробуем следующее",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            if(!appFound){
                emailIntent.setPackage(null)
                emailLauncher.launch(Intent.createChooser(emailIntent, "Выберите приложение"))
                Toast.makeText(fragment.requireContext(), "Почтовое приложение не найдено", Toast.LENGTH_SHORT).show()

            }
        }else{
            Toast.makeText(fragment.requireContext(),"Нет доступных изображений для отправки",Toast.LENGTH_SHORT).show()
        }
    }
}
