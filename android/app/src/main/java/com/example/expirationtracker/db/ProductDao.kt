package com.example.expirationtracker.db

import androidx.room.*
import com.example.expirationtracker.model.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY expirationDate ASC")
    fun getAllProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("SELECT * FROM products WHERE expirationDate < :today ORDER BY expirationDate ASC")
    fun getExpiredProducts(today: String): Flow<List<Product>>

    @Query("""
        SELECT * FROM products
        WHERE expirationDate >= :today AND expirationDate <= :limit
        ORDER BY expirationDate ASC
    """)
    fun getExpiringSoon(today: String, limit: String): Flow<List<Product>>
}
