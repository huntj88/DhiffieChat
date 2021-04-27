package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.document.AttributeUpdate
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler

data class QR(val scannedUserId: String)

class ScanQR : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<QR, Unit, Unit>(request, context) { body, _, identity ->
            val table = Singletons.dynamoDB.getTable("User")

            table.updateItem(
                PrimaryKey("userId", identity.userId),
                AttributeUpdate("sentRequests").addElements(body.scannedUserId)
            )

            table.updateItem(
                PrimaryKey("userId", body.scannedUserId),
                AttributeUpdate("receivedRequests").addElements(identity.userId)
            )
        }
    }
}
