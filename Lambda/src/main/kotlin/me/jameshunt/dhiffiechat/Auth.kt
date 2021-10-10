package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.fasterxml.jackson.module.kotlin.readValue
import me.jameshunt.dhiffiechat.crypto.*
import java.security.PublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit


private data class Token(
    val userId: String,
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

fun doesUserHavePrivateKey(publicKey: PublicKey, token: String, signature: String): Boolean {
    return if (RSACrypto.canVerify(token, signature, publicKey)) {
        val decoded = Singletons.objectMapper.readValue<Token>(token.base64ToByteArray().toString(Charsets.UTF_8))
        decoded.expiresInstant > Instant.now().minus(5, ChronoUnit.MINUTES)
    } else {
        false
    }
}

fun validateAndGetIdentity(userId: String, token: String, signature: String): Identity {
    val publicKey = getUserPublicKey(userId)
    return when (doesUserHavePrivateKey(publicKey, token, signature)) {
        true -> Identity(publicKey = publicKey)
        false -> throw HandledExceptions.Unauthorized()
    }
}

fun getUserPublicKey(userId: String): PublicKey {
    return Singletons.dynamoDB
        .userTable()
        .getItem(PrimaryKey("userId", userId))
        .asMap()
        .let { it["publicKey"] as String }
        .toRSAPublicKey()
}

fun ensureFriends(userIdA: String, userIdB: String) {
    if (!isFriends(userIdA, userIdB)) {
        throw HandledExceptions.Unauthorized()
    }
}

fun isFriends(userIdA: String, userIdB: String): Boolean {
    val table = Singletons.dynamoDB.userTable()
    val item = table.getItem("userId", userIdA)

    val friends = item?.getStringSet("friends") ?: emptySet()
    return friends.contains(userIdB)
}
