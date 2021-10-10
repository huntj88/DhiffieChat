package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.document.AttributeUpdate
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.dhiffiechat.crypto.toRSAPublicKey
import me.jameshunt.dhiffiechat.crypto.toUserId

data class Credentials(
    val token: String,
    val signature: String
)

data class RequestData(
    val publicKey: String,
    val credentials: Credentials,
    val fcmToken: String
)

class CreateIdentity : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransform<RequestData, Unit>(request, context) { body ->
            if (!doesUserHavePrivateKey(
                    publicKey = body.publicKey.toRSAPublicKey(),
                    token = body.credentials.token,
                    signature = body.credentials.signature
                )
            ) {
                throw HandledExceptions.Unauthorized()
            }
            val userTable = Singletons.dynamoDB.userTable()
            val userId = body.publicKey.toRSAPublicKey().toUserId()

            when (userTable.getItem("userId", userId) == null) {
                true -> {
                    val user = mapOf(
                        "userId" to userId,
                        "publicKey" to body.publicKey,
                        "fcmToken" to body.fcmToken
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