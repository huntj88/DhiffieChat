package me.jameshunt.dhiffiechat.service

import com.squareup.moshi.Moshi
import me.jameshunt.dhiffiechat.BuildConfig
import me.jameshunt.dhiffiechat.crypto.*
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
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

    /**
     * Not securely encrypted, because 3/4 the two KeySets is public.
     * Enough to see that the message is "signed" by the user KeyPair, since you need the users
     * public key + shared KeySet to decrypt it
     */

    fun signByEncrypting(publicKey: PublicKey): String {
        val identity = identityManager.getIdentity()
        val secret = DHCrypto.agreeSecretKey(identity.private, getSharedSigningKeyPair().public)
        return AESCrypto.encrypt(publicKey.encoded, secret).toBase64String()
    }

    fun verifySigningByDecrypting(base64: String, otherUser: PublicKey): PublicKey {
        val secret = DHCrypto.agreeSecretKey(getSharedSigningKeyPair().private, otherUser)
        val pkByteArray = AESCrypto.decrypt(base64.base64ToByteArray(), secret)

        return KeyFactory.getInstance("DH").generatePublic(X509EncodedKeySpec(pkByteArray))
    }

    private fun getSharedSigningKeyPair(): KeyPair {
        val public = "MIIBojCCARcGCSqGSIb3DQEDATCCAQgCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYQAAoGAVjlmBmEKonUv2b0vpbfIImRilwzF/eNwaU8FLtXKF0T5+fYe42izXEYuq/FNABfkFZKbghBtJPHYX0wDS3EvgoDfSUsBKtNJXYQepfirc8bwNCh4FnxC2Fjs0azcxSeYcE9lnG/xilWk8luipN3OACz4ZpOHaRKr0f5vXk1Xxl8="
        val private = "MIIBpAIBADCCARcGCSqGSIb3DQEDATCCAQgCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoEgYMCgYB9T0DJNCD7JPxcKcx7MEo2m8TtH3JUQofTw79jnJFzyYwYT7EFigzJS8cQ4OqSAC+NP9MOrJp9Mk3ZJ8KbCOL8hwHQwFzoV+bkOfMJ6vbPt1colQ/rTOaP0EDmINTMdhVpMPqlZ7PdoAeT7IcQ8NzT1cWNjomDPAAV3Qwq84pjDQ=="
        return KeyPair(public.toPublicKey(), private.toPrivateKey())
    }

    private fun getServerPublicKey(): PublicKey = BuildConfig.SERVER_PUBLIC_KEY.toPublicKey()
}
