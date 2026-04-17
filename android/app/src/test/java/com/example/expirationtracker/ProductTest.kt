package com.example.expirationtracker

// ============================================================
// RED PHASE — These tests define the expected behavior of
// the Product domain model BEFORE any implementation exists.
// ============================================================

import com.example.expirationtracker.model.Product
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ProductTest {

    // RED: Product must be creatable with a name and expiration date
    @Test
    fun `product is created with name and expiration date`() {
        val expiration = LocalDate.of(2026, 5, 20)
        val product = Product(name = "Leite", expirationDate = expiration)

        assertEquals("Leite", product.name)
        assertEquals(expiration, product.expirationDate)
    }

    // RED: Product id defaults to 0 (Room will assign real id on insert)
    @Test
    fun `product default id is zero`() {
        val product = Product(name = "Frango", expirationDate = LocalDate.now())
        assertEquals(0L, product.id)
    }

    // RED: A product whose expiration date is in the past must be considered expired
    @Test
    fun `product is expired when expiration date is yesterday`() {
        val product = Product(name = "Iogurte", expirationDate = LocalDate.now().minusDays(1))
        assertTrue(product.isExpired())
    }

    // RED: A product expiring today is NOT considered expired
    @Test
    fun `product expiring today is not expired`() {
        val product = Product(name = "Queijo", expirationDate = LocalDate.now())
        assertFalse(product.isExpired())
    }

    // RED: A product expiring in the future is not expired
    @Test
    fun `product expiring in the future is not expired`() {
        val product = Product(name = "Manteiga", expirationDate = LocalDate.now().plusDays(10))
        assertFalse(product.isExpired())
    }

    // RED: A product expiring within 3 days is "expiring soon"
    @Test
    fun `product expiring in 2 days is expiring soon`() {
        val product = Product(name = "Creme de leite", expirationDate = LocalDate.now().plusDays(2))
        assertTrue(product.isExpiringSoon())
    }

    // RED: A product expiring in exactly 3 days is still "expiring soon"
    @Test
    fun `product expiring in 3 days is expiring soon`() {
        val product = Product(name = "Requeijão", expirationDate = LocalDate.now().plusDays(3))
        assertTrue(product.isExpiringSoon())
    }

    // RED: A product expiring in 4+ days is NOT expiring soon
    @Test
    fun `product expiring in 4 days is not expiring soon`() {
        val product = Product(name = "Nata", expirationDate = LocalDate.now().plusDays(4))
        assertFalse(product.isExpiringSoon())
    }

    // RED: Expired products are not "expiring soon"
    @Test
    fun `expired product is not expiring soon`() {
        val product = Product(name = "Ovo", expirationDate = LocalDate.now().minusDays(1))
        assertFalse(product.isExpiringSoon())
    }

    // RED: daysUntilExpiration returns negative for past dates
    @Test
    fun `days until expiration is negative for expired product`() {
        val product = Product(name = "Pão", expirationDate = LocalDate.now().minusDays(3))
        assertTrue(product.daysUntilExpiration() < 0)
    }

    // RED: daysUntilExpiration returns 0 when expiring today
    @Test
    fun `days until expiration is zero when expiring today`() {
        val product = Product(name = "Bolo", expirationDate = LocalDate.now())
        assertEquals(0, product.daysUntilExpiration())
    }

    // RED: formatted expiration date in PT-BR format (dd/MM/yyyy)
    @Test
    fun `formatted expiration date is in PT-BR format`() {
        val product = Product(name = "Macarrão", expirationDate = LocalDate.of(2026, 3, 5))
        assertEquals("05/03/2026", product.formattedExpirationDate())
    }
}
