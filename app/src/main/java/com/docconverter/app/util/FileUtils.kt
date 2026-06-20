package com.docconverter.app.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream

object FileUtils {
    fun getOutputDir(context: Context): File {
        val dir = File(context.cacheDir, "converted")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun copyUriToTemp(context: Context, uri: Uri, prefix: String = "input"): File? {
        return try {
            val cr: ContentResolver = context.contentResolver
            val fileName = getFileName(cr, uri) ?: "$prefix.tmp"
            val ext = fileName.substringAfterLast('.', "")
            val tmpFile = File(context.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")
            cr.openInputStream(uri)?.use { input ->
                FileOutputStream(tmpFile).use { output -> input.copyTo(output) }
            }
            tmpFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(cr: ContentResolver, uri: Uri): String? {
        var name: String? = null
        cr.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(idx)
            }
        }
        return name
    }

    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
    }
}
