package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_session")
data class UserSession(
    @PrimaryKey val id: Int = 1, // Only 1 logged-in user session at a time
    val phoneNumber: String,
    val name: String,
    val passphraseHex: String, // Passphrase used for global AES encrypt/decrypt
    val registeredAt: Long = System.currentTimeMillis(),
    val avatarColorHex: String = "#0061A4",
    val avatarEmoji: String = "👤",
    val chatBgColorHex: String = "#FDFCFF", // default clean minimal
    val fontSizeMultiplier: Float = 1.0f, // 0.8f (small), 1.0f (medium), 1.2f (large)
    val isDarkMode: Boolean = false
)

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val phoneNumber: String,
    val name: String,
    val status: String,
    val avatarColorHex: String,
    val isGroup: Boolean = false,
    val groupMembersJson: String = "" // JSON list of names or member string
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderPhone: String,
    val receiverPhone: String,
    val cipherText: String,
    val ivString: String,
    val isUserSender: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val deliveryStatus: String, // SENDING -> SENT -> DELIVERED -> READ
    val passphraseUsed: String, // Passphrase used so we can show decryptions live!
    val senderName: String = "" // Used in group chats to identify who sent it
)
