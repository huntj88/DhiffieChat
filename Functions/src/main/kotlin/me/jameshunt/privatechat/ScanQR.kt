package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import java.util.*
import javax.crypto.spec.IvParameterSpec

data class QR(
    var selfHashedIdentity: String = "",
    var scannedHashedIdentity: String = "",
    var iv: String = "",
    var encryptedToken: String = ""
)

data class Response(val message: String)

class ScanQR: RequestHandler<QR, Response> {
    override fun handleRequest(data: QR, context: Context): Response {
        val identity = validateAndGetIdentity(
            hashedIdentity = data.selfHashedIdentity,
            iv = IvParameterSpec(Base64.getDecoder().decode(data.iv)),
            encryptedToken = data.encryptedToken
        ) ?: throw Exception("Not authed")

        val defaultClient = AmazonDynamoDBClientBuilder.defaultClient()
        val table = DynamoDB(defaultClient)
            .getTable("UserRelationship")

        // user 1 scanned it, set verified to true
        table.putItem(Item.fromMap(mapOf(
            "HashedIdentityUser1" to identity.hashedIdentity,
            "HashedIdentityUser2" to data.scannedHashedIdentity,
            "VerifiedByUser1" to true
        )))

        // user 2 had their QR scanned, but still has to verify
        // (user1, and user2 are swapped below)
        table.putItem(Item.fromMap(mapOf(
            "HashedIdentityUser1" to data.scannedHashedIdentity,
            "HashedIdentityUser2" to identity.hashedIdentity,
            "VerifiedByUser1" to false
        )))

        return Response(message = "Success")
    }
}
