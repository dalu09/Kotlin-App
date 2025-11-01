package com.example.kotlinapp.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object FileStorageManager {


    private const val IMAGE_DIRECTORY = "profile_images"


    fun saveProfileImage(context: Context, bitmap: Bitmap, userId: String): Boolean {
        return try {

            val directory = context.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)


            val file = File(directory, "$userId.jpg")


            val outputStream = FileOutputStream(file)


            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)


            outputStream.flush()
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    fun loadProfileImage(context: Context, userId: String): Bitmap? {
        return try {
            val directory = context.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
            val file = File(directory, "$userId.jpg")

            if (!file.exists()) {
                return null
            }


            val inputStream = FileInputStream(file)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}