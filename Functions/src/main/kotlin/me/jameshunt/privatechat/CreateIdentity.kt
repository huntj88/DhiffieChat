package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.module.kotlin.readValue
import me.jameshunt.privatechat.crypto.toIv
import me.jameshunt.privatechat.crypto.toPublicKey


data class RequestData(
    var publicKey: String = "",
    var iv: String = "",
    var encryptedToken: String = ""
)

class CreateIdentity : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        context.logger.log(objectMapper.writeValueAsBytes(request))
        val data = objectMapper.readValue<RequestData>(request["body"]!!.toString())

        if (!doesUserHavePrivateKey(data.publicKey.toPublicKey(), data.iv.toIv(), data.encryptedToken)) {
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
            GatewayResponse(body = objectMapper.writeValueAsString(mapOf("message" to "success")))
        } catch (e: Exception) {
            GatewayResponse(body = objectMapper.writeValueAsString(mapOf("message" to (e.message ?: hashedIdentity))))
        }
    }
}