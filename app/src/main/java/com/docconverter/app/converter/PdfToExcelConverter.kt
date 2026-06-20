package com.docconverter.app.converter

import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.io.MemoryUsageSetting
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*

/**
 * PDF 转 Excel (XLSX)
 * 使用 PDFBox 提取文本 + POI 重建 XLSX
 * 尝试智能识别表格结构
 */
object PdfToExcelConverter {

    fun convert(pdfFile: File, outputFile: File) {
        val doc = PDDocument.load(pdfFile, MemoryUsageSetting.setupTempFileOnly())
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("PDF Content")

        val stripper = org.apache.pdfbox.text.PDFTextStripper()
        stripper.sortByPosition = true

        var rowIdx = 0
        val headerStyle = workbook.createCellStyle()
        val headerFont = workbook.createFont()
        headerFont.bold = true
        headerStyle.setFont(headerFont)

        for (pageNum in 1..doc.numberOfPages) {
            stripper.startPage = pageNum
            stripper.endPage = pageNum
            val text = stripper.getText(doc)

            if (pageNum > 1) { rowIdx++; sheet.createRow(rowIdx) } // blank separator

            text.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty()) return@forEach

                val row = sheet.createRow(rowIdx++)
                // Try to split by multiple spaces (crude table detection)
                val cells = trimmed.split(Regex("\\s{2,}"))
                cells.forEachIndexed { colIdx, value ->
                    val cell = row.createCell(colIdx)
                    cell.setCellValue(value.trim())
                }
            }
        }

        // Auto-size columns (approximate)
        for (col in 0..<20) {
            sheet.autoSizeColumn(col)
        }

        FileOutputStream(outputFile).use { workbook.write(it) }
        workbook.close()
        doc.close()
    }
}
