package me.jameshunt.privatechat

import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.s3.model.ObjectMetadata
import com.fasterxml.jackson.annotation.JsonAlias
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
                    "ChatId" to chatId(identity.hashedIdentity, params.hashedIdentity),
                    "MessageCreatedAt" to DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                    "From" to identity.hashedIdentity,
                    "To" to params.hashedIdentity,
                    "FileKey" to key,
                    "Iv" to params.userUserIv,
                )
            )

            Singletons.dynamoDB.getTable("Chat").putItem(message)
        }
    }
}

data class UserUserQueryParams(
    @JsonAlias("HashedIdentity", "hashedIdentity")
    val hashedIdentity: String,
    @JsonAlias("UserUserIv", "userUserIv")
    val userUserIv: String
)
