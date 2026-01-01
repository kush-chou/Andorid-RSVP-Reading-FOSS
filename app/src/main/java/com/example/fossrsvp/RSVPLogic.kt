package com.example.fossrsvp

import android.content.Context
import android.net.Uri
import com.google.ai.client.generativeai.GenerativeModel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.BufferedInputStream
import java.io.File
import java.util.Locale
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import kotlin.math.min

// --- Logic ---

suspend fun parseMarkdownToTokens(text: String, chunkSize: Int = 1): List<RSVPToken> = withContext(Dispatchers.Default) {
    val rawTokens = mutableListOf<RSVPToken>()
    // Punctuation characters to split by, but keep attached if inside word logic is simpler
    // We split by whitespace primarily
    val regex = Regex("""(\$[^$]+\$)|(\S+)""")
    val matches = regex.findAll(text)

    for (match in matches) {
        var word = match.value
        var style = WordStyle.Normal

        if (word.startsWith("$") && word.endsWith("$")) {
            style = WordStyle.Code
        }
        else if (word.contains("***") && word.length > 6 && (word.length - word.replace("***", "").length == 6)) {
            word = word.replace("***", "")
            style = WordStyle.BoldItalic
        }
        else if (word.contains("**") && word.length > 4 && (word.length - word.replace("**", "").length == 4)) {
            word = word.replace("**", "")
            style = WordStyle.Bold
        }
        else if (word.contains("__") && word.length > 4 && (word.length - word.replace("__", "").length == 4)) {
            word = word.replace("__", "")
            style = WordStyle.Bold
        }
        else if (word.contains("*") && word.length > 2 && (word.length - word.replace("*", "").length == 2)) {
            word = word.replace("*", "")
            style = WordStyle.Italic
        }
        else if (word.contains("_") && word.length > 2 && (word.length - word.replace("_", "").length == 2)) {
            word = word.replace("_", "")
            style = WordStyle.Italic
        }
        else if (word.contains("`") && word.length > 2 && (word.length - word.replace("`", "").length == 2)) {
            word = word.replace("`", "")
            style = WordStyle.Code
        }
        else if (word.startsWith("[[IMG:") && word.endsWith("]]")) {
            val url = word.removePrefix("[[IMG:").removeSuffix("]]")
            rawTokens.add(RSVPToken(word = "[IMAGE]", type = TokenType.Image, imageUrl = url, delayMultiplier = 0f)) // Zero delay, logic will pause
            continue
        }
        else if (word.startsWith("#")) {
            word = word.trimStart('#')
            style = WordStyle.Header
        }
        var delayMultiplier = 1.0f
        
        if (word.startsWith("[") && word.contains("]")) {
            val endBracket = word.indexOf(']')
            if (endBracket > 1) {
                word = word.substring(1, endBracket)
                style = WordStyle.Link
            }
        }
        else if (word.matches(Regex("^\\d+\\.\$")) || word == "-" || word == "*") {
             style = WordStyle.Header // Use Header style for visual emphasis on list markers
             delayMultiplier = 2.0f // Pause on list markers
        }
        if (word.endsWith(".") || word.endsWith("!") || word.endsWith("?")) {
            delayMultiplier = 2.0f
        } else if (word.endsWith(",") || word.endsWith(";") || word.endsWith(":")) {
            delayMultiplier = 1.5f
        }
        if (word.length > 10 || style == WordStyle.Code) {
            delayMultiplier += 0.5f
        }

        if (word.isNotEmpty()) {
            rawTokens.add(RSVPToken(word, style, delayMultiplier))
        }
    }

    if (chunkSize > 1) {
        val chunkedTokens = mutableListOf<RSVPToken>()
        var i = 0
        while (i < rawTokens.size) {
            val chunk = mutableListOf<String>()
            var maxDelay = 0f
            var style = WordStyle.Normal
            
            val count = min(chunkSize, rawTokens.size - i)
            for (j in 0 until count) {
                val t = rawTokens[i + j]
                chunk.add(t.word)
                if (t.delayMultiplier > maxDelay) maxDelay = t.delayMultiplier
                if (t.style != WordStyle.Normal) style = t.style
            }
            
            chunkedTokens.add(RSVPToken(
                word = chunk.joinToString(" "),
                style = style,
                delayMultiplier = maxDelay
            ))
            i += count
        }
        chunkedTokens
    } else {
        rawTokens
    }
}

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

