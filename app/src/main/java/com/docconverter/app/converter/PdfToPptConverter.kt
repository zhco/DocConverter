package com.docconverter.app.converter

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.poi.xslf.usermodel.*
import java.awt.Rectangle
import java.io.*

/**
 * PDF 转 PPT (PPTX) - 基础模式
 * 每页 PDF 转为一页 PPT 幻灯片，保留文本内容
 */
object PdfToPptConverter {

    fun convert(pdfFile: File, outputFile: File) {
        val doc = PDDocument.load(pdfFile, MemoryUsageSetting.setupTempFileOnly())
        val ppt = XMLSlideShow()

        val stripper = org.apache.pdfbox.text.PDFTextStripper()
        stripper.sortByPosition = true

        for (i in 1..doc.numberOfPages) {
            stripper.startPage = i
            stripper.endPage = i
            val text = stripper.getText(doc)

            val slide = ppt.createSlide()
            val titleBox = slide.createTextBox().apply {
                anchor = Rectangle(50, 30, 600, 50)
                addNewTextParagraph().apply {
                    val run = addNewTextRun()
                    run.text = "Page $i"
                    run.fontSize = 28.0
                    run.isBold = true
                }
            }

            val contentBox = slide.createTextBox().apply {
                anchor = Rectangle(50, 100, 600, 350)
            }

            text.lines().take(20).forEach { line ->
                val para = contentBox.addNewTextParagraph()
                val run = para.addNewTextRun()
                run.text = line.take(120)
                run.fontSize = 16.0
            }
        }

        FileOutputStream(outputFile).use { ppt.write(it) }
        ppt.close()
        doc.close()
    }
}
