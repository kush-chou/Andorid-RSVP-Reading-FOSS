package com.example.fossrsvp

import android.content.Context
import android.net.Uri
import com.google.ai.client.generativeai.GenerativeModel
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import kotlin.math.min

suspend fun parseMarkdownToTokens(text: String, chunkSize: Int = 1): List<RSVPToken> = withContext(Dispatchers.Default) {
    val rawTokens = mutableListOf<RSVPToken>()
    // Corrected Regex: removed redundant escape on first $ and removed redundant second $ escape
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
        else if (word.startsWith("#")) {
            word = word.trimStart('#')
            style = WordStyle.Header
        }
        else if (word.startsWith("[") && word.contains("]")) {
            val endBracket = word.indexOf(']')
            if (endBracket > 1) {
                word = word.substring(1, endBracket)
                style = WordStyle.Link
            }
        }

        var delayMultiplier = 1.0f
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

suspend fun extractTextFromUrl(url: String): String = withContext(Dispatchers.IO) {
    try {
        val doc = Jsoup.connect(url).get()
        doc.body().text()
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
