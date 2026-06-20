package com.docconverter.app.converter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.io.*

/**
 * PDF 转图片：逐页转 PNG + 拼接成长图
 */
object PdfToImageConverter {

    fun pdfToImages(pdfFile: File, outputDir: File): List<File> {
        val doc = PDDocument.load(pdfFile, MemoryUsageSetting.setupTempFileOnly())
        val renderer = PDFRenderer(doc)
        val images = mutableListOf<File>()

        for (i in 0 until doc.numberOfPages) {
            val bitmap = renderer.renderImageWithDPI(i, 200f)
            val outFile = File(outputDir, "page_${i + 1}.png")
            FileOutputStream(outFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
            }
            images.add(outFile)
            bitmap.recycle()
        }
        doc.close()
        return images
    }

    fun pdfToLongImage(pdfFile: File, outputFile: File): File {
        val images = pdfToImages(pdfFile, outputFile.parentFile)
        if (images.isEmpty()) throw IllegalStateException("No pages in PDF")

        val bitmaps = images.map { BitmapFactory.decodeFile(it.absolutePath) }
        val totalWidth = bitmaps.maxOf { it.width }
        val totalHeight = bitmaps.sumOf { it.height }

        val longBitmap = Bitmap.createBitmap(totalWidth, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(longBitmap)
        var y = 0
        bitmaps.forEach { bmp ->
            canvas.drawBitmap(bmp, 0f, y.toFloat(), Paint())
            y += bmp.height
            bmp.recycle()
        }

        FileOutputStream(outputFile).use { fos ->
            longBitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
        }
        longBitmap.recycle()

        // Clean individual page images
        images.forEach { it.delete() }
        return outputFile
    }
}
