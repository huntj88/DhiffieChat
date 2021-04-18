package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.privatechat.crypto.toIv
import me.jameshunt.privatechat.crypto.toPublicKey


data class RequestData(
    val publicKey: String,
    val iv: String,
    val encryptedToken: String
)

class CreateIdentity : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformUnit<RequestData>(request, context) { data ->
            if (!doesUserHavePrivateKey(data.publicKey.toPublicKey(), data.iv.toIv(), data.encryptedToken)) {
                throw Unauthorized()
            }

            val hashedIdentity = Identity(data.publicKey.toPublicKey()).hashedIdentity
            val identity = mapOf(
                "HashedIdentity" to hashedIdentity,
                "PublicKey" to data.publicKey
            )
            val defaultClient = AmazonDynamoDBClientBuilder.defaultClient()
            DynamoDB(defaultClient).getTable("User").putItem(Item.fromMap(identity))
        }
    }
}