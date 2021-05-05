package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler

class GetMessageSummaries : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, Unit, List<MessageFromUserSummary>>(request, context) { _, _, identity ->
            val messageTable = Singletons.dynamoDB.getTable("Message")

            messageTable
                .query("to", identity.userId)
                .asIterable()
                .map { it.toMessage() }
                .groupBy { it.from }
                .map { (from, messagesFromOneUser) ->
                    MessageFromUserSummary(
                        from = from,
                        count = messagesFromOneUser.count(),
                        next = messagesFromOneUser.minByOrNull { it.messageCreatedAt }!!
                    )
                }
        }
    }
}

data class MessageFromUserSummary(
    val from: String,
    val count: Int,
    val next: Message
)

data class Message(
    val to: String,
    val from: String,
    val messageCreatedAt: String,
    val text: String?,
    val fileKey: String,
    val iv: String,
    val authedUrl: String?
)

fun Item.toMessage(): Message {
    return Message(
        to = this.getString("to"),
        from = this.getString("from"),
        messageCreatedAt = this.getString("messageCreatedAt"),
        text = this.getString("text"),
        fileKey = this.getString("fileKey"),
        iv = this.getString("iv"),
        authedUrl = this.getString("authedUrl")
    )
}

fun Message.toItem(): Item {
    val map = mapOf(
        "to" to to,
        "from" to from,
        "messageCreatedAt" to messageCreatedAt,
        "text" to text,
        "fileKey" to fileKey,
        "iv" to iv,
        "authedUrl" to authedUrl
    )

    return Item.fromMap(map)
}