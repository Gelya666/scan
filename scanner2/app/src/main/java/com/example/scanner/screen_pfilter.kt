package com.example.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class screen_pfilter : AppCompatActivity()
{
    private lateinit var btnRotate:ImageButton
    private var cropOverlayView: CropOverlayView?=null
    private lateinit var btnCrop: ImageButton
    private lateinit var btnFilter:ImageButton
    private lateinit var btnAddpage:ImageButton
    private lateinit var imageView: ImageView
    private lateinit var btnSave: Button
    private var currentBitmap: Bitmap?=null
    private var rotationAngel=0f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.screen_pfilter)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pfilter)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
            btnRotate=findViewById(R.id.btn_rotate)
            btnCrop=findViewById(R.id.btn_crop)
            btnFilter=findViewById(R.id.btn_filter)
            btnAddpage=findViewById(R.id.btn_add_page)
            imageView=findViewById(R.id.image_photo)
            btnSave=findViewById(R.id.btnSave)
            val byteArray=intent.getByteArrayExtra("image_bites")
            if(byteArray!=null){
                currentBitmap=BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                imageView.setImageBitmap(currentBitmap)
            }else{
                Toast.makeText(this,"Ошибка загрузки изображения",Toast.LENGTH_SHORT).show()
                finish()
            }
                fun rotateImage(){
                    rotationAngel=(rotationAngel*90)%360
                    imageView.rotation=rotationAngel
                    Toast.makeText(this,"rotated 90 degrees",Toast.LENGTH_SHORT).show()
                }
                    fun applyFilter() {
                        val matrix= ColorMatrix()
                        matrix.setSaturation(0f)
                        val filter= ColorMatrixColorFilter(matrix)
                        imageView.colorFilter=filter
                    }
                        fun confirmCrop() {
                           currentBitmap?.let{bitmap->
                               val cropArea=cropOverlayView?.getCroppedArea(bitmap,imageView) ?:return@let
                               if(cropArea.width()>0 && cropArea.height()>0){
                                   val croppedBitmap=Bitmap.createBitmap(bitmap,cropArea.left.coerceIn(0,bitmap.width-1),
                                       cropArea.top.coerceIn(0,bitmap.height-1),
                                       cropArea.width().coerceIn(1,bitmap.width-cropArea.left),
                                       cropArea.height().coerceIn(1,bitmap.height-cropArea.top))
                                      currentBitmap=croppedBitmap
                                      imageView.setImageBitmap(croppedBitmap)
                               }

                               Toast.makeText(this,"Image is cropped",Toast.LENGTH_SHORT).show()
                           }
                        }
                            fun saveImage(){}
                btnRotate.setOnClickListener{rotateImage()}
                btnCrop.setOnClickListener{confirmCrop()}
                btnFilter.setOnClickListener{applyFilter()}
               // btnAddpage.setOnClickListener{}
                btnSave.setOnClickListener {saveImage()}



    }

    override fun onDestroy() {
        super.onDestroy()
        currentBitmap?.recycle()
    }
}