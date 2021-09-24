package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler

class RemainingEphemeralReceiveKeys : RequestHandler<Map<String, Any?>, GatewayResponse> {
    data class Response(val remainingKeys: Int)

    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, Response>(request, context) { _, identity ->

            val remaining = Singletons.dynamoDB.ephemeralKeyTable()
                .query("userId", identity.userId)
                .map { it.toEphemeralReceiveKey() }
                .size

            Response(remainingKeys = remaining)
        }
    }
}

data class EphemeralReceiveKey(
    val userId: String,
    val sortKey: String,
    val publicKey: String
)

fun Item.toEphemeralReceiveKey(): EphemeralReceiveKey {
    return EphemeralReceiveKey(
        userId = this.getString("userId"),
        sortKey = this.getString("sortKey"),
        publicKey = this.getString("publicKey")
    )
}

fun EphemeralReceiveKey.toItem(): Item {
    val map = mapOf(
        "userId" to userId,
        "sortKey" to sortKey,
        "publicKey" to publicKey
    )

    return Item.fromMap(map)
}