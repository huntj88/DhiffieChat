package me.jameshunt.dhiffiechat

import com.amazonaws.HttpMethod
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


class SendFile : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, UserUserQueryParams, SendFileResponse>(request, context) { _, params, identity ->
            // TODO: check if friends

            val signedUrlRequest = GeneratePresignedUrlRequest("encrypted-file-bucket-z00001", params.s3Key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))

            val signedUrl: URL = Singletons.s3.generatePresignedUrl(signedUrlRequest)

            val message = Message(
                to = params.userId,
                from = identity.userId,
                messageCreatedAt = Instant.now(),
                text = null, // TODO
                fileKey = params.s3Key,
                iv = params.userUserIv,
                mediaType = params.mediaType,
                signedS3Url = null,
                signedS3UrlExpiration = null
            )

            Singletons.dynamoDB.getTable("Message").putItem(message.toItem())

            SendFileResponse(signedUrl)
        }
    }
}

data class UserUserQueryParams(
    val userId: String,
    val s3Key: String,
    val userUserIv: String,
    val mediaType: String
)

data class SendFileResponse(val uploadUrl: URL)
