package com.docconverter.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.docconverter.app.converter.*
import com.docconverter.app.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// Conversion type enum
enum class ConvertType(val label: String, val category: String) {
    WORD_TO_PDF("Word → PDF", "一期"),
    EXCEL_TO_PDF("Excel → PDF", "一期"),
    PPT_TO_PDF("PPT → PDF", "一期"),
    PDF_TO_IMAGES("PDF 逐页转图", "一期"),
    PDF_TO_LONG_IMAGE("PDF 转长图", "一期"),
    IMAGE_TO_PDF("图片 → PDF", "一期"),
    PDF_TO_WORD("PDF → Word", "二期"),
    PDF_TO_EXCEL("PDF → Excel", "二期"),
    PDF_TO_PPT("PDF → PPT", "二期"),
    IMAGE_TO_WORD("图片 → Word (OCR)", "二期"),
    IMAGE_TO_EXCEL("图片 → Excel (OCR)", "二期")
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedType by remember { mutableStateOf<ConvertType?>(null) }
    var inputUri by remember { mutableStateOf<Uri?>(null) }
    var statusMessage by remember { mutableStateOf("选择转换类型和文件开始") }
    var isProcessing by remember { mutableStateOf(false) }
    var outputFile by remember { mutableStateOf<File?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> inputUri = uri }

    val outputDir = remember { FileUtils.getOutputDir(context) }

    // Group by category
    val phase1 = ConvertType.entries.filter { it.category == "一期" }
    val phase2 = ConvertType.entries.filter { it.category == "二期" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DocConverter") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("状态", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(statusMessage, style = MaterialTheme.typography.bodyMedium)
                        if (outputFile != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "输出: ${outputFile!!.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // File picker
            item {
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val label = if (inputUri != null) "已选择文件" else "选择输入文件"
                    Text(label)
                }
            }

            // Phase 1 header
            item {
                Text("一期 · 正向转换", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            items(phase1) { type ->
                ConversionButton(
                    type = type,
                    isSelected = selectedType == type,
                    enabled = inputUri != null && !isProcessing,
                    onClick = {
                        selectedType = type
                        executeConversion(context, type, inputUri!!, outputDir) { msg, out ->
                            statusMessage = msg
                            outputFile = out
                        }
                    }
                )
            }

            // Phase 2 header
            item {
                Text("二期 · 反向转换 & OCR", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            items(phase2) { type ->
                ConversionButton(
                    type = type,
                    isSelected = selectedType == type,
                    enabled = inputUri != null && !isProcessing,
                    onClick = {
                        selectedType = type
                        executeConversion(context, type, inputUri!!, outputDir) { msg, out ->
                            statusMessage = msg
                            outputFile = out
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ConversionButton(type: ConvertType, isSelected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary
        )
    ) {
        Text(type.label)
    }
}

private fun executeConversion(
    context: android.content.Context,
    type: ConvertType,
    uri: Uri,
    outputDir: File,
    onResult: (String, File?) -> Unit
) {
    val scope = kotlinx.coroutines.MainScope()
    scope.launch {
        withContext(Dispatchers.IO) {
            try {
                val inputFile = FileUtils.copyUriToTemp(context, uri)
                    ?: throw Exception("无法读取输入文件")

                val prefix = inputFile.nameWithoutExtension
                val outputFile: File

                when (type) {
                    ConvertType.WORD_TO_PDF -> {
                        outputFile = File(outputDir, "${prefix}_converted.pdf")
                        DocToPdfConverter.wordToPdf(inputFile, outputFile)
                    }
                    ConvertType.EXCEL_TO_PDF -> {
                        outputFile = File(outputDir, "${prefix}_converted.pdf")
                        DocToPdfConverter.excelToPdf(inputFile, outputFile)
                    }
                    ConvertType.PPT_TO_PDF -> {
                        outputFile = File(outputDir, "${prefix}_converted.pdf")
                        DocToPdfConverter.pptToPdf(inputFile, outputFile)
                    }
                    ConvertType.PDF_TO_IMAGES -> {
                        outputFile = File(outputDir, prefix)
                        outputFile.mkdirs()
                        PdfToImageConverter.pdfToImages(inputFile, outputFile)
                    }
                    ConvertType.PDF_TO_LONG_IMAGE -> {
                        outputFile = File(outputDir, "${prefix}_long.png")
                        PdfToImageConverter.pdfToLongImage(inputFile, outputFile)
                    }
                    ConvertType.IMAGE_TO_PDF -> {
                        outputFile = File(outputDir, "${prefix}_to_pdf.pdf")
                        ImageToPdfConverter.imageToPdf(inputFile, outputFile)
                    }
                    ConvertType.PDF_TO_WORD -> {
                        outputFile = File(outputDir, "${prefix}_to_word.docx")
                        PdfToDocConverter.convert(inputFile, outputFile)
                    }
                    ConvertType.PDF_TO_EXCEL -> {
                        outputFile = File(outputDir, "${prefix}_to_excel.xlsx")
                        PdfToExcelConverter.convert(inputFile, outputFile)
                    }
                    ConvertType.PDF_TO_PPT -> {
                        outputFile = File(outputDir, "${prefix}_to_ppt.pptx")
                        PdfToPptConverter.convert(inputFile, outputFile)
                    }
                    ConvertType.IMAGE_TO_WORD -> {
                        outputFile = File(outputDir, "${prefix}_ocr.docx")
                        ImageOcrConverter.imageToWord(context, inputFile, outputFile)
                    }
                    ConvertType.IMAGE_TO_EXCEL -> {
                        outputFile = File(outputDir, "${prefix}_ocr.xlsx")
                        ImageOcrConverter.imageToExcel(context, inputFile, outputFile)
                    }
                }

                withContext(Dispatchers.Main) {
                    onResult("转换完成: ${type.label}", outputFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult("错误: ${e.message}", null)
                }
            }
        }
    }
}
