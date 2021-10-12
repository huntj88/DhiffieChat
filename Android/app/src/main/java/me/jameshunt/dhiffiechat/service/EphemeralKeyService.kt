package me.jameshunt.dhiffiechat.service

import me.jameshunt.dhiffiechat.Encryption_keyQueries
import me.jameshunt.dhiffiechat.crypto.toBase64String
import me.jameshunt.dhiffiechat.crypto.toDHPrivateKey
import java.security.PrivateKey
import java.security.PublicKey

interface EphemeralKeyService {
    fun getMatchingPrivateKey(ephemeralPublicKey: PublicKey): PrivateKey
    fun deleteKeyPair(ephemeralPublicKey: PublicKey)
}

class EphemeralKeyServiceImpl(
    private val encryptionKeyQueries: Encryption_keyQueries
) : EphemeralKeyService {
    override fun getMatchingPrivateKey(ephemeralPublicKey: PublicKey): PrivateKey {
        return encryptionKeyQueries
            .selectPrivate(publicKey = ephemeralPublicKey.toBase64String())
            .executeAsOne()
            .toDHPrivateKey()
    }

    override fun deleteKeyPair(ephemeralPublicKey: PublicKey) {
        encryptionKeyQueries.deleteEphemeral(publicKey = ephemeralPublicKey.toBase64String())
    }
}