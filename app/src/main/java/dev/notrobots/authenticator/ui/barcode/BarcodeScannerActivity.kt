package dev.notrobots.authenticator.ui.barcode

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.viewModels
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dev.notrobots.androidstuff.extensions.makeToast
import dev.notrobots.androidstuff.extensions.viewBindings
import dev.notrobots.androidstuff.util.Logger.Companion.loge
import dev.notrobots.authenticator.activities.AuthenticatorActivity
import dev.notrobots.authenticator.databinding.ActivityBarcodeScannerBinding
import dev.notrobots.authenticator.databinding.ItemBarcodeScannerResultBinding
import dev.notrobots.authenticator.extensions.toPx
import dev.notrobots.authenticator.models.QRCode
import kotlinx.android.synthetic.main.activity_barcode_scanner.*
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Activity that handles the QR code scanning
 *
 * Code adapted from: [https://github.com/khaled-qasem/MLBarcodeScanner]
 */
class BarcodeScannerActivity : AuthenticatorActivity(), ImageAnalysis.Analyzer {
    private var lensFacing = CameraSelector.LENS_FACING_BACK
    private var cameraSelector: CameraSelector? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var previewUseCase: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var barcodeScanner: BarcodeScanner? = null
    private val viewModel by viewModels<BarcodeScannerViewModel>()
    private val binding by viewBindings<ActivityBarcodeScannerBinding>()
    private val screenAspectRatio: Int
        get() {
            val metrics = DisplayMetrics().also { camera_preview.display.getRealMetrics(it) }

            return aspectRatio(metrics.widthPixels, metrics.heightPixels)
        }
    private val scanResults = mutableSetOf<String?>()
    private var multiScanEnabled = true

    override fun onResume() {
        super.onResume()
        setFullscreen()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFullscreen()
        setContentView(binding.root)
        setupCamera()

        multiScanEnabled = intent.getBooleanExtra(EXTRA_MULTI_SCAN, false)

        val multiScanVisibility = if (multiScanEnabled) View.VISIBLE else View.GONE

        binding.scanResultList.visibility = multiScanVisibility
        binding.importScanResult.visibility = multiScanVisibility
        binding.importScanResult.setOnClickListener {
            if (scanResults.isNotEmpty()) {
                setResultsAndFinish()
            } else {
                makeToast("Please scan at least one QR code")
            }
        }
        binding.scanResultScrollView.visibility = multiScanVisibility
        binding.back.setOnClickListener {
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_CAMERA_REQUEST) {
            val camera = permissions.indexOf(Manifest.permission.CAMERA)

            if (grantResults[camera] == PackageManager.PERMISSION_GRANTED) {
                bindPreview()
                bindImageAnalysis()
            } else {
                makeToast("Camera permission is required to scan a QR code")
                finish()
            }
        }
    }

    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)
        val task = barcodeScanner?.process(inputImage)

        if (task != null) {
            task.addOnSuccessListener {
                if (it.size >= 1) {
                    // This value is the content of a single QR code, that content could be a list of
                    // Uris or a single Uri.
                    // The "lines" method will always return a list, if there is only one Uri in the
                    // QR code then the list will have a single item
                    val rawValue = it.first().rawValue  //TODO: This could potentially scan multiple codes, we're only accessing (the first) one here
                    val value = rawValue?.lines()

                    if (value == null) {
                        return@addOnSuccessListener
                    }

                    if (multiScanEnabled) {
                        if (!scanResults.containsAll(value)) {
                            val importResultView = ItemBarcodeScannerResultBinding.inflate(layoutInflater)
                            val qrCode = QRCode(rawValue, 150)
                            val params = LinearLayout.LayoutParams(
                                60.toPx().toInt(),
                                60.toPx().toInt()
                            ).apply {
                                marginStart = 4.toPx().toInt()
                                marginEnd = 4.toPx().toInt()
                            }

                            importResultView.root.layoutParams = params
                            importResultView.image.setImageBitmap(qrCode.toBitmap())
                            importResultView.remove.setOnClickListener {
                                binding.scanResultList.removeView(importResultView.root)
                                scanResults.removeAll(value)
                            }

                            binding.scanResultList.post {
                                binding.scanResultScrollView.fullScroll(View.FOCUS_RIGHT)
                            }
                            binding.scanResultList.addView(importResultView.root)
                            scanResults.addAll(value)
                        }
                    } else {
                        scanResults.addAll(value)
                        setResultsAndFinish()
                    }
                }
            }
            task.addOnFailureListener {
                loge("Cannot process image", it)
            }
            task.addOnCompleteListener {
                image.close()
            }
        }
    }

    private fun setupCamera() {
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        viewModel.cameraProvider.observe(this) {
            cameraProvider = it

            if (isCameraPermissionGranted()) {
                bindPreview()
                bindImageAnalysis()
            } else {
                requestCameraPermission()
            }
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
            loge(null, e)
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
            loge(null, e)   //TODO: loge(Throwable)
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)

        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }

        return AspectRatio.RATIO_16_9
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            baseContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_CAMERA_REQUEST
        )
    }

    private fun setResultsAndFinish() {
        val data = Intent().apply {
            putExtra(EXTRA_QR_LIST, ArrayList(scanResults))
        }

        setResult(RESULT_OK, data)
        finish()
    }

    companion object {
        private const val PERMISSION_CAMERA_REQUEST = 1000
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        const val EXTRA_QR_LIST = "BarcodeScannerActivity.QR_LIST"
        const val EXTRA_MULTI_SCAN = "BarcodeScannerActivity.MULTI_SCAN"
    }
}