package com.example.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SecureTextRepository(private val dao: SecureTextDao) {

    // --- User Session ---
    val userSession: Flow<UserSession?> = dao.getUserSessionFlow()

    suspend fun registerUser(phoneNumber: String, name: String, passphraseHex: String) {
        val session = UserSession(phoneNumber = phoneNumber, name = name, passphraseHex = passphraseHex)
        dao.insertUserSession(session)
    }

    suspend fun updateUserSession(session: UserSession) {
        dao.insertUserSession(session)
    }

    suspend fun logout() {
        dao.clearUserSession()
        dao.clearAllMessages()
    }


    // --- Contacts ---
    fun getAllContacts(): Flow<List<Contact>> = dao.getAllContactsFlow()

    fun searchContacts(query: String): Flow<List<Contact>> {
        return if (query.isBlank()) {
            dao.getAllContactsFlow()
        } else {
            dao.searchContactsFlow(query)
        }
    }

    suspend fun addContact(name: String, phoneNumber: String) {
        // Aesthetic color palette for avatar avatars
        val colors = listOf("#E57373", "#81C784", "#64B5F6", "#FFB74D", "#BA68C8", "#4DB6AC", "#D4E157")
        val color = colors.random()
        val contact = Contact(
            phoneNumber = phoneNumber,
            name = name,
            status = "Hey there! I am using Frendo.",
            avatarColorHex = color,
            isGroup = false
        )
        dao.insertContact(contact)
    }

    suspend fun addGroup(name: String, members: List<String>) {
        val colors = listOf("#E57373", "#81C784", "#64B5F6", "#FFB74D", "#BA68C8", "#4DB6AC", "#D4E157")
        val color = colors.random()
        val groupPhone = "group_" + System.currentTimeMillis() // unique ID
        val contact = Contact(
            phoneNumber = groupPhone,
            name = name,
            status = "${members.size} members",
            avatarColorHex = color,
            isGroup = true,
            groupMembersJson = members.joinToString(", ")
        )
        dao.insertContact(contact)
    }

    suspend fun removeContact(contact: Contact) {
        dao.deleteContact(contact)
    }


    // --- Messages ---
    fun getMessagesForChat(userPhone: String, contactPhone: String): Flow<List<Message>> {
        return dao.getMessagesForChatFlow(userPhone, contactPhone)
    }

    /**
     * Sends an encrypted message.
     * Starts a background coroutine to simulate real-time delivery status updates:
     * SENDING -> SENT -> DELIVERED -> READ
     */
    suspend fun sendEncryptedMessage(
        senderPhone: String,
        receiverPhone: String,
        senderName: String,
        cipherText: String,
        ivString: String,
        passphraseUsed: String,
        isGroup: Boolean,
        groupMembersList: List<String>,
        coroutineScope: CoroutineScope
    ) {
        val msg = Message(
            senderPhone = senderPhone,
            receiverPhone = receiverPhone,
            cipherText = cipherText,
            ivString = ivString,
            isUserSender = true,
            deliveryStatus = "SENDING", // Initial state
            passphraseUsed = passphraseUsed,
            senderName = senderName
        )
        
        val id = dao.insertMessage(msg).toInt()
        
        // Launch a coroutine to handle delivery updates asynchronously
        coroutineScope.launch(Dispatchers.IO) {
            // 1. SENDING -> SENT (800ms)
            delay(800)
            dao.updateMessageStatus(id, "SENT")
            
            // 2. SENT -> DELIVERED (1200ms)
            delay(1200)
            dao.updateMessageStatus(id, "DELIVERED")
            
            // 3. DELIVERED -> READ (1000ms)
            delay(1000)
            dao.updateMessageStatus(id, "READ")

            // Optional: Simulate a secure encrypted response from the contact or a group member!
            // This brings the conversation to life and shows real decryption for received messages.
            delay(1500)
            val decryptedResponses = listOf(
                "Packet received and decrypted. Key checks out perfectly.",
                "Acknowledging secure transmission. Speak freely.",
                "Roger that! Speaking via our secure AES dynamic tunnel.",
                "Confirmed. Your message reads perfectly clean."
            )
            val replyText = decryptedResponses.random()
            
            // Re-encrypt the reply using the exact same chat passphrase
            val (replyCipher, replyIv) = com.example.crypto.EncryptionUtils.encrypt(replyText, passphraseUsed)
            
            // Check if group or direct message
            val replierPhone: String
            val replierName: String
            if (isGroup) {
                val list = if (groupMembersList.isEmpty()) listOf("Marcus Aurelius", "Sarah Jenkins", "Diana Prince") else groupMembersList
                replierName = list.random()
                replierPhone = "sim_" + replierName.replace(" ", "_").lowercase()
            } else {
                replierPhone = receiverPhone
                replierName = ""
            }

            val replyMsg = Message(
                senderPhone = replierPhone,
                receiverPhone = if (isGroup) receiverPhone else senderPhone, // group messages have groupPhone as receiver
                cipherText = replyCipher,
                ivString = replyIv,
                isUserSender = false,
                deliveryStatus = "READ", // Incoming message is read since we are looking at the screen
                passphraseUsed = passphraseUsed,
                senderName = replierName
            )
            dao.insertMessage(replyMsg)
        }
    }
}
