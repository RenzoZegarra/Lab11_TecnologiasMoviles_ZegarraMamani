package com.renzo.barcodescannerapp

import android.Manifest

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import com.renzo.bardcodescannerapp.R

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultText: TextView

    private val cameraExecutor =
        Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        previewView =
            findViewById(R.id.previewView)

        resultText =
            findViewById(R.id.resultText)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            startCamera()

        } else {

            requestPermission.launch(
                Manifest.permission.CAMERA
            )

        }
    }

    private val requestPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                startCamera()
            }

        }

    private fun startCamera() {

        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({

            val cameraProvider =
                cameraProviderFuture.get()

            val preview =
                Preview.Builder().build()

            preview.surfaceProvider =
                previewView.surfaceProvider

            val imageAnalyzer =
                ImageAnalysis.Builder()
                    .build()
                    .also {

                        it.setAnalyzer(
                            cameraExecutor
                        ) { imageProxy ->

                            scanBarcode(imageProxy)

                        }

                    }

            val cameraSelector =
                CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()

            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun scanBarcode(
        imageProxy: ImageProxy
    ) {

        val mediaImage =
            imageProxy.image

        if (mediaImage != null) {

            val image =
                InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

            val scanner =
                BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->

                    for (barcode in barcodes) {

                        resultText.text =
                            barcode.rawValue ?: "Sin datos"

                    }

                }
                .addOnFailureListener {

                    resultText.text =
                        "Error al leer código"

                }
                .addOnCompleteListener {

                    imageProxy.close()

                }

        } else {

            imageProxy.close()

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}