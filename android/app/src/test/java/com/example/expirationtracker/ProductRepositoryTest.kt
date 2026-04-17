package com.example.expirationtracker

// ============================================================
// RED PHASE — Repository tests using a fake in-memory DAO
// so no Android runtime is required for unit tests.
// ============================================================

import com.example.expirationtracker.db.ProductDao
import com.example.expirationtracker.model.Product
import com.example.expirationtracker.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class ProductRepositoryTest {

    private lateinit var fakeDao: FakeProductDao
    private lateinit var repository: ProductRepository

    @Before
    fun setUp() {
        fakeDao = FakeProductDao()
        repository = ProductRepository(fakeDao)
    }

    // RED: Saving a product must persist it so it can be retrieved
    @Test
    fun `saving a product makes it retrievable`() = runTest {
        val product = Product(name = "Leite", expirationDate = LocalDate.of(2026, 5, 20))
        repository.save(product)
        val all = repository.getAllProducts().first()
        assertEquals(1, all.size)
        assertEquals("Leite", all[0].name)
    }

    // RED: Multiple products can be saved and retrieved
    @Test
    fun `saving multiple products returns them all`() = runTest {
        repository.save(Product(name = "Leite", expirationDate = LocalDate.now().plusDays(5)))
        repository.save(Product(name = "Frango", expirationDate = LocalDate.now().plusDays(2)))
        repository.save(Product(name = "Queijo", expirationDate = LocalDate.now().plusDays(10)))
        val all = repository.getAllProducts().first()
        assertEquals(3, all.size)
    }

    // RED: A product can be deleted by id
    @Test
    fun `deleting a product removes it from the list`() = runTest {
        val product = Product(id = 1L, name = "Iogurte", expirationDate = LocalDate.now().plusDays(3))
        repository.save(product)
        repository.delete(product)
        val all = repository.getAllProducts().first()
        assertTrue(all.isEmpty())
    }

    // RED: getExpiredProducts returns only expired items
    @Test
    fun `getExpiredProducts returns only expired products`() = runTest {
        repository.save(Product(name = "Vencido", expirationDate = LocalDate.now().minusDays(1)))
        repository.save(Product(name = "Válido", expirationDate = LocalDate.now().plusDays(5)))
        val expired = repository.getExpiredProducts().first()
        assertEquals(1, expired.size)
        assertEquals("Vencido", expired[0].name)
    }

    // RED: getExpiringSoon returns products expiring within 3 days (not expired)
    @Test
    fun `getExpiringSoon returns products expiring within 3 days`() = runTest {
        repository.save(Product(name = "Quase vencendo", expirationDate = LocalDate.now().plusDays(2)))
        repository.save(Product(name = "Ok", expirationDate = LocalDate.now().plusDays(30)))
        repository.save(Product(name = "Vencido", expirationDate = LocalDate.now().minusDays(1)))
        val soon = repository.getExpiringSoon().first()
        assertEquals(1, soon.size)
        assertEquals("Quase vencendo", soon[0].name)
    }

    // RED: Repository starts empty
    @Test
    fun `repository starts with empty list`() = runTest {
        val all = repository.getAllProducts().first()
        assertTrue(all.isEmpty())
    }

    // ── Fake DAO (in-memory, no Android/Room runtime needed) ─────

    private class FakeProductDao : ProductDao {
        private val store = MutableStateFlow<List<Product>>(emptyList())

        override fun getAllProducts(): Flow<List<Product>> = store

        override suspend fun insert(product: Product) {
            store.value = store.value + product
        }

        override suspend fun delete(product: Product) {
            store.value = store.value.filter { it.id != product.id || it.name != product.name }
        }

        override fun getExpiredProducts(today: String): Flow<List<Product>> =
            MutableStateFlow(store.value.filter { it.expirationDate.isBefore(LocalDate.now()) })

        override fun getExpiringSoon(today: String, limit: String): Flow<List<Product>> =
            MutableStateFlow(
                store.value.filter {
                    val d = it.expirationDate
                    !d.isBefore(LocalDate.now()) && !d.isAfter(LocalDate.now().plusDays(3))
                }
            )
    }
}
