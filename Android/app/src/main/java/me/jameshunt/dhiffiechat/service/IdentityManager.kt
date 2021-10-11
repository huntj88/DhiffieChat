package me.jameshunt.dhiffiechat.service

import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.jameshunt.dhiffiechat.Encryption_keyQueries
import me.jameshunt.dhiffiechat.crypto.*
import java.security.KeyPair

interface IdentityManager {
    fun getIdentity(): KeyPair
}

class IdentityManagerImpl(
    private val encryptionKeyQueries: Encryption_keyQueries
): IdentityManager {
    private var cached: KeyPair? = null

    override fun getIdentity(): KeyPair = synchronized(this) {
        get() ?: new().also { save(it) }
    }

    private fun get(): KeyPair? {
        cached?.let { return it }

        val existing = encryptionKeyQueries.selectIdentity().executeAsOneOrNull() ?: return null
        return KeyPair(existing.public_key.toRSAPublicKey(), existing.private_key.toRSAPrivateKey())
            .also { cached = it }
    }

    private fun new(): KeyPair = RSACrypto.generateKeyPair()

    private fun save(keyPair: KeyPair) {
        cached = keyPair

        encryptionKeyQueries.addKey(
            publicKey = keyPair.public.toBase64String(),
            privateKey = keyPair.private.toBase64String()
        )

        FirebaseCrashlytics.getInstance().setUserId(keyPair.toUserId())
    }
}

fun KeyPair.toUserId(): String = public.toUserId()
