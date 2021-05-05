package me.jameshunt.dhiffiechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.s3.model.ObjectMetadata
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.format.DateTimeFormatter

class SendFile : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<ByteArray, UserUserQueryParams, Unit>(request, context) { body, params, identity ->
            context.logger.log("file length in bytes: ${body.size}")
            context.logger.log("user user iv: ${params.userUserIv}")

            // TODO: check if friends

            val key = body.toS3Key()
            Singletons.s3.putObject(
                "encrypted-file-bucket-z00001",
                key,
                ByteArrayInputStream(body),
                ObjectMetadata()
            )

            val message = Message(
                to = params.userId,
                from = identity.userId,
                messageCreatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                text = null, // TODO
                fileKey = key,
                iv = params.userUserIv,
                authedUrl = null
            )

            Singletons.dynamoDB.getTable("Message").putItem(message.toItem())
        }
    }
}

data class UserUserQueryParams(
    val userId: String,
    val userUserIv: String
)
