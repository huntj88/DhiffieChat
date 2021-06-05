package me.jameshunt.dhiffiechat.service

import com.squareup.moshi.Moshi
import me.jameshunt.dhiffiechat.crypto.*
import java.security.PublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

class AuthManager(
    private val identityManager: IdentityManager,
    private val moshi: Moshi
) {
    private data class Token(
        val type: String = "Authentication",
        val expires: Instant
    )

    data class ServerCredentials(
        val userId: String,
        val iv: IvParameterSpec,
        val encryptedToken: String,
        @Transient
        val sharedSecret: SecretKey
    )

    data class MessageCredentials(
        val userId: String,
        val iv: IvParameterSpec,
        @Transient
        val sharedSecret: SecretKey
    )

    private fun Token.toSerialized(): ByteArray = moshi
        .adapter(Token::class.java)
        .toJson(this)
        .toByteArray()

    fun userToServerAuth(): ServerCredentials {
        val sharedSecretKey = DHCrypto.agreeSecretKey(
            prkSelf = identityManager.getIdentity().private,
            pbkPeer = getServerPublicKey()
        )
        val iv = AESCrypto.generateIv()
        val token = Token(expires = Instant.now().plus(1L, ChronoUnit.MINUTES))

        val encryptedToken = AESCrypto
            .encrypt(input = token.toSerialized(), sharedSecretKey, iv)
            .toBase64String()

        return ServerCredentials(
            userId = identityManager.getIdentity().toUserId(),
            iv = iv,
            encryptedToken = encryptedToken,
            sharedSecret = sharedSecretKey
        )
    }

    fun userToUserMessage(serverPublicKey: PublicKey): MessageCredentials {
        val sharedSecretKey = DHCrypto.agreeSecretKey(
            prkSelf = identityManager.getIdentity().private,
            pbkPeer = serverPublicKey
        )

        return MessageCredentials(
            userId = identityManager.getIdentity().toUserId(),
            iv = AESCrypto.generateIv(),
            sharedSecret = sharedSecretKey
        )
    }

    private fun getServerPublicKey(): PublicKey {
        return serverPublic.toPublicKey()
    }
}
