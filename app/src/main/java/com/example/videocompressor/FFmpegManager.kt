package com.example.videocompressor

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object FFmpegManager {

    /**
     * Compresses a video using H.265 (HEVC) with Constant Rate Factor (CRF)
     * @param inputPath: Path to the original video
     * @param outputPath: Path to save the temporary compressed video
     * @param crfValue: Quality parameter. 18 = Super High, 23 = Balanced, 28 = Small Size
     * @param onProgress: Callback for progress (can be calculated based on duration later)
     */
    suspend fun compressVideo(
        inputPath: String,
        outputPath: String,
        crfValue: Int = 23,
        onProgress: (Int) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        
        // Remove output file if exists
        val outFile = File(outputPath)
        if (outFile.exists()) outFile.delete()

        // Magic FFmpeg command for size reduction without quality loss
        // -c:v libx265 (HEVC) is used for high compression efficiency
        // -crf (Constant Rate Factor) determines the quality
        // -preset medium ensures a good balance between compression speed and file size
        // -c:a aac compresses audio with good quality
        val command = "-i \"$inputPath\" -c:v libx265 -crf $crfValue -preset medium -c:a aac -b:a 128k \"$outputPath\""

        val session: FFmpegSession = FFmpegKit.execute(command)

        return@withContext ReturnCode.isSuccess(session.returnCode)
    }

    /**
     * Compress photo using WebP lossless/lossy compression via FFmpeg
     */
    suspend fun compressPhoto(
        inputPath: String,
        outputPath: String,
        quality: Int = 80 // WebP quality 0-100
    ): Boolean = withContext(Dispatchers.IO) {
        val outFile = File(outputPath)
        if (outFile.exists()) outFile.delete()

        // FFmpeg command to convert to WebP
        val command = "-i \"$inputPath\" -vcodec libwebp -qscale $quality \"$outputPath\""
        val session: FFmpegSession = FFmpegKit.execute(command)

        return@withContext ReturnCode.isSuccess(session.returnCode)
    }
}
