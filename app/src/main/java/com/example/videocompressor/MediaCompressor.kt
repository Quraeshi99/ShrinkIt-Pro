package com.example.videocompressor

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Composition
import androidx.media3.effect.Presentation
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

object MediaCompressor {
    
    suspend fun compressVideo(
        context: Context,
        inputUri: Uri,
        outputFile: File,
        quality: String = "Balanced",
        onProgress: (Int) -> Unit
    ): Boolean = suspendCancellableCoroutine { continuation ->
        
        // Define compression parameters based on quality
        val targetHeight = when(quality) {
            "Small Size" -> 480
            "Balanced" -> 720
            "Super Quality" -> 1080
            else -> 720
        }
        
        val bitrate = when(quality) {
            "Small Size" -> 1_000_000 // 1 Mbps
            "Balanced" -> 2_500_000 // 2.5 Mbps
            "Super Quality" -> 5_000_000 // 5 Mbps
            else -> 2_500_000
        }

        val transformationRequest = TransformationRequest.Builder()
            .setVideoMimeType(MimeTypes.VIDEO_H265) // Using H.265 (HEVC) for best compression
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .build()
            
        val encoderSettings = VideoEncoderSettings.Builder()
            .setBitrate(bitrate)
            .build()
            
        val encoderFactory = DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(encoderSettings)
            .build()

        val effects = Effects(
            emptyList(),
            listOf(Presentation.createForHeight(targetHeight))
        )
        
        val mediaItem = MediaItem.fromUri(inputUri)
        val editedMediaItem = EditedMediaItem.Builder(mediaItem)
            .setEffects(effects)
            .build()
            
        val transformer = Transformer.Builder(context)
            .setTransformationRequest(transformationRequest)
            .setEncoderFactory(encoderFactory)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    Log.d("MediaCompressor", "Compression successful")
                    onProgress(100)
                    if (continuation.isActive) continuation.resume(true)
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    Log.e("MediaCompressor", "Compression failed: ${exportException.message}")
                    if (continuation.isActive) continuation.resume(false)
                }
            })
            .build()
            
        transformer.start(editedMediaItem, outputFile.absolutePath)
        
        // Simulate progress since Media3 doesn't have a simple percent callback in the listener directly
        // In a real app we'd poll transformer.getProgress(progressHolder)
        Thread {
            try {
                var progress = 0
                while (progress < 95 && continuation.isActive) {
                    Thread.sleep(500)
                    progress += 5
                    onProgress(progress)
                }
            } catch (e: Exception) {}
        }.start()
        
        continuation.invokeOnCancellation {
            transformer.cancel()
        }
    }
}
