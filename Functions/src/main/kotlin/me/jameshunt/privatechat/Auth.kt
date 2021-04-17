package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.fasterxml.jackson.module.kotlin.readValue
import me.jameshunt.privatechat.crypto.AESCrypto
import me.jameshunt.privatechat.crypto.DHCrypto
import me.jameshunt.privatechat.crypto.toPrivateKey
import me.jameshunt.privatechat.crypto.toPublicKey
import java.security.KeyPair
import java.security.MessageDigest
import java.security.PublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.crypto.spec.IvParameterSpec

private data class Token(
    val type: String = "Authentication",
    val expires: String // instant
) {
    val expiresInstant: Instant
        get() = Instant.parse(expires)
}

data class Identity(val publicKey: PublicKey) {
    val hashedIdentity: String
        get() = MessageDigest
            .getInstance("SHA-256")
            .digest(publicKey.encoded)
            .let { Base64.getEncoder().encodeToString(it) }
}

fun validateNewIdentity(publicKey: PublicKey, iv: IvParameterSpec, encryptedToken: String): Boolean {
    val sharedSecretKey = DHCrypto.agreeSecretKey(getServerKeyPair().private, publicKey)
    val tokenString = AESCrypto.decrypt(encryptedToken, sharedSecretKey, iv)
    val token = objectMapper.readValue<Token>(tokenString)

    return token.expiresInstant > Instant.now().minus(5, ChronoUnit.MINUTES)
}

fun validateAndGetIdentity(hashedIdentity: String, iv: IvParameterSpec, encryptedToken: String): Identity? {
    val publicKey = getUserPublicKey(hashedIdentity)

    val sharedSecretKey = DHCrypto.agreeSecretKey(getServerKeyPair().private, publicKey)
    val tokenString = AESCrypto.decrypt(encryptedToken, sharedSecretKey, iv)
    val token = objectMapper.readValue<Token>(tokenString)
    if (token.expiresInstant < Instant.now().minus(5, ChronoUnit.MINUTES)) {
        return null
    }

    return Identity(publicKey = publicKey)
}

// for testing
fun getServerKeyPair(): KeyPair {
    val public = "MIIBojCCARcGCSqGSIb3DQEDATCCAQgCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYQAAoGAVjlmBmEKonUv2b0vpbfIImRilwzF/eNwaU8FLtXKF0T5+fYe42izXEYuq/FNABfkFZKbghBtJPHYX0wDS3EvgoDfSUsBKtNJXYQepfirc8bwNCh4FnxC2Fjs0azcxSeYcE9lnG/xilWk8luipN3OACz4ZpOHaRKr0f5vXk1Xxl8="
    val private = "MIIBpAIBADCCARcGCSqGSIb3DQEDATCCAQgCgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZPY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoEgYMCgYB9T0DJNCD7JPxcKcx7MEo2m8TtH3JUQofTw79jnJFzyYwYT7EFigzJS8cQ4OqSAC+NP9MOrJp9Mk3ZJ8KbCOL8hwHQwFzoV+bkOfMJ6vbPt1colQ/rTOaP0EDmINTMdhVpMPqlZ7PdoAeT7IcQ8NzT1cWNjomDPAAV3Qwq84pjDQ=="
    return KeyPair(public.toPublicKey(), private.toPrivateKey())
}

fun getUserPublicKey(hashedIdentity: String): PublicKey {
    val defaultClient = AmazonDynamoDBClientBuilder.defaultClient()
    return DynamoDB(defaultClient)
        .getTable("User")
        .getItem(PrimaryKey("HashedIdentity", hashedIdentity))
        .asMap()
        .let { it["PublicKey"] as String }
        .toPublicKey()
}