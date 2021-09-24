package me.jameshunt.dhiffiechat.service

import io.reactivex.rxjava3.core.Single
import me.jameshunt.dhiffiechat.Encryption_keyQueries
import me.jameshunt.dhiffiechat.crypto.DHCrypto
import me.jameshunt.dhiffiechat.crypto.toBase64String
import java.security.PublicKey

class EphemeralKeySyncService(
    private val lambdaApi: LambdaApi,
    private val encryptionKeyQueries: Encryption_keyQueries
) {
    fun populateEphemeralReceiveKeys(): Single<Unit> {
        return lambdaApi
            .remainingEphemeralReceiveKeys()
            .map {
                val numToGenerate = 100 - it.remainingKeys
                (0 until numToGenerate).map { publicKeyOfNewKeyPair() }
            }
            .map { LambdaApi.UploadReceiveKeys(it) }
            .flatMap { lambdaApi.uploadEphemeralReceiveKeys(body = it) }
            .map { Unit }
    }

    private fun publicKeyOfNewKeyPair(): PublicKey {
        val keyPair = DHCrypto.genDHKeyPair()
        encryptionKeyQueries.addKey(
            publicKey = keyPair.public.toBase64String(),
            privateKey = keyPair.private.toBase64String()
        )

        return keyPair.public
    }
}