package me.jameshunt.dhiffiechat.ephemeralkeys

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.dhiffiechat.*

class GetEphemeralPublicKey : RequestHandler<Map<String, Any?>, GatewayResponse> {
    data class Request(val userId: String)

    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Request, SignedKey>(request, context) { requestData, identity ->
            ensureFriends(identity.userId, requestData.userId)

            val ephemeralKeysTable = Singletons.dynamoDB.ephemeralKeyTable()
            val nextKey = ephemeralKeysTable
                .query("userId", requestData.userId)
                .firstOrNull()
                .let { it ?: throw HandledExceptions.NotFound() }
                .toEphemeralReceiveKey()
                .also {
                    ephemeralKeysTable.deleteItem(
                        "userId", it.userId,
                        "sortKey", it.sortKey
                    )
                }

            SignedKey(publicKey = nextKey.publicKey, signature = nextKey.signature)
        }
    }
}