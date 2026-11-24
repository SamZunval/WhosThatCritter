package com.example.whosthatcritter.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whosthatcritter.model.CritterModel
import com.example.whosthatcritter.repository.CritterRepository
import kotlinx.coroutines.launch

class CritterViewModel: ViewModel(){
    private val repository = CritterRepository()

    // Animals available to the Gallery and Statistics pages
    private val _allCritters =
        MutableLiveData<List<CritterModel>>(emptyList())

    val allCritters: LiveData<List<CritterModel>> = _allCritters

    // Success or error messages
    private val _status = MutableLiveData<String>()

    val status: LiveData<String> = _status

    // Read saved animals from Firebase
    fun loadCritters() {

        viewModelScope.launch {
            try {
                _allCritters.value = repository.getAllCritters()

            } catch (e: Exception) {
                _status.value = "Error loading animals: ${e.message}"
            }
        }
    }

    // Save a new animal
    fun insert(critter: CritterModel) {

        viewModelScope.launch {
            try {
                repository.insert(critter)

                _status.value = "Animal saved successfully"

                // Update the saved animal list
                loadCritters()

            } catch (e: Exception) {
                _status.value = "Error saving animal: ${e.message}"
            }
        }
    }

    // Delete an animal
    fun delete(critterId: String) {

        viewModelScope.launch {
            try {
                repository.delete(critterId)

                _status.value = "Animal deleted successfully"

                // Refresh the collection
                loadCritters()

            } catch (e: Exception) {
                _status.value = "Error deleting animal: ${e.message}"
            }
        }
    }

}