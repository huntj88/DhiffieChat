package me.jameshunt.dhiffiechat.service

import com.google.firebase.crashlytics.FirebaseCrashlytics
import me.jameshunt.dhiffiechat.Encryption_keyQueries
import me.jameshunt.dhiffiechat.crypto.*
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec

class IdentityManager(private val encryptionKeyQueries: Encryption_keyQueries) {
    private var cached: KeyPair? = null

    fun getIdentity(): KeyPair = synchronized(this) {
        get() ?: new().also { save(it) }
    }

    private fun get(): KeyPair? {
        cached?.let { return it }

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

        FirebaseCrashlytics.getInstance().setUserId(keyPair.toUserId())
    }
}

fun KeyPair.toUserId(): String = public.toUserId()


// TODO: found flaw, dhiffie-hellman will not work for signing/verifying public key came from a specific user, because 3/4 is public
// since 3/4 is public you can use the known parts to put any public key you want
// TODO: flow should still work, but signing ephemeral public keys needs to use different cryptography

/**
 * Not securely encrypted, because 3/4 the two KeySets is public.
 * Enough to see that the message is "signed" by the user KeyPair, since you need the users
 * public key + shared KeySet to decrypt it
 */

fun IdentityManager.signByEncrypting(publicKey: PublicKey): String {
    val secret = DHCrypto.agreeSecretKey(getIdentity().private, getSharedSigningKeyPair().public)
    return AESCrypto.encrypt(publicKey.encoded, secret).toBase64String()
}


// If public key can successfully be decrypted by the SharedSigning private key and the other users identity public key,
// then it was encrypted using other users identity private key
fun verifySigningByDecrypting(encryptedBase64PublicKey: String, otherUser: PublicKey): PublicKey {
    val secret = DHCrypto.agreeSecretKey(getSharedSigningKeyPair().private, otherUser)
    val pkByteArray = AESCrypto.decrypt(encryptedBase64PublicKey.base64ToByteArray(), secret)

    return KeyFactory.getInstance("DH").generatePublic(X509EncodedKeySpec(pkByteArray))
}

private fun getSharedSigningKeyPair(): KeyPair {
    val public = "MIIBojCCARcGCSqGSIb3DQEDATCCAQgCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYQAAoGAVjlmBmEKonUv2b0vpbfIImRilwzF/eNwaU8FLtXKF0T5+fYe42izXEYuq/FNABfkFZKbghBtJPHYX0wDS3EvgoDfSUsBKtNJXYQepfirc8bwNCh4FnxC2Fjs0azcxSeYcE9lnG/xilWk8luipN3OACz4ZpOHaRKr0f5vXk1Xxl8="
    val private = "MIIBpAIBADCCARcGCSqGSIb3DQEDATCCAQgCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoEgYMCgYB9T0DJNCD7JPxcKcx7MEo2m8TtH3JUQofTw79jnJFzyYwYT7EFigzJS8cQ4OqSAC+NP9MOrJp9Mk3ZJ8KbCOL8hwHQwFzoV+bkOfMJ6vbPt1colQ/rTOaP0EDmINTMdhVpMPqlZ7PdoAeT7IcQ8NzT1cWNjomDPAAV3Qwq84pjDQ=="
    return KeyPair(public.toPublicKey(), private.toPrivateKey())
}
