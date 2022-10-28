package dev.notrobots.authenticator.ui.barcode

import android.app.Application
import android.util.Log
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.notrobots.authenticator.App
import javax.inject.Inject

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {
    private var _cameraProvider: MutableLiveData<ProcessCameraProvider>? = null

    val cameraProvider: LiveData<ProcessCameraProvider>
        get() {
            if (_cameraProvider == null) {
                _cameraProvider = MutableLiveData()

                val cameraProviderFuture = ProcessCameraProvider.getInstance(getApplication())
                cameraProviderFuture.addListener(
                    {
                        try {
                            _cameraProvider!!.setValue(cameraProviderFuture.get())
                        } catch (e: Exception) {
                            Log.e(App.LOG_TAG, "Unhandled exception", e)
                        }
                    },
                    ContextCompat.getMainExecutor(getApplication())
                )
            }

            return _cameraProvider!!
        }
}