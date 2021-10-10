package me.jameshunt.dhiffiechat.user

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.dhiffiechat.GatewayResponse
import me.jameshunt.dhiffiechat.Singletons
import me.jameshunt.dhiffiechat.awsTransformAuthed
import me.jameshunt.dhiffiechat.userTable


class GetUserRelationships : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, UserRelationships>(request, context) { _, identity ->
            val user = Singletons.dynamoDB.userTable().getItem("userId", identity.userId)

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