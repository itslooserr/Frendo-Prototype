package com.example.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SecureTextRepository(private val dao: SecureTextDao) {

    // --- User Session & Local Accounts ---
    val userSession: Flow<UserSession?> = dao.getUserSessionFlow()
    val allLocalAccounts: Flow<List<LocalAccount>> = dao.getAllLocalAccountsFlow()

    suspend fun registerUser(phoneNumber: String, name: String, passphraseHex: String, bio: String = "Hey there! I am using Frendo.", avatarColorHex: String = "#0061A4") {
        val account = LocalAccount(
            phoneNumber = phoneNumber,
            name = name,
            passphraseHex = passphraseHex,
            bio = bio,
            avatarColorHex = avatarColorHex
        )
        dao.insertLocalAccount(account)

        val session = UserSession(
            phoneNumber = phoneNumber,
            name = name,
            passphraseHex = passphraseHex,
            bio = bio,
            avatarColorHex = avatarColorHex
        )
        dao.insertUserSession(session)

        // Insert default interactive contacts so user has active, beautiful lines right away!
        dao.insertContact(
            Contact(
                phoneNumber = "bot_frendo_advisor",
                name = "Frendo Secure Advisor",
                status = "Encryption Specialist • Online",
                avatarColorHex = "#FFB74D",
                isGroup = false
            )
        )

        dao.insertContact(
            Contact(
                phoneNumber = "group_general_crypto",
                name = "General Cryptography Room",
                status = "3 members",
                avatarColorHex = "#4DB6AC",
                isGroup = true,
                groupMembersJson = "Marcus Aurelius, Sarah Jenkins, Diana Prince"
            )
        )

        // MUTUAL AUTO-CONTACT LINKING:
        // Automatically make any already-registered accounts mutual contacts.
        val otherAccounts = dao.getAllLocalAccounts()
        otherAccounts.forEach { other ->
            if (other.phoneNumber != phoneNumber) {
                // Add the other account as a contact for the new user
                dao.insertContact(
                    Contact(
                        phoneNumber = other.phoneNumber,
                        name = other.name,
                        status = other.bio,
                        avatarColorHex = other.avatarColorHex,
                        isGroup = false
                    )
                )
                // Add this new user as a contact for the other account
                dao.insertContact(
                    Contact(
                        phoneNumber = phoneNumber,
                        name = name,
                        status = bio,
                        avatarColorHex = avatarColorHex,
                        isGroup = false
                    )
                )
            }
        }
    }

    /**
     * Switch current user session to another pre-registered local configuration.
     */
    suspend fun loginAsAccount(account: LocalAccount) {
        val session = UserSession(
            phoneNumber = account.phoneNumber,
            name = account.name,
            passphraseHex = account.passphraseHex,
            avatarColorHex = account.avatarColorHex,
            avatarEmoji = account.avatarEmoji,
            customPfpPath = account.customPfpPath,
            bio = account.bio,
            isDarkMode = account.isDarkMode,
            chatBgColorHex = account.chatBgColorHex,
            fontSizeMultiplier = account.fontSizeMultiplier
        )
        dao.insertUserSession(session)
    }

    suspend fun updateLocalAccountProfile(phoneNumber: String, name: String, bio: String, customPfpPath: String?) {
        val existingAccount = dao.getLocalAccount(phoneNumber)
        if (existingAccount != null) {
            val updatedAccount = existingAccount.copy(
                name = name,
                bio = bio,
                customPfpPath = customPfpPath
            )
            dao.insertLocalAccount(updatedAccount)
        }

        val existingSession = dao.getUserSession()
        if (existingSession != null && existingSession.phoneNumber == phoneNumber) {
            val updatedSession = existingSession.copy(
                name = name,
                bio = bio,
                customPfpPath = customPfpPath
            )
            dao.insertUserSession(updatedSession)
        }
    }

    suspend fun deleteLocalAccount(phoneNumber: String) {
        dao.deleteLocalAccount(phoneNumber)
    }

    suspend fun updateUserSession(session: UserSession) {
        dao.insertUserSession(session)
        // Also sync to local accounts database for persistence
        val existing = dao.getLocalAccount(session.phoneNumber)
        if (existing != null) {
            val synced = existing.copy(
                name = session.name,
                bio = session.bio,
                customPfpPath = session.customPfpPath,
                isDarkMode = session.isDarkMode,
                chatBgColorHex = session.chatBgColorHex,
                fontSizeMultiplier = session.fontSizeMultiplier,
                avatarColorHex = session.avatarColorHex,
                avatarEmoji = session.avatarEmoji
            )
            dao.insertLocalAccount(synced)
        }
    }

    suspend fun getAllRegisteredAccountsDirectly(): List<LocalAccount> {
        return dao.getAllLocalAccounts()
    }

    suspend fun logout() {
        dao.clearUserSession()
        // No longer deletes messages upon user-switch so both parties can chat persistently!
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

        // MUTUAL AUTO-CONTACT CHECK:
        // Ensure the recipient sees the sender in their contact list, so they can easily chat back!
        val isReceiverRegistered = dao.getLocalAccount(receiverPhone) != null
        if (isReceiverRegistered) {
            val existingContacts = dao.getAllContactsFlow().first()
            val hasSenderAsContact = existingContacts.any { 
                it.phoneNumber.replace(" ", "") == senderPhone.replace(" ", "")
            }
            if (!hasSenderAsContact) {
                val colors = listOf("#E57373", "#81C784", "#64B5F6", "#FFB74D", "#BA68C8", "#4DB6AC", "#D4E157")
                dao.insertContact(
                    Contact(
                        phoneNumber = senderPhone,
                        name = senderName.ifBlank { "User $senderPhone" },
                        status = "Hey there! I am using Frendo.",
                        avatarColorHex = colors.random()
                    )
                )
            }
        }
        
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

            // Simulate responses after marked as READ
            delay(1500)
            val replyText: String
            val replierPhone: String
            val replierName: String
            
            if (receiverPhone == "bot_frendo_advisor") {
                // Interactive dynamic advisor response
                val incomingPlain = try {
                    com.example.crypto.EncryptionUtils.decrypt(cipherText, ivString, passphraseUsed)
                } catch (e: Exception) {
                    ""
                }
                replyText = when {
                    incomingPlain.contains("hi", ignoreCase = true) || incomingPlain.contains("hello", ignoreCase = true) -> {
                        "Secure greetings, Agent! \uD83D\uDD10 I am Frendo's automated Cryptographic Advisor. Everything in this tunnel is encrypted on-device via AES-CBC. How can I help you today?"
                    }
                    incomingPlain.contains("whatsapp", ignoreCase = true) || incomingPlain.contains("telegram", ignoreCase = true) || incomingPlain.contains("discord", ignoreCase = true) -> {
                        "Excellent observation! Frendo brings together the absolute best of those worlds! \uD83D\uDCAC WhatsApp's solid delivery ticks and multi-account switching, Telegram's dynamic channels and security customizations, and Discord's unique server-category left rail with category filtering!"
                    }
                    incomingPlain.contains("how", ignoreCase = true) || incomingPlain.contains("work", ignoreCase = true) -> {
                        "Frendo derives a 128-bit key from your passphrase using SHA-256 truncation. We then encrypt your messages using AES-CBC-PKCS5Padding, generating a unique secure initialization vector (IV) per message packet. The server or local channels only transport closed packets!"
                    }
                    incomingPlain.contains("logo", ignoreCase = true) -> {
                        "The Frendo app launcher icon is now optimized with a spectacular Material You vector adaptive layout! The foreground is a clean orange speech bubble containing a sleek heart, set on a premium dark tech mesh background!"
                    }
                    else -> {
                        listOf(
                            "Secure packet processed. AES key derivation matches perfectly. Speak freely inside our dynamic tunnel! \uD83D\uDEAE",
                            "Confirmed: Zero trace of communication leak. Your end-to-end line is 100% active. \u2705",
                            "Acknowledge secure transfer! Did you know that clicking any message bubble allows you to inspect its live AES payload and key signature?",
                            "Line clear. Let's keep our communication secure. Keep your master passphrases private!"
                        ).random()
                    }
                }
                replierPhone = "bot_frendo_advisor"
                replierName = "Frendo Secure Advisor"
            } else if (isGroup) {
                // Group messages simulated members responses
                val list = if (groupMembersList.isEmpty()) listOf("Marcus Aurelius", "Sarah Jenkins", "Diana Prince") else groupMembersList
                replierName = list.random()
                replierPhone = "sim_" + replierName.replace(" ", "_").lowercase()
                val groupReplies = listOf(
                    "Decrypted and read! Glad we are chatting in a secured workspace.",
                    "AES tunnel confirmed. That makes total sense.",
                    "Checked the payload, looked perfect to me!",
                    "Roger that! Let's continue on this encrypted thread."
                )
                replyText = groupReplies.random()
            } else {
                // 1-to-1 simulated secure auto-reply
                val userReplies = listOf(
                    "Excellent! Decrypted your ciphertext. Message reads perfect.",
                    "Secured connection active! I received your message perfectly.",
                    "Awesome. Let's keep discussing on this encrypted line.",
                    "Got it! AES checks out."
                )
                replyText = userReplies.random()
                replierPhone = receiverPhone
                replierName = ""
            }

            // Re-encrypt response utilizing the exact same chat passphrase
            val (replyCipher, replyIv) = com.example.crypto.EncryptionUtils.encrypt(replyText, passphraseUsed)
            
            val replyMsg = Message(
                senderPhone = replierPhone,
                receiverPhone = if (isGroup) receiverPhone else senderPhone,
                cipherText = replyCipher,
                ivString = replyIv,
                isUserSender = false,
                deliveryStatus = "READ",
                passphraseUsed = passphraseUsed,
                senderName = replierName
            )
            dao.insertMessage(replyMsg)
        }
    }
}
