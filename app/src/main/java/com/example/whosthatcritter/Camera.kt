package com.example.whosthatcritter

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.whosthatcritter.databinding.ActivityCameraBinding
import com.example.whosthatcritter.viewmodel.CameraViewModel
import com.example.whosthatcritter.viewmodel.CameraViewModelFactory
import com.google.common.util.concurrent.ListenableFuture
import java.text.SimpleDateFormat
import java.util.Locale

class Camera : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private lateinit var viewModel: CameraViewModel
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var imageCapture: ImageCapture? = null
    private var cameraBound = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Initialize ViewModel
        viewModel = ViewModelProvider(this,CameraViewModelFactory(application))[CameraViewModel::class.java]
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        // Observe ViewModel
        observeViewModel()
        // Camera button
        binding.imageCaptureButton.setOnClickListener {
            takePhoto()
        }
        // Check camera permissions
        if (!allPermissionsGranted()) {
            requestPermissions()
        }
    }
    private fun startCamera() {
        if (cameraBound) return

        cameraProviderFuture.addListener({
            if (isFinishing || isDestroyed) {
                return@addListener
            }

            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(
                        binding.viewFinder.surfaceProvider
                    )
                }

            val capture = ImageCapture.Builder()
                .build()

            imageCapture = capture

            try {
                cameraProvider.bindToLifecycle(this,CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                cameraBound = true
                Log.d(TAG, "Camera successfully initialized")
            } catch (exc: Exception) {
                imageCapture = null
                Log.e(TAG, "Camera binding failed", exc)
                Toast.makeText(this, "Unable to start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    private fun takePhoto() {
        val capture = imageCapture ?: return
        val name = SimpleDateFormat(FILENAME_FORMAT,Locale.CANADA).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                name
            )
            put(
                MediaStore.MediaColumns.MIME_TYPE,
                "image/jpeg"
            )
        }
        val outputOptions = ImageCapture.OutputFileOptions.Builder(contentResolver,MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(baseContext,"Photo capture failed",Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = output.savedUri
                    if (uri == null) {
                        Toast.makeText(baseContext, "Unable to retrieve image",Toast.LENGTH_SHORT).show()
                        return
                    }
                    //Toast.makeText(baseContext,"Photo captured successfully",Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Photo saved: $uri")
                    // Send URI to ViewModel for processing
                    viewModel.processImage(uri)
                }
            }
        )
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this) { state ->
            // Image processing started
            if (state.isProcessing) {
                Log.d(TAG, "Processing image...")
            }

            // Image processing completed
            if (state.labels.isNotEmpty()) {
                Log.d(TAG, "Image processing successful")
                state.labels.forEach { label ->
                    Log.d(TAG,"Label: ${label.text}, " + "Confidence: ${label.confidence}, " + "Index: ${label.index}")
                }

                // Get highest confidence label
                val bestLabel = state.labels.maxByOrNull {
                    it.confidence
                }
                bestLabel?.let {
                    Toast.makeText(this, "Identified: ${it.text}", Toast.LENGTH_LONG).show()
                }
                //TODO:Add Firebase Stuff
            }

            // Handle errors
            state.error?.let { error ->
                Log.e(TAG, error)
            }
        }
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }
    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext,it) == PackageManager.PERMISSION_GRANTED
        }
    }
    private val activityResultLauncher =registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            val permissionGranted = REQUIRED_PERMISSIONS.all {
                permissions[it] == true || ContextCompat.checkSelfPermission(this,it) == PackageManager.PERMISSION_GRANTED
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,"Camera permission denied",Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    override fun onResume() {
        super.onResume()

        if (allPermissionsGranted()) {
            startCamera()
        }
    }

    override fun onPause() {
        super.onPause()

        if (::cameraProviderFuture.isInitialized && cameraProviderFuture.isDone) {
            cameraProviderFuture.get().unbindAll()
            cameraBound = false
            imageCapture = null
        }
    }
    fun onBack(view: View) {
        finish()
    }
    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT ="yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =arrayOf(Manifest.permission.CAMERA)
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent())
    {
        uri -> if (uri != null){
            viewModel.processImage(uri)
        }
    }


    fun openGallery(view: View) {
        imagePicker.launch("image/*")
    }
}
