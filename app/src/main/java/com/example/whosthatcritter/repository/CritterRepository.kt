package com.example.whosthatcritter.repository

import com.example.whosthatcritter.model.CritterModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.tasks.await


class CritterRepository {

    // Initialize Firebase services
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Get the current user's ID or sign in anonymously
    private suspend fun getUserId(): String {

        // Check if the user is already signed in
        val currentUser = auth.currentUser

        if (currentUser != null) {
            return currentUser.uid
        }

        // Create a new anonymous Firebase account
        val result = auth.signInAnonymously().await()

        return result.user?.uid
            ?: throw Exception("Anonymous authentication failed")
    }

    // Reference the current user's animal collection
    private suspend fun getCollection(): CollectionReference {

        val userId = getUserId()

        return db.collection("users")
            .document(userId)
            .collection("critters")
    }

    // Save an animal to Firestore
    suspend fun insert(critter: CritterModel): String {

        require(critter.animalName.isNotBlank()) {
            "Animal name cannot be empty"
        }

        val collection = getCollection()

        // Generate a unique Firestore document ID
        val document = collection.document()

        val animalToSave = critter.copy(
            id = document.id,
            animalName = critter.animalName.trim(),
            createdAt = System.currentTimeMillis()
        )

        // Wait for Firestore to save the document
        document.set(animalToSave).await()

        return document.id
    }
    // Retrieve all animals from Firestore
    suspend fun getAllCritters(): List<CritterModel> {

        val result = getCollection()
            .get()
            .await()

        return result.documents
            .mapNotNull { document ->
                document.toObject(CritterModel::class.java)
            }
            .sortedByDescending { it.createdAt }
    }

    // Delete an animal using its document ID
    suspend fun delete(critterId: String) {

        require(critterId.isNotBlank()) {
            "Animal ID cannot be empty"
        }

        getCollection()
            .document(critterId)
            .delete()
            .await()
    }

    suspend fun authenticate() {
        getUserId()
    }
}