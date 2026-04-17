package com.example.expirationtracker.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.expirationtracker.model.Product

@Database(entities = [Product::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class ExpirationDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: ExpirationDatabase? = null

        fun getInstance(context: Context): ExpirationDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    ExpirationDatabase::class.java,
                    "expiration_tracker.db"
                ).build().also { INSTANCE = it }
            }
    }
}
