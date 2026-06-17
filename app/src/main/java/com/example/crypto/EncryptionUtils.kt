package com.example.crypto

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

object EncryptionUtils {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    private const val KEY_LENGTH = 16 // 16 bytes for AES-128 or derivation

    /**
     * Derives a 128-bit key from any arbitrary passphrase using SHA-256 truncated.
     */
    fun deriveKey(passphrase: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(passphrase.toByteArray(Charsets.UTF_8))
        val keyBytes = hash.copyOf(KEY_LENGTH) // Take first 16 bytes for AES-128
        return SecretKeySpec(keyBytes, ALGORITHM)
    }

    /**
     * Encrypts the plain text using the secret key derived from passphrase.
     * Returns a Pair of (Ciphertext, IV) as Base64 encoded strings.
     */
    fun encrypt(plainText: String, passphrase: String): Pair<String, String> {
        return try {
            val key = deriveKey(passphrase)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            // Generate a random 16-byte IV
            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            
            val cipherTextBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            
            Pair(cipherTextBase64, ivBase64)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("", "")
        }
    }

    /**
     * Decrypts the ciphertext using the IV and the key derived from passphrase.
     */
    fun decrypt(cipherText: String, ivString: String, passphrase: String): String {
        return try {
            if (cipherText.isEmpty() || ivString.isEmpty()) return ""
            
            val key = deriveKey(passphrase)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            
            val iv = Base64.decode(ivString, Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(cipherText, Base64.NO_WRAP)
            
            val ivSpec = IvParameterSpec(iv)
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
            "[Decoding Error: Passphrase mismatched or packet integrity corrupted]"
        }
    }
}
