package com.example.expirationtracker

// ============================================================
// RED PHASE — ViewModel tests that verify the coordination
// between VoiceCommandParser and ProductRepository.
// Uses kotlinx-coroutines-test + arch-core-testing.
// ============================================================

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.expirationtracker.model.Product
import com.example.expirationtracker.repository.ProductRepository
import com.example.expirationtracker.viewmodel.ProductViewModel
import com.example.expirationtracker.viewmodel.UiState
import com.example.expirationtracker.voice.VoiceCommandParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeRepository
    private lateinit var viewModel: ProductViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeRepository()
        viewModel = ProductViewModel(fakeRepository, VoiceCommandParser())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // RED: Processing a valid PT-BR voice command saves the product
    @Test
    fun `valid voice command saves product and emits success state`() = runTest {
        viewModel.processVoiceCommand("adicionar leite vence 20 de maio de 2027")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue("Expected Success state but got: $state", state is UiState.Success)
        assertEquals(1, fakeRepository.savedProducts.size)
        assertEquals("leite", fakeRepository.savedProducts[0].name)
    }

    // RED: Processing an invalid command emits an error state (no save)
    @Test
    fun `invalid voice command emits error state and does not save`() = runTest {
        viewModel.processVoiceCommand("texto inválido sem comando")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue("Expected Error state but got: $state", state is UiState.Error)
        assertTrue(fakeRepository.savedProducts.isEmpty())
    }

    // RED: Initial state is Idle
    @Test
    fun `initial ui state is Idle`() = runTest {
        val state = viewModel.uiState.first()
        assertTrue("Expected Idle state but got: $state", state is UiState.Idle)
    }

    // RED: product list is updated after saving
    @Test
    fun `product list updates after processing valid command`() = runTest {
        viewModel.processVoiceCommand("adicionar frango vence 10 de junho de 2027")
        advanceUntilIdle()

        val products = viewModel.products.first()
        assertEquals(1, products.size)
        assertEquals("frango", products[0].name)
    }

    // RED: Deleting a product removes it from repository
    @Test
    fun `deleting product removes it from repository`() = runTest {
        val product = Product(id = 1L, name = "Queijo", expirationDate = LocalDate.now().plusDays(5))
        fakeRepository.savedProducts.add(product)
        fakeRepository.refreshFlow()

        viewModel.deleteProduct(product)
        advanceUntilIdle()

        assertTrue(fakeRepository.savedProducts.isEmpty())
    }

    // RED: UiState.Success carries the product name for the confirmation message
    @Test
    fun `success state contains product name`() = runTest {
        viewModel.processVoiceCommand("registrar iogurte vence 15 de julho de 2027")
        advanceUntilIdle()

        val state = viewModel.uiState.first()
        assertTrue(state is UiState.Success)
        assertTrue((state as UiState.Success).message.contains("iogurte", ignoreCase = true))
    }

    // RED: Empty string input emits error state
    @Test
    fun `empty voice input emits error state`() = runTest {
        viewModel.processVoiceCommand("")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.first() is UiState.Error)
    }

    // ── Fake Repository ───────────────────────────────────────────

    private inner class FakeRepository : ProductRepository(dao = null) {
        val savedProducts = mutableListOf<Product>()
        private val flow = MutableStateFlow<List<Product>>(emptyList())

        fun refreshFlow() { flow.value = savedProducts.toList() }

        override fun getAllProducts(): Flow<List<Product>> = flow
        override fun getExpiredProducts(): Flow<List<Product>> =
            MutableStateFlow(savedProducts.filter { it.isExpired() })
        override fun getExpiringSoon(): Flow<List<Product>> =
            MutableStateFlow(savedProducts.filter { it.isExpiringSoon() })

        override suspend fun save(product: Product) {
            savedProducts.add(product)
            refreshFlow()
        }

        override suspend fun delete(product: Product) {
            savedProducts.removeAll { it.id == product.id && it.name == product.name }
            refreshFlow()
        }
    }
}
