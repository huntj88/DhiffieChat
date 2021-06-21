package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.fasterxml.jackson.module.kotlin.readValue
import me.jameshunt.dhiffiechat.crypto.*
import java.security.GeneralSecurityException
import java.security.PublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit


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

fun doesUserHavePrivateKey(publicKey: PublicKey, encryptedToken: String): Boolean {
    val token = try {
        val sharedSecretKey = DHCrypto.agreeSecretKey(Singletons.credentials.serverPrivateKey, publicKey)
        val tokenString = AESCrypto.decrypt(encryptedToken.base64ToByteArray(), sharedSecretKey)
        Singletons.objectMapper.readValue<Token>(tokenString)
    } catch (e: GeneralSecurityException) {
        e.printStackTrace()
        return false
    }

    return token.expiresInstant > Instant.now().minus(5, ChronoUnit.MINUTES)
}

fun validateAndGetIdentity(userId: String, encryptedToken: String): Identity {
    val publicKey = getUserPublicKey(userId)
    return when (doesUserHavePrivateKey(publicKey, encryptedToken)) {
        true -> Identity(publicKey = publicKey)
        false -> throw Unauthorized()
    }
}

fun getUserPublicKey(userId: String): PublicKey {
    return Singletons.dynamoDB
        .userTable()
        .getItem(PrimaryKey("userId", userId))
        .asMap()
        .let { it["publicKey"] as String }
        .toPublicKey()
}
