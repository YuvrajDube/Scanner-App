package com.example.qrcodescanner


import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private val scanner: BarcodeScanner = BarcodeScanning.getClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            var showSplashScreen by remember { mutableStateOf(true) }

            if (showSplashScreen) {
                SplashScreen {
                    showSplashScreen = false
                }
            } else
            QRCodeScanner(
                        onBarcodeScanned = { barcode ->
                            val data = barcode.rawValue
                            if (data != null) {
                                if (data.startsWith("http")) {
                                    // If the data is a URL, open it in Chrome
                                    openInChrome(data)
                                } else {
                                    // Copy the data to clipboard
                                    copyToClipboard(data)
                                    Toast.makeText(this, "Copied to clipboard :- ${barcode.rawValue}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
        }
    }

    private fun openInChrome(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            // Specify Chrome as the browser
            setPackage("com.android.chrome")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // If Chrome is not installed, fallback to default browser
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(fallbackIntent)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("QR Code", text)
        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun QRCodeScanner(onBarcodeScanned: (Barcode) -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    Scaffold(
        topBar = { CustomTopAppBar() },
        content = {  }
    )

    if (hasCameraPermission) {
        Column(modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("HERE IS YOUR SCANNER:-")
            Spacer(modifier = Modifier.height(40.dp))
            CameraPreview(onBarcodeScanned = onBarcodeScanned)
            Spacer(modifier = Modifier.height(30.dp))
            Text("TOAST HERE:-")

        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {



            Text("Camera permission is required to scan QR codes.")
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }) {
                Text("Grant Camera Permission")
            }
        }
    }
}

@Composable
fun CameraPreview(onBarcodeScanned: (Barcode) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(

        modifier = Modifier
            .fillMaxSize(0.5f),

        factory = { AndroidViewContext ->
            PreviewView(AndroidViewContext).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(surfaceProvider)
                    }

                    val imageAnalyzer = ImageAnalysis.Builder()
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor, BarcodeAnalyzer(onBarcodeScanned))
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner, cameraSelector, preview, imageAnalyzer
                        )
                    } catch (e: Exception) {
                        Log.e("QRCodeScanner", "Camera initialization failed.", e)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        }



    )
}

class BarcodeAnalyzer(private val onBarcodeScanned: (Barcode) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner: BarcodeScanner = BarcodeScanning.getClient()

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.forEach { barcode ->
                        onBarcodeScanned(barcode)
                    }
                }
                .addOnFailureListener {
                    Log.e("QRCodeScanner", "Barcode detection failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            onTimeout()
        }, 2000)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.splashscreen),
            contentDescription = "Splash Screen",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}


@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTopAppBar() {
    TopAppBar(
        title = {
            Text(
                text = "SCAN",
                fontSize = 25.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = { /* Left icon action */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.vector),
                    contentDescription = "3 lines",
                    tint = White

                )
            }
        },
        actions = {
            IconButton(onClick = { /* First action */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.dot),
                    contentDescription = "3 dots"
                )
            }

        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF2E56D0),
        )
    )
}








