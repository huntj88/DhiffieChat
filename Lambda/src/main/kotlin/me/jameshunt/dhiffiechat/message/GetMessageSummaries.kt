package me.jameshunt.dhiffiechat.message

import com.amazonaws.services.dynamodbv2.document.QueryFilter
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import me.jameshunt.dhiffiechat.*
import java.time.Instant
import java.time.temporal.ChronoUnit

class GetMessageSummaries : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, List<MessageFromUserSummary>>(request, context) { _, identity ->
            val messageTable = Singletons.dynamoDB.messageTable()
            val startPeriod = Instant.now().minus(14, ChronoUnit.DAYS)

            messageTable
                .query(
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
