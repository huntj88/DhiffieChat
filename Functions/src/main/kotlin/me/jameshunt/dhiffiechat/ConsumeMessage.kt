package me.jameshunt.dhiffiechat

import com.amazonaws.HttpMethod
import com.amazonaws.services.dynamodbv2.document.AttributeUpdate
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

class ConsumeMessage : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<ConsumeMessageRequest, ConsumeMessageResponse>(request, context) { body, identity ->
            context.logger.log("body: $body")
            val messageTable = Singletons.dynamoDB.getTable("Message")

            val message = messageTable.getItem(
                "to", identity.userId,
                "messageCreatedAt", body.timeSent.format()
            ).toMessage()

            if (message.signedS3UrlExpiration?.isBefore(Instant.now()) == true) {
                return GatewayResponse(statusCode = 410, body = "")
            }

            val signedUrl = message.signedS3Url ?: generateAndSaveS3Url(message)

            ConsumeMessageResponse(s3Url = signedUrl)
        }
    }

    private fun generateAndSaveS3Url(message: Message): URL {
        val messageTable = Singletons.dynamoDB.getTable("Message")
        val expiration = Instant.now().plus(5, ChronoUnit.MINUTES)

        return generateS3Url(message.fileKey, expiration).also { newUrl ->
            messageTable.updateItem(
                "to", message.to,
                "messageCreatedAt", message.messageCreatedAt.format(),
                AttributeUpdate("signedS3Url").put(newUrl.toString()),
                AttributeUpdate("signedS3UrlExpiration").put(expiration.format())
            )
        }
    }

    private fun generateS3Url(fileKey: String, expiration: Instant): URL {
        val signedUrlRequest = GeneratePresignedUrlRequest(encryptedFileBucket, fileKey)
            .withMethod(HttpMethod.GET)
            .withExpiration(Date.from(expiration))

        return Singletons.s3.generatePresignedUrl(signedUrlRequest)
    }
}

data class ConsumeMessageRequest(
    val fileKey: String,
    val timeSent: Instant
)

data class ConsumeMessageResponse(
    val s3Url: URL
)
