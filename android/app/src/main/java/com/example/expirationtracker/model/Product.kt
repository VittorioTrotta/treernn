package com.example.expirationtracker.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.expirationtracker.db.DateConverter
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Entity(tableName = "products")
@TypeConverters(DateConverter::class)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val expirationDate: LocalDate
) {
    fun isExpired(): Boolean = expirationDate.isBefore(LocalDate.now())

    fun isExpiringSoon(): Boolean {
        val today = LocalDate.now()
        return !expirationDate.isBefore(today) && !expirationDate.isAfter(today.plusDays(3))
    }

    fun daysUntilExpiration(): Long = ChronoUnit.DAYS.between(LocalDate.now(), expirationDate)

    fun formattedExpirationDate(): String =
        expirationDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
}
