package me.jameshunt.privatechat

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
        return awsTransform<RequestData, Unit, Unit>(request, context) { body, _ ->
            if (!doesUserHavePrivateKey(body.publicKey.toPublicKey(), body.iv.toIv(), body.encryptedToken)) {
                throw Unauthorized()
            }

            val hashedIdentity = Identity(body.publicKey.toPublicKey()).hashedIdentity
            val user = mapOf(
                "HashedIdentity" to hashedIdentity,
                "PublicKey" to body.publicKey
            )
            Singletons.dynamoDB.getTable("User").putItem(Item.fromMap(user))
        }
    }
}