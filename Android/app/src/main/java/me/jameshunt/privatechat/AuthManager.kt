package me.jameshunt.privatechat

import com.squareup.moshi.Moshi
import me.jameshunt.privatechat.crypto.AESCrypto
import me.jameshunt.privatechat.crypto.DHCrypto
import java.security.PublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.crypto.spec.IvParameterSpec

class AuthManager(
    private val identityManager: IdentityManager,
    private val moshi: Moshi
) {
    private data class Token(
        val type: String = "Authentication",
        val expires: Instant
    )

    data class AuthHeaders(
        val hashedIdentity: String,
        val iv: IvParameterSpec,
        val encryptedToken: String
    )

    private fun Token.toSerialized(): String = moshi.adapter(Token::class.java).toJson(this)

    // TODO: caching, return null after expiration
    fun getAuthHeaders(serverPublicKey: PublicKey): AuthHeaders {
        val sharedSecretKey = DHCrypto.agreeSecretKey(
            prkSelf = identityManager.getIdentity().privateKey,
            pbkPeer = serverPublicKey
        )
        val iv = AESCrypto.generateIv()
        val token = Token(expires = Instant.now().plus(5L, ChronoUnit.MINUTES))
        val encryptedToken = AESCrypto.encrypt(input = token.toSerialized(), sharedSecretKey, iv)

        return AuthHeaders(
            hashedIdentity = identityManager.getIdentity().hashedIdentity,
            iv = iv,
            encryptedToken = encryptedToken
        )
    }

    fun getIdentity(): Identity {
        return identityManager.getIdentity()
    }
}