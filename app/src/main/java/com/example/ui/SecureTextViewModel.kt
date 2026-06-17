package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.crypto.EncryptionUtils
import com.example.data.Contact
import com.example.data.Message
import com.example.data.SecureTextRepository
import com.example.data.UserSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SecureTextViewModel(private val repository: SecureTextRepository) : ViewModel() {

    // --- User state ---
    val userSession: StateFlow<UserSession?> = repository.userSession
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Onboarding Live Visual Customization States ---
    private val _onboardingIsDarkMode = MutableStateFlow<Boolean?>(null)
    val onboardingIsDarkMode: StateFlow<Boolean?> = _onboardingIsDarkMode.asStateFlow()

    private val _onboardingFontSizeMultiplier = MutableStateFlow<Float?>(null)
    val onboardingFontSizeMultiplier: StateFlow<Float?> = _onboardingFontSizeMultiplier.asStateFlow()

    fun setOnboardingTheme(isDarkMode: Boolean) {
        _onboardingIsDarkMode.value = isDarkMode
    }

    fun setOnboardingFontSize(multiplier: Float) {
        _onboardingFontSizeMultiplier.value = multiplier
    }

    // --- Contacts state ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val contacts: StateFlow<List<Contact>> = _searchQuery
        .flatMapLatest { query -> repository.searchContacts(query) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Active Chat state ---
    private val _activeChatContact = MutableStateFlow<Contact?>(null)
    val activeChatContact: StateFlow<Contact?> = _activeChatContact.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChatMessages: StateFlow<List<Message>> = combine(
        userSession,
        _activeChatContact
    ) { session, contact ->
        if (session != null && contact != null) {
            session.phoneNumber to contact.phoneNumber
        } else {
            null
        }
    }.flatMapLatest { pair ->
        if (pair != null) {
            repository.getMessagesForChat(pair.first, pair.second)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Encryption Settings ---
    // User can toggle whether they want to display real-time encrypted ciphertext or plain text in bubbles
    private val _showCiphertextGlobal = MutableStateFlow(false)
    val showCiphertextGlobal: StateFlow<Boolean> = _showCiphertextGlobal.asStateFlow()

    fun toggleShowCiphertext() {
        _showCiphertextGlobal.value = !_showCiphertextGlobal.value
    }

    // --- Actions ---

    fun registerUser(phoneNumber: String, name: String, passphraseHex: String) {
        viewModelScope.launch {
            repository.registerUser(phoneNumber, name, passphraseHex)
        }
    }

    fun logout() {
        viewModelScope.launch {
            _activeChatContact.value = null
            repository.logout()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectContact(contact: Contact?) {
        _activeChatContact.value = contact
    }

    fun createContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            repository.addContact(name, phoneNumber)
        }
    }

    fun deleteContact(contact: Contact) {
        viewModelScope.launch {
            if (_activeChatContact.value?.phoneNumber == contact.phoneNumber) {
                _activeChatContact.value = null
            }
            repository.removeContact(contact)
        }
    }

    fun sendSecureMessage(plainText: String) {
        val session = userSession.value ?: return
        val contact = activeChatContact.value ?: return
        if (plainText.isBlank()) return

        viewModelScope.launch {
            // Encrypt using the user's password/phrase
            val phrase = session.passphraseHex
            val (cipher, iv) = EncryptionUtils.encrypt(plainText, phrase)
            
            val members = if (contact.isGroup) {
                contact.groupMembersJson.split(", ").map { it.trim() }
            } else {
                emptyList()
            }

            repository.sendEncryptedMessage(
                senderPhone = session.phoneNumber,
                receiverPhone = contact.phoneNumber,
                senderName = session.name,
                cipherText = cipher,
                ivString = iv,
                passphraseUsed = phrase,
                isGroup = contact.isGroup,
                groupMembersList = members,
                coroutineScope = viewModelScope
            )
        }
    }

    fun createGroup(name: String, membersStr: String) {
        val list = membersStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        viewModelScope.launch {
            repository.addGroup(name, list)
        }
    }

    fun updateUserProfile(name: String, avatarColorHex: String, avatarEmoji: String) {
        val session = userSession.value ?: return
        viewModelScope.launch {
            val updated = session.copy(
                name = name,
                avatarColorHex = avatarColorHex,
                avatarEmoji = avatarEmoji
            )
            repository.updateUserSession(updated)
        }
    }

    fun updateThemeCustomizations(chatBgColorHex: String, fontSizeMultiplier: Float, isDarkMode: Boolean) {
        val session = userSession.value ?: return
        viewModelScope.launch {
            val updated = session.copy(
                chatBgColorHex = chatBgColorHex,
                fontSizeMultiplier = fontSizeMultiplier,
                isDarkMode = isDarkMode
            )
            repository.updateUserSession(updated)
        }
    }
}

class SecureTextViewModelFactory(private val repository: SecureTextRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SecureTextViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SecureTextViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
