package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

data class RequestData(
    var publicKey: String = ""
)

class CreateIdentity: RequestHandler<RequestData, Response> {
    override fun handleRequest(data: RequestData, context: Context): Response {
        val defaultClient = AmazonDynamoDBClientBuilder.defaultClient()
        DynamoDB(defaultClient)
            .getTable("User")
            .putItem(Item.fromMap(mapOf(
                "HashedIdentity" to data.publicKey.toPublicKey().toHashedIdentity(),
                "PublicKey" to data.publicKey
            )))

        return Response(message = "Success")
    }

    private fun String.toPublicKey(): PublicKey {
        val bytes = Base64.getDecoder().decode(this)
        return KeyFactory.getInstance("DH").generatePublic(X509EncodedKeySpec(bytes))
    }

    private fun PublicKey.toHashedIdentity(): String {
        return MessageDigest
            .getInstance("SHA-256")
            .digest(encoded)
            .let { Base64.getEncoder().encodeToString(it) }
    }
}