package com.example.whosthatcritter.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.whosthatcritter.model.CritterModel
import com.example.whosthatcritter.model.ImageLabel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
class ImageRepository(context: Context) {
    private val appContext = context.applicationContext
    private val critterRepository = CritterRepository() //use critterRepository to insert
    val validAnimals = listOf("Bird", "Shetland sheepdog", "Gerbil","Bear","Person","Reef","Cattle","Cat","Dinosaur","Dragon","Dalmatian","Cairn terrier","Shell","Horse","Herd","Insect","Penguin","Pet","Fishing","Duck","Turtle","Crocodile","Dog","Bull","Butterfly","Larva","Seal","Farm","Sphynx","Basset hound","Baby","Rodeo","Swamp","Pomacentridae","Ragdoll","Pixie-bob")
    suspend fun processImage(uri: Uri): List<ImageLabel> {
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        try{
            //Load the image
            val image = InputImage.fromFilePath(appContext, uri)

            //Identifies the image
            val results = labeler.process(image).await()
            labeler.close()

            //Converts result into ImageLabel object
            var labels = results.map { label ->ImageLabel(text = label.text, confidence = label.confidence, index = label.index) }

            //try to send animals but send anything if not found
            var animals = labels.filter { it.text in validAnimals }
            if(animals.isEmpty()){
                return labels
            }

            //My turn
            //Finds the label with the highest confidence
            val bestLabel = animals.maxByOrNull { it.confidence }

            //We need to check otherelse tons of issues arise from String?
            if (bestLabel != null) {

                // Compress image and convert it to Base64
                val imageString = withContext(Dispatchers.IO) {
                    compressImage(uri)
                }

                // Create critter model
                val critter = CritterModel(
                    animalName = bestLabel.text,
                    imageUri = imageString,
                    confidence = bestLabel.confidence.toDouble()
                )

                // Save animal and image to Firestore
                critterRepository.insert(critter)
            }


            Log.d(TAG, "Animal and image successfully saved to Firestore")


            return animals

        } catch (e: Exception){
        }
        return TODO("Provide the return value")
    }

    private companion object {
        private const val TAG = "ImageRepository"
    }
    //compress image to not overload database
    private fun compressImage(uri: Uri): String {

        val inputStream = appContext.contentResolver.openInputStream(uri) ?: throw Exception("Unable to open image");

        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        //check to see if the image is properly loaded
        if (bitmap == null) {
            throw Exception("Invalid image or unable to decode image")
        }

        //Compress image
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)

        val imageBytes = outputStream.toByteArray()

        //Check to see if the compressed image is not empty
        if (imageBytes.isEmpty()) {
            throw Exception("Image compression failed")
        }
        val imageString = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        return imageString    }

}