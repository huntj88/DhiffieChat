package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.document.AttributeUpdate
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.dhiffiechat.crypto.toIv
import me.jameshunt.dhiffiechat.crypto.toPublicKey
import me.jameshunt.dhiffiechat.crypto.toUserId


data class RequestData(
    val publicKey: String,
    val iv: String,
    val encryptedToken: String,
    val fcmToken: String
)

class CreateIdentity : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransform<RequestData, Unit>(request, context) { body ->
            if (!doesUserHavePrivateKey(body.publicKey.toPublicKey(), body.iv.toIv(), body.encryptedToken)) {
                throw Unauthorized()
            }
            val userTable = Singletons.dynamoDB.getTable("User")
            val userId = body.publicKey.toPublicKey().toUserId()

            when (userTable.getItem("userId", userId) == null) {
                true -> {
                    val user = mapOf(
                        "userId" to userId,
                        "publicKey" to body.publicKey
                    )
                    userTable.putItem(Item.fromMap(user))
                }
                false -> userTable.updateItem(
                    "userId", userId,
                    AttributeUpdate("fcmToken").put(body.fcmToken)
                )
            }
        }
    }
}