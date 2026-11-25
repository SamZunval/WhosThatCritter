package com.example.whosthatcritter.model

data class ImageLabel(
    val text: String,
    val confidence: Float,
    val index: Int
)