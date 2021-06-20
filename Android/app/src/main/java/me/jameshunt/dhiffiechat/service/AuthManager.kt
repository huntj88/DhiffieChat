package me.jameshunt.dhiffiechat.service

import com.squareup.moshi.Moshi
import me.jameshunt.dhiffiechat.BuildConfig
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

    private fun getServerPublicKey(): PublicKey = when(BuildConfig.SERVER_PUBLIC_KEY.isEmpty()) {
        false -> BuildConfig.SERVER_PUBLIC_KEY.toPublicKey()
        true -> {
            // FAKE one to init if first launch of server ever
            // TODO: running performRequest (Init) could happen as a last step to terraform
            DHCrypto.genDHKeyPair().public
        }
    }
}
