package com.example.expirationtracker.repository

import com.example.expirationtracker.db.ProductDao
import com.example.expirationtracker.model.Product
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

open class ProductRepository(private val dao: ProductDao?) {

    open fun getAllProducts(): Flow<List<Product>> =
        dao!!.getAllProducts()

    open fun getExpiredProducts(): Flow<List<Product>> =
        dao!!.getExpiredProducts(LocalDate.now().toString())

    open fun getExpiringSoon(): Flow<List<Product>> =
        dao!!.getExpiringSoon(
            today = LocalDate.now().toString(),
            limit = LocalDate.now().plusDays(3).toString()
        )

    open suspend fun save(product: Product) {
        dao!!.insert(product)
    }

    open suspend fun delete(product: Product) {
        dao!!.delete(product)
    }
}
