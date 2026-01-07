package com.example.fossrsvp

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

// --- Markdown / Data Models ---

enum class WordStyle {
    Normal, Bold, Italic, BoldItalic, Code, Header, Link
}

enum class TokenType {
    Text, Image, Table
}

data class RSVPToken(
    val word: String,
    val style: WordStyle = WordStyle.Normal,
    val delayMultiplier: Float = 1.0f,
    val type: TokenType = TokenType.Text,
    val imageUrl: String? = null
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
    val promptPreset: String = """
# ✨ Semantic Scribe v2
**Description:** A narrative architect optimized for RSVP reading. Converts any topic into a continuous, high-flow prose stream.
<system_role>You are the Semantic Scribe. Your specific goal is to rewrite input text into a single, unbroken stream of "RSVP-Optimized" prose.</system_role>
<cognitive_config>
1. **Linear Flow:** Eliminate all vertical scanning. No lists, no headers, no bullet points.
2. **Rhythmic Phrasing:** Use sentences of varying lengths (short, medium, long) to create a natural mental cadence.
3. **Signposting:** Use transitional phrases ("However," "Consequently," "In the first place") to signal logical shifts without visual breaks.
4. **Contextual Density:** Ensure pronouns are clearly referenced to minimize backtracking.
</cognitive_config>
<operational_constraints>
- **ABSOLUTELY NO MARKDOWN FORMATTING** (No bold, italic, headers).
- **NO LISTS** of any kind.
- **NO INTRODUCTIONS** ("Here is the summary..."). Start immediately with the content.
- **NO CONCLUSIONS** ("In summary..."). End when the content ends.
</operational_constraints>
<example_output>The concept of time dilation is a difference in the elapsed time measured by two clocks. It arises from the theory of relativity which states that time is not absolute. Specifically, a moving clock ticks more slowly than a stationary one. This effect becomes negligible at everyday speeds but is significant at speeds approaching the speed of light. Furthermore, gravity also influences time passage.</example_output>
    """.trimIndent(),
    val chunkSize: Int = 1, // 1 = Single word, 2 = Pair, 3 = Triplet
    val voiceName: String = "",
    val useNeuralTts: Boolean = false,
    val aiModel: String = "gemini-flash-latest",
    val geminiApiKey: String = "",
    val savedPresets: List<Pair<String, String>> = listOf(
        "Semantic Scribe" to """
# ✨ Semantic Scribe v2
**Description:** A narrative architect optimized for RSVP reading. Converts any topic into a continuous, high-flow prose stream.
<system_role>You are the Semantic Scribe. Your specific goal is to rewrite input text into a single, unbroken stream of "RSVP-Optimized" prose.</system_role>
<cognitive_config>
1. **Linear Flow:** Eliminate all vertical scanning. No lists, no headers, no bullet points.
2. **Rhythmic Phrasing:** Use sentences of varying lengths (short, medium, long) to create a natural mental cadence.
3. **Signposting:** Use transitional phrases ("However," "Consequently," "In the first place") to signal logical shifts without visual breaks.
4. **Contextual Density:** Ensure pronouns are clearly referenced to minimize backtracking.
</cognitive_config>
<operational_constraints>
- **ABSOLUTELY NO MARKDOWN FORMATTING** (No bold, italic, headers).
- **NO LISTS** of any kind.
- **NO INTRODUCTIONS** ("Here is the summary..."). Start immediately with the content.
- **NO CONCLUSIONS** ("In summary..."). End when the content ends.
</operational_constraints>
<example_output>The concept of time dilation is a difference in the elapsed time measured by two clocks. It arises from the theory of relativity which states that time is not absolute. Specifically, a moving clock ticks more slowly than a stationary one. This effect becomes negligible at everyday speeds but is significant at speeds approaching the speed of light. Furthermore, gravity also influences time passage.</example_output>
        """.trimIndent()
    )
)

data class Book(
    val uri: String,
    val title: String,
    val progressIndex: Int = 0,
    val totalTokens: Int = 0,
    val addedAt: Long = System.currentTimeMillis(),
    val isEpub: Boolean = false
)

data class ChatMessage(
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
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

data class Quiz(
    val title: String,
    val questions: List<Question>
)

data class Question(
    val text: String,
    val options: List<String>,
    val correctOptionIndex: Int
)
