package com.docconverter.app.converter

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import java.io.*

/**
 * PDF 转 Word (DOCX)
 * 使用 PDFBox 提取文本 + POI 重建 DOCX
 * 标注：基础模式，保留文本内容，复杂排版会丢失
 */
object PdfToDocConverter {

    fun convert(pdfFile: File, outputFile: File) {
        val doc = PDDocument.load(pdfFile, MemoryUsageSetting.setupTempFileOnly())
        val wordDoc = XWPFDocument()

        val stripper = org.apache.pdfbox.text.PDFTextStripper()
        stripper.sortByPosition = true

        for (i in 1..doc.numberOfPages) {
            stripper.startPage = i
            stripper.endPage = i
            val pageText = stripper.getText(doc)

            if (pageText.isNotBlank()) {
                // Page header
                val headerPara: XWPFParagraph = wordDoc.createParagraph()
                val headerRun: XWPFRun = headerPara.createRun()
                headerRun.isBold = true
                headerRun.fontSize = 14
                headerRun.setText("--- Page $i ---")

                pageText.lines().forEach { line ->
                    val para: XWPFParagraph = wordDoc.createParagraph()
                    val run: XWPFRun = para.createRun()
                    run.setText(line.trim())
                }
                // Blank line between pages
                wordDoc.createParagraph()
            }
        }

        FileOutputStream(outputFile).use { wordDoc.write(it) }
        wordDoc.close()
        doc.close()
    }
}
