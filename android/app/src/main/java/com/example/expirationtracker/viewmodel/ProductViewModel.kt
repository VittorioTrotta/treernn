package com.example.expirationtracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expirationtracker.model.Product
import com.example.expirationtracker.repository.ProductRepository
import com.example.expirationtracker.voice.VoiceCommandParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UiState {
    object Idle : UiState()
    object Listening : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}

class ProductViewModel(
    private val repository: ProductRepository,
    private val parser: VoiceCommandParser = VoiceCommandParser()
) : ViewModel() {

    val products = repository.getAllProducts()

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun processVoiceCommand(transcript: String) {
        val parsed = parser.parse(transcript)
        if (parsed == null) {
            _uiState.value = UiState.Error("Comando não reconhecido. Tente: \"adicionar leite vence 20 de maio\"")
            return
        }
        viewModelScope.launch {
            val product = Product(name = parsed.productName, expirationDate = parsed.expirationDate)
            repository.save(product)
            _uiState.value = UiState.Success(
                "\"${parsed.productName}\" registrado com validade ${product.formattedExpirationDate()}"
            )
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.delete(product)
        }
    }

    fun resetState() {
        _uiState.value = UiState.Idle
    }

    fun setListening() {
        _uiState.value = UiState.Listening
    }
}
