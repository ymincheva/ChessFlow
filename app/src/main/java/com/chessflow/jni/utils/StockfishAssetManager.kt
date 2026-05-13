package com.chessflow.jni.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream

object StockfishAssetManager {

    fun copyNNUEFile(context: Context, fileName: String): String? {
        val file = File(context.filesDir, fileName)

        if (file.exists()) {
            return file.absolutePath
        }

        return try {
            context.assets.open(fileName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

