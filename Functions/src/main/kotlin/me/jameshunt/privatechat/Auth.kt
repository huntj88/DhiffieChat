package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.fasterxml.jackson.module.kotlin.readValue
import me.jameshunt.privatechat.crypto.*
import java.security.GeneralSecurityException
import java.security.KeyPair
import java.security.MessageDigest
import java.security.PublicKey
import java.time.Instant
import java.time.format.DateTimeFormatter
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
        get() = publicKey.toHashedIdentity()
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

fun validateAndGetIdentity(hashedIdentity: String, iv: IvParameterSpec, encryptedToken: String): Identity {
    val publicKey = getUserPublicKey(hashedIdentity)
    return when (doesUserHavePrivateKey(publicKey, iv, encryptedToken)) {
        true -> Identity(publicKey = publicKey)
        false -> throw Unauthorized()
    }
}

fun getUserPublicKey(hashedIdentity: String): PublicKey {
    return Singletons.dynamoDB
        .getTable("User")
        .getItem(PrimaryKey("HashedIdentity", hashedIdentity))
        .asMap()
        .let { it["PublicKey"] as String }
        .toPublicKey()
}

fun getClientPublicKey(hashedIdentity: String): PublicKey {
    return Singletons.dynamoDB.getTable("User")
        .getItem(PrimaryKey("HashedIdentity", hashedIdentity))
        .asMap()["PublicKey"]
        .let { it as String }
        .toPublicKey()
}

fun getServerKeyPair(): KeyPair {
    val existingPrivate = getConfigProperty("PrivateKey", checkExpiration = true)?.toPrivateKey()
    val existingPublic = getConfigProperty("PublicKey", checkExpiration = true)?.toPublicKey()

    return existingPrivate?.let { KeyPair(existingPublic!!, it) }
        ?: DHCrypto.genDHKeyPair().also { saveServerKeyPair(it) }
}

fun saveServerKeyPair(keyPair: KeyPair) {
    val expiresAt = Instant.now().plus(2, ChronoUnit.HOURS)

    setConfigProperty(name = "PrivateKey", value = keyPair.private.toBase64String(), expiresAt = expiresAt)
    setConfigProperty(name = "PublicKey", value = keyPair.public.toBase64String(), expiresAt = expiresAt)
}

private fun getConfigProperty(name: String, checkExpiration: Boolean): String? {
    return Singletons.dynamoDB
        .getTable("Config")
        .getItem(PrimaryKey("Name", name))
        ?.asMap()
        ?.let { item ->
            val expiresAt = (item["ExpiresAt"] as? String)?.let { Instant.parse(it) }
            when (!checkExpiration || expiresAt == null || expiresAt > Instant.now()) {
                true -> item["Value"] as String
                false -> null
            }
        }
}

private fun setConfigProperty(name: String, value: String, expiresAt: Instant?) {
    Singletons.dynamoDB
        .getTable("Config")
        .putItem(
            Item.fromMap(
                mapOf(
                    "Name" to name,
                    "Value" to value,
                    "ExpiresAt" to DateTimeFormatter.ISO_INSTANT.format(expiresAt)
                )
            )
        )
}