suspend fun extractTextFromEpub(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val tempFile = File(context.cacheDir, "temp_book.epub")
    try {
        // Copy to temp file
        context.contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext "Error reading EPUB stream."

        val zipFile = ZipFile(tempFile)

        // 1. Find OPF Path from container.xml
        val containerEntry = zipFile.getEntry("META-INF/container.xml")
        val opfPath = if (containerEntry != null) {
            val containerXml = zipFile.getInputStream(containerEntry).bufferedReader().use { it.readText() }
            val doc = Jsoup.parse(containerXml, "", org.jsoup.parser.Parser.xmlParser())
            doc.select("rootfile").attr("full-path")
        } else {
            null
        }

        if (opfPath == null) {
            zipFile.close()
            // Fallback to naive iteration if container.xml is missing (rare but possible)
            return@withContext extractTextFromEpubNaive(context, uri)
        }

        // 2. Parse OPF
        val opfEntry = zipFile.getEntry(opfPath)
        if (opfEntry == null) {
            zipFile.close()
            return@withContext "Invalid EPUB: OPF file not found at $opfPath"
        }

        val opfContent = zipFile.getInputStream(opfEntry).bufferedReader().use { it.readText() }
        val opfDoc = Jsoup.parse(opfContent, "", org.jsoup.parser.Parser.xmlParser())

        // 3. Get Manifest (id -> href)
        val manifest = mutableMapOf<String, String>()
        val manifestItems = opfDoc.select("manifest > item")
        for (item in manifestItems) {
            val id = item.attr("id")
            val href = item.attr("href")
            manifest[id] = href
        }

        // 4. Get Spine (ordered ids)
        val spineIds = opfDoc.select("spine > itemref").map { it.attr("idref") }

        // 5. Build Content
        val sb = StringBuilder()
        val opfDir = File(opfPath).parent ?: ""

        for (id in spineIds) {
            val href = manifest[id] ?: continue
            // Resolve relative path
            val fullPath = if (opfDir.isEmpty()) href else "$opfDir/$href"

            // Handle URL decoding if necessary (ZipEntry names are usually safe but href might be percent encoded)
            val entry = zipFile.getEntry(fullPath) ?: zipFile.getEntry(java.net.URLDecoder.decode(fullPath, "UTF-8"))

            if (entry != null) {
                val html = zipFile.getInputStream(entry).bufferedReader().use { it.readText() }
                // Parse and extract text
                val doc = Jsoup.parse(html)

                // Add newlines around block elements to preserve structure for parsing logic
                doc.select("br").append("\\n")
                doc.select("p, div, h1, h2, h3, h4, h5, h6, li").prepend("\\n\\n")

                // Clean the text
                val text = doc.text().replace("\\n", "\n")
                sb.append(text).append("\n\n")
            }
        }

        zipFile.close()
        tempFile.delete()

        sb.toString().ifBlank { "Could not extract text from EPUB." }

    } catch (e: Exception) {
        tempFile.delete()
        "Error parsing EPUB: ${e.localizedMessage}"
    }
}

// Keep the old method as a fallback
suspend fun extractTextFromEpubNaive(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    try {
        val stringBuilder = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val zipInputStream = ZipInputStream(BufferedInputStream(inputStream))
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name.endsWith(".html") || entry.name.endsWith(".xhtml")) {
                    val bytes = zipInputStream.readBytes()
                    val htmlContent = String(bytes, Charsets.UTF_8)
                    // Parse HTML fragment
                    val doc = Jsoup.parse(htmlContent)
                    stringBuilder.append(doc.body().text()).append("\n\n")
                }
                entry = zipInputStream.nextEntry
            }
        }
        stringBuilder.toString().ifBlank { "Could not extract text from EPUB." }
    } catch (e: Exception) {
        "Error reading EPUB: ${e.localizedMessage}"
    }
}

