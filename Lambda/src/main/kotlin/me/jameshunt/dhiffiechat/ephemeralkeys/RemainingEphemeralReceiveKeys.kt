package me.jameshunt.dhiffiechat.ephemeralkeys

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.dhiffiechat.GatewayResponse
import me.jameshunt.dhiffiechat.Singletons
import me.jameshunt.dhiffiechat.awsTransformAuthed
import me.jameshunt.dhiffiechat.ephemeralKeyTable

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
    val signedPublicKey: String
)

fun Item.toEphemeralReceiveKey(): EphemeralReceiveKey {
    return EphemeralReceiveKey(
        userId = this.getString("userId"),
        sortKey = this.getString("sortKey"),
        signedPublicKey = this.getString("signedPublicKey")
    )
}

fun EphemeralReceiveKey.toItem(): Item {
    val map = mapOf(
        "userId" to userId,
        "sortKey" to sortKey,
        "signedPublicKey" to signedPublicKey
    )

    return Item.fromMap(map)
}