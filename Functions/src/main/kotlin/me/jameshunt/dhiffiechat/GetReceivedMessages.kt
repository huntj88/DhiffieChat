package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.QueryFilter
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit

class GetMessageSummaries : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, Unit, List<MessageFromUserSummary>>(request, context) { _, _, identity ->
            val messageTable = Singletons.dynamoDB.getTable("Message")
            val startPeriod = Instant.now().minus(14, ChronoUnit.DAYS)

            messageTable
                .query(
                    // TODO: filter by file upload finished, plan is to use s3 event trigger which
                    // TODO: kicks off lambda to set upload finished, and send notification
                    "to", identity.userId,
                    RangeKeyCondition("messageCreatedAt").between(startPeriod.format(), Instant.now().format()),
                    QueryFilter("signedS3Url").eq(null)
                )
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
    val messageCreatedAt: Instant,
    val text: String?,
    val fileKey: String,
    val iv: String,
    val signedS3Url: URL?,
    val signedS3UrlExpiration: Instant?
)

fun Item.toMessage(): Message {
    return Message(
        to = this.getString("to"),
        from = this.getString("from"),
        messageCreatedAt = Instant.parse(this.getString("messageCreatedAt")),
        text = this.getString("text"),
        fileKey = this.getString("fileKey"),
        iv = this.getString("iv"),
        signedS3Url = this.getString("signedS3Url")?.let { URL(it) },
        signedS3UrlExpiration = this.getString("signedS3UrlExpiration")?.let { Instant.parse(it) },
    )
}

fun Message.toItem(): Item {
    val map = mapOf(
        "to" to to,
        "from" to from,
        "messageCreatedAt" to messageCreatedAt.format(),
        "text" to text,
        "fileKey" to fileKey,
        "iv" to iv,
        "signedS3Url" to signedS3Url?.toString(),
        "signedS3UrlExpiration" to signedS3UrlExpiration?.format()
    )

    return Item.fromMap(map)
}