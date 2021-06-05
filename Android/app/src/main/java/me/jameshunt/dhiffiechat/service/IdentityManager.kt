package me.jameshunt.dhiffiechat.service

import me.jameshunt.dhiffiechat.Encryption_keyQueries
import me.jameshunt.dhiffiechat.crypto.*
import java.security.KeyPair

class IdentityManager(private val encryptionKeyQueries: Encryption_keyQueries) {
    private var cached: KeyPair? = null

    fun getIdentity(): KeyPair = synchronized(this) {
        get() ?: new().also { save(it) }
    }

    private fun get(): KeyPair? {
        cached?.let { return it }

        // TODO: is Blocking a problem here?
        val existing = encryptionKeyQueries.selectIdentity().executeAsOneOrNull() ?: return null
        return KeyPair(existing.public_key.toPublicKey(), existing.private_key.toPrivateKey())
            .also { cached = it }
    }

    private fun new(): KeyPair = DHCrypto.genDHKeyPair()

    private fun save(keyPair: KeyPair) {
        cached = keyPair

        encryptionKeyQueries.addKey(
            publicKey = keyPair.public.toBase64String(),
            privateKey = keyPair.private.toBase64String()
        )
    }
}

fun KeyPair.toUserId(): String = public.toUserId()
