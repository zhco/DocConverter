package com.docconverter.app.converter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.googlecode.tesseract.android.TessBaseAPI
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileOutputStream

/**
 * 图片 OCR 转 Word/Excel
 * 使用 Tesseract 进行文字识别
 */
object ImageOcrConverter {

    private var tessApi: TessBaseAPI? = null

    private fun initTesseract(context: Context) {
        if (tessApi != null) return
        tessApi = TessBaseAPI().apply {
            // Uses bundled tessdata from tesseract4android
            init(context.filesDir.parentFile!!.absolutePath, "eng")
        }
    }

    fun imageToWord(context: Context, imageFile: File, outputFile: File) {
        initTesseract(context)
        val bmp = BitmapFactory.decodeFile(imageFile.absolutePath)
        tessApi!!.setImage(bmp)
        val text = tessApi!!.utF8Text

        val doc = XWPFDocument()
        val para = doc.createParagraph()
        val run = para.createRun()
        run.setText(text)

        FileOutputStream(outputFile).use { doc.write(it) }
        doc.close()
        bmp.recycle()
    }

    fun imageToExcel(context: Context, imageFile: File, outputFile: File) {
        initTesseract(context)
        val bmp = BitmapFactory.decodeFile(imageFile.absolutePath)
        tessApi!!.setImage(bmp)
        val text = tessApi!!.utF8Text

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("OCR Result")
        var rowIdx = 0

        text.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                val row = sheet.createRow(rowIdx++)
                val cells = trimmed.split(Regex("\\s{2,}"))
                cells.forEachIndexed { colIdx, value ->
                    row.createCell(colIdx).setCellValue(value.trim())
                }
            }
        }

        FileOutputStream(outputFile).use { workbook.write(it) }
        workbook.close()
        bmp.recycle()
    }

    fun release() {
        tessApi?.recycle()
        tessApi = null
    }
}
