package com.docconverter.app.converter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import java.io.*

/**
 * 图片转 PDF
 */
object ImageToPdfConverter {

    fun imageToPdf(imageFile: File, outputFile: File) {
        singleToPdf(imageFile, outputFile)
    }

    fun imagesToPdf(imageFiles: List<File>, outputFile: File) {
        if (imageFiles.size == 1) {
            singleToPdf(imageFiles[0], outputFile)
            return
        }

        val doc = PdfDocument()
        imageFiles.forEach { file ->
            val bmp = BitmapFactory.decodeFile(file.absolutePath)
            val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, doc.pages.size + 1).create()
            val page = doc.startPage(pageInfo)
            page.canvas.drawBitmap(bmp, 0f, 0f, null)
            doc.finishPage(page)
            bmp.recycle()
        }
        FileOutputStream(outputFile).use { doc.writeTo(it) }
        doc.close()
    }

    private fun singleToPdf(imageFile: File, outputFile: File) {
        val bmp = BitmapFactory.decodeFile(imageFile.absolutePath)
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, 1).create()
        val page = doc.startPage(pageInfo)
        page.canvas.drawBitmap(bmp, 0f, 0f, null)
        doc.finishPage(page)
        FileOutputStream(outputFile).use { doc.writeTo(it) }
        doc.close()
        bmp.recycle()
    }
}
