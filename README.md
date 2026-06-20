# DocConverter

一站式文件格式转换 Android 应用，支持 11 种转换模式。

## 功能

### 一期：正向转换
- Word → PDF
- Excel → PDF
- PPT → PDF
- PDF 逐页转图
- PDF 转长图
- 图片 → PDF

### 二期：反向转换 & OCR
- PDF → Word
- PDF → Excel
- PDF → PPT
- 图片 → Word (OCR)
- 图片 → Excel (OCR)

## 技术栈

| 组件 | 用途 |
|------|------|
| Apache POI | Word/Excel/PPT 读写 |
| PDFBox | PDF 解析与渲染 |
| Tesseract4Android | OCR 文字识别 |
| Jetpack Compose | UI |

## 构建

```bash
./gradlew assembleDebug
```

GitHub Actions 自动构建，每次 push 到 main 分支触发。
