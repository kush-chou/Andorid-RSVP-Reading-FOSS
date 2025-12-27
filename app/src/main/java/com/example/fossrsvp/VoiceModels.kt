package com.example.fossrsvp

data class Voice(
    val id: String,
    val name: String,
    val language: String,
    val quality: String, // "Low", "Medium", "High"
    val modelUrl: String,
    val tokensUrl: String,
    val description: String
)

object VoiceRepository {
    val availableVoices = listOf(
        Voice(
            id = "en_US-amy-medium",
            name = "Amy (US, Female)",
            language = "English (US)",
            quality = "Medium",
            modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/en_US-amy-medium.onnx?download=true",
            tokensUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/amy/medium/tokens.txt?download=true",
            description = "A warm, natural female voice. Good balance of speed and quality."
        ),
        Voice(
            id = "en_US-ryan-medium",
            name = "Ryan (US, Male)",
            language = "English (US)",
            quality = "Medium",
            modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/ryan/medium/en_US-ryan-medium.onnx?download=true",
            tokensUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/ryan/medium/tokens.txt?download=true",
            description = "A clear, professionally intonated male voice."
        ),
        Voice(
            id = "en_US-lessac-medium",
            name = "Lessac (US, Female)",
            language = "English (US)",
            quality = "Medium",
            modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/en_US-lessac-medium.onnx?download=true",
            tokensUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/lessac/medium/tokens.txt?download=true",
            description = "Crisp and articulate. Often used for speed reading."
        ),
        Voice(
            id = "en_US-libritts-high",
            name = "LibriTTS (US, Multi)",
            language = "English (US)",
            quality = "High",
            modelUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/libritts/high/en_US-libritts-high.onnx?download=true",
            tokensUrl = "https://huggingface.co/rhasspy/piper-voices/resolve/main/en/en_US/libritts/high/tokens.txt?download=true",
            description = "High fidelity voice trained on audiobooks. Large download size."
        )
    )
}
