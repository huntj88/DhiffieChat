package me.jameshunt.dhiffiechat

import com.squareup.moshi.Moshi
import me.jameshunt.dhiffiechat.crypto.AESCrypto
import me.jameshunt.dhiffiechat.crypto.DHCrypto
import java.nio.charset.StandardCharsets
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

    data class AuthCredentials(
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

    // TODO: caching, return null after expiration
    fun userToServerAuth(serverPublicKey: PublicKey): AuthCredentials {
        val sharedSecretKey = DHCrypto.agreeSecretKey(
            prkSelf = identityManager.getIdentity().private,
            pbkPeer = serverPublicKey
        )
        val iv = AESCrypto.generateIv()
        val token = Token(expires = Instant.now().plus(5L, ChronoUnit.MINUTES))
        val encryptedToken = AESCrypto.encrypt(input = token.toSerialized(), sharedSecretKey, iv)

        return AuthCredentials(
            userId = identityManager.getIdentity().toUserId(),
            iv = iv,
            encryptedToken = encryptedToken.toString(StandardCharsets.UTF_8),
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
}