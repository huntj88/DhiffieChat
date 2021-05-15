package me.jameshunt.dhiffiechat

import com.amazonaws.services.dynamodbv2.document.AttributeUpdate
import com.amazonaws.services.dynamodbv2.document.QueryFilter
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import java.net.URLDecoder
import java.time.Instant
import java.time.temporal.ChronoUnit

class HandleS3Upload : RequestHandler<Map<String, Any?>, Unit> {
    /**
     * Is not user accessible. Triggers from an s3 upload event
     */
    override fun handleRequest(request: Map<String, Any?>, context: Context) {
        context.logger.log(Singletons.objectMapper.writeValueAsString(request))

        val s3Key = request["Records"]
            .let { it as List<Map<String, Any?>> }
            .also { if (it.size != 1) TODO() }
            .first()["s3"]
            .let { it as Map<String, Any?> }["object"]
            .let { it as Map<String, Any?> }["key"]
            .let { it as String }
            .let { URLDecoder.decode(it, Charsets.UTF_8.toString()) }

        val file = Singletons.s3.getObject(Singletons.encryptedFileBucket, s3Key)
        val userId = file.objectMetadata.userMetadata["recipient-id"]

        val startRange = Instant.now().minus(30, ChronoUnit.MINUTES).format()
        val endRange = Instant.now().format()

        val messages = Singletons.dynamoDB.getTable("Message").query(
            "to", userId,
            RangeKeyCondition("messageCreatedAt").between(startRange, endRange),
            QueryFilter("signedS3Url").eq(null)
        )

        val message = messages
            .map { it.toMessage() }
            .filter { it.fileKey == s3Key }
            .also { if (it.size != 1) TODO() }
            .first()

        context.logger.log(Singletons.objectMapper.writeValueAsString(message))
        message.setUploadFinished()
        message.sendNotification()
    }

    private fun Message.setUploadFinished() {
        Singletons.dynamoDB.getTable("Message").updateItem(
            "to", this.to,
            "messageCreatedAt", this.messageCreatedAt.format(),
            AttributeUpdate("uploadFinished").put(true)
        )
    }

    private fun Message.sendNotification() {
        // TODO
    }
}