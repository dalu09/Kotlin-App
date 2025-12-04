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


            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true


            var inputStream = FileInputStream(file)
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()


            options.inSampleSize = calculateInSampleSize(options, 300, 300)


            options.inJustDecodeBounds = false


            inputStream = FileInputStream(file)
            val optimizedBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            optimizedBitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {

        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2


            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}