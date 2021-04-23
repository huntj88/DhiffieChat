package me.jameshunt.privatechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.ObjectMetadata
import com.fasterxml.jackson.annotation.JsonAlias
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.*

class SendFile : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<ByteArray, UserUserQueryParams, Unit>(request, context) { body, params, _ ->
            context.logger.log("file length in bytes: ${body.size}")
            context.logger.log("user user iv: ${params.userUserIv}")

            // TODO: check if friends

            val s3 = AmazonS3ClientBuilder.standard().build()
            s3.putObject("encrypted-file-bucket-z00001", body.toS3Key(), ByteArrayInputStream(body), ObjectMetadata())
        }
    }
}

data class UserUserQueryParams(
    @JsonAlias("HashedIdentity", "hashedIdentity")
    val hashedIdentity: String,
    @JsonAlias("UserUserIv", "userUserIv")
    val userUserIv: String
)

fun ByteArray.toS3Key(): String = MessageDigest
    .getInstance("SHA-256")
    .digest(this)
    .let { Base64.getEncoder().encodeToString(it) }
    .replace("/", "_") // don't make folders