package com.example.expirationtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expirationtracker.repository.ProductRepository
import com.example.expirationtracker.voice.VoiceCommandParser

class ProductViewModelFactory(
    private val repository: ProductRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return ProductViewModel(repository, VoiceCommandParser()) as T
    }
}
