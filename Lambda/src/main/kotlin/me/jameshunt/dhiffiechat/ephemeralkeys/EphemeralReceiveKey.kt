package me.jameshunt.dhiffiechat.ephemeralkeys

import com.amazonaws.services.dynamodbv2.document.Item

data class EphemeralReceiveKey(
    val userId: String,
    val sortKey: String,
    val publicKey: String,
    val signature: String
)

fun Item.toEphemeralReceiveKey(): EphemeralReceiveKey {
    return EphemeralReceiveKey(
        userId = this.getString("userId"),
        sortKey = this.getString("sortKey"),
        publicKey = this.getString("publicKey"),
        signature = this.getString("signature")
    )
}

fun EphemeralReceiveKey.toItem(): Item {
    val map = mapOf(
        "userId" to userId,
        "sortKey" to sortKey,
        "publicKey" to publicKey,
        "signature" to signature
    )

    return Item.fromMap(map)
}