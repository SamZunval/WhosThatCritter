package com.example.whosthatcritter.model

import android.net.Uri

data class CameraState(
    val isProcessing: Boolean = false,
    val labels: List<ImageLabel> = emptyList(),
    val imageUri: Uri? = null,
    val error: String? = null
)