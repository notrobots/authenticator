package dev.notrobots.authenticator.ui.barcode

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import dev.notrobots.authenticator.App
import dev.notrobots.authenticator.activities.ThemedActivity
import kotlinx.android.synthetic.main.activity_barcode_scanner.*
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class BarcodeScannerActivity : ThemedActivity(), ImageAnalysis.Analyzer {
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraSelector: CameraSelector? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var barcodeScanner: BarcodeScanner? = null
    private val viewModel by viewModels<BarcodeScannerViewModel>()
    private val screenAspectRatio: Int
        get() {
            val metrics = DisplayMetrics().also { camera_preview.display.getRealMetrics(it) }

            return aspectRatio(metrics.widthPixels, metrics.heightPixels)
        }

    override fun onResume() {
        super.onResume()
        setFullscreen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullscreen()
        setContentView(dev.notrobots.authenticator.R.layout.activity_barcode_scanner)

        //TODO: Ask permissions

        setupCamera()
    }

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)
        val task = barcodeScanner?.process(inputImage)

        if (task != null) {
            task.addOnSuccessListener {
                if (it.size >= 1) {
                    val data = Intent().apply {
                        putExtra(EXTRA_QR_DATA, it.first().rawValue)
                    }

                    setResult(Activity.RESULT_OK, data)
                    finish()
                }
            }
            task.addOnFailureListener {
                Log.e(App.TAG, it.message.toString())
            }
            task.addOnCompleteListener {
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                image.close()
            }
        } else {
            TODO("Task is null")
        }
    }

    private fun setupCamera() {
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        viewModel.processCameraProvider.observe(this) {
            cameraProvider = it

            //TODO: Ask permissions

            bindPreview()
            bindImageAnalysis()
        }
    }

    private fun bindPreview() {
        if (cameraProvider == null) {
            return
        }
        if (previewUseCase != null) {
            cameraProvider!!.unbind(previewUseCase)
        }

        previewUseCase = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(camera_preview.display.rotation)
            .build()
        previewUseCase!!.setSurfaceProvider(camera_preview.surfaceProvider)

        try {
            cameraProvider!!.bindToLifecycle(this, cameraSelector!!, previewUseCase)
        } catch (e: Exception) {
            Log.e(App.TAG, e.message.toString())
        }
    }

    private fun bindImageAnalysis() {
        if (cameraProvider == null) {
            return
        }
        if (imageAnalysis != null) {
            cameraProvider!!.unbind(imageAnalysis)
        }

        val cameraExecutor = Executors.newSingleThreadExecutor()
        val scannerOptions = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC
            )
            .build()
        barcodeScanner = BarcodeScanning.getClient(scannerOptions)
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(camera_preview!!.display.rotation)
            .build()
        imageAnalysis!!.setAnalyzer(cameraExecutor, this)

        try {
            cameraProvider!!.bindToLifecycle(this, cameraSelector!!, imageAnalysis)
        } catch (e: Exception) {
            Log.e(App.TAG, e.message.toString())
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    companion object {
        private const val PERMISSION_CAMERA_REQUEST = 1000
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        const val EXTRA_QR_DATA = "BarcodeScannerActivity.QR_DATA"
    }
}