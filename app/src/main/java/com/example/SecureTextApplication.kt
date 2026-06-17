package com.example

import android.app.Application
import com.example.data.SecureTextDatabase
import com.example.data.SecureTextRepository

class SecureTextApplication : Application() {

    private val database by lazy { SecureTextDatabase.getDatabase(this) }
    val repository by lazy { SecureTextRepository(database.secureTextDao()) }

    override fun onCreate() {
        super.onCreate()
    }
}
