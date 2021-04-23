package me.jameshunt.privatechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.s3.model.S3ObjectInputStream
import com.fasterxml.jackson.annotation.JsonAlias
import java.io.ByteArrayOutputStream
import java.util.*


class GetFile : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, GetFileQueryParams, String>(request, context) { body, params, identity ->
            // TODO: check if has access to file
            context.logger.log("params: $params")

            val blah = Singletons.s3.getObject("encrypted-file-bucket-z00001", params.fileKey)
            val s3is: S3ObjectInputStream = blah.objectContent
            val output = ByteArrayOutputStream(blah.objectMetadata.contentLength.toInt())
            val readBuf = ByteArray(1024)
            var readLen = 0
            while (s3is.read(readBuf).also { readLen = it } > 0) {
                output.write(readBuf, 0, readLen)
            }
            s3is.close()
            output.close()

            return GatewayResponse(
                isBase64Encoded = true,
                body = Base64.getEncoder().encodeToString(output.toByteArray()),
                headers = mapOf("Content-Type" to "application/octet-stream")
            )
        }
    }
}

data class GetFileQueryParams(
    @JsonAlias("FileKey", "fileKey")
    val fileKey: String
)
