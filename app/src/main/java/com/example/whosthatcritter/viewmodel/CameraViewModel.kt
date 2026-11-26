package com.example.whosthatcritter.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.whosthatcritter.repository.ImageRepository
import com.example.whosthatcritter.model.CameraState
import kotlinx.coroutines.launch

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ImageRepository(application)
    private val _uiState = MutableLiveData(CameraState())
    val uiState: LiveData<CameraState> = _uiState
    fun processImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = CameraState(isProcessing = true)
            try {
                val labels = repository.processImage(uri)
                _uiState.value = CameraState(isProcessing = false,labels = labels,imageUri = uri)
            } catch (e: Exception) {
                _uiState.value = CameraState(isProcessing = false,error = e.message ?: "Image processing failed")
            }
        }
    }
}
