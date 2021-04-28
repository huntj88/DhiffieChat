package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.document.Item
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

            val message = Item.fromMap(
                mapOf(
                    "chatId" to chatId(identity.userId, params.userId),
                    "messageCreatedAt" to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                    "from" to identity.userId,
                    "to" to params.userId,
                    "fileKey" to key,
                    "iv" to params.userUserIv,
                )
            )

            Singletons.dynamoDB.getTable("Chat").putItem(message)
        }
    }
}

data class UserUserQueryParams(
    val userId: String,
    val userUserIv: String
)
