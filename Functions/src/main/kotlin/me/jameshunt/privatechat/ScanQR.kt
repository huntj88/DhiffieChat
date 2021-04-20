package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.document.AttributeUpdate
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.privatechat.crypto.toIv

data class QR(val scannedHashedIdentity: String)

class ScanQR : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<QR, Unit>(request, context) { data, identity ->
            val table = Singletons.dynamoDB.getTable("User")

            table.updateItem(
                PrimaryKey("HashedIdentity", identity.hashedIdentity),
                AttributeUpdate("SentRequests").addElements(data.scannedHashedIdentity)
            )

            table.updateItem(
                PrimaryKey("HashedIdentity", data.scannedHashedIdentity),
                AttributeUpdate("ReceivedRequests").addElements(identity.hashedIdentity)
            )
        }
    }
}
