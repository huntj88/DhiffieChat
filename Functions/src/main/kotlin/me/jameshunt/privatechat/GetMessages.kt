package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler

class GetMessages : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, GetMessageQueryParams, List<Message>>(request, context) { _, params, identity ->
            val chatTable = Singletons.dynamoDB.getTable("Chat")
            val query = chatTable.query("chatId", chatId(identity.userId, params.userId))

            query.asIterable().map { it.toMessage() }
        }
    }
}

data class GetMessageQueryParams(
    val userId: String
)

data class Message(
    @Transient
    val chatId: String,
    val messageCreatedAt: String,
    val from: String,
    val to: String,
    val text: String?,
    val fileKey: String?,
    val iv: String
)

private fun Item.toMessage(): Message {
    return Message(
        chatId = this.getString("chatId"),
        messageCreatedAt = this.getString("messageCreatedAt"),
        from = this.getString("from"),
        to = this.getString("to"),
        text = this.getString("text"),
        fileKey = this.getString("fileKey"),
        iv = this.getString("iv")
    )
}