package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.QueryFilter
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.annotation.JsonIgnore
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit

class GetMessageSummaries : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, List<MessageFromUserSummary>>(request, context) { _, identity ->
            val messageTable = Singletons.dynamoDB.messageTable()
            val startPeriod = Instant.now().minus(14, ChronoUnit.DAYS)

            messageTable
                .query(
                    // TODO: filter by file upload finished, plan is to use s3 event trigger which
                    // TODO: kicks off lambda to set upload finished, and send notification
                    "to", identity.userId,
                    RangeKeyCondition("messageCreatedAt").between(startPeriod.format(), Instant.now().format()),
                    QueryFilter("signedS3Url").eq(null),
                    QueryFilter("uploadFinished").eq(true)
                )
                .map { it.toMessage() }
                .groupBy { it.from }
                .map { (from, messagesFromOneUser) ->
                    MessageFromUserSummary(
                        from = from,
                        count = messagesFromOneUser.count(),
                        mostRecentCreatedAt = messagesFromOneUser.maxOfOrNull { it.messageCreatedAt },
                        next = messagesFromOneUser.minByOrNull { it.messageCreatedAt }!!
                    )
                }
                .sortedByDescending { it.mostRecentCreatedAt }
        }
    }
}

data class MessageFromUserSummary(
    val from: String,
    val count: Int,
    val mostRecentCreatedAt: Instant?,
    val next: Message?
)

data class Message(
    val to: String,
    val from: String,
    val messageCreatedAt: Instant,
    val text: String?,
    val fileKey: String,
    val mediaType: String,
    val uploadFinished: Boolean,
    val signedS3Url: URL?,
    val signedS3UrlExpiration: Instant?,
    val ephemeralPublicKey: String,
    val signedSendingPublicKey: String,
    @JsonIgnore
    val expiresAt: Instant // used for amazon dynamoDB automatic item expiration
)

fun Item.toMessage(): Message {
    return Message(
        to = this.getString("to"),
        from = this.getString("from"),
        messageCreatedAt = Instant.parse(this.getString("messageCreatedAt")),
        text = this.getString("text"),
        fileKey = this.getString("fileKey"),
        mediaType = this.getString("mediaType"),
        uploadFinished = this.getBoolean("uploadFinished"),
        signedS3Url = this.getString("signedS3Url")?.let { URL(it) },
        signedS3UrlExpiration = this.getString("signedS3UrlExpiration")?.let { Instant.parse(it) },
        ephemeralPublicKey = this.getString("ephemeralPublicKey"),
        signedSendingPublicKey = this.getString("signedSendingPublicKey"),
        expiresAt = Instant.ofEpochSecond(this.getLong("expiresAt"))
    )
}

fun Message.toItem(): Item {
    val map = mapOf(
        "to" to to,
        "from" to from,
        "messageCreatedAt" to messageCreatedAt.format(),
        "text" to text,
        "fileKey" to fileKey,
        "mediaType" to mediaType,
        "uploadFinished" to uploadFinished,
        "signedS3Url" to signedS3Url?.toString(),
        "signedS3UrlExpiration" to signedS3UrlExpiration?.format(),
        "ephemeralPublicKey" to ephemeralPublicKey,
        "signedSendingPublicKey" to signedSendingPublicKey,
        "expiresAt" to expiresAt.epochSecond
    )

    return Item.fromMap(map)
}