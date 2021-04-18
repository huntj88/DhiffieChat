package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
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

            val defaultClient = AmazonDynamoDBClientBuilder.defaultClient()
            val table = DynamoDB(defaultClient).getTable("UserRelationship")

            // user 1 scanned it, set verified to true
            val relationshipOne = mapOf(
                "HashedIdentityUser1" to identity.hashedIdentity,
                "HashedIdentityUser2" to data.scannedHashedIdentity,
                "VerifiedByUser1" to true
            )
            table.putItem(Item.fromMap(relationshipOne))

            // user 2 had their QR scanned, but still has to verify
            // (user1, and user2 are swapped below)
            val relationshipTwo = mapOf(
                "HashedIdentityUser1" to data.scannedHashedIdentity,
                "HashedIdentityUser2" to identity.hashedIdentity,
                "VerifiedByUser1" to false
            )
            table.putItem(Item.fromMap(relationshipTwo))
        }
    }
}
