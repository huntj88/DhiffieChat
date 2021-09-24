package me.jameshunt.dhiffiechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import java.util.*

class UploadEphemeralReceiveKeys : RequestHandler<Map<String, Any?>, GatewayResponse> {
    data class UploadReceiveKeys(val newKeys: List<String>)

    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<UploadReceiveKeys, Unit>(request, context) { keys, identity ->
            val ephemeralKeysTable = Singletons.dynamoDB.ephemeralKeyTable()

            keys.newKeys
                .map { publicKeyAsBase64 ->
                    EphemeralReceiveKey(
                        userId = identity.userId,
                        sortKey = UUID.randomUUID().toString(),
                        publicKey = publicKeyAsBase64
                    )
                }
                .forEach { ephemeralKeysTable.putItem(it.toItem()) }
        }
    }
}