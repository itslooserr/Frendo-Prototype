package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureTextDao {

    // --- User Session Queries ---
    @Query("SELECT * FROM user_session WHERE id = 1 LIMIT 1")
    fun getUserSessionFlow(): Flow<UserSession?>

    @Query("SELECT * FROM user_session WHERE id = 1 LIMIT 1")
    suspend fun getUserSession(): UserSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserSession(session: UserSession)

    @Query("DELETE FROM user_session")
    suspend fun clearUserSession()


    // --- Contact Queries ---
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContactsFlow(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchContactsFlow(query: String): Flow<List<Contact>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Delete
    suspend fun deleteContact(contact: Contact)


    // --- Message Queries ---
    @Query("""
        SELECT * FROM messages 
        WHERE (senderPhone = :userPhone AND receiverPhone = :contactPhone) 
           OR (senderPhone = :contactPhone AND receiverPhone = :userPhone)
        ORDER BY timestamp ASC
    """)
    fun getMessagesForChatFlow(userPhone: String, contactPhone: String): Flow<List<Message>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("UPDATE messages SET deliveryStatus = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: Int, status: String)

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: Int): Message?

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()
}
