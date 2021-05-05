package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.document.AttributeUpdate
import com.amazonaws.services.dynamodbv2.document.PrimaryKey
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.amazonaws.services.lambda.runtime.RequestHandler

data class QR(val scannedUserId: String)

class ScanQR : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<QR, Unit, Unit>(request, context) { body, _, identity ->
            val table = Singletons.dynamoDB.getTable("User")
            val item = table.getItem("userId", identity.userId)

            context.logger.log("checking if already friends")
            val friends = item?.getStringSet("friends") ?: emptySet()
            if (friends.contains(body.scannedUserId)) {
                return@awsTransformAuthed
            }

            val existingRequestFromScanned = item
                .getStringSet("receivedRequests")
                ?.contains(body.scannedUserId) ?: false

            context.logger.log("existingRequestFromScanned: $existingRequestFromScanned")
            when (existingRequestFromScanned) {
                true -> upgradeToFriend(table, identity.userId, body.scannedUserId, context.logger)
                false -> sendFriendRequest(table, identity.userId, body.scannedUserId, context.logger)
            }
        }
    }

    private fun upgradeToFriend(table: Table, userId: String, scannedUserId: String, logger: LambdaLogger) {
        logger.log("upgrading to friend")
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

    private fun sendFriendRequest(table: Table, userId: String, scannedUserId: String, logger: LambdaLogger) {
        logger.log("sending friend request")
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
