package me.jameshunt.dhiffiechat

import com.amazonaws.HttpMethod
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import me.jameshunt.dhiffiechat.crypto.toS3Key
import java.io.ByteArrayInputStream
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalUnit
import java.util.*


class SendFile : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, UserUserQueryParams, SendFileResponse>(request, context) { _, params, identity ->
            // TODO: check if friends

            val generatePreSignedUrlRequest = GeneratePresignedUrlRequest("encrypted-file-bucket-z00001", params.s3Key)
                .withMethod(HttpMethod.PUT)
                .withExpiration(Date.from(Instant.now().plus(4, ChronoUnit.MINUTES)))

            val url: URL = Singletons.s3.generatePresignedUrl(generatePreSignedUrlRequest)

            val message = Message(
                to = params.userId,
                from = identity.userId,
                messageCreatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                text = null, // TODO
                fileKey = params.s3Key,
                iv = params.userUserIv,
                authedUrl = null
            )

            Singletons.dynamoDB.getTable("Message").putItem(message.toItem())

            SendFileResponse(url.toString())
        }
    }
}

data class UserUserQueryParams(
    val userId: String,
    val userUserIv: String,
    val s3Key: String
)

data class SendFileResponse(val uploadUrl: String)
