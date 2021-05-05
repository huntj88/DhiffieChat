package me.jameshunt.dhiffiechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler


class GetUserRelationships : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, Unit, UserRelationships>(request, context) { _, _, identity ->
            val user = Singletons.dynamoDB.getTable("User").getItem("userId", identity.userId)

            UserRelationships(
                sentRequests = user.getList("sentRequests") ?: emptyList(),
                receivedRequests = user.getList("receivedRequests") ?: emptyList(),
                friends = user.getList("friends") ?: emptyList(),
            )
        }
    }
}

data class UserRelationships(
    val sentRequests: List<String>,
    val receivedRequests: List<String>,
    val friends: List<String>,
)