package me.jameshunt.dhiffiechat.ephemeralkeys

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.dhiffiechat.GatewayResponse
import me.jameshunt.dhiffiechat.Singletons
import me.jameshunt.dhiffiechat.awsTransformAuthed
import me.jameshunt.dhiffiechat.ephemeralKeyTable
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