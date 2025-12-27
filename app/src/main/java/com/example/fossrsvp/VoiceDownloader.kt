package com.example.fossrsvp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object VoiceDownloader {
    private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress

    suspend fun downloadVoice(context: Context, voice: Voice) = withContext(Dispatchers.IO) {
        val voiceDir = getVoiceDir(context, voice.id)
        if (!voiceDir.exists()) voiceDir.mkdirs()

        val modelFile = File(voiceDir, "model.onnx")
        val tokensFile = File(voiceDir, "tokens.txt")

        try {
            // Signal start (0%)
            updateProgress(voice.id, 0f)

            // Download Tokens (Small)
            downloadFile(voice.tokensUrl, tokensFile)
            
            // Download Model (Large)
            downloadFile(voice.modelUrl, modelFile) { progress ->
                // Progress is 0.0 to 1.0. We map it to the UI map
                updateProgress(voice.id, progress)
            }
            
            // Done (remove from progress map to indicate completion)
            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply { remove(voice.id) }
            
        } catch (e: Exception) {
            e.printStackTrace()
            // Cleanup on failure
            modelFile.delete()
            tokensFile.delete()
            voiceDir.delete()
            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply { remove(voice.id) }
            throw e
        }
    }
    
    fun deleteVoice(context: Context, voiceId: String) {
        val voiceDir = getVoiceDir(context, voiceId)
        voiceDir.deleteRecursively()
    }
    
    fun isVoiceInstalled(context: Context, voiceId: String): Boolean {
        val voiceDir = getVoiceDir(context, voiceId)
        val model = File(voiceDir, "model.onnx")
        val tokens = File(voiceDir, "tokens.txt")
        return model.exists() && tokens.exists() && model.length() > 0
    }

    fun getVoiceDir(context: Context, voiceId: String): File {
        return File(context.filesDir, "voices/$voiceId")
    }

    private fun updateProgress(id: String, progress: Float) {
        _downloadProgress.value = _downloadProgress.value.toMutableMap().apply { put(id, progress) }
    }

    private fun downloadFile(urlStr: String, file: File, onProgress: ((Float) -> Unit)? = null) {
        URL(urlStr).openStream().use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(4 * 1024)
                var bytesRead: Int
                var totalBytesRead = 0L
                // We need content length for progress, but simple URL stream might not give it easily without connection check. 
                // For simplicity, we assume generic progress or just download.
                // To get real progress, need URLConnection.
                
                val connection = URL(urlStr).openConnection()
                val contentLength = connection.contentLengthLong
                
                connection.getInputStream().use { connInput -> 
                     // Re-open stream? No, just use the one we got if possible, or just use connection.getInputStream()
                     // Let's rely on the connection stream.
                     var inputStreamToUse = connInput
                     
                     while (inputStreamToUse.read(buffer).also { bytesRead = it } != -1) {
                         output.write(buffer, 0, bytesRead)
                         totalBytesRead += bytesRead
                         if (contentLength > 0 && onProgress != null) {
                             onProgress(totalBytesRead.toFloat() / contentLength)
                         }
                     }
                }
            }
        }
    }
}
