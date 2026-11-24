package com.example.whosthatcritter.model

data class CritterModel(
    var id: String = "",
    var animalName: String = "",
    var imageUri: String = "",
    var confidence: Double = 0.0,
    var createdAt: Long = 0L
)
