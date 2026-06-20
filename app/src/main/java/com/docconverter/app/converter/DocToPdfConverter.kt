package com.docconverter.app.converter

import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.converter.WordToFoConverter
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.hslf.usermodel.HSLFSlideShow
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.poi.sl.usermodel.SlideShowFactory
import java.io.*

/**
 * 文档转 PDF：Word / Excel / PPT -> PDF
 * 使用 Apache POI + PDFBox 实现
 */
object DocToPdfConverter {

    fun wordToPdf(inputFile: File, outputFile: File) {
        val ext = inputFile.extension.lowercase()
        when (ext) {
            "docx" -> docxToPdf(inputFile, outputFile)
            "doc" -> docToPdf(inputFile, outputFile)
            else -> throw IllegalArgumentException("Unsupported Word format: $ext")
        }
    }

    private fun docxToPdf(inputFile: File, outputFile: File) {
        val doc = XWPFDocument(FileInputStream(inputFile))
        val pdfDoc = PDDocument()

        // Render each paragraph as a page (simplified; for production use proper layout)
        val page = PDPage(PDRectangle.A4)
        pdfDoc.addPage(page)

        // Extract all text and write to PDF via content stream
        val text = doc.paragraphs.joinToString("\n") { it.text }
        val stream = pdfDoc.document
        val pageCount = maxOf(1, (text.length / 2000) + 1)

        // Add pages and render text
        var remaining = text
        for (i in 1..pageCount) {
            if (i > 1) pdfDoc.addPage(PDPage(PDRectangle.A4))
            val chunk = if (remaining.length > 2000) remaining.substring(0, 2000) else remaining
            remaining = remaining.removePrefix(chunk)

            val contentStream = org.apache.pdfbox.pdmodel.PDPageContentStream(
                pdfDoc, pdfDoc.getPage(i - 1),
                org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.OVERWRITE, true
            )
            contentStream.beginText()
            contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 11f)
            contentStream.newLineAtOffset(50f, 750f)
            chunk.lines().forEach { line ->
                contentStream.showText(line.take(80))
                contentStream.newLineAtOffset(0f, -14f)
            }
            contentStream.endText()
            contentStream.close()
        }

        pdfDoc.save(FileOutputStream(outputFile))
        pdfDoc.close()
        doc.close()
    }

    private fun docToPdf(inputFile: File, outputFile: File) {
        val doc = HWPFDocument(FileInputStream(inputFile))
        val pdfDoc = PDDocument()
        val page = PDPage(PDRectangle.A4)
        pdfDoc.addPage(page)

        val text = doc.range.text()
        val contentStream = org.apache.pdfbox.pdmodel.PDPageContentStream(
            pdfDoc, page,
            org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.OVERWRITE, true
        )
        contentStream.beginText()
        contentStream.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 11f)
        contentStream.newLineAtOffset(50f, 750f)
        text.lines().forEach { line ->
            contentStream.showText(line.take(80))
            contentStream.newLineAtOffset(0f, -14f)
        }
        contentStream.endText()
        contentStream.close()

        pdfDoc.save(FileOutputStream(outputFile))
        pdfDoc.close()
        doc.close()
    }

    fun excelToPdf(inputFile: File, outputFile: File) {
        val ext = inputFile.extension.lowercase()
        val workbook = when (ext) {
            "xlsx" -> XSSFWorkbook(FileInputStream(inputFile))
            "xls" -> HSSFWorkbook(FileInputStream(inputFile))
            else -> throw IllegalArgumentException("Unsupported Excel format: $ext")
        }

        val pdfDoc = PDDocument()
        val sheet = workbook.getSheetAt(0)
        val font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA
        val fontSize = 10f
        val rowHeight = 14f
        val margin = 30f
        val colWidths = mutableListOf<Float>()

        // Calculate column widths
        val rowIter = sheet.rowIterator()
        val sampleRows = mutableListOf<org.apache.poi.ss.usermodel.Row>()
        var rowCount = 0
        while (rowIter.hasNext() && rowCount < 50) {
            sampleRows.add(rowIter.next())
            rowCount++
        }
        val maxCols = sampleRows.maxOfOrNull { it.lastCellNum.toInt() } ?: 1
        for (c in 0 until maxCols) {
            val maxLen = sampleRows.maxOfOrNull { row ->
                row.getCell(c)?.toString()?.length ?: 0
            } ?: 10
            colWidths.add((maxLen * 6f + 20f).coerceIn(40f, 150f))
        }

        // Render rows
        val rowsPerPage = ((PDRectangle.A4.height - 2 * margin) / rowHeight).toInt()
        val allRows = mutableListOf<org.apache.poi.ss.usermodel.Row>()
        sheet.rowIterator().forEach { allRows.add(it) }
        val totalPages = (allRows.size + rowsPerPage - 1) / rowsPerPage

        for (pageIdx in 0 until maxOf(1, totalPages)) {
            val page = PDPage(PDRectangle.A4)
            pdfDoc.addPage(page)
            val cs = org.apache.pdfbox.pdmodel.PDPageContentStream(
                pdfDoc, page,
                org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.OVERWRITE, true
            )
            cs.setFont(font, fontSize)

            val startRow = pageIdx * rowsPerPage
            val endRow = minOf(startRow + rowsPerPage, allRows.size)

            for (r in startRow until endRow) {
                val y = PDRectangle.A4.height - margin - (r - startRow + 1) * rowHeight
                var x = margin
                val row = allRows[r]
                for (c in 0 until maxCols) {
                    val cell = row.getCell(c)
                    val text = cell?.toString()?.take(20) ?: ""
                    cs.beginText()
                    cs.newLineAtOffset(x, y)
                    cs.showText(text)
                    cs.endText()
                    x += colWidths[c]
                }
            }
            cs.close()
        }

        pdfDoc.save(FileOutputStream(outputFile))
        pdfDoc.close()
        workbook.close()
    }

    fun pptToPdf(inputFile: File, outputFile: File) {
        val ext = inputFile.extension.lowercase()
        val slides = when (ext) {
            "pptx" -> XMLSlideShow(FileInputStream(inputFile))
            "ppt" -> HSLFSlideShow(FileInputStream(inputFile)).let {
                // Convert HSLF to XMLSlideShow-like slides list
                it.slides.map { slide -> slide }
            }
            else -> throw IllegalArgumentException("Unsupported PPT format: $ext")
        }

        val pdfDoc = PDDocument()
        val font = org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA_BOLD

        if (slides is XMLSlideShow) {
            slides.slides.forEach { slide ->
                val page = PDPage(PDRectangle.A4)
                pdfDoc.addPage(page)
                val cs = org.apache.pdfbox.pdmodel.PDPageContentStream(
                    pdfDoc, page,
                    org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode.OVERWRITE, true
                )
                // Slide title
                val title = slide.title ?: "Slide"
                cs.beginText()
                cs.setFont(font, 20f)
                cs.newLineAtOffset(50f, 750f)
                cs.showText(title.take(70))
                cs.endText()

                // Slide content
                cs.setFont(org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA, 12f)
                var y = 720f
                slide.shapes.forEach { shape ->
                    val text = shape.toString().take(90)
                    cs.beginText()
                    cs.newLineAtOffset(50f, y)
                    cs.showText(text)
                    cs.endText()
                    y -= 16f
                }
                cs.close()
            }
            slides.close()
        }

        pdfDoc.save(FileOutputStream(outputFile))
        pdfDoc.close()
    }
}
