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

        val s3Key = getS3Key(request)
        val userId = Singletons.s3
            .getObject(encryptedFileBucket, s3Key)
            .objectMetadata
            .userMetadata["recipient-id"]

        val startRange = Instant.now().minus(30, ChronoUnit.MINUTES).format()
        val endRange = Instant.now().format()

        val messages = Singletons.dynamoDB.messageTable().query(
            "to", userId,
            RangeKeyCondition("messageCreatedAt").between(startRange, endRange),
            QueryFilter("signedS3Url").eq(null)
        )

        val message = messages
            .map { it.toMessage() }
            .filter { it.fileKey == s3Key }
            .singleItem()

        context.logger.log(Singletons.objectMapper.writeValueAsString(message))
        message.setUploadFinished()
        message.sendNotification()
    }

    private fun getS3Key(request: Map<String, Any?>): String {
        return request["Records"]
            .let { it as List<Map<String, Any?>> }
            .singleItem()["s3"]
            .let { it as Map<String, Any?> }["object"]
            .let { it as Map<String, Any?> }["key"]
            .let { it as String }
            .let { URLDecoder.decode(it, Charsets.UTF_8.toString()) }
    }

    private fun Message.setUploadFinished() {
        Singletons.dynamoDB.messageTable().updateItem(
            "to", this.to,
            "messageCreatedAt", this.messageCreatedAt.format(),
            AttributeUpdate("uploadFinished").put(true)
        )
    }

    private fun Message.sendNotification() {
        val fcmToken = Singletons.dynamoDB
            .getTable("User")
            .getItem("userId", this.to)["fcmToken"] as? String ?: throw Exception("fcmToken is null")
        Singletons.firebase.sendMessage(
            token = fcmToken,
            title = "New Message",
            body = "New Message in DhiffieChat"
        )
    }
}

fun <T> List<T>.singleItem(): T = this
    .also { if (it.size != 1) throw IllegalStateException("Expected list to have a single item") }
    .first()