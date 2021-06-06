package me.jameshunt.dhiffiechat

import com.amazonaws.HttpMethod
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.s3.Headers
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


class SendMessage : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<SendMessageRequest, SendMessageResponse>(request, context) { body, identity ->
            // TODO: check if friends
            val messageCreatedAt = Instant.now()

            val signedUrlRequest = GeneratePresignedUrlRequest(encryptedFileBucket, body.s3Key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(Date.from(messageCreatedAt.plus(5, ChronoUnit.MINUTES))).apply {
                    addRequestParameter(Headers.S3_USER_METADATA_PREFIX + "recipient-id", body.recipientUserId)
                }

            val signedUrl: URL = Singletons.s3.generatePresignedUrl(signedUrlRequest)

            val message = Message(
                to = body.recipientUserId,
                from = identity.userId,
                messageCreatedAt = messageCreatedAt,
                text = body.text,
                fileKey = body.s3Key,
                mediaType = body.mediaType,
                uploadFinished = false,
                signedS3Url = null,
                signedS3UrlExpiration = null,
                expiresAt = messageCreatedAt.plus(14, ChronoUnit.DAYS)
            )

            Singletons.dynamoDB.getTable("Message").putItem(message.toItem())

            SendMessageResponse(signedUrl)
        }
    }
}

data class SendMessageRequest(
    val recipientUserId: String,
    val text: String?,
    val s3Key: String,
    val mediaType: String
)

data class SendMessageResponse(val uploadUrl: URL)