suspend fun extractTextFromUrl(url: String): String = withContext(Dispatchers.IO) {
    try {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
            .get()
            
        // Smart Cleaning Algorithm
        
        // 1. Remove obvious clutter but KEEP images
        doc.select("script, style, nav, footer, header, aside, iframe, noscript").remove()
        
        // 2. Identify main content container
        // Heuristic: Look for long paragraphs <p> OR images inside content divs
        val contentElements = doc.select("p, img")
        val contentBuilder = StringBuilder()
        
        for (element in contentElements) {
             if (element.tagName() == "img") {
                 val src = element.absUrl("src")
                 val alt = element.attr("alt").lowercase()
                 val width = element.attr("width").toIntOrNull() ?: 999
                 val height = element.attr("height").toIntOrNull() ?: 999
                 
                 // Smart Ad/Icon Filtering
                 val isIcon = width < 50 || height < 50
                 val isAdKeyword = src.contains("doubleclick") || src.contains("adserver") || src.contains("banner") || 
                                  src.contains("pixel") || src.contains("tracker") || src.contains("shim.gif")
                 val isExplicitAd = alt.contains("sponsored") || alt.contains("advertisement")
                 
                 if (src.isNotBlank() && !isIcon && !isAdKeyword && !isExplicitAd) {
                     // Add special token for Image
                     contentBuilder.append("\n\n[[IMG:$src]]\n\n")
                 }
             } else {
                 val text = element.text().trim()
                 if (text.length > 40) { // arbitrary threshold for "meaningful sentence"
                     contentBuilder.append(text).append("\n\n")
                 }
             }
        }
        
        val cleanedText = contentBuilder.toString()
        
        if (cleanedText.length < 200) {
            // Fallback: If heuristic failed, look for images in body too
             val bodyText = doc.body().text()
             // Simple append of images found in body
             val bodyImages = doc.select("img")
             val imgBuilder = StringBuilder()
             for(img in bodyImages) {
                 val src = img.absUrl("src")
                 if(src.isNotBlank()) imgBuilder.append("\n[[IMG:$src]]\n")
             }
             bodyText + "\n" + imgBuilder.toString()
        } else {
            cleanedText
        }
    } catch (e: Exception) {
        "Error fetching URL: ${e.localizedMessage}"
    }
}

suspend fun generateTextWithGemini(apiKey: String, prompt: String, preset: String, modelName: String): String = withContext(Dispatchers.IO) {
    try {
        val model = GenerativeModel(modelName, apiKey)
        val systemContext = "You are an AI assistant embedded within a Speed Reading application (RSVP - Rapid Serial Visual Presentation). " +
                "The user wants to read the text you generate using this method, which displays one word at a time at high speed. " +
                "Therefore, please structure your response to be reader-friendly and linear, similar to a newspaper article or a clean essay. " +
                "Avoid complex formatting like tables, excessive bullet points, or ascii art that would break the flow. " +
                "Use standard paragraphs. " +
                "Here is the user's prompt:"

        val fullPrompt = if (preset.isNotBlank()) {
            "$systemContext\n\n[User Custom Preset Instruction]: $preset\n\n[User Prompt]: $prompt"
        } else {
            "$systemContext\n\n$prompt"
        }

        val response = model.generateContent(fullPrompt)
        response.text ?: "No response generated."
    } catch (e: Exception) {
        "Gemini Error: ${e.localizedMessage}"
    }
}

suspend fun generateQuiz(apiKey: String, textContext: String, modelName: String): Quiz? = withContext(Dispatchers.IO) {
    try {
        val model = GenerativeModel(modelName, apiKey)
        val prompt = """
            Based on the following text, generate a quiz with 3 to 5 multiple-choice questions.
            Return ONLY the JSON object with this structure:
            {
              "questions": [
                {
                  "question": "Question text here?",
                  "options": ["Option A", "Option B", "Option C", "Option D"],
                  "correctOptionIndex": 0 // Index (0-3) of the correct option
                }
              ]
            }
            Do not include any markdown formatting (like ```json), just the raw JSON string.

            Text:
            ${textContext.take(15000)} // Limit context to avoid token limits
        """.trimIndent()

        val response = model.generateContent(prompt)
        val jsonString = response.text?.trim()?.removePrefix("```json")?.removePrefix("```")?.removeSuffix("```")?.trim() ?: return@withContext null

        val gson = com.google.gson.Gson()
        gson.fromJson(jsonString, Quiz::class.java)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
