package com.example.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Saved login ("Save login info") for one-tap account switching.
 * The password is encrypted with a key kept inside the Android Keystore,
 * so it can only be read back on this phone by this app.
 */
object CredentialVault {
    private const val ALIAS = "vynora_vault_key"
    private const val PREFS = "vynora_vault"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(c: Context, accountId: String, loginId: String, password: String) {
        try {
            if (accountId.isBlank() || loginId.isBlank() || password.isBlank()) return
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val iv = cipher.iv
            val ct = cipher.doFinal("$loginId\n$password".toByteArray(Charsets.UTF_8))
            val packed = Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(ct, Base64.NO_WRAP)
            prefs(c).edit().putString(accountId, packed).apply()
        } catch (e: Exception) {
            // saving is optional: if it fails the user just types the password next time
        }
    }

    /** Returns Pair(loginId, password) or null if nothing saved / cannot be decrypted. */
    fun get(c: Context, accountId: String): Pair<String, String>? {
        return try {
            val packed = prefs(c).getString(accountId, null) ?: return null
            val parts = packed.split(":")
            if (parts.size != 2) return null
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val ct = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val text = String(cipher.doFinal(ct), Charsets.UTF_8)
            val i = text.indexOf('\n')
            if (i <= 0) null else Pair(text.substring(0, i), text.substring(i + 1))
        } catch (e: Exception) {
            null
        }
    }

    fun remove(c: Context, accountId: String) {
        try { prefs(c).edit().remove(accountId).apply() } catch (e: Exception) { }
    }
}
