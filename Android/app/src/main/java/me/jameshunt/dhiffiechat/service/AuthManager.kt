package me.jameshunt.dhiffiechat.service

import com.squareup.moshi.Moshi
import me.jameshunt.dhiffiechat.crypto.*
import java.security.PublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.crypto.SecretKey

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
        val encryptedToken: String,
        @Transient
        val sharedSecret: SecretKey
    )

    data class MessageCredentials(
        val userId: String,
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
        val token = Token(expires = Instant.now().plus(1L, ChronoUnit.MINUTES))

        val encryptedToken = AESCrypto
            .encrypt(input = token.toSerialized(), sharedSecretKey)
            .toBase64String()

        return ServerCredentials(
            userId = identityManager.getIdentity().toUserId(),
            encryptedToken = encryptedToken,
            sharedSecret = sharedSecretKey
        )
    }

    fun userToUserMessage(otherUser: PublicKey): MessageCredentials {
        val sharedSecretKey = DHCrypto.agreeSecretKey(
            prkSelf = identityManager.getIdentity().private,
            pbkPeer = otherUser
        )

        return MessageCredentials(
            userId = identityManager.getIdentity().toUserId(),
            sharedSecret = sharedSecretKey
        )
    }

    private fun getServerPublicKey(): PublicKey {
        // TODO: public key is generated and stored in s3 private bucket. provision this value from CI
        return "MIIBpjCCARsGCSqGSIb3DQEDATCCAQwCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoCAgIAA4GEAAKBgF7MsxRkNYr+N8oQEB0qCATd1iZqeRRFkJ5j5jEGysYwZ3WXTUNSBVDCySwHny0egoMZX92tIbOf0y3T10vB+8dG+E7QZ5jbN/UBjJPvrsztxit8Qu9ZP0TBTRU53uwPL5nhYu2dX62V4T2rTPhG1iOF50hNktKLyrhDDckzq3X/".toPublicKey()
    }
}
