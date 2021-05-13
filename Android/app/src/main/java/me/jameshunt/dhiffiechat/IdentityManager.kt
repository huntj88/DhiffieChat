package me.jameshunt.dhiffiechat

import android.content.SharedPreferences
import android.util.Log
import me.jameshunt.dhiffiechat.crypto.*
import java.security.KeyPair

// TODO: move keypair to db
class IdentityManager(private val prefs: SharedPreferences) {
    private var cached: KeyPair? = null

    fun getIdentity(): KeyPair = synchronized(this) {
        get() ?: new().also { save(it) }
    }

    private fun get(): KeyPair? {
        cached?.let { return it }

        val privateBase64 = prefs.getString("private", null) ?: return null
        val publicBase64 = prefs.getString("public", null) ?: return null

        Log.d("public base64", publicBase64)
        Log.d("private base64", privateBase64)
        return KeyPair(publicBase64.toPublicKey(), privateBase64.toPrivateKey())
            .also { cached = it }
    }

    private fun new(): KeyPair = DHCrypto.genDHKeyPair()

    private fun save(keyPair: KeyPair) {
        cached = keyPair

        prefs.edit()
            .putString("private", keyPair.private.toBase64String())
            .putString("public", keyPair.public.toBase64String())
            .apply()
    }
}

fun KeyPair.toUserId(): String = public.toUserId()
