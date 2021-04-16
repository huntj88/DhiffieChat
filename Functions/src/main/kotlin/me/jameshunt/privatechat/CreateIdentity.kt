package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.privatechat.crypto.toPublicKey

data class RequestData(
    var publicKey: String = ""
)

class CreateIdentity: RequestHandler<RequestData, Response> {
    override fun handleRequest(data: RequestData, context: Context): Response {
        val defaultClient = AmazonDynamoDBClientBuilder.defaultClient()
        DynamoDB(defaultClient)
            .getTable("User")
            .putItem(Item.fromMap(mapOf(
                "HashedIdentity" to Identity(data.publicKey.toPublicKey()).hashedIdentity,
                "PublicKey" to data.publicKey
            )))

        return Response(message = "Success")
    }
}