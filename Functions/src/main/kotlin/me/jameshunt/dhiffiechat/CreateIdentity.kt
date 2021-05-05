package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.dhiffiechat.crypto.toIv
import me.jameshunt.dhiffiechat.crypto.toPublicKey
import me.jameshunt.dhiffiechat.crypto.toUserId


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
            val userTable = Singletons.dynamoDB.getTable("User")
            val userId = body.publicKey.toPublicKey().toUserId()

            if (userTable.getItem("userId", userId) == null) {
                val user = mapOf(
                    "userId" to userId,
                    "publicKey" to body.publicKey
                )
                userTable.putItem(Item.fromMap(user))
            }
        }
    }
}