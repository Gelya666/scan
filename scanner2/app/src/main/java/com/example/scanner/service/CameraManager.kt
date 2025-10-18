package com.example.scanner.service

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.scanner.ui.activities.PhotoViewPagerActivity
import java.io.ByteArrayOutputStream

class CameraManager(val activity: Activity) {

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 100
        private const val REQUEST_IMAGE_CAPTURE = 101
        const val EXTRA_PHOTO_PATHS = "photo_paths"
    }

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
            activity, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            activity, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION
        )
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            if (imageBitmap != null) {
                val stream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val byteArray = stream.toByteArray()
                val intent = Intent(activity, PhotoViewPagerActivity::class.java)
                intent.putExtra("image_bites", byteArray)
                activity.startActivity(intent)
            } else {
                Toast.makeText(activity, "failed to get image", Toast.LENGTH_SHORT).show()
            }
        }
    }
}