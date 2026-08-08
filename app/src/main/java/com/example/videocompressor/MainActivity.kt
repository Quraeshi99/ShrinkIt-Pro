package com.example.videocompressor

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppScreen()
                }
            }
        }
    }
}

@Composable
fun AppScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isCompressing by remember { mutableStateOf(false) }
    var compressionProgress by remember { mutableStateOf(0) }
    var currentCompressingIndex by remember { mutableStateOf(0) }
    var quality by remember { mutableStateOf("Balanced") }

    val photoVideoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris = uris
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ShrinkIt Pro",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "Advanced Video Compressor",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { 
                photoVideoPickerLauncher.launch(
                    androidx.activity.result.PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.VideoOnly
                    )
                ) 
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE)),
            enabled = !isCompressing
        ) {
            Text("Select Videos", color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Quality Settings", color = Color.White, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                listOf("Small Size", "Balanced", "Super Quality").forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = (quality == option),
                            onClick = { quality = option },
                            enabled = !isCompressing
                        )
                        Text(option, color = Color.LightGray)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (selectedUris.isNotEmpty()) {
            Text(
                "Selected Files: ${selectedUris.size}", 
                color = Color.White,
                modifier = Modifier.align(Alignment.Start)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isCompressing) {
                Text(
                    text = "Compressing video ${currentCompressingIndex + 1} of ${selectedUris.size}...",
                    color = Color.Yellow
                )
                LinearProgressIndicator(
                    progress = compressionProgress / 100f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = Color(0xFF03DAC5)
                )
                Text(
                    text = "$compressionProgress%",
                    color = Color.White
                )
            } else {
                Button(
                    onClick = {
                        isCompressing = true
                        coroutineScope.launch {
                            val shrinkItFolder = File(
                                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                                "ShrinkIt"
                            )
                            if (!shrinkItFolder.exists()) shrinkItFolder.mkdirs()

                            for (i in selectedUris.indices) {
                                currentCompressingIndex = i
                                compressionProgress = 0
                                
                                val uri = selectedUris[i]
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                                val outputFile = File(shrinkItFolder, "ShrinkIt_VID_${timeStamp}_$i.mp4")
                                
                                val success = MediaCompressor.compressVideo(
                                    context = context,
                                    inputUri = uri,
                                    outputFile = outputFile,
                                    quality = quality,
                                    onProgress = { prog -> compressionProgress = prog }
                                )
                                
                                if (!success) {
                                    Toast.makeText(context, "Failed to compress video ${i+1}", Toast.LENGTH_SHORT).show()
                                }
                            }
                            
                            isCompressing = false
                            compressionProgress = 0
                            Toast.makeText(context, "All Compression Finished!\nSaved in Movies/ShrinkIt", Toast.LENGTH_LONG).show()
                            selectedUris = emptyList() // clear list
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5))
                ) {
                    Text("Start Compression", color = Color.Black)
                }
            }
        }
    }
}
