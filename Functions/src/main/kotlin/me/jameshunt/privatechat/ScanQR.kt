package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.document.AttributeUpdate
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler

data class QR(val scannedUserId: String)

class ScanQR : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<QR, Unit, Unit>(request, context) { body, _, identity ->
            val table = Singletons.dynamoDB.getTable("User")
            val item = table.getItem("userId", identity.userId)

            // check if already friends
            item?.getStringSet("friends")?.contains(body.scannedUserId) ?: return@awsTransformAuthed

            val existingRequestFromScanned = item
                .getStringSet("receivedRequests")
                ?.contains(body.scannedUserId) ?: false

            when (existingRequestFromScanned) {
                true -> upgradeToFriend(table, identity.userId, body.scannedUserId)
                false -> sendFriendRequest(table, identity.userId, body.scannedUserId)
            }
        }
    }

    private fun upgradeToFriend(table: Table, userId: String, scannedUserId: String) {
        table.updateItem(
            PrimaryKey("userId", userId),
            AttributeUpdate("receivedRequests").removeElements(scannedUserId),
            AttributeUpdate("friends").addElements(scannedUserId)
        )

        table.updateItem(
            PrimaryKey("userId", scannedUserId),
            AttributeUpdate("sentRequests").removeElements(userId),
            AttributeUpdate("friends").addElements(userId)
        )
    }

    private fun sendFriendRequest(table: Table, userId: String, scannedUserId: String) {
        table.updateItem(
            PrimaryKey("userId", userId),
            AttributeUpdate("sentRequests").addElements(scannedUserId)
        )

        table.updateItem(
            PrimaryKey("userId", scannedUserId),
            AttributeUpdate("receivedRequests").addElements(userId)
        )
    }
}
