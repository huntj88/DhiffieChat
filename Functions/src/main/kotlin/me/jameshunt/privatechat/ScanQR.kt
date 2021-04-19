package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.document.AttributeUpdate
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.privatechat.crypto.toIv

data class QR(
    val selfHashedIdentity: String,
    val scannedHashedIdentity: String,
    val iv: String,
    val encryptedToken: String
)

class ScanQR : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformUnit<QR>(request, context) { data ->
            val identity = validateAndGetIdentity(
                hashedIdentity = data.selfHashedIdentity,
                iv = data.iv.toIv(),
                encryptedToken = data.encryptedToken
            )

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
