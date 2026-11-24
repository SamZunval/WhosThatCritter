package com.example.whosthatcritter

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.whosthatcritter.databinding.ActivityCameraBinding
import android.Manifest
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.google.android.gms.tasks.Tasks
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class Camera : AppCompatActivity() {
    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCameraBinding.inflate(layoutInflater)

        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }
        binding.imageCaptureButton.setOnClickListener { takePhoto() }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startCamera() {

        cameraProviderFuture.addListener({

            if (isFinishing || isDestroyed) {
                return@addListener
            }

            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }

            val capture = ImageCapture.Builder()
                .build()

            imageCapture = capture

            try {
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture
                )

                Log.d(TAG, "Camera successfully initialized")

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }
    fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.CANADA)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    runCoroutine(output.savedUri?: Uri.EMPTY)
                }
            }
        )
    }

    private val job = SupervisorJob()
    // initialize coroutineScope using 3 types of dispatchers
    // MAIN - UI or main thread
    // IO for networks
    // DEFAULT - cpu intensive
    private val ioScope by lazy { CoroutineScope(job+ Dispatchers.IO) }
    private suspend fun processImage(uri: Uri):Boolean {
        var image: InputImage
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        try {
            image = InputImage.fromFilePath(this@Camera, uri)

            val task = labeler.process(image)
            val result = Tasks.await(task)
            for (label in result) {
                val text = label.text
                val confidence = label.confidence
                val index = label.index
                Log.d(TAG,"Text: ${text} Confidence: ${confidence} index: ${index}")
            }
            //TODO:Add Firebase Stuff

        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        finally {
            labeler.close()
        }
        return true
    }
    private fun runCoroutine(uri: Uri) {
        ioScope.launch {
            val procImage = async { processImage(uri) }
            if(procImage.await()) {
                Log.d("Success", "Process Image returned true")
            }
            else {
                Log.d("Error", "Process Image returned false")
            }
        }
    }
    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    override fun onPause() {
        super.onPause()

        // Release CameraX resources when leaving this activity
        if (::cameraProviderFuture.isInitialized &&
            cameraProviderFuture.isDone) {

            try {
                cameraProviderFuture.get().unbindAll()
                imageCapture = null

                Log.d(TAG, "Camera successfully released")

            } catch (e: Exception) {
                Log.e(TAG, "Error releasing camera", e)
            }
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {}.toTypedArray()
    }
    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT).show()
            } else {
                startCamera()
            }
        }

    fun onBack(view: View) {
        finish()
    }
}