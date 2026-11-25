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
    val validAnimals = listOf("Bird", "Shetland sheepdog", "Gerbil","Bear","Person","Reef","Cattle","Cat","Dinosaur","Dragon","Dalmatian","Cairn terrier","Shell","Horse","Herd","Insect","Penguin","Pet","Fishing","Duck","Turtle","Crocodile","Dog","Bull","Butterfly","Larva","Seal","Farm","Sphynx","Basset hound","Baby","Rodeo","Swamp","Pomacentridae","Ragdoll","Pixie-bob")
    suspend fun processImage(uri: Uri): List<ImageLabel> {
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        val image = InputImage.fromFilePath(appContext, uri)
        val results = labeler.process(image).await()
        labeler.close()
        var labels = results.map { label ->ImageLabel(text = label.text, confidence = label.confidence, index = label.index) }
        //try to send animals but send anything if not found
        var animals = labels.filter { it.text in validAnimals }
        if(animals.isEmpty()){
            return labels
        }
        return animals
    }
}