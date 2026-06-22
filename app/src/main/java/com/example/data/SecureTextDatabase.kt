package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [UserSession::class, Contact::class, Message::class, LocalAccount::class], version = 3, exportSchema = false)
abstract class SecureTextDatabase : RoomDatabase() {

    abstract fun secureTextDao(): SecureTextDao

    companion object {
        @Volatile
        private var INSTANCE: SecureTextDatabase? = null

        fun getDatabase(context: Context): SecureTextDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SecureTextDatabase::class.java,
                    "secure_texting_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
