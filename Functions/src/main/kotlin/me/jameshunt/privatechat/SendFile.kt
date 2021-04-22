package me.jameshunt.privatechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.fasterxml.jackson.annotation.JsonAlias

class SendFile : RequestHandler<Map<String, Any?>, GatewayResponse> {
    override fun handleRequest(request: Map<String, Any?>, context: Context): GatewayResponse {
        return awsTransformAuthed<ByteArray, UserUserQueryParams, Unit>(request, context) { body, params, _ ->
            context.logger.log("file length in bytes: ${body.size}")
            context.logger.log("user user iv: ${params.userUserIv}")
        }
    }
}

data class UserUserQueryParams(
    @JsonAlias("UserUserIv", "userUserIv")
    val userUserIv: String
)