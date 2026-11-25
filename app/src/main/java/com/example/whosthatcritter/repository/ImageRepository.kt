package com.example.whosthatcritter.repository

import android.content.Context
import android.net.Uri
import com.example.whosthatcritter.model.ImageLabel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.tasks.await

class ImageRepository(context: Context) {
    private val appContext = context.applicationContext
    suspend fun processImage(uri: Uri): List<ImageLabel> {
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        val image = InputImage.fromFilePath(appContext, uri)
        val results = labeler.process(image).await()
        labeler.close()
        return results.map { label ->ImageLabel(text = label.text, confidence = label.confidence, index = label.index) }
    }
}