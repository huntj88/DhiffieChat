package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.module.kotlin.readValue
import me.jameshunt.privatechat.crypto.toIv
import java.util.*
import javax.crypto.spec.IvParameterSpec

data class QR(
    val selfHashedIdentity: String,
    val scannedHashedIdentity: String,
    val iv: String,
    val encryptedToken: String
)

data class Response(val message: String)

class ScanQR: RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        context.logger.log(objectMapper.writeValueAsBytes(request))
        val data = objectMapper.readValue<QR>(request["body"]!!.toString())
        context.logger.log(data.toString())
        val identity = validateAndGetIdentity(
            hashedIdentity = data.selfHashedIdentity,
            iv = data.iv.toIv(),
            encryptedToken = data.encryptedToken
        ) ?:  return GatewayResponse(
            body = objectMapper.writeValueAsString(mapOf("message" to "Authentication error")),
            statusCode = 401
        )

        val defaultClient = AmazonDynamoDBClientBuilder.defaultClient()
        val table = DynamoDB(defaultClient).getTable("UserRelationship")

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

        return GatewayResponse(body = objectMapper.writeValueAsString(mapOf("message" to "success")))
    }
}
