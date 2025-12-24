package com.example.fossrsvp

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
fun RSVPWordDisplay(token: RSVPToken, settings: AppSettings, maxWidthPx: Float) {
    if (token.word.isEmpty()) return

    // Remove Markdown symbols BUT KEEP PUNCTUATION
    val word = when(token.style) {
        WordStyle.Bold -> token.word.replace("**", "").replace("__", "")
        WordStyle.Italic -> token.word.replace("*", "").replace("_", "")
        WordStyle.BoldItalic -> token.word.replace("***", "")
        WordStyle.Code -> token.word // Do NOT strip from code/math
        WordStyle.Header -> token.word.replace("#", "")
        WordStyle.Link -> token.word // Links usually need more parsing, keeping as is
        else -> token.word
    }

    // Calculate pivot based on CLEANED word
    val pivotIndex = (word.length - 1) / 2

    // Safety Limit: Calculate max possible font size to fit on screen
    // Reduced factor from 0.6f to 0.5f to be safer against overlap
    val estimatedMaxFontSize = (maxWidthPx / (word.length * 0.5f)).coerceAtLeast(12f)

    // Combine user setting with safety limit
    val density = LocalDensity.current
    val userFontSizePx = with(density) { settings.fontSize.sp.toPx() }

    // Use the SMALLER of: User's desired size OR the Safety Max size
    val finalFontSizePx = min(userFontSizePx, estimatedMaxFontSize)
    val fontSizeSp = with(density) { finalFontSizePx.toSp() }

    // Style logic
    val fontWeight = when(token.style) {
        WordStyle.Bold, WordStyle.BoldItalic, WordStyle.Header -> FontWeight.Bold
        else -> FontWeight.Normal
    }
    val fontStyle = when(token.style) {
        WordStyle.Italic, WordStyle.BoldItalic -> FontStyle.Italic
        else -> FontStyle.Normal
    }
    val fontFamily = if (token.style == WordStyle.Code) FontFamily.Monospace else settings.font.fontFamily
    val color = if (token.style == WordStyle.Link) Color(0xFF64B5F6) else settings.colorScheme.text
    val textDecoration = if (token.style == WordStyle.Link) TextDecoration.Underline else TextDecoration.None

    // Fix Jitter: Enforce a constant Line Height regardless of ascenders/descenders
    val actualLineHeightSp = fontSizeSp * 1.5f
    val fixedBoxHeightDp = with(density) { (userFontSizePx * 1.5f).toDp() }

    val commonTextStyle = MaterialTheme.typography.displayLarge.copy(
        fontSize = fontSizeSp,
        fontFamily = fontFamily,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        textDecoration = textDecoration,
        lineHeight = actualLineHeightSp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (settings.focusLetterIndicator) {
            Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                drawLine(
                    color = settings.colorScheme.contextText,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 2f
                )
                drawLine(
                    color = settings.colorScheme.text,
                    start = Offset(size.width / 2, size.height - 10f),
                    end = Offset(size.width / 2, size.height),
                    strokeWidth = 4f
                )
            }
        }

        // Fixed Spacer is okay, but the Container below is crucial
        Spacer(modifier = Modifier.height(16.dp))

        // Fix Jitter: Use a Box with a FIXED height derived from the font size
        // This anchors the visual center of the text row
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(fixedBoxHeightDp), // Anchor height
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically // Center within the fixed box
            ) {
                Text(
                    text = word.take(pivotIndex),
                    style = commonTextStyle,
                    textAlign = TextAlign.End,
                    color = color,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip // Prevent painting over center
                )
                Text(
                    text = word[pivotIndex].toString(),
                    style = commonTextStyle.copy(
                        color = if (settings.highlightCenterLetter) settings.centerLetterColor else color,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = word.drop(pivotIndex + 1),
                    style = commonTextStyle,
                    textAlign = TextAlign.Start,
                    color = color,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip // Prevent painting over center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (settings.focusLetterIndicator) {
            Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                drawLine(
                    color = settings.colorScheme.contextText,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = settings.colorScheme.text,
                    start = Offset(size.width / 2, 0f),
                    end = Offset(size.width / 2, 10f),
                    strokeWidth = 4f
                )
            }
        }
    }
}
