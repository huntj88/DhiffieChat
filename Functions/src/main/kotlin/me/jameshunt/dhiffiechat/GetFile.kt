package me.jameshunt.dhiffiechat

import com.amazonaws.HttpMethod
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


class GetFile : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<Unit, GetFileQueryParams, GetFileResponse>(request, context) { _, params, identity ->
            // TODO: check if has access to file
            context.logger.log("params: $params")

            val signedUrlRequest = GeneratePresignedUrlRequest("encrypted-file-bucket-z00001", params.fileKey)
                .withMethod(HttpMethod.GET)
                .withExpiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))

            val signedUrl: URL = Singletons.s3.generatePresignedUrl(signedUrlRequest)

            GetFileResponse(s3Url = signedUrl)
        }
    }
}

data class GetFileQueryParams(
    val fileKey: String
)

data class GetFileResponse(
    val s3Url: URL
)
