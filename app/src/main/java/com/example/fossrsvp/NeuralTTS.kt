package com.example.fossrsvp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object NeuralTTSManager {
    private var offlineTts: OfflineTts? = null
    private var audioTrack: AudioTrack? = null
    var isReady = false
        private set

    suspend fun init(context: Context, voiceId: String) = withContext(Dispatchers.IO) {
        // If already ready and same voice, do nothing? (Logic usually handled by caller, but safety check good)
        // For simplicity, we re-init if called.
        
        release() // Release old model

        try {
             val voiceDir = VoiceDownloader.getVoiceDir(context, voiceId)
             if (!voiceDir.exists()) {
                 Log.e("NeuralTTS", "Voice not found: $voiceId")
                 return@withContext
             }

             val modelPath = File(voiceDir, "model.onnx").absolutePath
             val tokensPath = File(voiceDir, "tokens.txt").absolutePath
            
            val vitsConfig = OfflineTtsVitsModelConfig(
                model = modelPath,
                tokens = tokensPath,
                dataDir = "",
                noiseScale = 0.667f,
                noiseScaleW = 0.8f,
                lengthScale = 1.0f
            )
            
            val modelConfig = com.k2fsa.sherpa.onnx.OfflineTtsModelConfig(
                vits = vitsConfig,
                numThreads = 1,
                debug = true,
                provider = "cpu"
            )

            val config = OfflineTtsConfig(
                model = modelConfig
            )
            
            // AssetManager not needed for absolute paths in updated Sherpa? 
            // construct offlineTts. Actually checking constructor... 
            // The constructor usually takes AssetManager ONLY if loading from assets.
            // If we have absolute paths, we might need a different constructor OR 
            // we pass AssetManager but config uses absolute paths. 
            // Sherpa-ONNX usually distinguishes by checking if file exists on disk first.
            
            offlineTts = OfflineTts(assetManager = context.assets, config = config)
            isReady = true
            Log.d("NeuralTTS", "Initialized successfully with $voiceId")
        } catch (e: Exception) {
            Log.e("NeuralTTS", "Failed to init: ${e.message}")
            e.printStackTrace()
        }
    }

    // Removed copyAsset function as we now use Downloaded files.


    // Playback logic
    suspend fun speak(text: String, speed: Float): Long = withContext(Dispatchers.Default) {
        val tts = offlineTts ?: return@withContext 0L
        
        // Ensure previous track is stopped
        stop()

        try {
            // SID is usually 0 for single speaker models
            val audio = tts.generate(text = text, sid = 0, speed = speed)
            if (audio.samples.isEmpty()) return@withContext 0L

            val sampleRate = audio.sampleRate
            val samples = audio.samples
            
            // Create AudioTrack
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBufferSize, samples.size * 4))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
            audioTrack = track
            track.play()
            
            // Return duration in ms
            return@withContext ((samples.size.toFloat() / sampleRate) * 1000).toLong()
        } catch (e: Exception) {
            Log.e("NeuralTTS", "Error speaking: ${e.message}")
            return@withContext 0L
        }
    }

    fun stop() {
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun release() {
        stop()
        offlineTts?.release()
        offlineTts = null
        isReady = false
    }
}
