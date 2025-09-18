package com.example.scanner

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

class CameraManager(val activity: Activity) {
    private val REQUEST_IMAGE_CAPTURE = 1
    private val СAMERA_PERMISSION_REQUEST_CODE = 100
    fun openCamera() {
        if (checkCameraPermission()) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            try {
                activity.startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } catch (e: Exception) {
                Toast.makeText(activity, "Camera app not found!!!", Toast.LENGTH_SHORT).show()
            }
        } else {
            requestCameraPermission()
        }
    }
    fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            activity, arrayOf(android.Manifest.permission.CAMERA), СAMERA_PERMISSION_REQUEST_CODE
        )
    }
    fun onRequestPermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == СAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(activity, "Camera permisiion denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            if (imageBitmap != null) {
                val stream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()
                val intent = Intent(activity, ScreenPfilterAcitivty::class.java)
                intent.putExtra("image_bites", byteArray)
                activity.startActivity(intent)
            } else {
                Toast.makeText(activity, "failed to get image", Toast.LENGTH_SHORT).show()
            }
        }
    }
}