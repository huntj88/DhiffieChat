package me.jameshunt.privatechat

import com.squareup.moshi.Moshi
import me.jameshunt.privatechat.crypto.AESCrypto
import me.jameshunt.privatechat.crypto.DHCrypto
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
        val hashedIdentity: String,
        val iv: IvParameterSpec,
        val encryptedToken: String,
        @Transient
        val sharedSecret: SecretKey
    )

    private fun Token.toSerialized(): ByteArray = moshi
        .adapter(Token::class.java)
        .toJson(this)
        .toByteArray()

    // TODO: caching, return null after expiration
    // for user-to-server or user-to-user
    fun userToOtherAuth(serverPublicKey: PublicKey): AuthCredentials {
        val sharedSecretKey = DHCrypto.agreeSecretKey(
            prkSelf = identityManager.getIdentity().privateKey,
            pbkPeer = serverPublicKey
        )
        val iv = AESCrypto.generateIv()
        val token = Token(expires = Instant.now().plus(5L, ChronoUnit.MINUTES))
        val encryptedToken = AESCrypto.encrypt(input = token.toSerialized(), sharedSecretKey, iv)

        return AuthCredentials(
            hashedIdentity = identityManager.getIdentity().hashedIdentity,
            iv = iv,
            encryptedToken = encryptedToken.toString(StandardCharsets.UTF_8),
            sharedSecret = sharedSecretKey
        )
    }

    fun getIdentity(): Identity {
        return identityManager.getIdentity()
    }
}