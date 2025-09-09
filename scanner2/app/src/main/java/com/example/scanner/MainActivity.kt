package com.example.scanner

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.wear.compose.material.Button
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity(), FileOptionsDialogFragment.FileOptionsListener
 {
     private val REQUEST_IMAGE_CAPTURE=1
     private val СAMERA_PERMISSION_REQUEST_CODE=100
     override fun onCreate(savedInstanceState: Bundle?) {
         super.onCreate(savedInstanceState)
         enableEdgeToEdge()
         setContentView(R.layout.activity_main)
         ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
             val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
             v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
             insets
         }
            fun checkCameraPermission():Boolean{
                return ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED
         }
         fun requestCameraPermission(){
              ActivityCompat.requestPermissions(this,arrayOf(android.Manifest.permission.CAMERA),СAMERA_PERMISSION_REQUEST_CODE)
         }
            findViewById<ImageButton>(R.id.btnOpenCamera).setOnClickListener {
                if (checkCameraPermission()) {
                    openCamera()
                }else{
                    requestCameraPermission()
                }
            }
            findViewById<ImageButton>(R.id.points1).setOnClickListener {
             val dialog = FileOptionsDialogFragment.newInstance("document.pdf")
             dialog.show(supportFragmentManager, "file_options_dialog")
             }
                findViewById<ImageButton>(R.id.points).setOnClickListener {
                val dialog = FileOptionsDialogFragment.newInstance("document.pdf")
                dialog.show(supportFragmentManager, "file_options_dialog")
                }
     }
     fun openCamera(){
         val takePictureIntent=Intent(MediaStore.ACTION_IMAGE_CAPTURE)
         try {
             startActivityForResult(takePictureIntent,REQUEST_IMAGE_CAPTURE)
         }catch(e:Exception){
             Toast.makeText(this,"Camera app not found!!!",Toast.LENGTH_SHORT).show()
         }
     }
     override fun onRequestPermissionsResult(
         requestCode:Int,
         permissions:Array<out String>,
         grantResults:IntArray
     ) {
         super.onRequestPermissionsResult(requestCode, permissions, grantResults)
         if (requestCode == СAMERA_PERMISSION_REQUEST_CODE) {
             if
                     (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 openCamera()
             } else {
                 Toast.makeText(this, "Camera permisiion denied", Toast.LENGTH_SHORT).show()
             }
         }
     }

     override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
         super.onActivityResult(requestCode, resultCode, data)
         if(requestCode== REQUEST_IMAGE_CAPTURE && resultCode==RESULT_OK) {
             val imageBitmap=data?.extras?.get("data") as Bitmap
             if(imageBitmap!=null){
                 val stream= ByteArrayOutputStream()
                 imageBitmap.compress(Bitmap.CompressFormat.PNG,100,stream)
                 val byteArray=stream.toByteArray()
                 val intent=Intent(this, screen_pfilter::class.java)
                 intent.putExtra("image_bites",byteArray)
                 startActivity(intent)
             }else{
                 Toast.makeText(this,"failed to get image",Toast.LENGTH_SHORT).show()
             }
         }
     }
     override fun onHide(filename:String)
        {
            Toast.makeText(this,"Файл скрыт:$filename",Toast.LENGTH_SHORT).show()
        }
            override fun onDownload(filename: String)
            {
                Toast.makeText(this,"Файл сохранен:$filename",Toast.LENGTH_SHORT).show()
            }
                    override fun onDelete(filename:String)
                    {
                        Toast.makeText(this,"Файл удален:$filename",Toast.LENGTH_SHORT).show()
                    }
                        override fun onReject(filename: String)
                        {
                            Toast.makeText(this,"Файл отклонен:$filename",Toast.LENGTH_SHORT).show()
                        }
                            private fun showRenameDialog(filename:String)
                            {
                                val builder= AlertDialog.Builder(this)
                                val view=layoutInflater.inflate(R.layout.dialog_rename,null)
                                val editText=view.findViewById<EditText>(R.id.rename_edittext)
                                editText.setText(filename)
                                    builder.setView(view)
                                    .setTitle("Переименовать")
                                    .setPositiveButton("Сохранить"){dialog,_->
                                        val newName=editText.text.toString()
                                        Toast.makeText(this,"Переименован в:$filename",Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                    }
                                    .setNegativeButton("Отмена"){dialog,_->dialog.dismiss()}
                                    .show()
                            }
         override fun onRename(filename:String)
         {
             showRenameDialog(filename)
         }

 }

