package com.example.fossrsvp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class WordStyle {
    Normal, Bold, Italic, BoldItalic, Code, Header, Link
}

data class RSVPToken(
    val word: String,
    val style: WordStyle = WordStyle.Normal,
    val delayMultiplier: Float = 1.0f
)

data class AppSettings(
    val colorScheme: ColorSchemeOption = ColorSchemeOption.WhiteOnBlack,
    val font: FontOption = FontOption.Bookerly,
    val showContext: ContextDisplayOption = ContextDisplayOption.Always,
    val contextStyle: ContextStyleOption = ContextStyleOption.MultiLine,
    val contextOpacity: Float = 0.5f,
    val highlightCenterLetter: Boolean = true,
    val focusLetterIndicator: Boolean = true,
    val centerLetterColor: Color = Color(0xFFFF9800),
    val wpmStep: Int = 25,
    val autoStartSeconds: Int = 2,
    val wpm: Float = 300f,
    val fontSize: Int = 20,
    val promptPreset: String = "",
    val chunkSize: Int = 1, // 1 = Single word, 2 = Pair, 3 = Triplet
    val voiceName: String = "",
    val aiModel: String = "gemini-flash-latest",
    val geminiApiKey: String = "",
    val savedPresets: List<Pair<String, String>> = listOf(
        "Summarize" to "Summarize the following text in a concise manner:",
        "Explain Like I'm 5" to "Explain the following concept simply, as if to a 5-year-old:",
        "Key Points" to "Extract the main key points from the following text:"
    )
)

enum class ColorSchemeOption(val displayName: String, val background: Color, val text: Color, val contextText: Color) {
    WhiteOnBlack("White on Black", Color(0xFF222222), Color.White, Color.Gray),
    BlackOnWhite("Black on White", Color.White, Color.Black, Color.DarkGray),
    Beige("Beige", Color(0xFFF5F5DC), Color(0xFF5D4037), Color(0xFFA1887F)),
    BlackOnBlue("Black on Blue", Color(0xFFBBDEFB), Color.Black, Color.DarkGray),
    BlackOnYellow("Black on Yellow", Color(0xFFFFF9C4), Color.Black, Color.DarkGray)
}

enum class FontOption(val displayName: String, val fontFamily: FontFamily) {
    Georgia("Georgia", FontFamily.Serif),
    Arial("Arial", FontFamily.SansSerif),
    Bookerly("Bookerly", FontFamily.Serif),
    OpenSans("Open Sans", FontFamily.SansSerif),
    HelveticaNeue("Helvetica Neue", FontFamily.SansSerif),
    Garamond("Garamond", FontFamily.Serif),
    Minion("Minion", FontFamily.Serif),
    Merriweather("Merriweather", FontFamily.Serif),
    Dyslexie("Dyslexie", FontFamily.Monospace),
    Tisa("Tisa", FontFamily.Serif)
}

enum class ContextDisplayOption(val displayName: String) {
    Always("Always"),
    Paused("Only show when paused"),
    Never("Never")
}

enum class ContextStyleOption(val displayName: String) {
    CurrentLine("Current line only"),
    MultiLine("Multi-line")
}
