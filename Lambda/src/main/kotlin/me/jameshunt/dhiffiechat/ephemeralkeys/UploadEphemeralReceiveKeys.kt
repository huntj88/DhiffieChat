package me.jameshunt.dhiffiechat.ephemeralkeys

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.dhiffiechat.GatewayResponse
import me.jameshunt.dhiffiechat.Singletons
import me.jameshunt.dhiffiechat.awsTransformAuthed
import me.jameshunt.dhiffiechat.ephemeralKeyTable
import java.util.*

class UploadEphemeralReceiveKeys : RequestHandler<Map<String, Any?>, GatewayResponse> {
    data class UploadReceiveKeys(val newSignedKeys: List<SignedKey>)

    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<UploadReceiveKeys, Unit>(request, context) { keys, identity ->
            val ephemeralKeysTable = Singletons.dynamoDB.ephemeralKeyTable()

            keys.newSignedKeys
                .map { signedKey ->
                    EphemeralReceiveKey(
                        userId = identity.userId,
                        sortKey = UUID.randomUUID().toString(),
                        publicKey = signedKey.publicKey,
                        signature = signedKey.signature
                    )
                }
                .forEach { ephemeralKeysTable.putItem(it.toItem()) }
        }
    }
}
