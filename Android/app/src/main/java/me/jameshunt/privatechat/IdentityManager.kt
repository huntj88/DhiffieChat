package me.jameshunt.privatechat

import android.content.SharedPreferences
import android.util.Log
import me.jameshunt.privatechat.crypto.DHCrypto
import me.jameshunt.privatechat.crypto.toPrivateKey
import me.jameshunt.privatechat.crypto.toPublicKey
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.util.*

class IdentityManager(private val getSharedPreferences: () -> SharedPreferences) {
    private var cached: Identity? = null

    fun getIdentity(): Identity = synchronized(this) {
        get() ?: new().also { save(it) }
    }

    private fun get(): Identity? {
        cached?.let { return it }

        val sharedPreferences = getSharedPreferences()
        val privateBase64 = sharedPreferences.getString("private", null) ?: return null
        val publicBase64 = sharedPreferences.getString("public", null) ?: return null

        Log.d("public base64", publicBase64)
        Log.d("private base64", privateBase64)
        return Identity(
            privateKey = privateBase64.toPrivateKey(),
            publicKey = publicBase64.toPublicKey()
        ).also { cached = it }
    }

    private fun new(): Identity = DHCrypto.genDHKeyPair().let {
        Identity(
            privateKey = it.private,
            publicKey = it.public
        )
    }

    private fun save(identity: Identity) {
        cached = identity
        val encoder = Base64.getEncoder()
        val privateKey = encoder.encodeToString(identity.privateKey.encoded)
        val publicKey = encoder.encodeToString(identity.publicKey.encoded)

        getSharedPreferences().edit()
            .putString("private", privateKey)
            .putString("public", publicKey)
            .apply()
    }
}

data class Identity(
    val privateKey: PrivateKey,
    val publicKey: PublicKey
) {
    val hashedIdentity: String
        get() = publicKey.encoded.toHashedIdentity()
}

fun ByteArray.toHashedIdentity(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this)
    .let { Base64.getEncoder().encodeToString(it) }