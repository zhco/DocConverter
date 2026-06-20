# Apache POI
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-keep class org.openxmlformats.schemas.** { *; }
-dontwarn org.openxmlformats.schemas.**

# PDFBox
-keep class org.apache.pdfbox.** { *; }
-dontwarn org.apache.pdfbox.**
-keep class org.apache.fontbox.** { *; }
-keep class com.sun.media.** { *; }

# Tesseract
-keep class com.googlecode.tesseract.** { *; }
-keep class cz.adaptech.tesseract4android.** { *; }
