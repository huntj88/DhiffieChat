package me.jameshunt.dhiffiechat

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class PrefManager(private val prefs: SharedPreferences) {
    fun isFirstLaunch(): Boolean = prefs.getBoolean("isFirstLaunch", true).also {
        if (it) {
            prefs.edit().putBoolean("isFirstLaunch", false).apply()
        }
    }

    fun getDBPassword(): String {
        val key = "encryptedDBPassword"
        val androidCrypto = AndroidCrypto(key)

        fun generateDefaultPassword(): String = androidCrypto.encrypt(UUID.randomUUID().toString())
            .also { prefs.edit().putString(key, it).apply() }

        return androidCrypto.decrypt(
            toDecrypt = prefs.getString(key, null) ?: generateDefaultPassword()
        )
    }


}

private class AndroidCrypto(private val keyName: String) {
    private var keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    private var secretKey: SecretKey = getKey()

    private fun getKey(): SecretKey {
        return keyStore
            .getEntry(keyName, null)
            ?.let { it as KeyStore.SecretKeyEntry }
            ?.secretKey
            ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val keyGenerator =
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val keyGenParameterSpec = KeyGenParameterSpec
            .Builder(keyName, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .build()
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    fun encrypt(toEncrypt: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val encrypted = Base64.encodeToString(
            cipher.doFinal(toEncrypt.toByteArray(StandardCharsets.UTF_8)),
            Base64.NO_WRAP
        )
        return encrypted + SEPARATOR + iv
    }

    fun decrypt(toDecrypt: String): String {
        val parts = toDecrypt.split(SEPARATOR).toTypedArray()
        if (parts.size != 2) throw AssertionError("String to decrypt must be of the form: 'BASE64_DATA" + SEPARATOR + "BASE64_IV'")
        val encrypted = Base64.decode(parts[0], Base64.DEFAULT)
        val iv = Base64.decode(parts[1], Base64.DEFAULT)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    companion object {
        private const val TRANSFORMATION =
            KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val SEPARATOR = ","
    }
}