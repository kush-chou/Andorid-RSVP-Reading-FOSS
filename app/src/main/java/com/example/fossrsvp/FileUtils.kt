package com.example.fossrsvp

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

suspend fun extractTextFromPdf(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            PDDocument.load(inputStream).use { document ->
                val stripper = PDFTextStripper()
                stripper.getText(document)
            }
        } ?: "Error reading file"
    } catch (e: Exception) {
        "Error: ${e.localizedMessage}"
    }
}

suspend fun extractTextFromUrl(url: String): String = withContext(Dispatchers.IO) {
    try {
        val doc = Jsoup.connect(url).get()
        doc.body().text()
    } catch (e: Exception) {
        "Error fetching URL: ${e.localizedMessage}"
    }
}

suspend fun extractTextFromEpub(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val tempFile = File(context.cacheDir, "temp_epub_${System.currentTimeMillis()}.epub")
    try {
        // 1. Copy to temp file
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext "Error reading file"

        ZipFile(tempFile).use { zip ->
            // 2. Find OPF file path from META-INF/container.xml
            val containerEntry = zip.getEntry("META-INF/container.xml")
                ?: return@withContext "Invalid EPUB: Missing META-INF/container.xml"

            val containerDoc = Jsoup.parse(zip.getInputStream(containerEntry), "UTF-8", "", Parser.xmlParser())
            val opfPath = containerDoc.select("rootfile").attr("full-path")
            if (opfPath.isBlank()) return@withContext "Invalid EPUB: No rootfile found"

            val opfEntry = zip.getEntry(opfPath) ?: return@withContext "Invalid EPUB: OPF file missing"
            val opfDoc = Jsoup.parse(zip.getInputStream(opfEntry), "UTF-8", "", Parser.xmlParser())

            // 3. Parse OPF to get reading order (spine) and file paths (manifest)
            val manifest = opfDoc.select("manifest > item").associate {
                it.attr("id") to it.attr("href")
            }

            val spine = opfDoc.select("spine > itemref").mapNotNull {
                manifest[it.attr("idref")]
            }

            // 4. Extract text from each chapter in order
            val sb = StringBuilder()
            val opfDir = File(opfPath).parent ?: "" // handle if opf is at root

            for (href in spine) {
                // Resolve relative path
                val entryPath = if (opfDir.isEmpty()) href else "$opfDir/$href"
                // Handle potential path normalization issues (simple check)
                val cleanPath = entryPath.replace("\\", "/")

                val chapterEntry = zip.getEntry(cleanPath)
                if (chapterEntry != null) {
                    val chapterDoc = Jsoup.parse(zip.getInputStream(chapterEntry), "UTF-8", "")
                    // Add a newline/space between chapters
                    sb.append(chapterDoc.body().text()).append("\n\n")
                }
            }

            if (sb.isEmpty()) "No text found in EPUB" else sb.toString()
        }
    } catch (e: Exception) {
        "Error parsing EPUB: ${e.localizedMessage}"
    } finally {
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }
}
