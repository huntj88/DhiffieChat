package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.fasterxml.jackson.module.kotlin.readValue
import me.jameshunt.privatechat.crypto.*
import java.security.GeneralSecurityException
import java.security.KeyPair
import java.security.PublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.crypto.spec.IvParameterSpec

private data class Token(
    val type: String = "Authentication",
    val expires: String // instant
) {
    val expiresInstant: Instant
        get() = Instant.parse(expires)
}

data class Identity(val publicKey: PublicKey) {
    val userId: String
        get() = publicKey.toUserId()
}

fun doesUserHavePrivateKey(publicKey: PublicKey, iv: IvParameterSpec, encryptedToken: String): Boolean {
    val token = try {
        val sharedSecretKey = DHCrypto.agreeSecretKey(getServerKeyPair().private, publicKey)
        val tokenString = AESCrypto.decrypt(encryptedToken.toByteArray(), sharedSecretKey, iv)
        Singletons.objectMapper.readValue<Token>(tokenString)
    } catch (e: GeneralSecurityException) {
        e.printStackTrace()
        return false
    }

    return token.expiresInstant > Instant.now().minus(5, ChronoUnit.MINUTES)
}

fun validateAndGetIdentity(userId: String, iv: IvParameterSpec, encryptedToken: String): Identity {
    val publicKey = getUserPublicKey(userId)
    return when (doesUserHavePrivateKey(publicKey, iv, encryptedToken)) {
        true -> Identity(publicKey = publicKey)
        false -> throw Unauthorized()
    }
}

fun getUserPublicKey(userId: String): PublicKey {
    return Singletons.dynamoDB
        .getTable("User")
        .getItem(PrimaryKey("userId", userId))
        .asMap()
        .let { it["publicKey"] as String }
        .toPublicKey()
}

fun getClientPublicKey(userId: String): PublicKey {
    return Singletons.dynamoDB.getTable("User")
        .getItem(PrimaryKey("userId", userId))
        .asMap()["publicKey"]
        .let { it as String }
        .toPublicKey()
}

fun getServerKeyPair(): KeyPair {
    return KeyPair(serverPublic.toPublicKey(), serverPrivate.toPrivateKey())
}