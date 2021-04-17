package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.privatechat.crypto.toIv
import me.jameshunt.privatechat.crypto.toPublicKey

data class RequestData(
    var publicKey: String = "",
    var iv: String = "",
    var encryptedToken: String = ""
)

class CreateIdentity : RequestHandler<RequestData, Response> {
    override fun handleRequest(data: RequestData, context: Context): Response {
        if (!validateNewIdentity(data.publicKey.toPublicKey(), data.iv.toIv(), data.encryptedToken)) {
            throw Exception("not authed")
        }

        val defaultClient = AmazonDynamoDBClientBuilder.defaultClient()
        val hashedIdentity = Identity(data.publicKey.toPublicKey()).hashedIdentity

        return try {
            DynamoDB(defaultClient).getTable("User").putItem(
                Item.fromMap(
                    mapOf(
                        "HashedIdentity" to hashedIdentity,
                        "PublicKey" to data.publicKey
                    )
                )
            )
            Response(message = "success")
        } catch (e: Exception) {
            Response(message = e.message ?: hashedIdentity)
        }
    }
}